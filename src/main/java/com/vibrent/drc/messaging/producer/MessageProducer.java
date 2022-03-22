package com.vibrent.drc.messaging.producer;

public interface MessageProducer<T> {
    void setKafkaEnabled(boolean newState);

    default void send(T msg) {
        throw new UnsupportedOperationException();
    }
}

