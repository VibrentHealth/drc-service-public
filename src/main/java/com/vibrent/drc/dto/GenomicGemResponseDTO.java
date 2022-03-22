package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GenomicGemResponseDTO {

    @JsonProperty("data")
    private List<ParticipantGenomicStatusDTO> data;

    @JsonProperty("timestamp")
    private String timestamp;
}
