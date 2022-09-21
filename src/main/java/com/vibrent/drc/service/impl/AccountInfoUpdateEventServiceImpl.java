package com.vibrent.drc.service.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.form.*;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.converter.FormEntryConverter;
import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.dto.Participant;
import com.vibrent.drc.enumeration.ConsentWithdrawStatus;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.repository.DRCUpdateInfoSyncRetryRepository;
import com.vibrent.drc.service.*;
import com.vibrent.drc.util.*;
import com.vibrent.drc.vo.DrcResponseVo;
import com.vibrent.vxp.push.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;
import java.util.function.Predicate;

import static com.vibrent.drc.constants.DrcConstant.ROLE_CATI;
import static com.vibrent.drc.constants.DrcConstant.URL_PARTICIPANT;
import static com.vibrent.drc.constants.ProfileAccountConstants.*;
import static com.vibrent.drc.util.FHIRConverterUtility.findAllInputFields;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInfoUpdateEventServiceImpl implements AccountInfoUpdateEventService {

    private Map<String, Long> idMap;
    private final ApiService apiService;
    private final DRCBackendProcessorWrapper drcBackendProcessorWrapper;
    private final DrcProperties drcProperties;
    private final AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService;
    private final FormEntryConverter formEntryConverter;
    private final DRCParticipantService participantService;
    private final DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository;

    private final DRCRetryService retryService;
    private final FhirContext fhirContext = FhirContext.forDstu2();

    @Transactional
    @Override
    public void processAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        processAccountInfoUpdates(accountInfoUpdateEventDto, true);
    }

    @Override
    public void processAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto, boolean isNewMessage) {
        if (accountInfoUpdateEventDto == null || accountInfoUpdateEventDto.getParticipant() == null) {
            return;
        }

        try {
            if (isNewMessage) {
                drcUpdateInfoSyncRetryRepository.deleteByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
            }

            if (isParticipantWithdrawn(accountInfoUpdateEventDto.getParticipant())) {
                drcUpdateInfoSyncRetryRepository.deleteByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
                return;
            }

            //Check with last sync record, if there is change in user account info then sends user account info to DRC and save the successfully sent fields to DB.
            accountInfoUpdateEventHelperService.processIfUserAccountUpdated(accountInfoUpdateEventDto, () -> sendAccountInfoUpdates(accountInfoUpdateEventDto));

            accountInfoUpdateEventHelperService.processIfUserSecondaryContactOrSSNUpdated(accountInfoUpdateEventDto, (ssn, secondaryContactTypes) ->
                    sendSecondaryContactInfoAndSsnUpdates(accountInfoUpdateEventDto, ssn, secondaryContactTypes));
        } catch (Exception e) {
            addToRetryQueue(accountInfoUpdateEventDto, true, e.getMessage());
        }
    }

    @Override
    public boolean sendAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        ParticipantDto participant = accountInfoUpdateEventDto.getParticipant();
        FormEntryDTO formEntryDto = null;

        try {
            // Get details to build QR
            formEntryDto = apiService.getFormEntryDtoByFormName(participant.getVibrentID(), FORM_NAME_CONSENT);

            if (formEntryDto == null || !Boolean.TRUE.equals(formEntryDto.getConsentProvided())) {
                log.info("DRC-Service: Primary Consent not found or Consent not Provided for the user {}", participant.getVibrentID());
                return false;
            }

            if (StringUtils.isEmpty(formEntryDto.getExternalId())) {
                log.info("DRC-Service: Primary Consent is not sync with drc for user id {}, Partial Questionnaire Response can not be sent. Add entries to retry table", participant.getVibrentID());
                addToRetryQueue(accountInfoUpdateEventDto, false, "Primary Consent is not sync with drc");
                return false;
            }

            ActiveFormVersionDTO activeFormVersionByFormId = apiService.getActiveFormVersionByFormId(formEntryDto.getFormId());
            FormVersionDTO formVersionDTO = apiService.getFormVersionById(activeFormVersionByFormId.getActiveFormVersionId());

            if (formVersionDTO == null) {
                return false;
            }

            if (CollectionUtils.isEmpty(idMap)) {
                initIdMap(formEntryDto.getFormId(), formVersionDTO.getId(), formVersionDTO.getEditMode());
            }

            FormEntryDTO formEntry = convertParticipantDtoToFormEntryDto(participant, formVersionDTO.getId(), formEntryDto);
            boolean drcImpersonation = isImpersonationEnable(formEntryDto.getUpdatedById());

            DrcResponseVo drcResponseVo = retryService.executeWithRetry(() -> prepareQuestionnaireResponseAndSendToDRC(formEntry, formVersionDTO, participant, drcImpersonation));
            return processDrcResponse(drcResponseVo, accountInfoUpdateEventDto);

        } catch (Exception e) {
            log.warn("DRC-Service: Error while processing account info for the participantId: {} .", participant.getExternalID(), e);
            addToRetryQueue(accountInfoUpdateEventDto, true, e.getMessage());
        }

        return false;
    }

    private boolean isParticipantWithdrawn(@NonNull ParticipantDto participantDto) throws Exception {
        Participant participant = retryService.executeWithRetry(() -> participantService
                .getParticipantById(participantDto.getVibrentID(), participantDto.getExternalID()));

        return participant != null && (participant.getWithdrawalStatus() == ConsentWithdrawStatus.EARLY_OUT
                || participant.getWithdrawalStatus() == ConsentWithdrawStatus.NO_USE);
    }

    @Override
    public boolean sendSecondaryContactInfoAndSsnUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto, String ssn, Set<String> changedFields) {
        if (CollectionUtils.isEmpty(changedFields)) {
            return false;
        }
        List<SecondaryContactDto> secondaryContacts = accountInfoUpdateEventDto.getParticipant().getSecondaryContacts();
        try {
            if (!CollectionUtils.isEmpty(secondaryContacts)) {

                FormEntryDTO basicFormEntryDto = apiService.getFormEntryDtoByFormName(accountInfoUpdateEventDto.getParticipant().getVibrentID(), FORM_NAME_BASICS);
                if (basicFormEntryDto == null) {
                    log.info("DRC-Service: The Basic form is not found for user id {}.", accountInfoUpdateEventDto.getParticipant().getVibrentID());
                    return false;
                }

                if (StringUtils.isEmpty(basicFormEntryDto.getExternalId())) {
                    log.info("DRC-Service: The Basic form is not sync with drc for user id {}, Partial Questionnaire Response is not sent.", accountInfoUpdateEventDto.getParticipant().getVibrentID());
                    addToRetryQueue(accountInfoUpdateEventDto, false, "The Basic form is not sync with drc");
                    return false;
                }

                FormEntryDTO formEntryDTO = formEntryConverter.convertSecondaryContactInformationToBasicsForm(accountInfoUpdateEventDto.getParticipant(), ssn, changedFields);

                FormVersionDTO formVersionDTO = apiService.getFormVersionById(formEntryDTO.getFormVersionId());

                if (formVersionDTO == null) {
                    return false;
                }

                DrcResponseVo drcResponseVo = retryService.executeWithRetry(() -> prepareQuestionnaireResponseAndSendToDRC(formEntryDTO, formVersionDTO, accountInfoUpdateEventDto.getParticipant(), false));
                return processDrcResponse(drcResponseVo, accountInfoUpdateEventDto);
            }
        } catch (Exception e) {
            log.warn("DRC-Service: Error while processing Secondary Contact Info for the participantId: {} .", accountInfoUpdateEventDto.getParticipant().getExternalID(), e);
            addToRetryQueue(accountInfoUpdateEventDto, true, e.getMessage());
        }
        return false;
    }

    private DrcResponseVo prepareQuestionnaireResponseAndSendToDRC(FormEntryDTO formEntryDTO, FormVersionDTO formVersionDTO, ParticipantDto participantDto, boolean drcImpersonation) throws Exception {
        //Build QR
        QuestionnaireResponse questionnaireResponse = null;
        questionnaireResponse = FHIRConverterUtility
                .convertFormEntryToQuestionnaireResponse(formEntryDTO, formVersionDTO, participantDto.getExternalID(), StringUtil.getLanguageKey(participantDto.getLanguage()), drcImpersonation);
        return sendToDRC(questionnaireResponse, formEntryDTO, participantDto);
    }

    private DrcResponseVo sendToDRC(QuestionnaireResponse questionnaireResponse, FormEntryDTO formEntryDTO, ParticipantDto participantDto) throws Exception {
        HttpResponseWrapper httpResponseWrapper = null;

        String url = drcProperties.getDrcApiBaseUrl() + URL_PARTICIPANT + "/" + participantDto.getExternalID() + "/QuestionnaireResponse";
        String questionnaireResponseString = fhirContext.newJsonParser().encodeResourceToString(questionnaireResponse);
        String description = formEntryDTO.getFormName() + "| fid: " + formEntryDTO.getFormId() + "| fvId: " + formEntryDTO.getFormVersionId() + "| feId: " + formEntryDTO.getId();
        ExternalApiRequestLog externalApiRequestLog = ExternalApiRequestLogUtil.createExternalApiRequestLog(ExternalEventType.DRC_FORM_ENTRY, participantDto.getVibrentID(), participantDto.getExternalID(), description);


        httpResponseWrapper = drcBackendProcessorWrapper.sendRequestReturnDetails(url, questionnaireResponseString, RequestMethod.POST, null, externalApiRequestLog);

        return  buildDrcResponse(httpResponseWrapper);
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

    private void addToRetryQueue(AccountInfoUpdateEventDto accountInfoUpdateEventDto,
                                 boolean incrementRetryCounter,
                                 String reason) {

        DRCUpdateInfoSyncRetry entry = drcUpdateInfoSyncRetryRepository.findByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        if (entry == null) {
            entry = new DRCUpdateInfoSyncRetry();
            entry.setVibrentId(accountInfoUpdateEventDto.getVibrentID());
            entry.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);
            entry.setRetryCount(0L);
            entry.setErrorDetails(reason);
        }

        if (incrementRetryCounter) {
            entry.setRetryCount(entry.getRetryCount() == null ? 1L : entry.getRetryCount() + 1L);
        }

        entry.setErrorDetails(reason);

        try {
            entry.setPayload(JacksonUtil.getMapper().writeValueAsString(accountInfoUpdateEventDto));
            drcUpdateInfoSyncRetryRepository.save(entry);
        } catch (JsonProcessingException e) {
            throw new BusinessProcessingException("Failed to add entry to retry queue ");
        }
    }

    private static DrcResponseVo buildDrcResponse(@NonNull HttpResponseWrapper httpResponseWrapper) {
        boolean isSuccess = (httpResponseWrapper.getStatusCode() == 200);

        DrcResponseVo drcResponseVo = new DrcResponseVo();
        drcResponseVo.setSuccess(isSuccess);
        drcResponseVo.setHttpCode(httpResponseWrapper.getStatusCode());

        if (!isSuccess) {
            drcResponseVo.setErrorResponse(httpResponseWrapper.getResponseBody());
        }

        return drcResponseVo;
    }

    private  boolean processDrcResponse(DrcResponseVo drcResponseVo, AccountInfoUpdateEventDto accountInfoUpdateEventDto) {

        if (drcResponseVo != null) {
            if (drcResponseVo.isSuccess()) {
                drcUpdateInfoSyncRetryRepository.deleteByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
                return true;
            }

            if (drcResponseVo.getHttpCode() == 400) {
                addToRetryQueue(accountInfoUpdateEventDto, true, drcResponseVo.getErrorResponse());
            }
        }

        return false;
    }
}
