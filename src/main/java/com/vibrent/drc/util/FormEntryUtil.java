package com.vibrent.drc.util;

import com.vibrent.acadia.domain.enumeration.FormEntryMode;
import com.vibrent.acadia.web.rest.dto.form.FormEntryDTO;

public final class FormEntryUtil {

    private FormEntryUtil() {
    }

    public static FormEntryDTO createFormEntryStructure(long userId, long formVersionId, long formId, String formName, Long timeStamp) {
        FormEntryDTO dto = new FormEntryDTO();

        // set top level properties
        dto.setId(timeStamp);
        dto.setUserId(userId);
        dto.setFormVersionId(formVersionId);
        dto.setFormId(formId);
        dto.setFormName(formName);

        dto.setDraft(false);
        dto.setMeasurementTime(timeStamp);
        dto.setEntryRecordedTime(timeStamp);
        dto.setMode(FormEntryMode.MANUAL);

        return dto;

    }
}
