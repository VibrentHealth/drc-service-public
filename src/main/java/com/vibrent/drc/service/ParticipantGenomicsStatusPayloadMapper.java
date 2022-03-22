package com.vibrent.drc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.dto.ParticipantGenomicStatusDTO;
import com.vibrent.vxp.push.DRCExternalEventDto;

import java.io.IOException;
import java.util.List;

public interface ParticipantGenomicsStatusPayloadMapper {

    String mapListOfParticipantGenomicsStatusToJsonString(List<ParticipantGenomicStatusDTO> templateFields) throws JsonProcessingException;

    List<DRCExternalEventDto> mapJsonStringToDrcExternalEventDto(String jsonString) throws IOException;
}

