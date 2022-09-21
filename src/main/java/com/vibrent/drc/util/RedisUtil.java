package com.vibrent.drc.util;

import com.vibrent.drc.exception.BusinessValidationException;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Redis Utility Class
 *
 * This Utility Class is used to help utilize redis commands such as getting values or putting values using key values
 * Reddis saves data structure in key value pairing
 * RedissonClient commands require serializable objects in order to store into Redis
 *
 */
@Component
public class RedisUtil {


    private static final Logger LOGGER = LoggerFactory.getLogger(RedisUtil.class);

    private final RedissonClient redisClient;

    @Inject
    public RedisUtil(@Nullable RedissonClient redisClient) {
        this.redisClient = redisClient;
    }


    /**
     * Helper to take a map of data, create the appropriate Redis key and add the map in Redis
     *
     * @param mapName - name of the redis map that is being replace
     * @param mapData - map data that is being stored into reddis
     *
     * @throws BusinessValidationException - if redisClient commands cannot be performed,
     *              ex. if Map contains objects that do not implement serializable,
     *              Put command for Redis requires data to be serializable
     */
    public void replaceMap(String mapName, Map mapData) throws BusinessValidationException {

        String keyPrefix = UUID.randomUUID().toString();
        Set<String> keys = new HashSet<>();

        String tempKey = keyPrefix + mapName;
        keys.add(tempKey);

        // Create a map in redis
        if (redisClient == null) {
            LOGGER.error("Unable add existing map - no Redis client available!");
            throw new RedissonShutdownException("redis-client-unavailable");
        }

        try {
            redisClient.getMap(tempKey).putAll(mapData);

            // rename temporary keys to replace redd
            updateRedisKeys(keys, keyPrefix.length());
        } catch ( Exception e ){
            LOGGER.error("Unable to execute Redis Command", e);
            throw new BusinessValidationException("Unable to Execute Redis Command");
        }
    }

    /**
     * Utility method to swap geo keys with new values
     *
     * @param keys - list of old keys to rename.
     * @param prefixLengthToRemove - number of character to remove from the old key
     *
     * note: old keys = ['abc-ddd', 'abc-eee', 'ddd-fff'], prefixToRemoveFromKey = 4,
     *       new keys = ['ddd', 'eee', 'fff']
     */
    private void updateRedisKeys(Set<String> keys, int prefixLengthToRemove) {

        for (String oldKey : keys) {
            if (oldKey.length() <= prefixLengthToRemove) {
                LOGGER.error("unable to trim the key : {} because it's too short, prefixLengthToRemove: {}", oldKey, prefixLengthToRemove);
                continue;
            }

            String oldKeySubstring = oldKey.substring(prefixLengthToRemove);
            LOGGER.debug("Updating redis key '{}'", oldKeySubstring );
            redisClient.getSet(oldKey).rename( oldKeySubstring );
        }
    }

    /**
     * Utillity method for get Redis Map by name
     * @param mapName name of the Map
     * @return NULL if mapName is empty or null otherwise returns RMap
     */
    public Map getMap(String mapName) {

        if (StringUtils.isEmpty(mapName)) {
            return null;
        }

        return this.redisClient.getMap(mapName);
    }


    public RMapCache getMapCache(String mapName) {
        return this.redisClient.getMapCache(mapName);
    }

}
