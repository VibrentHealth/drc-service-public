package com.vibrent.drc.service;


import com.vibrent.drc.dto.ExternalApiRequestLog;

public interface ExternalApiRequestLogsService {

    /**
     * Sending ExternalApiRequestLogs to externalLogs kafka topic
     *
     * @param
     */
    void send(ExternalApiRequestLog externalApiRequestLog);
}
