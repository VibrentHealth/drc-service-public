package com.vibrent.drc.configuration;

import com.vibrent.drc.constants.DrcConstant;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.URL;

@Configuration
public class CacheConfigration {


    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDIS_SECURE_PROTOCOL_PREFIX = "rediss://";
    private static final String CLASSPATH_PREFIX = "classpath:";

    @Value("${redis.properties.redisson-ssl-enaled:false}")
    private boolean isSecureProtocolEnabled;

    @Value("${redis.properties.redisson-config}")
    private String singleStackConfigFile;

    @Value("${redis.properties.redisson-replicated-config}")
    private String replicatedConfigFile;

    @Value("${redis.properties.redisson-cluster-config}")
    private String clusterConfigFile;

    @Value("${redis.properties.redisson-address}")
    private String redissonAddress;

    @Value("${redis.properties.redisson-is-clustered}")
    private boolean isRedisClustered;

    @Value("${redis.properties.redisson-cluster-type}")
    private String redisClusterType;

    @Value("${vibrent.drc-service.caffeineSpec}")
    String caffeineSpec;


    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() throws IOException {
        String path;
        String prefixedNodeAddresses;
        if (isSecureProtocolEnabled) {
            prefixedNodeAddresses = REDIS_SECURE_PROTOCOL_PREFIX + redissonAddress;
            prefixedNodeAddresses = prefixedNodeAddresses.replace(",", "," + REDIS_SECURE_PROTOCOL_PREFIX);
        } else {
            prefixedNodeAddresses = REDIS_PROTOCOL_PREFIX + redissonAddress.replace("'", "");
            prefixedNodeAddresses = prefixedNodeAddresses.replace(",", "," + REDIS_PROTOCOL_PREFIX);
        }
        String[] redisNodes = prefixedNodeAddresses.split(",");
        if (isRedisClustered) {
            if ("ha".equals(redisClusterType)) {
                path = clusterConfigFile.substring(CLASSPATH_PREFIX.length());
            } else {
                path = replicatedConfigFile.substring(CLASSPATH_PREFIX.length());
            }
        } else {
            path = singleStackConfigFile.substring(CLASSPATH_PREFIX.length());
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource(path);

        Config config = Config.fromYAML(url);

        if (isRedisClustered) {
            switch (redisClusterType.toLowerCase()) {
                // replicated would be used for a base elasticache setup, ha would be used when cluster mode is enabled
                case "replicated":
                    config.useReplicatedServers().addNodeAddress(redisNodes);
                    break;
                case "ha":
                    config.useClusterServers().addNodeAddress(redisNodes);
                    break;
                default:
                    break;
            }
        } else {
            config.useSingleServer().setAddress(prefixedNodeAddresses);
        }
        return Redisson.create(config);
    }


    @Bean(name = "redissonSpringCacheManager")
    public CacheManager redissonSpringCacheManager(RedissonClient redisson) {
        return new RedissonSpringCacheManager(redisson);
    }

    @Primary
    @Bean(name = "caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DrcConstant.VIBRENTID_CACHE,
                DrcConstant.SALIVERY_BIOBANK_ADDRESS_CACHE, DrcConstant.SALIVERY_ORDER_DEVICE_CACHE);
        cacheManager.setCacheSpecification(caffeineSpec);
        return cacheManager;
    }
}
