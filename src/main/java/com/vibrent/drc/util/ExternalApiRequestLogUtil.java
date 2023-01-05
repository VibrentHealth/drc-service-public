package com.vibrent.drc.util;

import com.google.common.base.Preconditions;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventSource;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalServiceType;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import com.vibrent.vxp.workflow.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.logging.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ExternalApiRequestLogUtil
 */
@Slf4j
public class ExternalApiRequestLogUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalApiRequestLogUtil.class);

    private static final String DESCRIPTION_MUST_BE_PROVIDED = "Description must be provided";
    private static final String KAFKA_TOPIC = "Kafka topic: ";
    private static final String INPUT_DTO_MUST_BE_PROVIDED = "Input Dto must be provided";

    private ExternalApiRequestLogUtil() {
        // private constructor for util class
    }

    /**
     * Update external api request log field with the http response values like response code, response body etc.
     *
     * @param externalApiRequestLog - ExternalApiRequestLog to update
     * @param responseWrapper - http response
     * @param responseTime - response time
     * @param exception - request failed exception
     * @return return updated ExternalApiRequestLog object
     */
    public static ExternalApiRequestLog updateExternalAPILogResponseParams(ExternalApiRequestLog externalApiRequestLog,
                                                                           HttpResponseWrapper responseWrapper,
                                                                           long responseTime,
                                                                           Exception exception) {
        try {
            String responseString;

            if (exception != null) {
                responseString = exception.getMessage(); // there was some error while sending, log it
                externalApiRequestLog.setResponseCode(getResponseCodeFromException(exception));
            } else {
                responseString = responseWrapper == null ? "No Response" : responseWrapper.getResponseBody();
            }
            externalApiRequestLog.setResponseBody(responseString);
            externalApiRequestLog.setExternalId(getIdFromResponse(responseString));
            if (responseWrapper != null) {
                externalApiRequestLog.setResponseCode(responseWrapper.getStatusCode());
            }
            externalApiRequestLog.setResponseTimestamp(responseTime);
        } catch (Exception e) {
            log.warn("DRC-service: error received when building ExternalApiRequestLog", e);
        }

        return externalApiRequestLog;
    }

    /**
     * Update external api request log field with the http request values like url, headers, method etc.
     *
     * @param externalApiRequestLog - ExternalApiRequestLog to update
     * @param method - Http request method
     * @param url - request url
     * @param headers - request headers
     * @param requestBody - request body
     * @param requestTime - http requested time
     * @return return updated ExternalApiRequestLog object
     */
    public static ExternalApiRequestLog updateExternalAPILogRequestParams(ExternalApiRequestLog externalApiRequestLog,
                                                                          RequestMethod method,
                                                                          String url,
                                                                          Map<String, String> headers,
                                                                          String requestBody,
                                                                          long requestTime) {
        externalApiRequestLog.setHttpMethod(method);
        externalApiRequestLog.setRequestUrl(url);
        externalApiRequestLog.setRequestHeaders(headers == null ? null : headers.toString());
        externalApiRequestLog.setRequestBody(requestBody);
        externalApiRequestLog.setRequestTimestamp(requestTime);
        return externalApiRequestLog;
    }

    /**
     * This method extract the status code for the error message.
     * Example error messages are:
     * 1. 400 Bad request
     * 2. Error Response Received: Response Code : 500
     * @param message - error message
     * @return returns http status code
     */
    private static String extractStatusCode(String message) {
        String[] regex = {"(^(\\s)*[0-9]+) ", "Error Response Received: Response Code : ([0-9]+)"};

        for (String expression : regex) {
            Pattern pattern = Pattern.compile(expression);
            var matcher = pattern.matcher(message);
            if (matcher.find() && matcher.groupCount() > 0) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private static Integer getResponseCodeFromException(Exception exception) {

        if (exception == null) {
            return null;
        }

        if (exception.getCause() instanceof com.google.api.client.http.HttpResponseException) {
            return ((com.google.api.client.http.HttpResponseException) exception.getCause() ).getStatusCode();
        } else {
            //Fetch the response code from error string.
            try {
                String statusCode = extractStatusCode(exception.getMessage());
                if (statusCode != null) {
                    return Integer.parseInt(statusCode);
                }
            } catch (Exception e) {
                log.debug("DRC-Service: Error while getting error code from response", e);
            }
        }
        return null;
    }


    /**
     * Create external api request log object with given event type
     *
     * @param eventType - even type
     * @return returns updated ExternalApiRequestLog object.
     */
    public static ExternalApiRequestLog createExternalApiRequestLog(
            ExternalEventType eventType) {
        return createExternalApiRequestLog(eventType, null, null, null);
    }

    /**
     * Create external api request log object with the given input params
     *
     * @param internalId  - internal id (ie: form id, form entry id, user id, etc)
     * @param eventType   - even type
     * @param externalId  - external id received from api call
     * @param description - brief description of the call
     * @return returns updated ExternalApiRequestLog object
     */
    public static ExternalApiRequestLog createExternalApiRequestLog(ExternalEventType eventType, Long internalId, String externalId, String description) {

        ExternalApiRequestLog externalApiRequestLog = new ExternalApiRequestLog();
        externalApiRequestLog.setService(ExternalServiceType.DRC);
        externalApiRequestLog.setEventSource(ExternalEventSource.DRC_SERVICE);

        if (internalId != null) {
            externalApiRequestLog.setInternalId(internalId);
        }
        externalApiRequestLog.setEventType(eventType);
        if (externalId != null) {
            externalApiRequestLog.setExternalId(externalId);
        }
        if (!StringUtils.isEmpty(description)) {
            externalApiRequestLog.setDescription(description);
        }
        return externalApiRequestLog;
    }

    /**
     * Create ExternalApiRequestLog for received create track order response
     * @param headers - Message headers
     * @param createTrackOrderResponseDto - create track order response
     * @param topic - kafka topic
     * @param description - external event log event description
     * @param statusCode - external event log status code
     * @return returns ExternalApiRequestLog object
     */
    public static ExternalApiRequestLog createExternalApiRequestLogForResponseReceived(MessageHeaderDto headers, CreateTrackOrderResponseDto createTrackOrderResponseDto, String topic, String description, Integer statusCode) {
        Preconditions.checkArgument(createTrackOrderResponseDto != null, INPUT_DTO_MUST_BE_PROVIDED);
        Preconditions.checkArgument(description != null, DESCRIPTION_MUST_BE_PROVIDED);
        Long internalId = null;
        String externalId = null;
        ExternalEventType eventType = null;
        ExternalApiRequestLog externalApiRequestLogsDTO = new ExternalApiRequestLog();

        if (createTrackOrderResponseDto.getParticipant() != null) {
            internalId = createTrackOrderResponseDto.getParticipant().getVibrentID();
            externalId = createTrackOrderResponseDto.getParticipant().getExternalID();
        }

        eventType = getExternalEventTypeListener(createTrackOrderResponseDto.getOperation(), createTrackOrderResponseDto.getStatus());
        externalApiRequestLogsDTO.setService(ExternalServiceType.VXP_GENOTEK);
        externalApiRequestLogsDTO.setEventSource(ExternalEventSource.GENOTEK_SERVICE);
        externalApiRequestLogsDTO.setRequestBody("");
        externalApiRequestLogsDTO.setResponseBody(PrettyPrintUtil.prettyPrint(createTrackOrderResponseDto));

        populateExternalLogDTO(externalApiRequestLogsDTO, RequestMethod.GET, internalId, externalId, PrettyPrintUtil.prettyPrint(headers), statusCode);
        externalApiRequestLogsDTO.setEventType(eventType);
        externalApiRequestLogsDTO.setDescription(description);
        externalApiRequestLogsDTO.setRequestUrl(StringUtils.isEmpty(topic) ? "" : KAFKA_TOPIC + topic);
        return externalApiRequestLogsDTO;
    }

    public static ExternalApiRequestLog createExternalApiRequestLogForResponseReceived(MessageHeaderDto headers, FulfillmentResponseDto fulfillmentResponseDto, String topic, String description, Integer statusCode, ParticipantDto participantDto) {
        Preconditions.checkArgument(fulfillmentResponseDto != null, INPUT_DTO_MUST_BE_PROVIDED);
        Preconditions.checkArgument(description != null, DESCRIPTION_MUST_BE_PROVIDED);
        Long internalId = null;
        String externalId = null;
        ExternalEventType eventType = null;
        ExternalApiRequestLog externalApiRequestLogsDTO = new ExternalApiRequestLog();

        if (participantDto != null) {
            internalId = participantDto.getVibrentID();
            externalId = String.valueOf(participantDto.getExternalID());
        }

        eventType = ExternalEventType.FULFILLMENT_RESPONSE_RECEIVED;
        externalApiRequestLogsDTO.setService(ExternalServiceType.FULFILLMENT_SERVICE);
        externalApiRequestLogsDTO.setEventSource(ExternalEventSource.FULFILLMENT_SERVICE);
        externalApiRequestLogsDTO.setRequestBody("");
        externalApiRequestLogsDTO.setResponseBody(PrettyPrintUtil.prettyPrint(fulfillmentResponseDto));

        populateExternalLogDTO(externalApiRequestLogsDTO, RequestMethod.GET, internalId, externalId, PrettyPrintUtil.prettyPrint(headers), statusCode);
        externalApiRequestLogsDTO.setEventType(eventType);
        externalApiRequestLogsDTO.setDescription(description);
        externalApiRequestLogsDTO.setRequestUrl(StringUtils.isEmpty(topic) ? "" : KAFKA_TOPIC + topic);
        return externalApiRequestLogsDTO;
    }


    /**
     * Create ExternalApiRequestLog for received track delivery response
     * @param headers - Message headers
     * @param trackDeliveryResponseDto - track delivery response
     * @param topic - kafka topic
     * @param description - external event log event description
     * @param statusCode - external event log status code
     * @return returns ExternalApiRequestLog object
     */
    public static ExternalApiRequestLog createExternalApiRequestLogForResponseReceived(MessageHeaderDto headers, TrackDeliveryResponseDto trackDeliveryResponseDto, String topic, String description, Integer statusCode) {
        Preconditions.checkArgument(trackDeliveryResponseDto != null, INPUT_DTO_MUST_BE_PROVIDED);
        Preconditions.checkArgument(description != null, DESCRIPTION_MUST_BE_PROVIDED);
        Long internalId = null;
        String externalId = null;
        ExternalEventType eventType = null;
        ExternalApiRequestLog externalApiRequestLogsDTO = new ExternalApiRequestLog();

        if (trackDeliveryResponseDto.getParticipant() != null) {
            internalId = trackDeliveryResponseDto.getParticipant().getVibrentID();
            externalId = trackDeliveryResponseDto.getParticipant().getExternalID();
        }

        eventType = getExternalEventTypeListener(trackDeliveryResponseDto.getOperation(), trackDeliveryResponseDto.getStatus());
        externalApiRequestLogsDTO.setService(ExternalServiceType.AFTER_SHIP);
        externalApiRequestLogsDTO.setEventSource(ExternalEventSource.AFTER_SHIP_SERVICE);
        externalApiRequestLogsDTO.setRequestBody("");
        externalApiRequestLogsDTO.setResponseBody(PrettyPrintUtil.prettyPrint(trackDeliveryResponseDto));

        populateExternalLogDTO(externalApiRequestLogsDTO, RequestMethod.GET, internalId, externalId, PrettyPrintUtil.prettyPrint(headers), statusCode);
        externalApiRequestLogsDTO.setEventType(eventType);
        externalApiRequestLogsDTO.setDescription(description);
        externalApiRequestLogsDTO.setRequestUrl(StringUtils.isEmpty(topic) ? "" : KAFKA_TOPIC + topic);
        return externalApiRequestLogsDTO;
    }

    public static ExternalApiRequestLog createExternalApiRequestLogForAccountInfoReceived(com.vibrent.vxp.push.MessageHeaderDto headers, AccountInfoUpdateEventDto accountInfoUpdateEventDto, String topic, String description, Integer statusCode) {
        Preconditions.checkArgument(accountInfoUpdateEventDto != null, INPUT_DTO_MUST_BE_PROVIDED);
        Preconditions.checkArgument(description != null, DESCRIPTION_MUST_BE_PROVIDED);
        Long internalId = null;
        String externalId = null;
        ExternalApiRequestLog externalApiRequestLogsDTO = new ExternalApiRequestLog();

        if (accountInfoUpdateEventDto.getParticipant() != null) {
            internalId = accountInfoUpdateEventDto.getParticipant().getVibrentID();
            externalId = accountInfoUpdateEventDto.getParticipant().getExternalID();
        }

        externalApiRequestLogsDTO.setService(ExternalServiceType.DRC);
        externalApiRequestLogsDTO.setEventSource(ExternalEventSource.DRC_SERVICE);
        externalApiRequestLogsDTO.setRequestBody("");
        externalApiRequestLogsDTO.setResponseBody(PrettyPrintUtil.prettyPrint(accountInfoUpdateEventDto));

        populateExternalLogDTO(externalApiRequestLogsDTO, RequestMethod.GET, internalId, externalId, PrettyPrintUtil.prettyPrint(headers), statusCode);
        externalApiRequestLogsDTO.setEventType(ExternalEventType.ACCOUNT_INFORMATION_UPDATE_RECEIVED);
        externalApiRequestLogsDTO.setDescription(description);
        externalApiRequestLogsDTO.setRequestUrl(StringUtils.isEmpty(topic) ? "" : KAFKA_TOPIC + topic);
        return externalApiRequestLogsDTO;
    }

    private static void populateExternalLogDTO(ExternalApiRequestLog externalApiRequestLogsDTO, RequestMethod requestMethod, Long internalId, String externalId, String headers, Integer statusCode) {
        externalApiRequestLogsDTO.setHttpMethod(requestMethod);
        externalApiRequestLogsDTO.setInternalId(internalId);
        externalApiRequestLogsDTO.setResponseCode(statusCode);
        externalApiRequestLogsDTO.setRequestHeaders(headers);
        externalApiRequestLogsDTO.setExternalId(externalId);
        externalApiRequestLogsDTO.setRequestTimestamp(Instant.now().toEpochMilli());
        externalApiRequestLogsDTO.setResponseTimestamp(Instant.now().toEpochMilli());
    }


    private static ExternalEventType getExternalEventTypeListener(final OperationEnum operation, final StatusEnum status) {
        ExternalEventType eventType = null;
        if (operation != null && status != null) {
            switch (operation) {
                case CREATE_TRACK_ORDER:
                    eventType = status.equals(StatusEnum.CREATED) ? ExternalEventType.GENOTEK_CREATE_ORDER_RESPONSE_RECEIVED : ExternalEventType.GENOTEK_STATUS_UPDATE_RESPONSE_RECEIVED;
                    break;
                case TRACK_DELIVERY:
                    eventType = ExternalEventType.AFTER_SHIP_TRACKING_RESPONSE_RECEIVED;
                    break;
                default:
                    break;
            }
        }
        return eventType;
    }
    public static String getIdFromResponse(String response) {
        try {
            JSONObject jsonObj = new JSONObject(response);
            if(jsonObj.has("id")) {
                return jsonObj.getString("id");
            } else{
                log.info("drc-service: External id not found");
                return null;
            }
        } catch (Exception e) {
            ErrorCode.DRC_RESPONSE_VALUE.log(LOGGER, "id", org.apache.commons.lang3.StringUtils.truncate(response, 100), e);
            return null;
        }
    }
}
