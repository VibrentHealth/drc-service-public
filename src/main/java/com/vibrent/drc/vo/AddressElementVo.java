package com.vibrent.drc.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressElementVo implements Serializable {
    @NotNull
    @JsonProperty("city")
    private String city;
    @JsonProperty("country")
    private String country;
    @NotNull
    @JsonProperty("line1")
    private String line1;
    @JsonProperty("line2")
    private String line2;
    @NotNull
    @JsonProperty("postalCode")
    private String postalCode;
    @NotNull
    @JsonProperty("state")
    private String state;
    @JsonProperty("validated")
    private Boolean validated;
}
