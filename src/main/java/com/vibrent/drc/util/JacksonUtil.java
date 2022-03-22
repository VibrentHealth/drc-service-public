package com.vibrent.drc.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public class JacksonUtil {
    protected static final ObjectMapper mapper = new ObjectMapper();
    private static boolean initialized = false;

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // turbo boost
        mapper.registerModule(new AfterburnerModule());
    }

    private JacksonUtil() {
    }

    public static ObjectMapper getMapper() {
        initializeIfNeeded();
        return mapper;
    }

    private static void initializeIfNeeded() {
        if (!initialized) {
            synchronized (mapper) {
                if (!initialized) {
                    SimpleModule simpleModule = new SimpleModule();
                    mapper.registerModule(simpleModule);
                    initialized = true;
                }
            }
        }
    }


}

