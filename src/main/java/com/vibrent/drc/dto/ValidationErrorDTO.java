package com.vibrent.drc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for transferring error code with description.
 */
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
public class ValidationErrorDTO extends ErrorDTO {
    private List<FieldErrorDTO> fieldErrors;

    public ValidationErrorDTO(String description) {
        super(description);
    }

    public void add(String objectName, String field, String message) {
        if (fieldErrors == null) {
            fieldErrors = new ArrayList<>();
        }
        fieldErrors.add(new FieldErrorDTO(objectName, field, message));
    }

    public List<FieldErrorDTO> getFieldErrors() {
        return fieldErrors;
    }
}
