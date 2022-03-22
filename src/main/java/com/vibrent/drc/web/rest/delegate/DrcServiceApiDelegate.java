package com.vibrent.drc.web.rest.delegate;


import com.vibrent.drc.domain.ParticipantLookupResponseMapper;
import com.vibrent.drc.dto.UserSearchResponseDTO;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.DrcNotificationRequestService;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.CollectionUtil;
import com.vibrent.drc.util.HttpRequestUtils;
import com.vibrent.vxp.drc.dto.DrcNotificationRequestDTO;
import com.vibrent.vxp.drc.dto.DrcNotificationResponseDTO;
import com.vibrent.vxp.drc.dto.ParticipantLookupResponse;
import com.vibrent.vxp.drc.dto.ParticipantLookupResponseLink;
import com.vibrent.vxp.drc.resource.DrcApiDelegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
public class DrcServiceApiDelegate implements DrcApiDelegate {

    private final DrcNotificationRequestService drcNotificationRequestService;
    private final ParticipantService participantService;
    private final ParticipantLookupResponseMapper participantLookupResponseMapper;
    private final DataSharingMetricsService dataSharingMetricsService;

    public DrcServiceApiDelegate(DrcNotificationRequestService drcNotificationRequestService, ParticipantService participantService,
                                 ParticipantLookupResponseMapper participantLookupResponseMapper, DataSharingMetricsService dataSharingMetricsService) {
        this.drcNotificationRequestService = drcNotificationRequestService;
        this.participantService = participantService;
        this.participantLookupResponseMapper = participantLookupResponseMapper;
        this.dataSharingMetricsService = dataSharingMetricsService;
    }

    @Override
    public ResponseEntity<DrcNotificationResponseDTO> getEventNotification(DrcNotificationRequestDTO drcNotificationRequestDTO) {
        dataSharingMetricsService.incrementRealTimeApiInitiatedCounter();
        DrcNotificationResponseDTO drcNotificationResponseDTO = new DrcNotificationResponseDTO();
        if (drcNotificationRequestDTO != null) {
            log.info("DRC Service: Received DRC Notification event for participantID: {}, eventType: {}, authoredTime: {}",
                    drcNotificationRequestDTO.getParticipantId(), drcNotificationRequestDTO.getEvent(), drcNotificationRequestDTO.getEventAuthoredTime());
            this.drcNotificationRequestService.processDrcNotificationRequest(drcNotificationRequestDTO);
            dataSharingMetricsService.incrementRealTimeApiCallInvokedSuccessfullyCounter();
            return ResponseEntity.status(HttpStatus.OK).body(getResponseBody(drcNotificationRequestDTO, drcNotificationResponseDTO));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    private DrcNotificationResponseDTO getResponseBody(DrcNotificationRequestDTO drcNotificationRequestDTO, DrcNotificationResponseDTO drcNotificationResponseDTO) {
        return drcNotificationResponseDTO.event(drcNotificationRequestDTO.getEvent())
                .participantId(drcNotificationRequestDTO.getParticipantId()).responseCode(HttpStatus.OK.value()).responseBody("");
    }

    @Override
    public ResponseEntity<ParticipantLookupResponse> drcParticipantLookupGet(Optional<String> startDate, Optional<String> endDate,
                                                                             Optional<List<String>> vibrentId, Optional<List<String>> drcId,
                                                                             Optional<Integer> page, Optional<Integer> pageSize) {
        log.info("DRC Service: DRC participant Lookup service invoked with startDate: {}, endDate; {}, vibrentId: {}, " +
                "drcId: {}, page: {}, pageSize: {}", startDate, endDate, vibrentId, drcId, page, pageSize);
        dataSharingMetricsService.incrementParticipantLookupApiInitiatedCounter();

        List<String> vibrentIds = CollectionUtil.getInputIds(vibrentId);
        List<String> drcIds = CollectionUtil.getInputIds(drcId);

        UserSearchResponseDTO userSearchResponseDTO = this.participantService.getParticipants(vibrentIds, drcIds, startDate, endDate, page, pageSize);
        ParticipantLookupResponse participantLookupResponse = this.participantLookupResponseMapper.convertUserSearchResponse(userSearchResponseDTO);
        updateLinks(participantLookupResponse, vibrentIds, drcIds, startDate, endDate);
        dataSharingMetricsService.incrementParticipantLookupApiCallInvokedSuccessfullyCounter();
        return ResponseEntity.status(HttpStatus.OK).body(participantLookupResponse == null ? new ParticipantLookupResponse() : participantLookupResponse);
    }

    /**
     * Update Links in the participant Response
     *
     * @param participantLookupResponse
     * @param vibrentIds
     * @param drcIds
     * @param startDate
     * @param endDate
     */
    private static void updateLinks(ParticipantLookupResponse participantLookupResponse, List<String> vibrentIds, List<String> drcIds,
                                    Optional<String> startDate, Optional<String> endDate) {
        if (participantLookupResponse == null || participantLookupResponse.getLink() == null) {
            return;
        }

        ParticipantLookupResponseLink link = participantLookupResponse.getLink();
        ParticipantLookupResponseLink updatedLink = new ParticipantLookupResponseLink();
        HttpServletRequest servletRequest = HttpRequestUtils.getCurrentRequest();
        StringBuilder requestUrl = new StringBuilder(servletRequest.getRequestURL().toString());
        boolean isQueryStringAdded = false;
        isQueryStringAdded = buildQueryString(vibrentIds, drcIds, startDate, endDate, requestUrl, isQueryStringAdded);

        if (!StringUtils.isEmpty(link.getNextPageQuery())) {
            updatedLink.setNextPageQuery(requestUrl + link.getNextPageQuery().replace("/api/userInfo/search?", (isQueryStringAdded ? "&" : "?")));
        }

        if (!StringUtils.isEmpty(link.getPreviousPageQuery())) {
            updatedLink.setPreviousPageQuery(requestUrl + link.getPreviousPageQuery().replace("/api/userInfo/search?", (isQueryStringAdded ? "&" : "?")));
        }

        //Update links
        participantLookupResponse.setLink(updatedLink);
    }

    /**
     * Build Query string for given query params
     *
     * @param vibrentIds
     * @param drcIds
     * @param startDate
     * @param endDate
     * @param requestUrl
     * @param queryStringAdded
     * @return
     */
    private static boolean buildQueryString(List<String> vibrentIds, List<String> drcIds, Optional<String> startDate,
                                            Optional<String> endDate, StringBuilder requestUrl, boolean queryStringAdded) {
        if(!CollectionUtils.isEmpty(vibrentIds)) {
            requestUrl.append ("?").append("vibrentId").append("=").append(String.join(",", vibrentIds));
            queryStringAdded = true;
        }

        if(!CollectionUtils.isEmpty(drcIds)) {
            requestUrl.append(queryStringAdded ? "&" : "?").append ("drcId").append("=").append(String.join(",", drcIds));
            queryStringAdded = true;
        }

        if(startDate.isPresent()) {
            requestUrl.append(queryStringAdded ? "&" : "?").append ("startDate").append("=").append(startDate.get());
            queryStringAdded = true;
        }

        if(endDate.isPresent()) {
            requestUrl.append(queryStringAdded ? "&" : "?").append ("endDate").append("=").append(endDate.get());
            queryStringAdded = true;
        }
        return queryStringAdded;
    }
}
