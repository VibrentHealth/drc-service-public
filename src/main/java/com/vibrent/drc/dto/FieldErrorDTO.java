package com.vibrent.drc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class FieldErrorDTO implements Serializable {

    private static final long serialVersionUID = 2093343451807729963L;

    private final String objectName;

    private final String field;

    private final String message;
}
