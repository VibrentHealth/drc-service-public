package com.vibrent.drc.configuration;

import com.vibrent.drc.constants.DrcConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfigration {

    @Value("${vibrent.drc-service.caffeineSpec}")
    String caffeineSpec;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DrcConstant.VIBRENTID_CACHE,
                DrcConstant.SALIVERY_BIOBANK_ADDRESS_CACHE, DrcConstant.SALIVERY_ORDER_DEVICE_CACHE);
        cacheManager.setCacheSpecification(caffeineSpec);
        return cacheManager;
    }
}
