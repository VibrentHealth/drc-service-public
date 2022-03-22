package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.drc.converter.ParticipantConverter;
import com.vibrent.drc.converter.ParticipantConverterImpl;
import com.vibrent.drc.domain.DrcSyncedStatus;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.repository.DrcSyncedStatusRepository;
import com.vibrent.drc.service.AccountInfoUpdateEventHelperService;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.vo.ParticipantVo;
import com.vibrent.drc.vo.SecondaryContactVo;
import com.vibrent.vxp.push.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Transactional
@ExtendWith(MockitoExtension.class)
class AccountInfoUpdateEventHelperServiceImplUnitTest {

    private static final String EXTERNAL_ID2 = "P1232374";
    private static final long VIBRENT_ID2 = 2000L;


    private AccountInfoUpdateEventHelperService accountInfoUpdateEventHelperService;

    @Mock
    DrcSyncedStatusRepository drcSyncedStatusRepository;

    ParticipantConverter participantConverter;

    @Mock
    ApiService apiService;

    @BeforeEach
    public void setUp() {
        participantConverter = new ParticipantConverterImpl();
        accountInfoUpdateEventHelperService = new AccountInfoUpdateEventHelperServiceImpl(drcSyncedStatusRepository, participantConverter, apiService);
    }

    @Test
    void processIfUserSecondaryContactUpdated() {
        when(apiService.getUserSsnByUserId(VIBRENT_ID2)).thenReturn(getUserSSNDTO());
        AccountInfoUpdateEventDto secondaryContactUpdatedDto = getSecondaryContactUpdatedDto("someName1", "SomeLastName1", "CONTACT_ONE");
        accountInfoUpdateEventHelperService.processIfUserSecondaryContactOrSSNUpdated(secondaryContactUpdatedDto, (ssn, secondaryContactChanges) -> true);
        verify(drcSyncedStatusRepository, Mockito.times(1)).save(ArgumentMatchers.any(DrcSyncedStatus.class));

    }

    @DisplayName("When Secondary Contact ONE is Updated " +
            "Then verify DB updated with latest entry")
    @Test
    void processIfUserSecondaryContactOneUpdated() throws JsonProcessingException {
        when(drcSyncedStatusRepository.findByVibrentIdAndType(VIBRENT_ID2, DataTypeEnum.ACCOUNT_UPDATE_DATA)).thenReturn(getFirstEntry());
        when(apiService.getUserSsnByUserId(VIBRENT_ID2)).thenReturn(null);
        AccountInfoUpdateEventDto secondaryContactUpdatedDto = getSecondaryContactUpdatedDto("changedFirstName", "changedLastName", "CONTACT_ONE");
        accountInfoUpdateEventHelperService.processIfUserSecondaryContactOrSSNUpdated(secondaryContactUpdatedDto, (ssn, secondaryContactChanges) -> true);

        ArgumentCaptor<DrcSyncedStatus> argumentCaptor = ArgumentCaptor.forClass(DrcSyncedStatus.class);
        verify(drcSyncedStatusRepository, Mockito.times(1)).save(argumentCaptor.capture());
        DrcSyncedStatus entry = argumentCaptor.getValue();
        assertNotNull(entry);
        ParticipantVo participantVo = JacksonUtil.getMapper().readValue(entry.getData(), ParticipantVo.class);
        SecondaryContactVo updatedContactOne = participantVo.getSecondaryContacts().get("CONTACT_ONE");

        assertNotNull(updatedContactOne);
        assertEquals("changedFirstName", updatedContactOne.getFirstName());
        assertEquals("changedLastName", updatedContactOne.getLastName());

    }

    @DisplayName("When Secondary Contact TWO is Updated " +
            "Then verify DB updated with latest entry")
    @Test
    void processIfUserSecondaryContactTwoUpdated() throws JsonProcessingException {
        when(drcSyncedStatusRepository.findByVibrentIdAndType(VIBRENT_ID2, DataTypeEnum.ACCOUNT_UPDATE_DATA)).thenReturn(getSecondEntry());
        when(apiService.getUserSsnByUserId(VIBRENT_ID2)).thenReturn(null);
        AccountInfoUpdateEventDto secondaryContactUpdatedDto = getSecondaryContactUpdatedDto("changedFirstName", "changedLastName", "CONTACT_TWO");
        accountInfoUpdateEventHelperService.processIfUserSecondaryContactOrSSNUpdated(secondaryContactUpdatedDto, (ssn, secondaryContactChanges) -> true);

        ArgumentCaptor<DrcSyncedStatus> argumentCaptor = ArgumentCaptor.forClass(DrcSyncedStatus.class);
        verify(drcSyncedStatusRepository, Mockito.times(1)).save(argumentCaptor.capture());
        DrcSyncedStatus entry = argumentCaptor.getValue();

        assertNotNull(entry);
        ParticipantVo participantVo = JacksonUtil.getMapper().readValue(entry.getData(), ParticipantVo.class);
        SecondaryContactVo updatedContactTwo = participantVo.getSecondaryContacts().get("CONTACT_TWO");

        assertNotNull(updatedContactTwo);
        assertEquals("changedFirstName", updatedContactTwo.getFirstName());
        assertEquals("changedLastName", updatedContactTwo.getLastName());

    }

    DrcSyncedStatus getFirstEntry() {
        DrcSyncedStatus drcSyncedStatus = new DrcSyncedStatus();
        drcSyncedStatus.setData("{\"lastName\": \"Ram\", \"firstName\": \"Jay\", \"vibrentId\": 2000, \"dateOfBirth\": \"2000-02-05\", \"emailAddress\": \"t3@gmail.com\", \"middleInitial\": \"M\", \"accountAddress\": {\"city\": \"Portland\", \"line1\": \"2585 Se 14th Ave\", \"state\": \"OR\", \"country\": \"US\", \"postalCode\": \"97202\"}, \"secondaryContacts\": {\"CONTACT_ONE\": {\"address\": {\"city\": \"Portland\", \"line1\": \"2585 Se 14th Ave\", \"state\": \"PIIState_AR\", \"postalCode\": \"97202\"}, \"lastName\": \"Ram\", \"firstName\": \"R\", \"preference\": \"CONTACT_ONE\", \"phoneNumber\": \"9797979685\", \"middleInitial\": \"G\"}}}");
        drcSyncedStatus.setVibrentId(2000L);
        drcSyncedStatus.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);

        return drcSyncedStatus;
    }


    DrcSyncedStatus getSecondEntry() {
        DrcSyncedStatus drcSyncedStatus = new DrcSyncedStatus();
        drcSyncedStatus.setData("{\"lastName\": \"Ram\", \"firstName\": \"Jay\", \"vibrentId\": 2000, \"dateOfBirth\": \"2000-02-05\", \"emailAddress\": \"t3@gmail.com\", \"middleInitial\": \"M\", \"accountAddress\": {\"city\": \"Portland\", \"line1\": \"2585 Se 14th Ave\", \"state\": \"OR\", \"country\": \"US\", \"postalCode\": \"97202\"}, \"secondaryContacts\": {\"CONTACT_TWO\": {\"address\": {\"city\": \"Portland\", \"line1\": \"2585 Se 14th Ave\", \"state\": \"PIIState_AR\", \"postalCode\": \"97202\"}, \"lastName\": \"Ram\", \"firstName\": \"R\", \"preference\": \"CONTACT_ONE\", \"phoneNumber\": \"9797979685\", \"middleInitial\": \"G\"}}}");
        drcSyncedStatus.setVibrentId(2000L);
        drcSyncedStatus.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);

        return drcSyncedStatus;
    }


    private AccountInfoUpdateEventDto getSecondaryContactUpdatedDto(String name, String lastName, String preferance) {
        var accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID(EXTERNAL_ID2);
        accountInfoUpdateEventDto.setVibrentID(VIBRENT_ID2);

        ParticipantDto participantDto = getParticipantDTO();
        participantDto.setExternalID(EXTERNAL_ID2);
        participantDto.setVibrentID(VIBRENT_ID2);
        participantDto.setFirstName("SomeFN");
        participantDto.setSecondaryContacts(getSecondaryContactDtoList(name, lastName, preferance));
        participantDto.setHasSSN(true);


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

    private List<SecondaryContactDto> getSecondaryContactDtoList(String name, String lastName, String preference) {
        List<SecondaryContactDto> secondaryContactDtoList = new ArrayList<>();
        SecondaryContactDto secondaryContactDto = new SecondaryContactDto();
        secondaryContactDto.setFirstName(name);
        secondaryContactDto.setMiddleInitial("M");
        secondaryContactDto.setLastName(lastName);
        secondaryContactDto.setPreference(preference);
        secondaryContactDto.setRelationship("partner");
        secondaryContactDto.setAddresses(getSecondaryUserAccountAddress());
        secondaryContactDto.setContacts(getContacts());

        secondaryContactDtoList.add(secondaryContactDto);

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

    UserSSNDTO getUserSSNDTO() {
        UserSSNDTO userSSNDTO = new UserSSNDTO();
        userSSNDTO.setUserId(VIBRENT_ID2);
        userSSNDTO.setSsn("222222222");
        return userSSNDTO;
    }

}