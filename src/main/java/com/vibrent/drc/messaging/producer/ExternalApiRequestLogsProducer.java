package com.vibrent.drc.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.acadia.web.rest.dto.ExternalLogCacheDto;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Service
@Slf4j
public class ExternalApiRequestLogsProducer implements MessageProducer<ExternalApiRequestLog> {
    public static final String EXTERNAL_LOG_CACHE_NAME = "externalLogCache";
    private boolean kafkaEnabled;

    private final KafkaTemplate<String, ExternalApiRequestLog> kafkaTemplate;

    private final String topicName;

    private  final Integer externalLogBodyMaxSize;
    private final RedisUtil redisUtil;
    public ExternalApiRequestLogsProducer(@Value("${spring.kafka.enabled}") boolean kafkaEnabled,
                                          @Value("${externalLog.maxBodySize}") Integer externalLogBodyMaxSize,
                                          KafkaTemplate<String, ExternalApiRequestLog> kafkaTemplate,
                                          @Value("${spring.kafka.topics.externalApiRequestLogs}") String topicName,
                                          RedisUtil redisUtil) {
        this.kafkaEnabled = kafkaEnabled;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
        this.externalLogBodyMaxSize = externalLogBodyMaxSize;
        this.redisUtil = redisUtil;
    }

    @Override
    public void setKafkaEnabled(boolean isEnabled) {
        this.kafkaEnabled = isEnabled;
    }

    @Override
    @Async
    public void send(ExternalApiRequestLog msg) {
        if (!kafkaEnabled) {
            return;
        }
        String key = checkResponseBodySizeReturnKey(msg);

        //Send empty payload and the cache key in header when payload is oversize
        Message<ExternalApiRequestLog> message = MessageBuilder
                .withPayload(msg)
                .setHeader(KafkaHeaders.TOPIC, topicName)
                .setHeader("Ext-Log-Key-Header", key)
                .setHeader(KafkaHeaders.MESSAGE_KEY, String.valueOf(msg.getInternalId()))
                .build();

        kafkaTemplate.send(message)
                .addCallback(new ListenableFutureCallback<SendResult<String, ExternalApiRequestLog>>() {

                    @Override
                    public void onSuccess(SendResult<String, ExternalApiRequestLog> result) {
                        // Not doing anything on success
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        log.error("PUBLISHING TO EXTERNAL_LOG_TOPIC FAILED", ex);
                    }
                });
    }

    /**
     * Check the response/request body size, if one of them is bigger than configured max size then save the payload into redis cache and return a key. Otherwise return null.
     *
     * @param msg ExternalApiRequestLog
     * @return key
     */
    private String checkResponseBodySizeReturnKey(ExternalApiRequestLog msg) {
        String responseBody = msg.getResponseBody();
        String requestBody = msg.getRequestBody();
        int responseLength = 0;
        int requestLength = 0;
        ExternalLogCacheDto bodyObj = new ExternalLogCacheDto();
        if (responseBody != null) {
            responseLength = responseBody.getBytes(StandardCharsets.UTF_8).length;
        }
        if (requestBody != null) {
            requestLength = requestBody.getBytes(StandardCharsets.UTF_8).length;
        }

        if (responseLength > externalLogBodyMaxSize && requestLength > externalLogBodyMaxSize) {
            log.debug("Both requestBody and responseBody exceeding max response/request responseBody size {}", externalLogBodyMaxSize);
            bodyObj.setResponseBody(msg.getResponseBody());
            bodyObj.setRequestBody(msg.getRequestBody());
            msg.setRequestBody(null);
            msg.setResponseBody(null);
            return saveToCache(bodyObj);
        }
        if (responseLength > externalLogBodyMaxSize) {
            log.debug("responseBody is exceeding max response/request responseBody size {}", externalLogBodyMaxSize);
            bodyObj.setResponseBody(msg.getResponseBody());
            msg.setResponseBody(null);
            return saveToCache(bodyObj);
        }
        if (requestLength > externalLogBodyMaxSize) {
            log.debug("requestBody is exceeding max response/request responseBody size {}", externalLogBodyMaxSize);
            bodyObj.setRequestBody(msg.getRequestBody());
            msg.setRequestBody(null);
            return saveToCache(bodyObj);
        }
        return null;
    }

    /**
     * Save the body in redis cache
     *
     * @param bodyObj ExternalLogCache
     * @return key
     */
    private String saveToCache(ExternalLogCacheDto bodyObj) {
        String msgString;
        try {
            msgString = JacksonUtil.getMapper().writeValueAsString(bodyObj);
        } catch (JsonProcessingException e) {
            log.warn("Unable to parse ExternalApiRequestLog into String");
            return null;
        }
        String keyStr = UUID.randomUUID().toString();

        redisUtil.getMapCache(EXTERNAL_LOG_CACHE_NAME).put(keyStr, msgString);

        log.debug("Saving body object into redis cache {} with key {}", msgString, keyStr);
        return keyStr;
    }
}
