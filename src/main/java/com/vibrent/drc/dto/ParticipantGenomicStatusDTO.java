package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ParticipantGenomicStatusDTO {

    @JsonProperty("participant_id")
    public String participantId;

    @JsonProperty("status")
    public String status;

    @JsonProperty("module")
    public String module;

    @JsonProperty("type")
    public String type;

    @JsonProperty("appointment_id")
    public String appointmentId;

    @JsonProperty("decision")
    public String decision;

}
