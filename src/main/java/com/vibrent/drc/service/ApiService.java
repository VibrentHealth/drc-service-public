package com.vibrent.drc.service;

import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.acadia.web.rest.dto.form.ActiveFormVersionDTO;
import com.vibrent.acadia.web.rest.dto.form.FormEntryDTO;
import com.vibrent.acadia.web.rest.dto.form.FormVersionDTO;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface ApiService {
    /**
     * This method fetch Delivery Biobank Address details from API service.
     */
    String getBioBankAddress();

    /**
     * Get User Details
     * @return
     */
    UserDTO getUserDTO(Long vibrentId);

    List<FormEntryDTO>  getUserFormEntryDTO(Long userId, String formName);

    FormVersionDTO getFormVersionById(Long id);

    ActiveFormVersionDTO getActiveFormVersionByFormId(Long formId);

    UserSSNDTO getUserSsnByUserId(long userId);

    FormEntryDTO getFormEntryDtoByFormName(@NotNull Long userId, @NotNull String formName);
}
