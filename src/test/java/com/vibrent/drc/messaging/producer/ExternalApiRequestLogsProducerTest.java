package com.vibrent.drc.messaging.producer;


import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalServiceType;
import com.vibrent.drc.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RMapCache;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExternalApiRequestLogsProducerTest {
    private static final String TOPIC_NAME = "event.vrp.externalApiRequestLogs";

    private ExternalApiRequestLogsProducer externalLogProducer;

    private ExternalApiRequestLog externalApiRequestLog;

    @Mock
    private KafkaTemplate<String, ExternalApiRequestLog> kafkaTemplate;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private ListenableFuture<SendResult<String, ExternalApiRequestLog>> future;
    @Mock
    private RMapCache rMapCache;


    @BeforeEach
    public void setup() {
        externalApiRequestLog = generateMessage();
    }

    @Test
    public void sendPayloadLessThanMax() {
        externalLogProducer =  new ExternalApiRequestLogsProducer(true, 6400, kafkaTemplate, TOPIC_NAME, redisUtil);
        when(kafkaTemplate.send(ArgumentMatchers.<Message<ExternalApiRequestLog>> any())).thenReturn(future);
        externalLogProducer.send(externalApiRequestLog);
        verify(redisUtil, times(0)).getMapCache(any());
    }

    @Test
    public void sendPayloadExceedMax() {
        when(redisUtil.getMapCache(any())).thenReturn(rMapCache);
        when(rMapCache.put(any(), any())).thenReturn(null);

        externalLogProducer =  new ExternalApiRequestLogsProducer(true, 10, kafkaTemplate, TOPIC_NAME, redisUtil);
        when(kafkaTemplate.send(ArgumentMatchers.<Message<ExternalApiRequestLog>> any())).thenReturn(future);
        externalLogProducer.send(externalApiRequestLog);
        verify(redisUtil, times(1)).getMapCache(any());
    }

    @Test
    public void sendResponseBodyExceedMax() {
        when(redisUtil.getMapCache(any())).thenReturn(rMapCache);
        when(rMapCache.put(any(), any())).thenReturn(null);

        externalLogProducer =  new ExternalApiRequestLogsProducer(true, 10, kafkaTemplate, TOPIC_NAME, redisUtil);
        externalApiRequestLog.setRequestBody("1");
        when(kafkaTemplate.send(ArgumentMatchers.<Message<ExternalApiRequestLog>> any())).thenReturn(future);
        externalLogProducer.send(externalApiRequestLog);
        verify(redisUtil, times(1)).getMapCache(any());
    }

    @Test
    public void sendRequestBodyExceedMax() {
        when(redisUtil.getMapCache(any())).thenReturn(rMapCache);
        when(rMapCache.put(any(), any())).thenReturn(null);
        
        externalLogProducer =  new ExternalApiRequestLogsProducer(true, 10, kafkaTemplate, TOPIC_NAME, redisUtil);
        externalApiRequestLog.setResponseBody("1");
        when(kafkaTemplate.send(ArgumentMatchers.<Message<ExternalApiRequestLog>> any())).thenReturn(future);
        externalLogProducer.send(externalApiRequestLog);
        verify(redisUtil, times(1)).getMapCache(any());
    }

    private ExternalApiRequestLog generateMessage() {
        ExternalApiRequestLog externalApiRequestLog = new ExternalApiRequestLog();
        externalApiRequestLog.setResponseBody("responseBody");
        externalApiRequestLog.setExternalId("12345");
        externalApiRequestLog.setRequestBody("requestBody");
        externalApiRequestLog.setEventType(ExternalEventType.DRC_SUPPLY_STATUS);
        externalApiRequestLog.setService(ExternalServiceType.AFTER_SHIP);

        return externalApiRequestLog;
    }
}
