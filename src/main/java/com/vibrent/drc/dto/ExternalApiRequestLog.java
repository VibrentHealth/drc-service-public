package com.vibrent.drc.dto;


import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalEventSource;
import com.vibrent.drc.enumeration.ExternalServiceType;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;

@Data
public class ExternalApiRequestLog {

    @NotNull
    private ExternalServiceType service;

    @NotNull
    private RequestMethod httpMethod;

    @NotNull
    private String requestUrl;

    private String requestHeaders;

    private String requestBody;

    private String responseBody;

    private Integer responseCode;

    @NotNull
    private Long requestTimestamp;

    private Long responseTimestamp;

    private Long internalId;

    private String externalId;

    private ExternalEventType eventType;

    private String description;
    private ExternalEventSource eventSource;
}
