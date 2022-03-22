package com.vibrent.drc.converter;


import com.vibrent.acadia.domain.enumeration.SecondaryContactType;
import com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO;
import com.vibrent.vxp.push.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FormFieldEntryConverterTest {
    private static final String EXTERNAL_ID2 = "P1232374";
    private static final long VIBRENT_ID2 = 2000L;

    private FormFieldEntryConverter formFieldEntryConverter;
    Set<String> secondaryContactTypes;
    AccountInfoUpdateEventDto accountInfoUpdateEventDto;

    @BeforeEach
    void setUp() {
        formFieldEntryConverter = new FormFieldEntryConverter();
        secondaryContactTypes = new HashSet<>();
        accountInfoUpdateEventDto = getSecondaryContactUpdatedDto();

    }

    @Test
    void testGetFormFieldEntryDTOSContactOne() {
        secondaryContactTypes.add(SecondaryContactType.CONTACT_ONE.toString());
        List<FormFieldEntryDTO> formFieldEntryDTOS = formFieldEntryConverter.getFormFieldEntryDTOS(accountInfoUpdateEventDto.getParticipant(), secondaryContactTypes, null);
        assertEquals(11, formFieldEntryDTOS.size());
    }

    @Test
    void testGetFormFieldEntryDTOSContactTwo() {
        secondaryContactTypes.add(SecondaryContactType.CONTACT_TWO.toString());
        List<FormFieldEntryDTO> formFieldEntryDTOS = formFieldEntryConverter.getFormFieldEntryDTOS(accountInfoUpdateEventDto.getParticipant(), secondaryContactTypes, null);
        assertEquals(11, formFieldEntryDTOS.size());
    }

    @Test
    void testGetFormFieldEntryDTOSSN() {
        secondaryContactTypes.add("SSN");
        List<FormFieldEntryDTO> formFieldEntryDTOS = formFieldEntryConverter.getFormFieldEntryDTOS(accountInfoUpdateEventDto.getParticipant(), secondaryContactTypes, null);
        assertEquals(1, formFieldEntryDTOS.size());
    }

    @Test
    void testGetFormFieldEntryAll() {
        secondaryContactTypes.add(SecondaryContactType.CONTACT_ONE.toString());
        secondaryContactTypes.add(SecondaryContactType.CONTACT_TWO.toString());
        secondaryContactTypes.add("SSN");
        List<FormFieldEntryDTO> formFieldEntryDTOS = formFieldEntryConverter.getFormFieldEntryDTOS(accountInfoUpdateEventDto.getParticipant(), secondaryContactTypes, null);
        assertEquals(23, formFieldEntryDTOS.size());
    }


    private AccountInfoUpdateEventDto getSecondaryContactUpdatedDto() {
        var accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID(EXTERNAL_ID2);
        accountInfoUpdateEventDto.setVibrentID(VIBRENT_ID2);

        ParticipantDto participantDto = getParticipantDTO();
        participantDto.setExternalID(EXTERNAL_ID2);
        participantDto.setVibrentID(VIBRENT_ID2);
        participantDto.setFirstName("SomeFN");
        participantDto.setSecondaryContacts(getSecondaryContactDtoList());

        accountInfoUpdateEventDto.setParticipant(participantDto);
        return accountInfoUpdateEventDto;
    }

    private ParticipantDto getParticipantDTO() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID(EXTERNAL_ID2);
        participantDto.setFirstName("FN");
        participantDto.setMiddleInitial("L");
        participantDto.setLastName("LN");
        participantDto.setDateOfBirth("1989-01-02");
        participantDto.setTestUser(false);
        participantDto.setEmailAddress("a@b.com");
        participantDto.setLanguage(LanguageEnum.ENGLISH);
        participantDto.setAddresses(getUserAccountAddress());
        participantDto.setContacts(getContacts());
        return participantDto;
    }

    private List<SecondaryContactDto> getSecondaryContactDtoList() {
        List<SecondaryContactDto> secondaryContactDtoList = new ArrayList<>();
        SecondaryContactDto secondaryContactDto = new SecondaryContactDto();
        secondaryContactDto.setFirstName("SomeName");
        secondaryContactDto.setMiddleInitial("M");
        secondaryContactDto.setLastName("L");
        secondaryContactDto.setPreference("CONTACT_ONE");
        secondaryContactDto.setRelationship("partner");
        secondaryContactDto.setAddresses(getSecondaryUserAccountAddress());
        secondaryContactDto.setContacts(getContacts());

        SecondaryContactDto secondaryContactDto2 = new SecondaryContactDto();
        secondaryContactDto2.setFirstName("SomeName2");
        secondaryContactDto2.setMiddleInitial("N");
        secondaryContactDto2.setLastName("M");
        secondaryContactDto2.setPreference("CONTACT_TWO");
        secondaryContactDto2.setRelationship("relative");
        secondaryContactDto2.setAddresses(getSecondaryUserAccountAddress());
        secondaryContactDto2.setContacts(getContacts());

        secondaryContactDtoList.add(secondaryContactDto);
        secondaryContactDtoList.add(secondaryContactDto2);
        return secondaryContactDtoList;
    }

    private List<AddressElementDto> getSecondaryUserAccountAddress() {
        List<AddressElementDto> accountAddress = new ArrayList<>();
        AddressElementDto addressDto = new AddressElementDto();
        addressDto.setCity("Mesa");
        addressDto.setLine1("506Hudson");
        addressDto.setPostalCode("97202");
        addressDto.setState("OR");
        accountAddress.add(addressDto);
        return accountAddress;
    }

    private List<ContactElementDto> getContacts() {
        List<ContactElementDto> contactElementDtos = new ArrayList<>();
        ContactElementDto emailContact = new ContactElementDto();
        emailContact.setContactType(TypeEnum.EMAIL);
        emailContact.setContact("some@domain.com");
        emailContact.setNotifications(true);
        emailContact.setVerified(true);

        ContactElementDto phoneContact = new ContactElementDto();
        phoneContact.setContactType(TypeEnum.PHONE);
        phoneContact.setContact("5241634785");
        phoneContact.setVerified(true);
        phoneContact.setVerified(true);
        contactElementDtos.add(emailContact);
        contactElementDtos.add(phoneContact);
        return contactElementDtos;
    }

    private List<AddressElementDto> getUserAccountAddress() {
        List<AddressElementDto> accountAddress = new ArrayList<>();
        AddressElementDto addressDto = new AddressElementDto();
        addressDto.setCity("Mesa");
        addressDto.setLine1("506Hudson");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        addressDto.setAddressType(AddressTypeEnum.ACCOUNT_ADDRESS);
        accountAddress.add(addressDto);
        return accountAddress;
    }


}