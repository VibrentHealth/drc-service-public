package com.vibrent.drc.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DateTimeUtilTest {

    @Test
    void getTimestampFromStringDate() {
        Long timestampFromStringDate = DateTimeUtil.getTimestampFromStringDate("1987-10-24");
        assertEquals(562032000000L, timestampFromStringDate);
    }
}