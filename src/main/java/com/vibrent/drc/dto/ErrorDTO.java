package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for transferring error code with description.
 */
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDTO implements Serializable {
    private static final long serialVersionUID = 2495426545524192738L;

    protected String description;
    public String getDescription() {
        return description;
    }
}
