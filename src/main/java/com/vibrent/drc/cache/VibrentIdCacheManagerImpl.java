package com.vibrent.drc.cache;

import com.vibrent.drc.constants.DrcConstant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class VibrentIdCacheManagerImpl implements  VibrentIdCacheManager{

    private CacheManager cacheManager;

    public VibrentIdCacheManagerImpl(@Qualifier("caffeineCacheManager") CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void addVibrentIdToCache(String externalId, Long vibrentId) {
        if (externalId != null && vibrentId != null) {
            cacheManager.getCache(DrcConstant.VIBRENTID_CACHE).put(externalId, vibrentId);
        }
    }


}
