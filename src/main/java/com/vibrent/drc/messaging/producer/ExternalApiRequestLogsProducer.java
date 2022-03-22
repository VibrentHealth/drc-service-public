package com.vibrent.drc.messaging.producer;

import com.vibrent.drc.dto.ExternalApiRequestLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;


@Service
@Slf4j
public class ExternalApiRequestLogsProducer implements MessageProducer<ExternalApiRequestLog> {

    private boolean kafkaEnabled;

    private final KafkaTemplate<String, ExternalApiRequestLog> kafkaTemplate;

    private final String topicName;


    public ExternalApiRequestLogsProducer(@Value("${spring.kafka.enabled}") boolean kafkaEnabled,
                                          KafkaTemplate<String,ExternalApiRequestLog> kafkaTemplate,
                                          @Value("${spring.kafka.topics.externalApiRequestLogs}") String topicName) {
        this.kafkaEnabled = kafkaEnabled;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Override
    public void setKafkaEnabled(boolean isEnabled) {
        this.kafkaEnabled = isEnabled;
    }

    @Override
    public void send(ExternalApiRequestLog msg) {

        if (!kafkaEnabled) {
            return;
        }

        kafkaTemplate.send(topicName, msg.getExternalId(), msg)
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
}
