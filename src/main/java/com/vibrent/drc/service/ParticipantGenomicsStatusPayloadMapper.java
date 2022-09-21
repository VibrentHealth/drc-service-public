package com.vibrent.drc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.vxp.push.DRCExternalEventDto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ParticipantGenomicsStatusPayloadMapper {

    String mapListOfParticipantGenomicsStatusToJsonString(List<Map<String, Object>> templateFields) throws JsonProcessingException;

    List<DRCExternalEventDto> mapJsonStringToDrcExternalEventDto(String jsonString) throws IOException;
}

