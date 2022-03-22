package com.vibrent.drc.service.impl;

import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.messaging.producer.ExternalApiRequestLogsProducer;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;

@Service
@Slf4j
public class ExternalApiRequestLogsServiceImpl implements ExternalApiRequestLogsService {

    private ExternalApiRequestLogsProducer externalApiRequestLogsProducer;

    public ExternalApiRequestLogsServiceImpl(ExternalApiRequestLogsProducer externalApiRequestLogsListener) {
        this.externalApiRequestLogsProducer = externalApiRequestLogsListener;
    }

    @Override
    public void send(@NotNull ExternalApiRequestLog externalApiRequestLog) {
        try {
            this.externalApiRequestLogsProducer.send(externalApiRequestLog);
        } catch (KafkaException e) {
            log.warn("DRC-Service| Failed to send to external api request log", e);
        }
    }


}
