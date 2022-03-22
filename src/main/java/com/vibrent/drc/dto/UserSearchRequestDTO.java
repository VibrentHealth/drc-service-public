package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vibrent.drc.enumeration.UserInfoType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

import static com.vibrent.drc.constants.DrcConstant.*;

/**
 * Request dto to fetch the User information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSearchRequestDTO {

    /**
     * List of search Params
     */
    private List<UserSearchParamRequestDTO> searchParams;

    /**
     * Return the required type in response.
     */
    @NotNull
    @NotEmpty
    private Set<UserInfoType> requiredTypes;

    private int page = DEFAULT_PAGE;

    private int pageSize = DEFAULT_PAGE_SIZE;
}


