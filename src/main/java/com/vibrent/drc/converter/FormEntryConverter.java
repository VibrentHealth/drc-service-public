package com.vibrent.drc.converter;

import com.vibrent.acadia.domain.enumeration.FormEntryMode;
import com.vibrent.acadia.web.rest.dto.form.ActiveFormVersionDTO;
import com.vibrent.acadia.web.rest.dto.form.FormEntryDTO;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.service.ApiService;
import com.vibrent.vxp.push.ParticipantDto;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FormEntryConverter {
    private final ApiService apiService;
    private final DrcProperties drcProperties;
    private final FormFieldEntryConverter formFieldEntryConverter;

    public FormEntryConverter(ApiService apiService, DrcProperties drcProperties, FormFieldEntryConverter formFieldEntryConverter) {
        this.apiService = apiService;
        this.drcProperties = drcProperties;
        this.formFieldEntryConverter = formFieldEntryConverter;
    }

    public FormEntryDTO convertSecondaryContactInformationToBasicsForm(ParticipantDto participant, String ssn, Set<String> changedFields) {
        ActiveFormVersionDTO activeFormVersionByFormId = apiService.getActiveFormVersionByFormId(drcProperties.getBasicsFormId());

        FormEntryDTO formEntryDTO = new FormEntryDTO();
        formEntryDTO.setDraft(false);
        formEntryDTO.setFormName(drcProperties.getBasicsFormName());
        formEntryDTO.setFormId(activeFormVersionByFormId.getFormId());
        formEntryDTO.setFormVersionId(activeFormVersionByFormId.getActiveFormVersionId());
        formEntryDTO.setUserId(participant.getVibrentID());
        formEntryDTO.setEntryRecordedTime(System.currentTimeMillis());
        formEntryDTO.setMode(FormEntryMode.AUTOMATIC);
        formEntryDTO.setFormFieldEntries(formFieldEntryConverter.getFormFieldEntryDTOS(participant, changedFields, ssn));
        return formEntryDTO;
    }
}
