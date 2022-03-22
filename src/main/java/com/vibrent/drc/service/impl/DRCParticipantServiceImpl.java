package com.vibrent.drc.service.impl;

import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.dto.Participant;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.service.AccountInfoUpdateEventHelperService;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.DRCParticipantService;
import com.vibrent.drc.util.ExternalApiRequestLogUtil;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.ParticipantUtil;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.vibrent.drc.constants.DrcConstant.URL_PARTICIPANT;

@Slf4j
@Service
public class DRCParticipantServiceImpl implements DRCParticipantService {

    private final DRCRetryService retryService;

    private final DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    private final DrcProperties drcProperties;

    private final AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService;

    public DRCParticipantServiceImpl(DRCRetryService retryService, DRCBackendProcessorWrapper drcBackendProcessorWrapper, DrcProperties drcProperties,
                                     AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService) {
        this.retryService = retryService;
        this.drcBackendProcessorWrapper = drcBackendProcessorWrapper;
        this.drcProperties = drcProperties;
        this.accountInfoUpdateEventHelperService = accountInfoUpdateEventHelperService;
    }

    /**
     * Patching the participant, usually used after user test participant flag is updated
     *
     * @param accountInfoUpdateEventDto - accountInfoUpdateEventDto object received
     */
    @Override
    public void patchTestParticipant(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        accountInfoUpdateEventHelperService.processIfTestParticipantUpdated(accountInfoUpdateEventDto, () -> validateAndCallDrcEndpoint(accountInfoUpdateEventDto));
    }

    boolean validateAndCallDrcEndpoint(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        try {
            if (accountInfoUpdateEventDto == null || StringUtils.isEmpty(accountInfoUpdateEventDto.getExternalID()) ||
                    accountInfoUpdateEventDto.getParticipant() == null || accountInfoUpdateEventDto.getParticipant().getTestUser() == null ||
                    accountInfoUpdateEventDto.getParticipant().getTestUser() == Boolean.FALSE) {
                log.info("DRC Service: UserId is null or participantId is null or test user flag is null or false. Cannot patch participant with DRC");
                return false;
            }

            Participant participant = new Participant();
            participant.setTestParticipant(accountInfoUpdateEventDto.getParticipant().getTestUser());
            participant.setParticipantId(accountInfoUpdateEventDto.getExternalID());
            return retryService.executeWithRetry(() -> patchParticipantInternal(accountInfoUpdateEventDto.getVibrentID(), participant));
        } catch (Exception e) {
            log.warn("DRC Service: Error while calling DRC endpoint for patching user", e);
            return false;
        }
    }

    boolean patchParticipantInternal(Long userId, @NotNull Participant participant) throws Exception {
        String participantId = participant.getParticipantId();

        //  if patching participant in DRC test environment,
        //  then make sure that when participant objects are merged, it keeps external ID with the timestamp
        Participant retrievedParticipant = getParticipantById(userId, participantId);
        if (retrievedParticipant == null) {
            log.warn("DRC Service: Retrieved participant is null from DRC. Cannot patch participant");
            return false;
        }

        retrievedParticipant.setTestParticipant(participant.getTestParticipant());

        ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLog(ExternalEventType.DRC_UPDATE_USER, userId, participantId, null);
        String fullUrl = getParticipantFullUrl(participantId);

        String jsonString = JacksonUtil.getMapper().writeValueAsString(retrievedParticipant);

        // setting the header of If-Match
        Map<String, String> headers = new HashMap<>();
        headers.put(DrcConstant.HEADER_KEY_IF_MATCH, ParticipantUtil.findMetaVersion(retrievedParticipant));

        // PATCH is not supported by google http client (not correctly anyway), use PUT instead for this operation
        drcBackendProcessorWrapper.sendRequestReturnDetails(fullUrl, jsonString, RequestMethod.PUT, headers, externalApiRequestLog);
        return true;
    }

    /**
     * get participant by id
     *
     * @return participant object received from DRC
     */
    public Participant getParticipantById(Long userId, String participantId) throws Exception {
        if (StringUtils.isEmpty(participantId)) {
            log.warn("DRC Service: participantId is null or empty. Cannot fetch participant details from DRC");
            return null;
        }

        String fullUrl = getParticipantFullUrl(participantId);
        ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLog(ExternalEventType.DRC_RETRIEVE_USER_INFO, userId, participantId, null);
        HttpResponseWrapper response = drcBackendProcessorWrapper.sendRequest(fullUrl, null, RequestMethod.GET, null, externalApiRequestLog);
        return createParticipantFromResponse(response.getResponseBody());
    }

    private Participant createParticipantFromResponse(String response)
            throws IOException {
        if (response == null) {
            return null;
        }
        return JacksonUtil.getMapper().readValue(response, Participant.class);
    }

    private String getParticipantFullUrl(String participantId) {
        return drcProperties.getDrcApiBaseUrl() + URL_PARTICIPANT + "/" + participantId;
    }
}
