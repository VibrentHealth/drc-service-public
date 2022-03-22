package com.vibrent.drc.constants;

public class KafkaConstants {

    public static final String BROKERS_PROPERTY = "spring.kafka.server";


    // Kafka Message Headers
    public static final String KAFKA_HEADER_VERSION = "VXP-Header-Version";
    public static final String KAFKA_HEADER_ORIGINATOR = "VXP-Originator";
    public static final String KAFKA_HEADER_PATTERN = "VXP-Pattern";
    public static final String KAFKA_HEADER_MESSAGE_SPEC = "VXP-Message-Spec";
    public static final String KAFKA_HEADER_MESSAGE_SPEC_VERSION = "VXP-Message-Spec-Version";
    public static final String KAFKA_HEADER_TENANT_ID = "VXP-Tenant-ID";
    public static final String KAFKA_HEADER_PROGRAM_ID = "VXP-Program-ID";
    public static final String KAFKA_HEADER_USER_ID = "VXP-User-ID";
    public static final String KAFKA_HEADER_TRIGGER = "VXP-Trigger";
    public static final String KAFKA_HEADER_WORKFLOWNAME = "VXP-Workflow-Name";
    public static final String KAFKA_HEADER_WORKFLOW_INSTANCE_ID = "VXP-Workflow-Instance-ID";
    public static final String KAFKA_HEADER_REPLY_TO_ID = "VXP-In-Reply-To-ID";
    public static final String KAFKA_HEADER_MESSAGE_ID = "VXP-Message-ID";
    public static final String KAFKA_HEADER_MESSAGE_TIMESTAMP = "VXP-Message-Timestamp";

    public static final String VXP_HEADER_VERSION = "2.1.4";
    public static final String VXP_MESSAGE_SPEC_VERSION = "4.0.0";
    public static final String EXTERNAL_ID = "External-Id";

    public static final Long POLL_TIMEOUT = 3000L;
    public static final String LATEST = "latest";
    public static final String EARLIEST = "earliest";
    public static final String VXP_TRACK_DELIVERY_RESPONSE_GROUP_ID = "drcTrackDeliveryResponseGroupId";
    public static final String DEFAULT_CONCURRENCY = "spring.kafka.defaultConcurrency";

    private KafkaConstants() {
    }
}