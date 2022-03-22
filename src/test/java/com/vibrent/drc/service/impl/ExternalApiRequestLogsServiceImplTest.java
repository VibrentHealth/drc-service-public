package com.vibrent.drc.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.messaging.producer.DrcExternalEventProducer;
import com.vibrent.drc.messaging.producer.ExternalApiRequestLogsProducer;
import com.vibrent.drc.scheduling.DRCParticipantGenomicsStatusFetchJob;
import com.vibrent.vxp.drc.dto.DrcNotificationRequestDTO;
import com.vibrent.vxp.drc.dto.EventTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ExternalApiRequestLogsServiceImplTest {

    @Mock
    private ExternalApiRequestLogsProducer externalApiRequestLogsProducer;

    private ExternalApiRequestLogsServiceImpl externalApiRequestLogsServiceImpl;


    @BeforeEach
    void setUp() {
        externalApiRequestLogsServiceImpl = new ExternalApiRequestLogsServiceImpl(externalApiRequestLogsProducer);

    }

    @DisplayName("When external event is sent," +
            "And encountered Kafka exception " +
            "Then verify warning gets logged.")
    @Test
    void send() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExternalApiRequestLogsServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        doThrow(KafkaException.class).when(externalApiRequestLogsProducer).send(any(ExternalApiRequestLog.class));
        listAppender.start();
        externalApiRequestLogsServiceImpl.send(new ExternalApiRequestLog());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(Level.WARN, logsList.get(0).getLevel());
        assertEquals("DRC-Service| Failed to send to external api request log", logsList.get(0).getMessage());
    }

    @DisplayName("When external event is sent," +
            "And if Kafka exception is not encountered" +
            "Then verify warning not gets logged.")
    @Test
    void sendSuccessMessage() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExternalApiRequestLogsServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
        externalApiRequestLogsServiceImpl.send(new ExternalApiRequestLog());
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(0, logsList.size());
    }
}