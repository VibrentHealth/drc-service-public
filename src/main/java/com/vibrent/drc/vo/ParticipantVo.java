package com.vibrent.drc.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@JsonInclude(Include.NON_NULL)
public class ParticipantVo implements Serializable {

    @JsonProperty("emailAddress")
    private String emailAddress;
    @JsonProperty("verifiedPhoneNumber")
    private String verifiedPhoneNumber;
    @JsonProperty("accountAddress")
    private AddressElementVo accountAddress;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("middleInitial")
    private String middleInitial;
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("dateOfBirth")
    private String dateOfBirth;
    @JsonProperty("secondaryContacts")
    private Map<String, SecondaryContactVo> secondaryContacts;
    @JsonProperty("testUser")
    private Boolean testUser;
    @JsonProperty("ssn")
    private String ssn;
    @JsonProperty("vibrentId")
    private long vibrentID;
}



