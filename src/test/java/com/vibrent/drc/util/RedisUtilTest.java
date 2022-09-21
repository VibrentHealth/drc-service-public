package com.vibrent.drc.util;


import ch.qos.logback.classic.Level;
import com.vibrent.acadia.web.rest.bo.DRCOrganizationBO;
import com.vibrent.drc.exception.BusinessValidationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RedisUtilTest {

    String testKeyName = "{test}test:testlookup";

    @Mock
    private RedissonClient redisClient;

    private RedisUtil redisUtil;

    @Mock
    private RMap<Object, Object> redisMap;

    @Mock
    private RSet<Object> redisSet;

    @Before
    public void setUp() {
        redisUtil = spy(new RedisUtil(redisClient));
    }

    @Test (expected = RedissonShutdownException.class)
    public void testRedisDownExceptionThrown() throws Exception {
        ReflectionTestUtils.setField(redisUtil, "redisClient", null);
        Map<String, DRCOrganizationBO> testRedisMap = new HashMap<>();

        // turn off logging on expected error class
        Level oldLevel = TestLoggingUtil.turnOffLogging(RedisUtil.class.getCanonicalName());
        redisUtil.replaceMap(testKeyName, testRedisMap);
        TestLoggingUtil.changeLoggingLevel(RedisUtil.class.getCanonicalName(), oldLevel);
    }

    @Test (expected = BusinessValidationException.class)
    public void testUnprocessableCommandExceptionThrown() throws Exception {
        Map<String, Serializable> testRedisMap = new HashMap<>();
        when(redisClient.getMap(any(String.class))).thenThrow(RedisException.class);

        // turn off logging on expected error class
        Level oldLevel = TestLoggingUtil.turnOffLogging(RedisUtil.class.getCanonicalName());
        redisUtil.replaceMap(testKeyName, testRedisMap);
        TestLoggingUtil.changeLoggingLevel(RedisUtil.class.getCanonicalName(), oldLevel);
    }

    @Test
    public void testReplaceMap() {
        Map<String, Serializable> testRedisMap = Map.of("Key1","Val1");

        when(redisClient.getMap(anyString())).thenReturn(redisMap);
        when(redisClient.getSet(anyString())).thenReturn(redisSet);

        redisUtil.replaceMap(testKeyName, testRedisMap);
        verify(redisMap, times(1)).putAll(anyMap());
        verify(redisSet, times(1)).rename(anyString());
    }

    @Test
    public void getMapTest() {
        when(redisClient.getMap(any(String.class))).thenReturn(redisMap);

        Map<String, ?> newMap = redisUtil.getMap("mapName");
        assertEquals(0, newMap.size());
    }
    @Test
    public void getMapEmptyNameTest() {
        Map<String, Object> newMap =  redisUtil.getMap("");
        assertNull(newMap);

        newMap =  redisUtil.getMap(null);
        assertNull(newMap);
    }

}
