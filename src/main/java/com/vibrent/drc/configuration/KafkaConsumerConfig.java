package com.vibrent.drc.configuration;

import com.vibrent.drc.constants.KafkaConstants;
import com.vibrent.vxp.workflow.MessageSpecificationEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.vibrent.drc.constants.KafkaConstants.DEFAULT_CONCURRENCY;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final String DRC_TRACK_ORDER_RESPONSE_LISTENER_GROUP_ID = "drcCreateTrackOrderResponseListener";
    private static final String DRC_ACCOUNT_INFO_UPDATE_LISTENER_GROUP_ID = "drcAccountInfoUpdateEventListener";
    private static final String FULFILLMENT_ORDER_RESPONSE_LISTENER_GROUP_ID = "fulfillmentOrderResponseListener";

    private Environment environment;

    @Inject
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private Map<String, Object> getConfigProps(String groupId) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getProperty(KafkaConstants.BROKERS_PROPERTY));
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return configProps;
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactoryTrackDeliveryResponse() {
        Map<String, Object> configProps = this.getConfigProps(KafkaConstants.VXP_TRACK_DELIVERY_RESPONSE_GROUP_ID);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConstants.EARLIEST);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactoryTrackDeliveryResponseEvent() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryTrackDeliveryResponse());
        factory.setConcurrency(Integer.valueOf(Objects.requireNonNull(environment.getProperty(DEFAULT_CONCURRENCY))));
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        factory.setRecordFilterStrategy(consumerRecord -> {
            String messageSpec = extractHeader(consumerRecord.headers(), KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC);

            //discard the Record if MessageSpecification is not equal to TRACK_DELIVERY_RESPONSE
            final boolean canDiscardRecord = null == messageSpec
                    || !MessageSpecificationEnum.TRACK_DELIVERY_RESPONSE.toString().equals(messageSpec);

            if (canDiscardRecord) {
                log.debug("Drc-Service: Discarding the Non TRACK_DELIVERY_RESPONSE. Request Type: {}", messageSpec);
            }

            return canDiscardRecord;
        });
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactoryCreateTrackOrderListener() {
        Map<String, Object> configProps = this.getConfigProps(DRC_TRACK_ORDER_RESPONSE_LISTENER_GROUP_ID);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConstants.EARLIEST);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactoryTrackOrderResponseListener() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryCreateTrackOrderListener());
        factory.setConcurrency(Integer.valueOf(Objects.requireNonNull(environment.getProperty(DEFAULT_CONCURRENCY))));
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        factory.setRecordFilterStrategy(consumerRecord -> {
            String messageSpec = extractHeader(consumerRecord.headers(), KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC);

            //discard the Record if MessageSpecification is not equal to CREATE_TRACK_ORDER_RESPONSE
            final boolean canDiscardRecord = null == messageSpec
                    || !MessageSpecificationEnum.CREATE_TRACK_ORDER_RESPONSE.toString().equals(messageSpec);

            if (canDiscardRecord) {
                log.debug("Drc-Service: Discarding the Non CREATE_TRACK_ORDER_RESPONSE. Request Type: {}", messageSpec);
            }

            return canDiscardRecord;
        });
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactoryAccountInfoUpdateEventListener() {
        Map<String, Object> configProps = this.getConfigProps(DRC_ACCOUNT_INFO_UPDATE_LISTENER_GROUP_ID);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConstants.LATEST);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactoryAccountInfoUpdateEventListener() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryAccountInfoUpdateEventListener());
        factory.setConcurrency(Integer.valueOf(Objects.requireNonNull(environment.getProperty(DEFAULT_CONCURRENCY))));
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        factory.setRecordFilterStrategy(consumerRecord -> {
            String messageSpec = extractHeader(consumerRecord.headers(), KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC);

            //discard the Record if MessageSpecification is not equal to ACCOUNT_INFORMATION_UPDATE
            final boolean canDiscardRecord = null == messageSpec
                    || !com.vibrent.vxp.push.MessageSpecificationEnum.ACCOUNT_INFORMATION_UPDATE.toString().equals(messageSpec);

            if (canDiscardRecord) {
                log.debug("Drc-Service: Discarding the Non ACCOUNT_INFORMATION_UPDATE. Request Type: {}", messageSpec);
            }

            return canDiscardRecord;
        });
        return factory;
    }

    @Bean
    public ConsumerFactory<String, byte[]> consumerFactoryFulfillmentOrderResponseListener() {
        Map<String, Object> configProps = this.getConfigProps(FULFILLMENT_ORDER_RESPONSE_LISTENER_GROUP_ID);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConstants.EARLIEST);
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new ByteArrayDeserializer());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactoryFulfillmentOrderResponseListener() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactoryFulfillmentOrderResponseListener());
        factory.setConcurrency(Integer.valueOf(Objects.requireNonNull(environment.getProperty(DEFAULT_CONCURRENCY))));
        factory.getContainerProperties().setPollTimeout(KafkaConstants.POLL_TIMEOUT);
        factory.setRecordFilterStrategy(consumerRecord -> {
            String messageSpec = extractHeader(consumerRecord.headers(), KafkaConstants.KAFKA_HEADER_MESSAGE_SPEC);

            //discard the Record if MessageSpecification is not equal to FULFILLMENT_RESPONSE
            final boolean canDiscardRecord = null == messageSpec
                    || !MessageSpecificationEnum.FULFILLMENT_RESPONSE.toString().equals(messageSpec);

            if (canDiscardRecord) {
                log.debug("Drc-Service: Discarding the Non FULFILLMENT_RESPONSE. Request Type: {}", messageSpec);
            }

            return canDiscardRecord;
        });
        return factory;
    }

    public static String extractHeader(Headers headers, String headerKey) {
        String headerValue = null;

        if (headers != null) {
            for (Header header : headers) {
                if (headerKey.equalsIgnoreCase(header.key())
                        && header.value() != null
                        && header.value().length > 0) {
                    headerValue = new String(header.value(), StandardCharsets.UTF_8).trim();
                    //Remove leading and tailing quotes
                    headerValue = headerValue.replaceAll("(^\"+)|(\"+$)", "");
                    break;
                }
            }
        }
        return headerValue;
    }

    private static <T> JsonDeserializer<T> getJsonDeserializer(Class<T> clazz) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(clazz);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.vibrent.*");
        deserializer.setUseTypeMapperForKey(true);
        return deserializer;
    }
}
