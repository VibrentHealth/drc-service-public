package com.vibrent.drc.service.impl;

import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.repository.DRCUpdateInfoSyncRetryRepository;
import com.vibrent.drc.service.SyncRetryHelperService;
import com.vibrent.vxp.push.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SyncRetryHelperServiceImplTest {
    private static final String EXTERNAL_ID = "P1232322";
    private static final long VIBRENT_ID = 1000L;
    public static String ssn = "123456789";


    private SyncRetryHelperService syncRetryHelperService;

    private AccountInfoUpdateEventDto accountInfoUpdateEventDto;

    @Mock
    private DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository;

    @BeforeEach
    public void setUp() {
        syncRetryHelperService = new SyncRetryHelperServiceImpl(drcUpdateInfoSyncRetryRepository);
        initializeAccountInfoUpdateDto();
    }


    @DisplayName("When addToRetryQueue is called  " +
            "Then verify retry count.")
    @Test
    void testAddToRetryQueue() {

        syncRetryHelperService.addToRetryQueue(accountInfoUpdateEventDto, true, "Reason for Exception");

        ArgumentCaptor<DRCUpdateInfoSyncRetry> captor = ArgumentCaptor.forClass(DRCUpdateInfoSyncRetry.class);
        verify(drcUpdateInfoSyncRetryRepository, times(1)).save(captor.capture());
        DRCUpdateInfoSyncRetry entry = captor.getValue();
        assertEquals(1, entry.getRetryCount());
    }

    @DisplayName("When addToRetryQueue is called Without IncrementRetryCounter  " +
            "Then verify retry count.")
    @Test
    void testAddToRetryQueueWithoutIncrementRetryCounter() {

        syncRetryHelperService.addToRetryQueue(accountInfoUpdateEventDto, false, "Reason for Exception");

        ArgumentCaptor<DRCUpdateInfoSyncRetry> captor = ArgumentCaptor.forClass(DRCUpdateInfoSyncRetry.class);
        verify(drcUpdateInfoSyncRetryRepository, times(1)).save(captor.capture());
        DRCUpdateInfoSyncRetry entry = captor.getValue();
        assertEquals(0, entry.getRetryCount());
    }

    @DisplayName("When deleteByVibrentIdAndType is called  " +
            "Then record should be deleted")
    @Test
    void testDeleteByVibrentIdAndType() {

        syncRetryHelperService.deleteByVibrentIdAndType(VIBRENT_ID, DataTypeEnum.ACCOUNT_UPDATE_DATA);
        verify(drcUpdateInfoSyncRetryRepository, times(1)).deleteByVibrentIdAndType(anyLong(), any());
    }


    //private functions
    private void initializeAccountInfoUpdateDto() {
        accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID(EXTERNAL_ID);
        accountInfoUpdateEventDto.setVibrentID(VIBRENT_ID);

        ParticipantDto participantDto = getParticipantDTO();
        participantDto.setExternalID(EXTERNAL_ID);
        participantDto.setVibrentID(VIBRENT_ID);
        participantDto.setFirstName("SomeFName");
        participantDto.setSecondaryContacts(getSecondaryContactDtoList());

        accountInfoUpdateEventDto.setParticipant(participantDto);
    }


    private ParticipantDto getParticipantDTO() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID(EXTERNAL_ID);
        participantDto.setFirstName("SomeFName");
        participantDto.setMiddleInitial("L");
        participantDto.setLastName("SomeLastName");
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
        addressDto.setLine1("6644 E Baywood Ave");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        addressDto.setAddressType(AddressTypeEnum.ACCOUNT_ADDRESS);
        accountAddress.add(addressDto);
        return accountAddress;
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
}