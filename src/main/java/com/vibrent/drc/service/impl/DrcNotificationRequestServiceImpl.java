package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.messaging.producer.DrcExternalEventProducer;
import com.vibrent.drc.service.DrcNotificationRequestService;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.drc.dto.DrcNotificationRequestDTO;
import com.vibrent.vxp.push.DRCExternalEventDto;
import com.vibrent.vxp.push.ExternalEventSourceEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DrcNotificationRequestServiceImpl implements DrcNotificationRequestService {


    private final ParticipantService participantService;
    private final DrcExternalEventProducer drcExternalEventProducer;

    public DrcNotificationRequestServiceImpl(ParticipantService participantService, DrcExternalEventProducer drcExternalEventProducer) {
        this.participantService = participantService;
        this.drcExternalEventProducer = drcExternalEventProducer;
    }

    @Override
    public void processDrcNotificationRequest(DrcNotificationRequestDTO drcNotificationRequestDTO) {
        DRCExternalEventDto drcExternalEventDto = buildExternalEventDto(drcNotificationRequestDTO);
        drcExternalEventProducer.send(drcExternalEventDto);
    }

    public DRCExternalEventDto buildExternalEventDto(DrcNotificationRequestDTO drcNotificationRequestDTO) {
        try {
            String externalId = drcNotificationRequestDTO.getParticipantId();
            //If external ID doesn't start with P then add
            if (!StringUtils.isEmpty(externalId) && !externalId.startsWith("P")) {
                externalId = "P" + externalId;
            }

            DRCExternalEventDto drcExternalEventDto = new DRCExternalEventDto();
            drcExternalEventDto.setSource(ExternalEventSourceEnum.DRC);
            drcExternalEventDto.setEventType(String.valueOf(drcNotificationRequestDTO.getEvent()));
            drcExternalEventDto.setExternalID(externalId);
            drcExternalEventDto.setVibrentID(getVibrentID(externalId));
            drcExternalEventDto.setBody(JacksonUtil.getMapper().writeValueAsString(drcNotificationRequestDTO.getMessageBody()));
            return drcExternalEventDto;
        } catch (JsonProcessingException e) {
            log.error("DRC Service: Error while converting message body to JSON String format", e);
        }
        return null;
    }

    /**
     * Get Vibrent ID for given participant ID (externalID)
     *
     * @param participantId
     * @return
     */
    private long getVibrentID(String participantId) {
        return this.participantService.getVibrentId(participantId);
    }

}
