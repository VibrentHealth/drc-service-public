package com.vibrent.drc.integration;

import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.drc.converter.ParticipantConverter;
import com.vibrent.drc.domain.DrcSyncedStatus;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.repository.DrcSyncedStatusRepository;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.impl.AccountInfoUpdateEventHelperServiceImpl;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.ParticipantDataUtil;
import com.vibrent.drc.vo.AddressElementVo;
import com.vibrent.drc.vo.ParticipantVo;
import com.vibrent.vxp.push.*;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@Category(IntegrationTest.class)
@Transactional
public class AccountInfoUpdateEventHelperServiceImplTest extends IntegrationTest {
    private static final String EXTERNAL_ID = "P1232322";
    private static final long VIBRENT_ID = 1000L;
    private static final String EXTERNAL_ID2 = "P1232374";
    private static final long VIBRENT_ID2 = 2000L;

    @Inject
    private AccountInfoUpdateEventHelperServiceImpl accountInfoUpdateEventHelperService;

    @Inject
    DrcSyncedStatusRepository drcSyncedStatusRepository;

    @Inject
    private ParticipantConverter participantConverter;

    @Mock
    ApiService apiService;

    @Before
    public void setUp(){
        ReflectionTestUtils.setField(accountInfoUpdateEventHelperService, "apiService", apiService);
    }

    @SneakyThrows
    @Test
    public void whenNewUserAccountInfoReceivedThenVerifyDbUpdatedAfterSuccessfulSendToDrc() {
        var accountInfoUpdateDto = getAccountInfoUpdateDto();
        //Check there are no entries in db
        DrcSyncedStatus initialEntry = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        assertNull(initialEntry);

        accountInfoUpdateEventHelperService.processIfUserAccountUpdated(accountInfoUpdateDto, () -> true);

        DrcSyncedStatus entry = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertNotNull(entry.getData());
        assertNotNull(entry.getVibrentId());
        assertNotNull(entry.getCreatedOn());
        assertNotNull(entry.getUpdatedOn());
        assertEquals(accountInfoUpdateDto.getVibrentID(), entry.getVibrentId());

        ParticipantVo participantVo = JacksonUtil.getMapper().readValue(entry.getData(), ParticipantVo.class);
        assertNotNull(participantVo);
        assertNull(participantVo.getSecondaryContacts());
        assertNull(participantVo.getSsn());
        var participantDto = accountInfoUpdateDto.getParticipant();
        assertEquals(participantDto.getDateOfBirth(), participantVo.getDateOfBirth());
        assertEquals(participantDto.getFirstName(), participantVo.getFirstName());
        assertEquals(participantDto.getLastName(), participantVo.getLastName());
        assertEquals(participantDto.getMiddleInitial(), participantVo.getMiddleInitial());
        assertEquals(ParticipantDataUtil.getContactByTypeAndVerification(participantDto, TypeEnum.EMAIL, Boolean.TRUE), participantVo.getEmailAddress());
        assertEquals(ParticipantDataUtil.getContactByTypeAndVerification(participantDto, TypeEnum.PHONE, Boolean.TRUE), participantVo.getVerifiedPhoneNumber());
        verifyAddress(ParticipantDataUtil.getAccountAddress(participantDto), participantVo.getAccountAddress());

        //Verify database is not updated if there is no change in user Account information.
        accountInfoUpdateEventHelperService.processIfUserAccountUpdated(accountInfoUpdateDto, () -> true);
        DrcSyncedStatus newEntry = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);

        //Check no change in DB
        assertEquals(entry.getId(), newEntry.getId());
        assertEquals(entry.getVibrentId(), newEntry.getVibrentId());
        assertEquals(entry.getData(), newEntry.getData());
        assertEquals(entry.getUpdatedOn(), newEntry.getUpdatedOn());
        assertEquals(entry.getCreatedOn(), newEntry.getCreatedOn());

    }



    @SneakyThrows
    @Test
    public void whenNewUserAccountInfoReceivedThenVerifyDbUpdatedAfterSuccessfulSendToDrcForTestParticipant() {
        var accountInfoUpdateDto = getAccountInfoUpdateDto();
        //Check there are no entries in db
        DrcSyncedStatus initialEntry = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        assertNull(initialEntry);

        accountInfoUpdateEventHelperService.processIfTestParticipantUpdated(accountInfoUpdateDto, () -> true);

        DrcSyncedStatus entry = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertNotNull(entry.getData());
        assertNotNull(entry.getVibrentId());
        assertNotNull(entry.getCreatedOn());
        assertNotNull(entry.getUpdatedOn());
        assertEquals(accountInfoUpdateDto.getVibrentID() ,entry.getVibrentId());

        ParticipantVo participantVo = JacksonUtil.getMapper().readValue(entry.getData(), ParticipantVo.class);
        assertNotNull(participantVo);
        assertNotNull(participantVo.getTestUser());

        //Verify database is not updated if there is no change in test participant flag
        accountInfoUpdateEventHelperService.processIfTestParticipantUpdated(accountInfoUpdateDto, () -> true);
        DrcSyncedStatus newEntry = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);

        //Check no change in DB
        assertEquals(entry.getId(), newEntry.getId());
        assertEquals(entry.getVibrentId(), newEntry.getVibrentId());
        assertEquals(entry.getData(), newEntry.getData());
        assertEquals(entry.getUpdatedOn(), newEntry.getUpdatedOn());
        assertEquals(entry.getCreatedOn(), newEntry.getCreatedOn());
    }

    void verifyAddress(AddressElementDto dto, AddressElementVo vo) {
        assertEquals(dto.getCity(), vo.getCity());
        assertEquals(dto.getCountry(), vo.getCountry());
        assertEquals(dto.getLine1(), vo.getLine1());
        assertEquals(dto.getLine2(), vo.getLine2());
        assertEquals(dto.getPostalCode(), vo.getPostalCode());
        assertEquals(dto.getState(), vo.getState());
        assertEquals(dto.getValidated(), vo.getValidated());
    }

    //private functions
    private AccountInfoUpdateEventDto getAccountInfoUpdateDto() {
        var accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID(EXTERNAL_ID);
        accountInfoUpdateEventDto.setVibrentID(VIBRENT_ID);

        ParticipantDto participantDto = getParticipantDTO();
        participantDto.setExternalID(EXTERNAL_ID);
        participantDto.setVibrentID(VIBRENT_ID);
        participantDto.setFirstName("FN");
        participantDto.setTestUser(false);

        accountInfoUpdateEventDto.setParticipant(participantDto);
        return accountInfoUpdateEventDto;
    }

    private AccountInfoUpdateEventDto getSecondaryContactUpdatedDto(String name, String lastName, String name2, String lastName2,boolean hasSsn) {
        var accountInfoUpdateEventDto = new AccountInfoUpdateEventDto();
        accountInfoUpdateEventDto.setExternalID(EXTERNAL_ID2);
        accountInfoUpdateEventDto.setVibrentID(VIBRENT_ID2);

        ParticipantDto participantDto = getParticipantDTO();
        participantDto.setExternalID(EXTERNAL_ID2);
        participantDto.setVibrentID(VIBRENT_ID2);
        participantDto.setFirstName("SomeFN");
        participantDto.setSecondaryContacts(getSecondaryContactDtoList(name, lastName, name2, lastName2));
        participantDto.setHasSSN(hasSsn);

        accountInfoUpdateEventDto.setParticipant(participantDto);
        return accountInfoUpdateEventDto;
    }


    private ParticipantDto getParticipantDTO() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID(EXTERNAL_ID);
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

    private List<SecondaryContactDto> getSecondaryContactDtoList(String name, String lastName, String name2, String lastName2) {
        List<SecondaryContactDto> secondaryContactDtoList = new ArrayList<>();
        SecondaryContactDto secondaryContactDto = new SecondaryContactDto();
        secondaryContactDto.setFirstName(name);
        secondaryContactDto.setMiddleInitial("M");
        secondaryContactDto.setLastName(lastName);
        secondaryContactDto.setPreference("CONTACT_ONE");
        secondaryContactDto.setRelationship("partner");
        secondaryContactDto.setAddresses(getSecondaryUserAccountAddress());
        secondaryContactDto.setContacts(getContacts());

        SecondaryContactDto secondaryContactDto2 = new SecondaryContactDto();
        secondaryContactDto2.setFirstName(name2);
        secondaryContactDto2.setMiddleInitial("N");
        secondaryContactDto2.setLastName(lastName2);
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

    UserSSNDTO getUserSSNDTO(){
        UserSSNDTO  userSSNDTO = new UserSSNDTO();
        userSSNDTO.setUserId(VIBRENT_ID2);
        userSSNDTO.setSsn("222222222");
        return userSSNDTO;
    }

    DrcSyncedStatus getDrcSyncedStatus() {
        String data = "{\"ssn\": \"111111111\", \"hasSSN\": true, \"lastName\": \"K\", \"firstName\": \"R\", \"vibrentId\": 2000, \"dateOfBirth\": \"1987-02-05\", \"emailAddress\": \"test1@gmail.com\", \"middleInitial\": \"G\", \"accountAddress\": {\"city\": \"Arizona\", \"line1\": \"Address 1\", \"state\": \"AZ\", \"country\": \"US\", \"postalCode\": \"85001\"}, \"secondaryContacts\": {\"CONTACT_ONE\": {\"address\": {\"city\": \"Portland\", \"line1\": \"HN 856\", \"line2\": \"L Street\", \"state\": \"PIIState_OR\", \"postalCode\": \"97202\"}, \"lastName\": \"Ram\", \"firstName\": \"Pank\", \"preference\": \"CONTACT_ONE\", \"phoneNumber\": \"9797979685\", \"middleInitial\": \"L\"}}}";
        DrcSyncedStatus drcSyncedStatus = new DrcSyncedStatus();
        drcSyncedStatus.setId(1L);
        drcSyncedStatus.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);
        drcSyncedStatus.setVibrentId(VIBRENT_ID2);
        drcSyncedStatus.setData(data);
        return drcSyncedStatus;
    }
}
