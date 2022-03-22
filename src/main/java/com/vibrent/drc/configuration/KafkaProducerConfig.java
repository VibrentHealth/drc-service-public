package com.vibrent.drc.configuration;

import com.vibrent.drc.constants.KafkaConstants;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.vxp.push.DRCExternalEventDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {


    private Environment environment;

    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private Map<String, Object> getConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty(KafkaConstants.BROKERS_PROPERTY));
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    @Bean
    public ProducerFactory<String, DRCExternalEventDto> drcExternalEventDtoProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    @Bean
    public KafkaTemplate<String, DRCExternalEventDto> drcExternalEventDtoKafkaTemplate() {
        return new KafkaTemplate<>(drcExternalEventDtoProducerFactory());
    }


    @Bean
    public ProducerFactory<String, ExternalApiRequestLog> externalLogProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    @Bean
    public KafkaTemplate<String, ExternalApiRequestLog> externalLogKafkaTemplate() {
        return new KafkaTemplate<>(externalLogProducerFactory());
    }
}


