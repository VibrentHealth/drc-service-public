package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GenomicGemResponseDTO {

    @JsonProperty("data")
    private List<Map<String, Object>> data;

    @JsonProperty("timestamp")
    private String timestamp;
}
