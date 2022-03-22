package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vibrent.drc.enumeration.UserInfoType;
import com.vibrent.vxp.drc.dto.ParticipantLookupResponseLink;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchResponseDTO {
    private List<Map<UserInfoType, Object>> results;

    private Integer total;

    private Integer page;

    private ParticipantLookupResponseLink link = null;
}
