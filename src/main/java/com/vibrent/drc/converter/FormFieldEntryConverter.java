package com.vibrent.drc.converter;

import com.vibrent.acadia.domain.enumeration.SecondaryContactType;
import com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO;
import com.vibrent.acadia.web.rest.dto.form.FormFieldEntryValueDTO;
import com.vibrent.drc.constants.FormFieldConstant;
import com.vibrent.vxp.push.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;

import static com.vibrent.drc.constants.DrcConstant.*;
import static com.vibrent.drc.constants.SecondaryContactStateRelationshipMaps.getContactOneStateRelationshipDRCMap;
import static com.vibrent.drc.constants.SecondaryContactStateRelationshipMaps.getContactTwoStateRelationshipDRCMap;

@Component
public class FormFieldEntryConverter {

    public List<FormFieldEntryDTO> getFormFieldEntryDTOS(ParticipantDto participant, Set<String> secondaryContactTypes, String ssn) {
        List<FormFieldEntryDTO> formFieldEntryDTOS = new ArrayList<>();

        if (CollectionUtils.isEmpty(secondaryContactTypes)) {
            return formFieldEntryDTOS;
        }
        for (String secondaryContactType : secondaryContactTypes) {
            switch (secondaryContactType) {
                case CONTACT_ONE:
                    SecondaryContactDto secondaryContactDto = getSecondaryContactDtoByPreference(participant.getSecondaryContacts(), SecondaryContactType.CONTACT_ONE.toString());
                    updateContactOne(formFieldEntryDTOS, secondaryContactDto);
                    break;
                case CONTACT_TWO:
                    SecondaryContactDto secondaryContactDtoTwo = getSecondaryContactDtoByPreference(participant.getSecondaryContacts(), SecondaryContactType.CONTACT_TWO.toString());
                    updateContactTwo(formFieldEntryDTOS, secondaryContactDtoTwo);
                    break;
                case SSN:
                    updateSSN(formFieldEntryDTOS, ssn);
                    break;
                default:
                    break;

            }

        }

        return formFieldEntryDTOS;
    }

    private void updateSSN(List<FormFieldEntryDTO> formFieldEntryDTOS, String ssn) {
        formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SSN_FIELD_ID, ssn));
    }

    private void updateContactTwo(List<FormFieldEntryDTO> formFieldEntryDTOS, SecondaryContactDto secondaryContactDto) {
        Map<String, String> secondaryContactStateRelationshipMap;
        if (secondaryContactDto != null) {
            secondaryContactStateRelationshipMap = getContactTwoStateRelationshipDRCMap();

            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_FIRST_NAME_FIELD_ID, secondaryContactDto.getFirstName()));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_MIDDLE_INITIAL_FIELD_ID, secondaryContactDto.getMiddleInitial()));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_LAST_NAME_FIELD_ID, secondaryContactDto.getLastName()));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_EMAIL_ADDRESS_FIELD_ID, getContactByType(secondaryContactDto.getContacts(), TypeEnum.EMAIL)));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_PHONE_NUMBER_FIELD_ID, getContactByType(secondaryContactDto.getContacts(), TypeEnum.PHONE)));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_RELATIONSHIP_FIELD_ID, findValueFromMap(secondaryContactStateRelationshipMap, secondaryContactDto.getRelationship())));

            if (!CollectionUtils.isEmpty(secondaryContactDto.getAddresses())) {
                AddressElementDto addressElementDto = secondaryContactDto.getAddresses().get(0);
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_ADDRESS_LINE_ONE_FIELD_ID, addressElementDto.getLine1()));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_ADDRESS_LINE_TWO_FIELD_ID, addressElementDto.getLine2()));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_CITY_FIELD_ID, addressElementDto.getCity()));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_STATE_FIELD_ID, findValueFromMap(secondaryContactStateRelationshipMap, addressElementDto.getState())));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_TWO_ZIP_FIELD_ID, addressElementDto.getPostalCode()));
            }

        }
    }

    private void updateContactOne(List<FormFieldEntryDTO> formFieldEntryDTOS, SecondaryContactDto secondaryContactDto) {
        Map<String, String> secondaryContactStateRelationshipMap;
        if (secondaryContactDto != null) {
            secondaryContactStateRelationshipMap = getContactOneStateRelationshipDRCMap();

            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_FIRST_NAME_FIELD_ID, secondaryContactDto.getFirstName()));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_MIDDLE_INITIAL_FIELD_ID, secondaryContactDto.getMiddleInitial()));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_LAST_NAME_FIELD_ID, secondaryContactDto.getLastName()));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_EMAIL_ADDRESS_FIELD_ID, getContactByType(secondaryContactDto.getContacts(), TypeEnum.EMAIL)));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_PHONE_NUMBER_FIELD_ID, getContactByType(secondaryContactDto.getContacts(), TypeEnum.PHONE)));
            formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_RELATIONSHIP_FIELD_ID, findValueFromMap(secondaryContactStateRelationshipMap, secondaryContactDto.getRelationship())));

            if (!CollectionUtils.isEmpty(secondaryContactDto.getAddresses())) {
                AddressElementDto addressElementDto = secondaryContactDto.getAddresses().get(0);
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_ADDRESS_LINE_ONE_FIELD_ID, addressElementDto.getLine1()));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_ADDRESS_LINE_TWO_FIELD_ID, addressElementDto.getLine2()));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_CITY_FIELD_ID, addressElementDto.getCity()));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_STATE_FIELD_ID, findValueFromMap(secondaryContactStateRelationshipMap, addressElementDto.getState())));
                formFieldEntryDTOS.add(addFormFieldEntries(FormFieldConstant.SECONDARY_CONTACT_ONE_ZIP_FIELD_ID, addressElementDto.getPostalCode()));

            }
        }
    }

    private SecondaryContactDto getSecondaryContactDtoByPreference(List<SecondaryContactDto> secondaryContacts, String preference) {
        Predicate<SecondaryContactDto> byPreference = secondaryElementDto -> (!(secondaryElementDto.getPreference().isEmpty()) && (secondaryElementDto.getPreference().equalsIgnoreCase(preference)));
        Optional<SecondaryContactDto> any = secondaryContacts.stream().filter(byPreference).findAny();
        return any.orElse(null);
    }

    private FormFieldEntryDTO addFormFieldEntries(Long formFieldId, String formFieldValue) {
        FormFieldEntryDTO formFieldEntry = new FormFieldEntryDTO();
        formFieldEntry.setFormFieldId(formFieldId);
        List<FormFieldEntryValueDTO> formFieldEntryValues = new ArrayList<>();
        FormFieldEntryValueDTO formFieldEntryValueDTO = new FormFieldEntryValueDTO();
        formFieldEntryValueDTO.setValueAsString(formFieldValue);
        formFieldEntryValues.add(formFieldEntryValueDTO);
        formFieldEntry.setFormFieldEntryValues(formFieldEntryValues);
        return formFieldEntry;
    }

    private String findValueFromMap(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value != null) {
            return value;
        }
        return key;
    }

    private String getContactByType(List<ContactElementDto> contactElementList, TypeEnum contactType) {
        String contact = null;
        Optional<ContactElementDto> optionalContactElementDto = Optional.empty();
        if (!CollectionUtils.isEmpty(contactElementList)) {
            Predicate<ContactElementDto> byContactType = contactElementDto -> (!contactElementDto.getContactType().toValue().isEmpty() && contactElementDto.getContactType().equals(contactType));
            optionalContactElementDto = contactElementList.stream().filter(byContactType).findFirst();
        }
        if (optionalContactElementDto.isPresent()) {
            contact = optionalContactElementDto.get().getContact();
        }
        return contact;
    }

}
