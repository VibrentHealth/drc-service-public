package com.vibrent.drc.service.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.form.*;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.converter.FormEntryConverter;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.service.AccountInfoUpdateEventHelperService;
import com.vibrent.drc.service.AccountInfoUpdateEventService;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.util.*;
import com.vibrent.vxp.push.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;
import java.util.function.Predicate;

import static com.vibrent.drc.constants.DrcConstant.ROLE_CATI;
import static com.vibrent.drc.constants.DrcConstant.URL_PARTICIPANT;
import static com.vibrent.drc.constants.ProfileAccountConstants.*;
import static com.vibrent.drc.util.FHIRConverterUtility.findAllInputFields;

@Slf4j
@Service
public class AccountInfoUpdateEventServiceImpl implements AccountInfoUpdateEventService {

    private Map<String, Long> idMap;
    private final ApiService apiService;
    private final DRCBackendProcessorWrapper drcBackendProcessorWrapper;
    private final DrcProperties drcProperties;
    private final AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService;
    private final FormEntryConverter formEntryConverter;

    private final DRCRetryService retryService;
    private FhirContext fhirContext = FhirContext.forDstu2();

    public AccountInfoUpdateEventServiceImpl(ApiService apiService, DRCBackendProcessorWrapper drcBackendProcessorWrapper,
                                             DrcProperties drcProperties,
                                             DRCRetryService retryService,
                                             AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService, FormEntryConverter formEntryConverter) {
        this.apiService = apiService;
        this.drcBackendProcessorWrapper = drcBackendProcessorWrapper;
        this.drcProperties = drcProperties;
        this.retryService = retryService;
        this.accountInfoUpdateEventHelperService = accountInfoUpdateEventHelperService;
        this.formEntryConverter = formEntryConverter;
    }

    @Override
    public void processAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {

        //Check with last sync record, if there is change in user account info then sends user account info to DRC and save the successfully sent fields to DB.
        accountInfoUpdateEventHelperService.processIfUserAccountUpdated(accountInfoUpdateEventDto, () -> sendAccountInfoUpdates(accountInfoUpdateEventDto));

        accountInfoUpdateEventHelperService.processIfUserSecondaryContactOrSSNUpdated(accountInfoUpdateEventDto, (ssn, secondaryContactTypes) ->
                sendSecondaryContactInfoAndSsnUpdates(accountInfoUpdateEventDto, ssn, secondaryContactTypes));

    }

    @Override
    public boolean sendAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        ParticipantDto participant = accountInfoUpdateEventDto.getParticipant();

        boolean isSentToDrc = false;
        FormEntryDTO formEntryDto = null;

        try {
            // Get details to build QR

            formEntryDto = apiService.getFormEntryDtoByFormName(participant.getVibrentID(), FORM_NAME_CONSENT);

            if (formEntryDto == null || !formEntryDto.getConsentProvided()) {
                // when user is not consented
                return false;
            }

            FormVersionDTO formVersionDTO = apiService.getFormVersionById(formEntryDto.getFormVersionId());

            if (formVersionDTO == null) {
                return false;
            }

            if (CollectionUtils.isEmpty(idMap)) {
                initIdMap(formEntryDto.getFormId(), formVersionDTO.getId(), formVersionDTO.getEditMode());
            }

            FormEntryDTO formEntry = convertParticipantDtoToFormEntryDto(participant, formVersionDTO.getId(), formEntryDto);
            boolean drcImpersonation = isImpersonationEnable(formEntryDto.getUpdatedById());

            isSentToDrc = retryService.executeWithRetry(() -> prepareQuestionnaireResponseAndSendToDRC(formEntry, formVersionDTO, participant, drcImpersonation));

        } catch (Exception e) {
            log.warn("DRC-Service: Error while processing account info for the participantId: {} .", participant.getExternalID(), e);
        }

        return isSentToDrc;
    }

    @Override
    public boolean sendSecondaryContactInfoAndSsnUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto, String ssn, Set<String> changedFields) {
        if (CollectionUtils.isEmpty(changedFields)) {
            return false;
        }
        List<SecondaryContactDto> secondaryContacts = accountInfoUpdateEventDto.getParticipant().getSecondaryContacts();
        try {
            if (!CollectionUtils.isEmpty(secondaryContacts)) {
                FormEntryDTO formEntryDTO = formEntryConverter.convertSecondaryContactInformationToBasicsForm(accountInfoUpdateEventDto.getParticipant(), ssn, changedFields);

                FormVersionDTO formVersionDTO = apiService.getFormVersionById(formEntryDTO.getFormVersionId());

                if (formVersionDTO == null) {
                    return false;
                }

                return retryService.executeWithRetry(() -> prepareQuestionnaireResponseAndSendToDRC(formEntryDTO, formVersionDTO, accountInfoUpdateEventDto.getParticipant(), false));
            }
        } catch (Exception e) {
            log.warn("DRC-Service: Error while processing Secondary Contact Info for the participantId: {} .", accountInfoUpdateEventDto.getParticipant().getExternalID(), e);
        }
        return false;
    }

    private boolean prepareQuestionnaireResponseAndSendToDRC(FormEntryDTO formEntryDTO, FormVersionDTO formVersionDTO, ParticipantDto participantDto, boolean drcImpersonation) throws Exception {
        //Build QR
        QuestionnaireResponse questionnaireResponse = null;
        questionnaireResponse = FHIRConverterUtility
                .convertFormEntryToQuestionnaireResponse(formEntryDTO, formVersionDTO, participantDto.getExternalID(), StringUtil.getLanguageKey(participantDto.getLanguage()), drcImpersonation);
        return sendToDRC(questionnaireResponse, formEntryDTO, participantDto);

    }

    private boolean sendToDRC(QuestionnaireResponse questionnaireResponse, FormEntryDTO formEntryDTO, ParticipantDto participantDto) throws Exception {
        HttpResponseWrapper httpResponseWrapper = null;

        String url = drcProperties.getDrcApiBaseUrl() + URL_PARTICIPANT + "/" + participantDto.getExternalID() + "/QuestionnaireResponse";
        String questionnaireResponseString = fhirContext.newJsonParser().encodeResourceToString(questionnaireResponse);
        String description = formEntryDTO.getFormName() + "| fid: " + formEntryDTO.getFormId() + "| fvId: " + formEntryDTO.getFormVersionId() + "| feId: " + formEntryDTO.getId();
        ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLog(ExternalEventType.DRC_FORM_ENTRY, participantDto.getVibrentID(), participantDto.getExternalID(), description);


        httpResponseWrapper = drcBackendProcessorWrapper.sendRequestReturnDetails(url, questionnaireResponseString, RequestMethod.POST, null, externalApiRequestLog);
        return (httpResponseWrapper != null && (httpResponseWrapper.getStatusCode() == 200));

    }

    private boolean isImpersonationEnable(Long userId) {
        boolean isEnable = false;
        if (userId != null) {
            UserDTO userDTO = apiService.getUserDTO(userId);
            isEnable = (userDTO != null && userDTO.getAuthorities() != null && userDTO.getAuthorities().contains(ROLE_CATI));
        }
        return isEnable;
    }


    public FormEntryDTO convertParticipantDtoToFormEntryDto(ParticipantDto participant, Long formVersionId, FormEntryDTO formEntryDTO) {
        Long timestamp = null;

        if (formEntryDTO.getMeasurementTime() != null) {
            timestamp = formEntryDTO.getMeasurementTime();
        } else if (formEntryDTO.getEntryRecordedTime() != null) {
            timestamp = formEntryDTO.getEntryRecordedTime();
        }

        FormEntryDTO dto = FormEntryUtil.createFormEntryStructure(participant.getVibrentID(), formVersionId, formEntryDTO.getFormId(), FORM_NAME_CONSENT, timestamp);

        // setting field entries
        dto.setFormFieldEntries(createFormFieldEntries(participant));
        return dto;
    }

    private List<FormFieldEntryDTO> createFormFieldEntries(ParticipantDto participant) {
        List<FormFieldEntryDTO> entryDTOS = new ArrayList<>();
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_FIRST_NAME, participant.getFirstName()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_MIDDLE_NAME, participant.getMiddleInitial()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_LAST_NAME, participant.getLastName()));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_EMAIL, getContactByType(participant, TypeEnum.EMAIL, true)));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_VERIFIED_PHONE_NUMBER, getContactByType(participant, TypeEnum.PHONE, true)));
        entryDTOS.add(createFormFieldEntry(FIELD_NAME_DOB,
                participant.getDateOfBirth() == null ? null : DateTimeUtil.getTimestampFromStringDate(participant.getDateOfBirth()).doubleValue()));

        List<AddressElementDto> userAddress = participant.getAddresses();
        Optional<AddressElementDto> addressElementDto = userAddress.stream().filter(a -> (AddressTypeEnum.ACCOUNT_ADDRESS).equals(a.getAddressType())).findAny();
        if (addressElementDto.isPresent()) {
            AddressElementDto address = addressElementDto.get();
            // add address info
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_STREET_ADDRESS, address.getLine1()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_STREET_ADDRESS_TWO, address.getLine2()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_CITY, address.getCity()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_STATE, StringUtil.prefixPIIState(address.getState())));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_ZIP, address.getPostalCode()));
            entryDTOS.add(createFormFieldEntry(FIELD_NAME_PHONE, getContactByType(participant, TypeEnum.PHONE, false)));
        }

        return entryDTOS;
    }

    private String getContactByType(ParticipantDto participant, TypeEnum contactType, boolean isVerified) {
        String contact = null;
        Predicate<ContactElementDto> byContactType = contactElementDto -> contactElementDto.getContactType().equals(contactType);
        Predicate<ContactElementDto> byVerified = ContactElementDto::getVerified;

        List<ContactElementDto> contactElementList = participant.getContacts();
        Optional<ContactElementDto> optionalContactElementDto;
        if (!contactElementList.isEmpty()) {
            if (isVerified) {
                optionalContactElementDto = contactElementList.stream().filter(byContactType.and(byVerified)).findAny();
            } else {
                optionalContactElementDto = contactElementList.stream().filter(byContactType.and(byVerified.negate())).findAny();
            }
            if (optionalContactElementDto.isPresent()) {
                contact = optionalContactElementDto.get().getContact();
            }
        }

        return contact;
    }

    private FormFieldEntryDTO createFormFieldEntry(String fieldName, String fieldValue) {
        FormFieldEntryDTO dto = new FormFieldEntryDTO();

        dto.setFormFieldId(getIdByName(fieldName));
        dto.setFormFieldEntryValues(Collections.singletonList(createEntryValue(fieldValue)));

        return dto;
    }

    private FormFieldEntryDTO createFormFieldEntry(String fieldName, Double fieldValue) {
        FormFieldEntryDTO dto = new FormFieldEntryDTO();

        dto.setFormFieldId(getIdByName(fieldName));
        dto.setFormFieldEntryValues(Collections.singletonList(createEntryValue(fieldValue)));

        return dto;
    }

    private Long getIdByName(String propertyName) {
        return idMap.get(propertyName);
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


    private void initIdMap(Long formId, Long formVersionId, FormModeDTO formModeDTO) {
        idMap = new HashMap<>();
        idMap.put(FORM_NAME_CONSENT, formId);
        idMap.put(FORM_VERSION_CONSENT, formVersionId);

        // add field ids
        Map<String, Long> formFieldMap = getFormFieldMapFromFormMode(formModeDTO);

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

    static Map<String, Long> getFormFieldMapFromFormMode(FormModeDTO modeDTO) {
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

}
