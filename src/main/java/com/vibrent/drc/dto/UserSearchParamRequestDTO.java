package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vibrent.drc.enumeration.UserInfoType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request dto to fetch the User information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchParamRequestDTO {

    /**
     * Type of the inputIds list example External Id
     */
    private UserInfoType inputType;

    /**
     * Maps of parameter values to fetch the user information.
     * e.g. For VIBRENT_ID input type, key can be IDS and Value will be list of ids
     * e.g. for CREATED_ON input type, key can be startDate or endDate and value will be corresponding String date in UTC format
     */
    private Map<String, Object> inputParams;
}


