package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.dto.ParticipantGenomicStatusDTO;
import com.vibrent.drc.service.DataSharingMetricsService;
import com.vibrent.drc.service.ParticipantGenomicsStatusPayloadMapper;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.push.DRCExternalEventDto;
import com.vibrent.vxp.push.ExternalEventSourceEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ParticipantGenomicsStatusPayloadMapperImpl implements ParticipantGenomicsStatusPayloadMapper {
    private final ParticipantService participantService;
    private final DataSharingMetricsService dataSharingMetricsService;

    public ParticipantGenomicsStatusPayloadMapperImpl(ParticipantService participantService, DataSharingMetricsService dataSharingMetricsService) {
        this.participantService = participantService;

        this.dataSharingMetricsService = dataSharingMetricsService;
    }


    @Override
    public String mapListOfParticipantGenomicsStatusToJsonString(List<ParticipantGenomicStatusDTO> templateFields) throws JsonProcessingException {
        if (templateFields != null) {
            return JacksonUtil.getMapper().writeValueAsString(templateFields);
        } else {
            return null;
        }
    }

    @Override
    public List<DRCExternalEventDto> mapJsonStringToDrcExternalEventDto(String jsonString) throws IOException {

        List<DRCExternalEventDto> drcExternalEventDtos = new ArrayList<>();
        if (StringUtils.isEmpty(jsonString)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> drcExternalEventDtoMapList = JacksonUtil.getMapper().readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {
        });

        Set<String> externalIds = drcExternalEventDtoMapList.stream().map(dto -> dto.get(DrcConstant.PARTICIPANT_ID).toString()).collect(Collectors.toSet());
        participantService.fetchAndCacheVibrentIds(externalIds);

        for (Map<String, Object> drcExternalEventDtoMap : drcExternalEventDtoMapList) {
            if (!drcExternalEventDtoMap.containsKey(DrcConstant.TYPE) && !drcExternalEventDtoMap.containsKey(DrcConstant.PARTICIPANT_ID)) {
                log.warn("Drc-Service: Mandatory filed not found for: {}", JacksonUtil.getMapper().writeValueAsString(drcExternalEventDtoMap));
            } else {
                DRCExternalEventDto drcExternalEventDto = buildDRCExternalEventDto(drcExternalEventDtoMap);
                if (drcExternalEventDto != null) {
                    drcExternalEventDtos.add(drcExternalEventDto);
                }
            }

        }
        return drcExternalEventDtos;
    }

    DRCExternalEventDto buildDRCExternalEventDto(Map<String, Object> drcExternalEventDtoMap) throws IOException {
        DRCExternalEventDto drcExternalEventDto = null;
        String externalId = drcExternalEventDtoMap.get(DrcConstant.PARTICIPANT_ID).toString();
        Long vibrentId = participantService.getVibrentId(externalId);
        if (vibrentId == null) {
            log.warn("Drc-Service: Vibrent id not for payload {}", JacksonUtil.getMapper().writeValueAsString(drcExternalEventDtoMap));
            dataSharingMetricsService.incrementGenomicsStatusProcessingFailureCounter();
        } else {
            drcExternalEventDto = new DRCExternalEventDto();
            drcExternalEventDto.setSource(ExternalEventSourceEnum.DRC);
            drcExternalEventDto.setBody(JacksonUtil.getMapper().writeValueAsString(drcExternalEventDtoMap));
            drcExternalEventDto.setEventType(drcExternalEventDtoMap.get(DrcConstant.TYPE).toString());
            drcExternalEventDto.setExternalID(externalId);
            drcExternalEventDto.setVibrentID(vibrentId);
        }
        return drcExternalEventDto;
    }
}
