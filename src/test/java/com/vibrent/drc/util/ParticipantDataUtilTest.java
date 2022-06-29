package com.vibrent.drc.util;

import com.vibrent.acadia.domain.enumeration.SecondaryContactType;
import com.vibrent.drc.converter.ParticipantConverter;
import com.vibrent.drc.converter.ParticipantConverterImpl;
import com.vibrent.drc.vo.AddressElementVo;
import com.vibrent.drc.vo.ParticipantVo;
import com.vibrent.drc.vo.SecondaryContactVo;
import com.vibrent.vxp.push.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ParticipantDataUtilTest {

    public static final String PHONE_NUMBER = "9123456789";
    public static final String MAIL_ID = "temp@gmail.com";

    ParticipantConverter participantConverter = new ParticipantConverterImpl();

    @Test
    void testIsSSNUpdated() {
        //Check both values are null
        assertFalse(ParticipantDataUtil.isSSNUpdated(null, null));

        //Check if no previously send available in DRC, then isSSNUpdated returns true
        assertTrue(ParticipantDataUtil.isSSNUpdated("ssn", null));

        //Check if no changes from the prevuosly send send data, then verify isSSNUpdatedreturns returns true
        var vo = buildParticipantVo();
        vo.setSsn("ssn");
        assertFalse(ParticipantDataUtil.isSSNUpdated("ssn", vo));

        //Check if SSN data is not available, then verify isSSNUpdated returns true
        assertTrue(ParticipantDataUtil.isSSNUpdated(null, buildParticipantVo()));

        //Check if SSN data is changes, then verify isSSNUpdated returns true
        assertTrue(ParticipantDataUtil.isSSNUpdated("ssn", buildParticipantVo()));
    }


    @Test
    void testIsUserAccountUpdated() {
        ParticipantDto dto = buildParticipantDto();
        //Check if no records are send to DRC, then isUserAccountUpdated returns true
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, null));

        //Check if no change in User Account data that are previously to DRC, then isUserAccountUpdated returns false
        ParticipantVo vo = buildParticipantVo();
        assertFalse(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if Phone number not available or different
        dto = buildParticipantDtoWithContact(List.of(buildContactDto(PHONE_NUMBER, TypeEnum.PHONE, false), buildContactDto(MAIL_ID, TypeEnum.EMAIL, true)));
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto = buildParticipantDtoWithContact(List.of(buildContactDto("9992224444", TypeEnum.PHONE, true), buildContactDto(MAIL_ID, TypeEnum.EMAIL, true)));
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if email number not available or different.
        dto = buildParticipantDtoWithContact(null);
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto = buildParticipantDtoWithContact(Collections.emptyList());
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto = buildParticipantDtoWithContact(List.of(buildContactDto(PHONE_NUMBER, TypeEnum.PHONE, true), buildContactDto(MAIL_ID, TypeEnum.EMAIL, false)));
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto = buildParticipantDtoWithContact(List.of(buildContactDto(PHONE_NUMBER, TypeEnum.PHONE, true), buildContactDto("a@gmail.com", TypeEnum.EMAIL, false)));
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if Address not available or different.
        dto = buildParticipantDtoWithAddress(null);
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto = buildParticipantDtoWithAddress(Collections.emptyList());
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto = buildParticipantDtoWithAddress(List.of(buildAddressDto(AddressTypeEnum.MAILING_ADDRESS)));
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if First name not available or different.
        dto = buildParticipantDto();
        dto.setFirstName(null);
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto.setFirstName("different Name");
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if Middle initial not available or different.
        dto = buildParticipantDto();
        dto.setMiddleInitial(null);
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto.setMiddleInitial("different Name");
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if Last name not available or different.
        dto = buildParticipantDto();
        dto.setLastName(null);
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto.setLastName("different Name");
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        //isUserAccountUpdated return true, if date of birth not available or different.
        dto = buildParticipantDto();
        dto.setDateOfBirth(null);
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));

        dto.setDateOfBirth("1998-01-01");
        assertTrue(ParticipantDataUtil.isUserAccountUpdated(dto, vo));
    }

    @Test
    void testSecondaryContactUpdated() {
        String ssn = null;
        Set<String> both = Set.of(SecondaryContactType.CONTACT_ONE.toString(), SecondaryContactType.CONTACT_TWO.toString());
        Set<String> contactOne = Set.of(SecondaryContactType.CONTACT_ONE.toString());
        Set<String> contactTwo = Set.of(SecondaryContactType.CONTACT_TWO.toString());

        var retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(new ParticipantVo(), new ParticipantDto(), null);
        assertEquals(0, retValue.size());

        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(new ParticipantVo(), new ParticipantDto(), "SSN");
        assertEquals(1, retValue.size());

        var dtoList = List.of(buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE), buildSecondaryContactDto(SecondaryContactType.CONTACT_TWO));
        var voList = convertToSecondaryContactVoMap(dtoList);
        ParticipantVo participantVo3 = new ParticipantVo();
        participantVo3.setSecondaryContacts(voList);
        ParticipantDto participantDto3 = new ParticipantDto();
        participantDto3.setSecondaryContacts(dtoList);
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo3, participantDto3, ssn);
        assertEquals(0, retValue.size());

        ParticipantVo participantVo = new ParticipantVo();
        participantVo.setSecondaryContacts(Collections.emptyMap());
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setSecondaryContacts(dtoList);
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo, participantDto, ssn);
        assertEquals(both, retValue);



        ParticipantVo participantVo1 = new ParticipantVo();
        participantVo1.setSecondaryContacts(voList);
        ParticipantDto participantDto1 = new ParticipantDto();
        participantDto1.setSecondaryContacts(Collections.emptyList());
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo1, participantDto1, ssn);
        assertEquals(both, retValue);


        ParticipantDto participantDto2 = new ParticipantDto();
        participantDto2.setSecondaryContacts(dtoList);
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(new ParticipantVo(), participantDto2, ssn);
        assertEquals(both, retValue);


        ParticipantVo participantVo2 = new ParticipantVo();
        participantVo2.setSecondaryContacts(voList);
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo2, new ParticipantDto(), ssn);
        assertEquals(both, retValue);

        voList = convertToSecondaryContactVoMap(dtoList);
        voList.remove(SecondaryContactType.CONTACT_ONE.toString());

        ParticipantVo participantVo5 = new ParticipantVo();
        participantVo5.setSecondaryContacts(voList);
        ParticipantDto participantDto5 = new ParticipantDto();
        participantDto5.setSecondaryContacts(dtoList);
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo5, participantDto5, ssn);
        assertEquals(contactOne, retValue);


        voList = convertToSecondaryContactVoMap(dtoList);
        voList.remove(SecondaryContactType.CONTACT_TWO.toString());
        ParticipantVo participantVo6 = new ParticipantVo();
        participantVo6.setSecondaryContacts(voList);
        ParticipantDto participantDto6 = new ParticipantDto();
        participantDto6.setSecondaryContacts(dtoList);
        retValue = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo6, participantDto6, ssn);
        assertEquals(contactTwo, retValue);
    }

    @Test
    void testAddressIsDifferent() {
        var dto = buildAddressDto(AddressTypeEnum.ACCOUNT_ADDRESS);
        var vo = participantConverter.toAddress(dto);

        assertFalse(ParticipantDataUtil.isDifferent(vo, dto));
        assertFalse(ParticipantDataUtil.isDifferent((AddressElementVo) null, null));


        vo = participantConverter.toAddress(dto);
        vo.setCity(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setCity("different City");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo = participantConverter.toAddress(dto);
        vo.setCountry(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setCountry("different Country");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo = participantConverter.toAddress(dto);
        vo.setLine1(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setLine1("different Line1");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo = participantConverter.toAddress(dto);
        vo.setLine2(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setLine2("different Line2");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo = participantConverter.toAddress(dto);
        vo.setPostalCode(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setPostalCode("different PostalCode");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo = participantConverter.toAddress(dto);
        vo.setState(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setState("different State");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo = participantConverter.toAddress(dto);
        vo.setValidated(!vo.getValidated());
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setValidated(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

    }

    @Test
    void testSecondaryContactIsDifferent() {
        var dto = buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE);
        var vo = participantConverter.toSecondaryContact(dto);

        assertFalse(ParticipantDataUtil.isDifferent(vo, dto));
        assertFalse(ParticipantDataUtil.isDifferent((SecondaryContactVo) null, null));

        assertTrue(ParticipantDataUtil.isDifferent(null, dto));
        assertTrue(ParticipantDataUtil.isDifferent(vo, null));

        // verify address is null or different
        vo = participantConverter.toSecondaryContact(dto);
        vo.setAddress(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        vo.setAddress(participantConverter.toAddress(buildAddressDto(AddressTypeEnum.MAILING_ADDRESS)));
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        // verify firstName is null or different
        vo = participantConverter.toSecondaryContact(dto);
        vo.setFirstName(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setFirstName("different first  name");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        // verify lastName is null or different
        vo = participantConverter.toSecondaryContact(dto);
        vo.setLastName(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setLastName("different last name");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        // verify middleInitial is null or different
        vo = participantConverter.toSecondaryContact(dto);
        vo.setMiddleInitial(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setMiddleInitial("different Middle Initial");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));


        // verify emailAddress is null or different
        vo = participantConverter.toSecondaryContact(dto);
        vo.setEmailAddress(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setEmailAddress("t@gmail.com");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));


        // verify phoneNumber is null or different
        vo = participantConverter.toSecondaryContact(dto);
        vo.setPhoneNumber(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        vo.setPhoneNumber("121-232-2323");
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        // verify dto has null address or no address
        dto = buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE);
        vo = participantConverter.toSecondaryContact(dto);
        dto.setAddresses(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        dto.setAddresses(Collections.emptyList());
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        // verify dto has null contacts or no contacts or missed contacts
        dto = buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE);
        vo = participantConverter.toSecondaryContact(dto);
        dto.setContacts(Arrays.asList(buildContactDto("tt@gmail.com", TypeEnum.EMAIL, false)));
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));

        dto.setContacts(null);
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
        dto.setContacts(Collections.emptyList());
        assertTrue(ParticipantDataUtil.isDifferent(vo, dto));
    }

    @Test
    void testGetContactFromSecondaryContact() {
        var secondaryContact = buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE);
        secondaryContact.getContacts().stream().filter(contactElementDto -> contactElementDto.getContactType() == TypeEnum.EMAIL)
                .forEach(contactElementDto -> contactElementDto.setContact(null));

        assertNull(ParticipantDataUtil.getContact(secondaryContact, TypeEnum.EMAIL));
        assertNotNull(ParticipantDataUtil.getContact(secondaryContact, TypeEnum.PHONE));
    }

    @Test
    void testGetVerifiedContact() {
        var participantDto = buildParticipantDto();
        participantDto.getContacts().stream().filter(contactElementDto -> contactElementDto.getContactType() == TypeEnum.PHONE)
                .forEach(contactElementDto -> contactElementDto.setContact(null));

        assertNotNull(ParticipantDataUtil.getVerifiedContact(participantDto, TypeEnum.EMAIL));
        assertNull(ParticipantDataUtil.getVerifiedContact(participantDto, TypeEnum.PHONE));
    }

    @Test
    void testIsTestFlagUpdated() {
        ParticipantDto participantDto = buildParticipantDto();
        ParticipantVo participantVo = buildParticipantVo();

        assertTrue(ParticipantDataUtil.isTestFlagUpdated(participantDto, null));

        participantDto.setTestUser(true);
        participantVo.setTestUser(false);
        assertTrue(ParticipantDataUtil.isTestFlagUpdated(participantDto, participantVo));

        participantDto.setTestUser(false);
        participantVo.setTestUser(true);
        assertTrue(ParticipantDataUtil.isTestFlagUpdated(participantDto, participantVo));

        participantDto.setTestUser(true);
        participantVo.setTestUser(true);
        assertFalse(ParticipantDataUtil.isTestFlagUpdated(participantDto, participantVo));

        participantDto.setTestUser(false);
        participantVo.setTestUser(false);
        assertFalse(ParticipantDataUtil.isTestFlagUpdated(participantDto, participantVo));

        participantDto.setTestUser(null);
        participantVo.setTestUser(null);
        assertFalse(ParticipantDataUtil.isTestFlagUpdated(participantDto, participantVo));
    }

    private ParticipantDto buildParticipantDto() {
        var contactDtos = Arrays.asList(buildContactDto(MAIL_ID, TypeEnum.EMAIL, true), buildContactDto(PHONE_NUMBER, TypeEnum.PHONE, true));
        List<AddressElementDto> address = Arrays.asList(buildAddressDto(AddressTypeEnum.ACCOUNT_ADDRESS), buildAddressDto(AddressTypeEnum.MAILING_ADDRESS));
        List<SecondaryContactDto> secondaryContactDtos = Arrays.asList(buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE), buildSecondaryContactDto(SecondaryContactType.CONTACT_TWO));
        return buildParticipantDtoWithAddress(contactDtos, address, secondaryContactDtos);
    }

    private ParticipantDto buildParticipantDtoWithContact(List<ContactElementDto> contactElementDtos) {
        List<AddressElementDto> address = Arrays.asList(buildAddressDto(AddressTypeEnum.ACCOUNT_ADDRESS), buildAddressDto(AddressTypeEnum.MAILING_ADDRESS));
        List<SecondaryContactDto> secondaryContactDtos = Arrays.asList(buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE), buildSecondaryContactDto(SecondaryContactType.CONTACT_TWO));
        return buildParticipantDtoWithAddress(contactElementDtos, address, secondaryContactDtos);
    }

    private ParticipantDto buildParticipantDtoWithAddress(List<AddressElementDto> address) {
        var contactDtos = Arrays.asList(buildContactDto(MAIL_ID, TypeEnum.EMAIL, true), buildContactDto(PHONE_NUMBER, TypeEnum.PHONE, true));
        List<SecondaryContactDto> secondaryContactDtos = Arrays.asList(buildSecondaryContactDto(SecondaryContactType.CONTACT_ONE), buildSecondaryContactDto(SecondaryContactType.CONTACT_TWO));
        return buildParticipantDtoWithAddress(contactDtos, address, secondaryContactDtos);
    }


    ParticipantDto buildParticipantDtoWithAddress(List<ContactElementDto> contacts, List<AddressElementDto> accountAddress, List<SecondaryContactDto> secondaryContacts) {
        ParticipantDto dto = new ParticipantDto();
        dto.setEmailAddress(MAIL_ID);
        dto.setPhoneNumber(PHONE_NUMBER);
        dto.setAddresses(accountAddress);
        dto.setContacts(contacts);
        dto.setFirstName("first");
        dto.setMiddleInitial("m");
        dto.setLastName("last");
        dto.setDateOfBirth("2020-02-02");
        dto.setSecondaryContacts(secondaryContacts);
        dto.setTestUser(true);
        dto.setVibrentID(1L);
        return dto;
    }


    AddressElementDto buildAddressDto(AddressTypeEnum type) {
        AddressElementDto dto = new AddressElementDto();
        dto.setAddressType(type);
        dto.setCity("city");
        dto.setCountry("country");
        dto.setLine1("line1 " + type);
        dto.setLine2("line2 " + type);
        dto.setPostalCode("333222");
        dto.setState("state");
        dto.setUpdatedOn(System.currentTimeMillis());
        dto.setValidated(true);
        return dto;
    }

    SecondaryContactDto buildSecondaryContactDto(SecondaryContactType type) {
        SecondaryContactDto dto = new SecondaryContactDto();
        dto.setAddresses(List.of(buildAddressDto(AddressTypeEnum.ACCOUNT_ADDRESS)));
        dto.setContacts(Arrays.asList(buildContactDto(PHONE_NUMBER, TypeEnum.PHONE, true),
                buildContactDto(MAIL_ID, TypeEnum.EMAIL, false)));
        dto.setFirstName("firstName-" + type);
        dto.setLastName("lastName-" + type);
        dto.setMiddleInitial("m");
        dto.setPreference(type.toString());
        dto.setRelationship("relationship");

        return dto;
    }

    ContactElementDto buildContactDto(String contact, TypeEnum type, boolean verified) {
        ContactElementDto contactDto = new ContactElementDto();
        contactDto.setContact(contact);
        contactDto.setContactType(type);
        contactDto.setVerified(verified);
        return contactDto;
    }


    //VO
    private ParticipantVo buildParticipantVo() {
        ParticipantVo participantVo = new ParticipantVo();
        participantVo.setSsn("SSN-updated");
        participantConverter.updateParticipant(buildParticipantDto(), participantVo);
        return participantVo;
    }

    private Map<String, SecondaryContactVo> convertToSecondaryContactVoMap(List<SecondaryContactDto> secondaryContactDtos) {
        Map<String, SecondaryContactVo> secondaryContactVoMap = new HashMap<>();
        if (secondaryContactDtos != null) {
            for (var contact : secondaryContactDtos) {
                secondaryContactVoMap.put(contact.getPreference(), participantConverter.toSecondaryContact(contact));
            }
        }
        return secondaryContactVoMap;
    }
}
