package com.vibrent.drc.util;

import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import ca.uhn.fhir.model.dstu2.valueset.AnswerFormatEnum;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.acadia.domain.enumeration.AddressType;
import com.vibrent.acadia.domain.enumeration.FormComponentFieldType;
import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.UserPreferencesDTO;
import com.vibrent.acadia.web.rest.dto.form.*;
import com.vibrent.acadia.web.rest.dto.helpers.form.enums.ButtonActionType;
import com.vibrent.acadia.web.rest.dto.helpers.form.enums.SubmitButtonInputType;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.FieldValueSubmitButtonModel;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.OptionsValue;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vibrent.drc.constants.ProfileAccountConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ExtendWith(MockitoExtension.class)
class FHIRConverterUtilityTest {

    private boolean drcImpersonation = false;
    private UserDTO userDTO;

    private FormVersionDTO formVersionDTO;
    private static final String EXTERNAL_ID = "P1232322";
    private static final long VIBRENT_ID = 1000L;
    private static final Set<String> authorities = Stream.of("ROLE_USER").collect(Collectors.toCollection(HashSet::new));
    private Map<String, Long> idMap = new HashMap<>();

    @BeforeEach
    public void setUp() {
        formVersionDTO();
        initializeUserDTO();
        initIdMap(295L, 2L, formVersionDTO.getEditMode());

    }

    @Test
    public void convertFormEntryToQuestionnaireResponse() throws Exception {
        FormVersionDTO formVersionDTO = formVersionDTO();
        FormEntryDTO formEntryDTO = buildFormEntryDTO(1L);

        Logger logger = (Logger) LoggerFactory.getLogger(FHIRConverterUtility.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        QuestionnaireResponse questionnaireResponse = FHIRConverterUtility.convertFormEntryToQuestionnaireResponse(formEntryDTO, formVersionDTO, formEntryDTO.getExternalId(), "en", drcImpersonation);
        assertNotNull(questionnaireResponse);

        List<ILoggingEvent> logsList = listAppender.list;
        int index = Math.max(0, logsList.size() - 1);

        assertEquals(Level.INFO, logsList.get(index).getLevel());
        assertEquals(String.format("DRC-Service: QuestionnaireResponse is generated for Participant Id - %s, formEntryId - %d, formVersion(Id - %d, formId - %d, Version - %d)",
                formEntryDTO.getExternalId(), formEntryDTO.getId(), formVersionDTO.getId(), formVersionDTO.getFormId(), formVersionDTO.getVersionId()),
                logsList.get(index).getFormattedMessage());
    }


    @Test
    public void testGetAnswerFormatEnum() {
        FormComponentFieldDTO formComponentFieldDTO = null;
        Assert.assertThrows("input can not be null", IllegalArgumentException.class, () -> FHIRConverterUtility.getAnswerFormatEnum(formComponentFieldDTO));

        FormComponentFieldDTO formComponentFieldDTO1 = getFormComponentFieldDTO(FormComponentFieldType.SUBMIT, 2L, SubmitButtonInputType.AS_NUMBER);
        AnswerFormatEnum answerFormatEnum = FHIRConverterUtility.getAnswerFormatEnum(formComponentFieldDTO1);
        assertEquals(AnswerFormatEnum.DECIMAL, answerFormatEnum);

        FormComponentFieldDTO formComponentFieldDTO2 = getFormComponentFieldDTO(FormComponentFieldType.SUBMIT, 3L, SubmitButtonInputType.AS_STRING);
        AnswerFormatEnum answerFormatEnum2 = FHIRConverterUtility.getAnswerFormatEnum(formComponentFieldDTO2);
        assertEquals(AnswerFormatEnum.TEXT, answerFormatEnum2);

        FormComponentFieldDTO formComponentFieldDTO3 = getFormComponentFieldDTO(FormComponentFieldType.SUBMIT, 3L, SubmitButtonInputType.ON_CLICK_TIME);
        AnswerFormatEnum answerFormatEnum3 = FHIRConverterUtility.getAnswerFormatEnum(formComponentFieldDTO3);
        assertEquals(AnswerFormatEnum.DATE_TIME, answerFormatEnum3);

    }


    private FormEntryDTO buildFormEntryDTO(Long formId) {
        FormEntryDTO formEntryDTO = new FormEntryDTO();
        formEntryDTO.setId(4L);
        formEntryDTO.setExternalId(EXTERNAL_ID);
        formEntryDTO.setFormId(formId);
        formEntryDTO.setDraft(false);
        formEntryDTO.setFormFieldEntries(createFormFieldEntries(userDTO));
        return formEntryDTO;
    }


    FormVersionDTO formVersionDTO() {
        formVersionDTO = new FormVersionDTO();
        formVersionDTO.setId(1L);
        formVersionDTO.setFormId(1L);
        formVersionDTO.setVersionId(2);
        formVersionDTO.setSemanticVersion("SemanticVersion");
        formVersionDTO.setCreatedOn(123L);
        formVersionDTO.setVersionComment("VersionComment");

        FormModeDTO editMode = new FormModeDTO();
        List<FormPageDTO> formPageDTOS = new ArrayList<>();

        List<FormSectionDTO> sections = new ArrayList<>();
        FormSectionDTO sectionDTO = new FormSectionDTO();

        List<FormComponentDTO> formComponents = new ArrayList<>();

        FormComponentDTO formComponentDTO = new FormComponentDTO();

        List<FormComponentFieldDTO> formComponentFieldDTOS = new ArrayList<>();
        FormComponentFieldDTO formComponentFieldDTO = new FormComponentFieldDTO();
        formComponentFieldDTO.setType(FormComponentFieldType.MATRIX_QUESTION);
        FormFieldDTO formFieldDTO = new FormFieldDTO();
        formFieldDTO.setId(1L);
        formComponentFieldDTO.setFormField(formFieldDTO);
        formComponentFieldDTO.setDisplayOrder(1);
        formComponentFieldDTOS.add(formComponentFieldDTO);

        //Additional sample field in the form. Not used in Questionnaire
        FormComponentFieldDTO formComponentFieldDTO2 = new FormComponentFieldDTO();
        formComponentFieldDTO2.setType(FormComponentFieldType.MATRIX_QUESTIONNAIRE_RESPONSE);
        formComponentFieldDTO2.setFormField(formFieldDTO);
        formComponentFieldDTO2.setDisplayOrder(2);
        formComponentFieldDTOS.add(formComponentFieldDTO2);

        formComponentDTO.setFormComponentFields(formComponentFieldDTOS);

        FormComponentMetaDataDTO formComponentMetadataDTO = new FormComponentMetaDataDTO();
        MatrixQuestionnaireMetadataDTO matrixQuestionnaireMetadataDTO = new MatrixQuestionnaireMetadataDTO();
        matrixQuestionnaireMetadataDTO.setSingleSelect(false);

        List<OptionsValue> optionsValues = new ArrayList<>();
        OptionsValue optionsValue = new OptionsValue();
        optionsValue.setScore(1D);
        optionsValues.add(optionsValue);
        matrixQuestionnaireMetadataDTO.setValues(optionsValues);
        formComponentMetadataDTO.setMatrixQuestionnaireMetadataDTO(matrixQuestionnaireMetadataDTO);
        formComponentDTO.setFormComponentMetaDataDTO(formComponentMetadataDTO);

        formComponents.add(formComponentDTO);
        sectionDTO.setFormComponents(formComponents);
        sections.add(sectionDTO);
        FormPageDTO formPageDTO = new FormPageDTO();
        formPageDTO.setSections(sections);
        formPageDTOS.add(formPageDTO);

        editMode.setPages(formPageDTOS);
        formVersionDTO.setEditMode(editMode);

        return formVersionDTO;    }

    private List<FormFieldEntryDTO> createFormFieldEntries(UserDTO userDTO) {
        List<FormFieldEntryDTO> entryDTOS = new ArrayList<>();
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_FIRST_NAME, userDTO.getFirstName()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_MIDDLE_NAME, userDTO.getMiddleInitial()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_LAST_NAME, userDTO.getLastName()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_EMAIL, userDTO.getEmail()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_VERIFIED_PHONE_NUMBER, userDTO.getVerifiedPrimaryPhoneNumber()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_DOB, userDTO.getDob() == null ? null : userDTO.getDob().doubleValue()));

        UserAddressDTO address = userDTO.getAccountAddress();
        if (address != null) {
            // add address info
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_STREET_ADDRESS, address.getStreetOne()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_STREET_ADDRESS_TWO, address.getStreetTwo()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_CITY, address.getCity()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_STATE, address.getState()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_ZIP, address.getZip()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_PHONE, address.getPhoneNumber()));

        }

        return entryDTOS;
    }

    private FormFieldEntryDTO createFormFieldEntry(String fieldName, String fieldValue) {
        FormFieldEntryDTO dto = new FormFieldEntryDTO();
        Long formFieldId = getIdByName(fieldName);
        dto.setFormFieldId(formFieldId);
        dto.setFormFieldEntryValues(Collections.singletonList(createEntryValue(fieldValue)));

        return dto;
    }

    private FormFieldEntryDTO createFormFieldEntry(String fieldName, Double fieldValue) {
        FormFieldEntryDTO dto = new FormFieldEntryDTO();

        dto.setFormFieldId(getIdByName(fieldName));
        dto.setFormFieldEntryValues(Collections.singletonList(createEntryValue(fieldValue)));

        return dto;
    }

    private FormFieldEntryValueDTO createEntryValue(String fieldValue) {
        FormFieldEntryValueDTO value = new FormFieldEntryValueDTO();
        value.setValueAsString(fieldValue);
        return value;
    }

    private FormFieldEntryValueDTO createEntryValue(Double fieldValue) {
        FormFieldEntryValueDTO value = new FormFieldEntryValueDTO();
        value.setValueAsNumber(fieldValue);
        return value;
    }

    private Long getIdByName(String propertyName) {
        return idMap.get(propertyName);
    }

    private void initIdMap(Long formId, Long formVersionId, FormModeDTO editMode) {
        idMap = new HashMap<>();
        idMap.put(FORM_NAME_CONSENT, formId);
        idMap.put(FORM_VERSION_CONSENT, formVersionId);

        // add field ids
        Map<String, Long> formFieldMap = getFormFieldMapFromFormMode(editMode);

        idMap.put(FIELD_NAME_FIRST_NAME, formFieldMap.get(FIELD_NAME_FIRST_NAME));
        idMap.put(FIELD_NAME_MIDDLE_NAME, formFieldMap.get(FIELD_NAME_MIDDLE_NAME));
        idMap.put(FIELD_NAME_LAST_NAME, formFieldMap.get(FIELD_NAME_LAST_NAME));
        idMap.put(FIELD_NAME_STREET_ADDRESS, formFieldMap.get(FIELD_NAME_STREET_ADDRESS));
        idMap.put(FIELD_NAME_STREET_ADDRESS_TWO, formFieldMap.get(FIELD_NAME_STREET_ADDRESS_TWO));
        idMap.put(FIELD_NAME_CITY, formFieldMap.get(FIELD_NAME_CITY));
        idMap.put(FIELD_NAME_STATE, formFieldMap.get(FIELD_NAME_STATE));
        idMap.put(FIELD_NAME_ZIP, formFieldMap.get(FIELD_NAME_ZIP));
        idMap.put(FIELD_NAME_EMAIL, formFieldMap.get(FIELD_NAME_EMAIL));
        idMap.put(FIELD_NAME_VERIFIED_PHONE_NUMBER, formFieldMap.get(FIELD_NAME_VERIFIED_PHONE_NUMBER));
        idMap.put(FIELD_NAME_PHONE, formFieldMap.get(FIELD_NAME_PHONE));
        idMap.put(FIELD_NAME_DOB, formFieldMap.get(FIELD_NAME_DOB));

    }

    private Map<String, Long> getFormFieldMapFromFormMode(FormModeDTO modeDTO) {
        Map<String, Long> fieldIdMap = new HashMap<>();
        List<FormComponentFieldDTO> allInputFields = findAllInputFields(modeDTO);

        if (!allInputFields.isEmpty()) {
            allInputFields.forEach(f -> {
                String fieldName = f.getName();
                if (fieldName != null && fieldIdMap.get(f.getName()) == null) {
                    fieldIdMap.put(fieldName, f.getFormField().getId());
                }
            });
        }

        return fieldIdMap;
    }

    private List<FormComponentFieldDTO> findAllInputFields(FormModeDTO formMode) {
        return findAllInputFields(formMode.getPages());
    }

    private List<FormComponentFieldDTO> findAllInputFields(List<FormPageDTO> formPageDTOS) {
        List<FormComponentFieldDTO> inputFields = new ArrayList<>();

        if (formPageDTOS == null || formPageDTOS.isEmpty()) {
            return inputFields;
        }

        // collecting the sections
        List<FormSectionDTO> sections = new ArrayList<>();

        for (FormPageDTO pageDTO : formPageDTOS) {
            if (pageDTO.getSections() != null) {
                sections.addAll(pageDTO.getSections());
            }
        }

        sections.stream()
                .filter(section -> section.getFormComponents() != null && !section.getFormComponents().isEmpty())
                .forEach(section -> section.getFormComponents().stream()
                        .filter(formComponentDTO -> formComponentDTO.getFormComponentFields() != null && !formComponentDTO.getFormComponentFields().isEmpty())
                        .forEach(formComponentDTO -> formComponentDTO.getFormComponentFields().stream()
                                .filter(FHIRConverterUtility::isInputFieldType)
                                .forEach(formComponentFieldDTO -> {
                                    formComponentFieldDTO.setFormComponentDto(formComponentDTO);
                                    inputFields.add(formComponentFieldDTO);
                                })));

        return inputFields;
    }


    private void initializeUserDTO() {
        userDTO = new UserDTO();
        userDTO.setId(VIBRENT_ID);
        userDTO.setExternalId(EXTERNAL_ID);
        userDTO.setFirstName("SomeFName");
        userDTO.setMiddleInitial("L");
        userDTO.setLastName("SomeLastName");
        userDTO.setDob(1323456879L);
        userDTO.setTestUser(false);
        userDTO.setEmail("a@b.com");
        userDTO.setVerifiedPrimaryPhoneNumber("9874563210");
        userDTO.setAuthorities(authorities);
        userDTO.setUserPreferences(getUserPreferencesDTO());
        userDTO.setUserAddresses(getUserMailingAddress());
    }

    private List<UserAddressDTO> getUserMailingAddress() {
        List<UserAddressDTO> accountAddress = new ArrayList<>();
        UserAddressDTO addressDto = new UserAddressDTO();
        addressDto.setCity("Mesa");
        addressDto.setStreetOne("6644 E Baywood Ave");
        addressDto.setZip("85206");
        addressDto.setState("AZ");
        addressDto.setType(AddressType.ACCOUNT);
        addressDto.setPhoneNumber("1234567859");
        accountAddress.add(addressDto);
        return accountAddress;
    }

    UserPreferencesDTO getUserPreferencesDTO() {
        UserPreferencesDTO userPreferencesDTO = new UserPreferencesDTO();
        userPreferencesDTO.setLocale("un");
        userPreferencesDTO.setUserId(VIBRENT_ID);
        return userPreferencesDTO;
    }

    FormComponentFieldDTO getFormComponentFieldDTO(FormComponentFieldType type, Long id, SubmitButtonInputType submitButtonInputType) {
        FormComponentFieldDTO formComponentFieldDTO = new FormComponentFieldDTO();
        formComponentFieldDTO.setType(type);
        FormFieldDTO formFieldDTO = new FormFieldDTO();
        formFieldDTO.setId(id);
        formComponentFieldDTO.setFormField(formFieldDTO);
        formComponentFieldDTO.setDisplayOrder(1);

        FieldValueSubmitButtonModel fv = new FieldValueSubmitButtonModel();
        fv.setActionType(ButtonActionType.AS_SUBMIT);
        fv.setAsInput(true);
        fv.setInputType(submitButtonInputType);
        formComponentFieldDTO.setFieldValue(fv);
        return formComponentFieldDTO;
    }

}