package com.vibrent.drc.service.impl;

import com.vibrent.acadia.domain.enumeration.AddressType;
import com.vibrent.acadia.domain.enumeration.FormComponentFieldType;
import com.vibrent.acadia.domain.enumeration.SecondaryContactType;
import com.vibrent.acadia.web.rest.dto.UserAddressDTO;
import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.UserPreferencesDTO;
import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.acadia.web.rest.dto.form.*;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.OptionsValue;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.converter.FormEntryConverter;
import com.vibrent.drc.converter.FormFieldEntryConverter;
import com.vibrent.drc.service.AccountInfoUpdateEventService;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.service.DRCBackendProcessorWrapper;
import com.vibrent.drc.service.ExternalApiRequestLogsService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.push.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import com.vibrenthealth.drcutils.service.DRCRetryService;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vibrent.drc.constants.DrcConstant.ROLE_CATI;
import static com.vibrent.drc.constants.ProfileAccountConstants.FORM_NAME_CONSENT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountInfoUpdateEventServiceImplTest {
    private static final String EXTERNAL_ID = "P1232322";
    private static final long VIBRENT_ID = 1000L;
    private static final long CATI_USER_ID = 212121L;
    private static final String FORM_NAME_BASICS = "TheBasics";
    private static final Set<String> authorities = Stream.of("ROLE_USER").collect(Collectors.toCollection(HashSet::new));
    public static String FORM_ENTRY_DTO_SECONDARY_CONTACT = "testData/form-version-secondory-contact1.json";
    public static String FORM_ENTRY_DTO_SECONDARY_CONTACT_TWO = "testData/form-version-dto-secondory-contact2.json";
    public static String ACCOUNT_INFO_UPDATE_EVENT_DTO = "testData/account-info-event-dto.json";
    public static String ACCOUNT_INFO_UPDATE_EVENT_DTO_TWO = "testData/account-info-update-event-dto-contact2.json";
    public static String ssn = "123456789";

    Set<String> secondaryContactChanges;

    @Mock
    private ApiService apiService;

    private DRCBackendProcessorWrapper drcBackendProcessorWrapper;

    @Mock
    private DrcProperties drcProperties;

    @Mock
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    @Mock
    private DRCBackendProcessorService drcBackendProcessorService;

    private FormFieldEntryConverter formFieldEntryConverter;

    private FormEntryConverter formEntryConverter;

    private DRCRetryService retryService;

    @Mock
    DRCConfigService drcConfigService;

    private AccountInfoUpdateEventService accountInfoUpdateEventService;

    private boolean drcImpersonation = false;

    private AccountInfoUpdateEventDto accountInfoUpdateEventDto;
    private UserDTO userDTO;

    List<FormEntryDTO> entries = new ArrayList<>();

    @Mock
    private AccountInfoUpdateEventHelperServiceImpl accountInfoUpdateEventHelperService;

    @BeforeEach
    public void setUp() {
        formFieldEntryConverter = new FormFieldEntryConverter();
        formEntryConverter = new FormEntryConverter(apiService, drcProperties, formFieldEntryConverter);
        retryService = new DRCRetryServiceImpl(drcConfigService);
        drcBackendProcessorWrapper = new DRCBackendProcessorWrapperImpl(externalApiRequestLogsService, drcBackendProcessorService);
        accountInfoUpdateEventService = new AccountInfoUpdateEventServiceImpl(apiService, drcBackendProcessorWrapper, drcProperties, retryService, accountInfoUpdateEventHelperService, formEntryConverter);
        initializeAccountInfoUpdateDto();
        initializeUserDTO();
        initializeFormEntryDTO(FORM_NAME_CONSENT);
        initializeFormEntryDTOBasics(FORM_NAME_BASICS, VIBRENT_ID);
        formVersionDTO();
        secondaryContactChanges = new HashSet<>();
    }

    @DisplayName("When account info updated" +
            "And Consent Form entry is submitted" +
            "Then verify account information is sent.")
    @Test
    void sendAccountInfoUpdates() throws Exception {
        when(this.apiService.getFormEntryDtoByFormName(VIBRENT_ID, FORM_NAME_CONSENT)).thenReturn(initializeFormEntryDTO());
        when(this.apiService.getFormVersionById(2L)).thenReturn(formVersionDTO());
        when(drcProperties.getDrcApiBaseUrl()).thenReturn("https://pmi-drc-api-test.appspot.com");
        when(drcConfigService.getRetryNum()).thenReturn(1L);
        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenReturn(getDrcResponse());

        assertTrue(accountInfoUpdateEventService.sendAccountInfoUpdates(accountInfoUpdateEventDto));
    }

    @DisplayName("When account info updated" +
            "And Consent Form entry is submitted" +
            "And form is submitted by cati user" +
            "Then verify account information is sent.")
    @Test
    void sendAccountInfoUpdatesWhenUserIsCati() throws Exception {
        when(this.apiService.getFormEntryDtoByFormName(VIBRENT_ID, FORM_NAME_CONSENT)).thenReturn(initializeFormEntryDTO());
        when(this.apiService.getFormVersionById(2L)).thenReturn(formVersionDTO());
        when(this.apiService.getUserDTO(CATI_USER_ID)).thenReturn(getCatiUser(CATI_USER_ID));
        when(drcProperties.getDrcApiBaseUrl()).thenReturn("https://pmi-drc-api-test.appspot.com");
        when(drcConfigService.getRetryNum()).thenReturn(1L);
        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenReturn(getDrcResponse());

        assertTrue(accountInfoUpdateEventService.sendAccountInfoUpdates(accountInfoUpdateEventDto));
    }

    @DisplayName("When account info updated" +
            "And Consent Form entry is null" +
            "Then verify account information is not sent.")
    @Test
    void testAccountInfoUpdatesNotSentIfConsentFormIsNull() {
        when(this.apiService.getFormEntryDtoByFormName(VIBRENT_ID, "SOME_OTHER_FORM")).thenReturn(initializeFormEntryDTO());
        assertFalse(accountInfoUpdateEventService.sendAccountInfoUpdates(accountInfoUpdateEventDto));
    }

    @DisplayName("When account info updated" +
            "And Form version dto NULL" +
            "Then verify account Info Updates not sent")
    @Test
    void testSendAccountInfoUpdates() throws Exception {
        when(this.apiService.getFormEntryDtoByFormName(VIBRENT_ID, FORM_NAME_CONSENT)).thenReturn(initializeFormEntryDTO());
        when(this.apiService.getFormVersionById(2L)).thenReturn(null);

        assertFalse(accountInfoUpdateEventService.sendAccountInfoUpdates(accountInfoUpdateEventDto));
    }

    @DisplayName("When account info updated" +
            "And DRC communications fails" +
            "Then verify account Info Updates not sent.")
    @Test
    void testAccountInfoUpdatesWhenDrcCommunicationFailsThenExpectExceptionThrown() throws Exception {
        when(this.apiService.getFormEntryDtoByFormName(VIBRENT_ID, FORM_NAME_CONSENT)).thenReturn(initializeFormEntryDTO());
        when(this.apiService.getFormVersionById(2L)).thenReturn(formVersionDTO());
        when(this.apiService.getUserDTO(CATI_USER_ID)).thenReturn(getCatiUser(CATI_USER_ID));
        when(drcProperties.getDrcApiBaseUrl()).thenReturn("https://pmi-drc-api-test.appspot.com");
        when(drcConfigService.getRetryNum()).thenReturn(1L);
        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenThrow(DrcConnectorException.class);

        assertFalse(accountInfoUpdateEventService.sendAccountInfoUpdates(accountInfoUpdateEventDto));
    }

    @DisplayName("When second person contact is updated" +
            "Then verify form entry sent to DRC.")
    @Test
    void sendSecondaryContactUpdatesOnContactTwoUpdated() throws Exception {
        secondaryContactChanges.add(SecondaryContactType.CONTACT_TWO.toString());
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = convertToAccountInfoUpdateEventDto(ACCOUNT_INFO_UPDATE_EVENT_DTO_TWO);
        when(drcProperties.getBasicsFormId()).thenReturn(284L);
        when(this.apiService.getActiveFormVersionByFormId(284L)).thenReturn(getActiveFormVersionDTO());
        when(this.apiService.getFormVersionById(25097L)).thenReturn(convertToFormVersionDTO(FORM_ENTRY_DTO_SECONDARY_CONTACT_TWO));
        when(drcProperties.getDrcApiBaseUrl()).thenReturn("https://pmi-drc-api-test.appspot.com");
        when(drcConfigService.getRetryNum()).thenReturn(1L);
        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenReturn(getDrcResponse());
        assertTrue(accountInfoUpdateEventService.sendSecondaryContactInfoAndSsnUpdates(accountInfoUpdateEventDto, ssn, secondaryContactChanges));
    }

    @DisplayName("When form version received as null from API" +
            "Then verify Secondary Contact Update is not sent.")
    @Test
    void sendSecondaryContactUpdatesWhenFormVersionDtoIsNull() throws Exception {
        secondaryContactChanges.add(SecondaryContactType.CONTACT_TWO.toString());
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = convertToAccountInfoUpdateEventDto(ACCOUNT_INFO_UPDATE_EVENT_DTO_TWO);
        when(this.apiService.getActiveFormVersionByFormId(284L)).thenReturn(getActiveFormVersionDTO());
        when(this.apiService.getFormVersionById(284L)).thenReturn(null);
        assertFalse(accountInfoUpdateEventService.sendSecondaryContactInfoAndSsnUpdates(accountInfoUpdateEventDto, ssn, secondaryContactChanges));
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

    private UserDTO getCatiUser(Long id) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setExternalId(EXTERNAL_ID);
        userDTO.setFirstName("SomeCatiName");
        userDTO.setMiddleInitial("L");
        userDTO.setLastName("CAtiLastName");
        userDTO.setDob(1323456879L);
        userDTO.setTestUser(false);
        userDTO.setEmail("abc@b.com");
        userDTO.setVerifiedPrimaryPhoneNumber("9874563210");
        authorities.add(ROLE_CATI);
        userDTO.setAuthorities(authorities);
        return userDTO;
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

    List<FormEntryDTO> initializeFormEntryDTO(String formName) {
        FormEntryDTO formEntryDTO = new FormEntryDTO();
        formEntryDTO.setId(295L);
        formEntryDTO.setFormName(formName);
        formEntryDTO.setFormId(295L);
        formEntryDTO.setFormVersionId(2L);
        formEntryDTO.setEntryRecordedTime(1644563733929L);
        formEntryDTO.setUpdatedById(CATI_USER_ID);
        formEntryDTO.setIsConsentProvided(true);
        entries.add(formEntryDTO);
        return entries;
    }

    FormEntryDTO initializeFormEntryDTO() {
        FormEntryDTO formEntryDTO = new FormEntryDTO();
        formEntryDTO.setId(295L);
        formEntryDTO.setFormName(FORM_NAME_CONSENT);
        formEntryDTO.setFormId(295L);
        formEntryDTO.setFormVersionId(2L);
        formEntryDTO.setEntryRecordedTime(1644563733929L);
        formEntryDTO.setUpdatedById(CATI_USER_ID);
        formEntryDTO.setIsConsentProvided(true);
        return formEntryDTO;
    }

    List<FormEntryDTO> initializeFormEntryDTOBasics(String formName, Long userId) {
        FormEntryDTO formEntryDTO = new FormEntryDTO();

        formEntryDTO.setId(284L);
        formEntryDTO.setFormName(formName);
        formEntryDTO.setFormId(284L);
        formEntryDTO.setFormVersionId(4L);
        formEntryDTO.setEntryRecordedTime(1644563733929L);
        formEntryDTO.setUpdatedById(userId);

        return entries;
    }

    FormVersionDTO formVersionDTO() {
        FormVersionDTO formVersionDTO = new FormVersionDTO();
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
        return formVersionDTO;
    }

    public HttpResponseWrapper getDrcResponse() {
        HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper(200, "VALID_RESPONSE");
        return httpResponseWrapper;
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

    ActiveFormVersionDTO getActiveFormVersionDTO() {
        ActiveFormVersionDTO activeFormVersionByFormId = new ActiveFormVersionDTO();
        activeFormVersionByFormId.setActiveFormVersionId(25097L);
        activeFormVersionByFormId.setFormId(284L);
        return activeFormVersionByFormId;
    }

    FormVersionDTO convertToFormVersionDTO(String resourcePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedInputStream bufferedInputStream = (BufferedInputStream) classLoader.getResource(resourcePath).getContent();
        FormVersionDTO formVersionDTO = JacksonUtil.getMapper().readValue(bufferedInputStream, FormVersionDTO.class);
        bufferedInputStream.close();
        return formVersionDTO;
    }

    AccountInfoUpdateEventDto convertToAccountInfoUpdateEventDto(String resourcePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedInputStream bufferedInputStream = (BufferedInputStream) classLoader.getResource(resourcePath).getContent();
        AccountInfoUpdateEventDto accountInfoUpdateEventDto = JacksonUtil.getMapper().readValue(bufferedInputStream, AccountInfoUpdateEventDto.class);
        bufferedInputStream.close();
        return accountInfoUpdateEventDto;
    }

    UserSSNDTO getUserSSNDTO() {
        UserSSNDTO userSSNDTO = new UserSSNDTO();
        userSSNDTO.setUserId(VIBRENT_ID);
        userSSNDTO.setSsn("222222222");
        return userSSNDTO;
    }

}