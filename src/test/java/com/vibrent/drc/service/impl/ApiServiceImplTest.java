package com.vibrent.drc.service.impl;

import com.vibrent.acadia.web.rest.dto.UserDTO;
import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.acadia.web.rest.dto.form.ActiveFormVersionDTO;
import com.vibrent.acadia.web.rest.dto.form.FormEntryDTO;
import com.vibrent.acadia.web.rest.dto.form.FormVersionDTO;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.RestClientUtil;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiServiceImplTest {

    public static final String apiUrl = "http://api:8080";
    public static final String SALIVARY_KIT_DETAILS_API_RESPONSE = "{\"SKU\": 4081,\"name\": \"OGD-500.015\",\"type\": \"manufacturer-name\",\"display\": \"Oragene.Dx self-collection kit\"}";
    public static final String BIOBANK_ADDRESS_API_RESPONSE = "{\"city\": \"Rochester\", \"line\": [\"Mayo Clinic Laboratories\", \"3050 Superior Drive NW\"], \"state\": \"MN\", \"postalCode\": \"55901\"}";
    public static final String USER_DETAILS_API_RESPONSE = "{\"id\":79871410,\"createdDate\":1637662446000,\"createdBy\":\"system\",\"lastModifiedBy\":\"testuser1@gmail.com\",\"lastModifiedDate\":1637662466000,\"login\":\"testuser1@gmail.com\",\"email\":\"testuser1@gmail.com\",\"activated\":true,\"locked\":false,\"testUser\":false,\"hasSSN\":false,\"registrationComplete\":true,\"failedLoginAttempts\":0,\"failedQuestionAttempts\":0,\"passwordLastModifiedOn\":null,\"authorities\":[\"ROLE_USER\"],\"externalId\":\"P363744797\",\"userAddresses\":[{\"id\":5,\"zip\":\"97001\",\"latitude\":44.9552895,\"longitude\":-120.6265837,\"verificationState\":\"NONE\",\"type\":\"ACCOUNT\",\"userId\":79871410},{\"id\":6,\"state\":\"PIIState_OR\",\"city\":\"Portland\",\"streetOne\":\"2585 Se 14th Ave\",\"streetTwo\":\"Lane 2\",\"zip\":\"97202\",\"latitude\":45.5042617,\"longitude\":-122.6519337,\"verificationState\":\"SYSTEM\",\"type\":\"MAILING\",\"userId\":79871410,\"userAddressMetadataDTO\":{\"stateFipsCode\":\"41\",\"countyFipsCode\":\"051\"}}],\"userPreferences\":{\"timezone\":\"Asia/Kolkata\",\"locale\":\"en\",\"userId\":79871410,\"emailNotifications\":true,\"pushNotifications\":true,\"smsNotifications\":false,\"alternateEmailPreferred\":false},\"keycloakId\":\"ce9a4fe1-8b70-4616-9710-1cc3d91a5bf3\"}";
    public static final String FORM_ENTRY_LIST_RESPONSE = "[{\"createdOn\":1644812490423,\"updatedOn\":1644812541614,\"createdById\":87125411,\"updatedById\":87125411,\"isDeleted\":false,\"id\":2,\"measurementTime\":1644812536550,\"entryRecordedTime\":1644812536550,\"mode\":\"MANUAL\",\"draft\":false,\"isConsentProvided\":true,\"consentUpdated\":true,\"userId\":87125411,\"userLogin\":\"test2@gmail.com\",\"formVersionId\":24950,\"formId\":295,\"formName\":\"ConsentPII\",\"form\":{\"createdOn\":1644810435039,\"updatedOn\":1644810435040,\"createdById\":87125409,\"updatedById\":87125409,\"isDeleted\":false,\"id\":295,\"name\":\"ConsentPII\",\"displayName\":\"Primary Consent\",\"category\":\"CONSENT_FORM\",\"description\":\"\",\"isHidden\":false,\"isLatestVersionActive\":true,\"localizationMap\":{\"es\":{\"displayName\":\"Consentimiento Básico\"}},\"formFields\":[{\"id\":6606,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":6698,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":6701,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":6707,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":6728,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":6770,\"type\":\"BOOLEAN_SELECTOR\",\"formId\":295},{\"id\":6773,\"type\":\"BOOLEAN_SELECTOR\",\"formId\":295},{\"id\":6779,\"type\":\"SIGNATURE_BOX\",\"formId\":295},{\"id\":6780,\"type\":\"DAY_SELECTOR\",\"formId\":295},{\"id\":6814,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":6821,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":6830,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":6835,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":6842,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":9458,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":9459,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9460,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9461,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9462,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9463,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9464,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9469,\"type\":\"DAY_SELECTOR\",\"formId\":295},{\"id\":9503,\"type\":\"DROPDOWN\",\"formId\":295},{\"id\":9837,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9838,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":9841,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":10436,\"type\":\"IMAGE_DISPLAY\",\"formId\":295},{\"id\":10465,\"type\":\"SIGNATURE_BOX\",\"formId\":295},{\"id\":10466,\"type\":\"DAY_SELECTOR\",\"formId\":295},{\"id\":10732,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10733,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10734,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10735,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10736,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10737,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10738,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10739,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10740,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10741,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":10751,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10754,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10757,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10760,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10763,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10766,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10769,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10771,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10772,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10775,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10778,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":10783,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":10932,\"type\":\"DIVIDER\",\"formId\":295},{\"id\":21024,\"type\":\"DROPDOWN\",\"formId\":295},{\"id\":21025,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":21028,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":21031,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":21257,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":21258,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":21259,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":21260,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":21261,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":21262,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":21263,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":34052,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":34053,\"type\":\"DROPDOWN\",\"formId\":295},{\"id\":34514,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34517,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34518,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34708,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34709,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34712,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34713,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34714,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34715,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34716,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34717,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34718,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34719,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34720,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34723,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34726,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34727,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34728,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34729,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34730,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34731,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34732,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34733,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34734,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34735,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34736,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34737,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34738,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34739,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34740,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34741,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34742,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34743,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34744,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34745,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34746,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34747,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34748,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34749,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34750,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":34751,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":35065,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":35078,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":35213,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":41139,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":41194,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":42119,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":42120,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":43989,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":43990,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":44371,\"type\":\"IMAGE_DISPLAY\",\"formId\":295},{\"id\":44373,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":44382,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":44383,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":44384,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":44388,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":44390,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":45151,\"type\":\"DIVIDER\",\"formId\":295},{\"id\":45152,\"type\":\"BOOLEAN_SELECTOR\",\"formId\":295},{\"id\":45221,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":45853,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":45859,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":45899,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":45900,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":45904,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":45905,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":45906,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":45911,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":45912,\"type\":\"VIDEO_PLAYER\",\"formId\":295},{\"id\":45913,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":46246,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":46369,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":47281,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":47286,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":47288,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":50483,\"type\":\"TEXT_INPUT\",\"formId\":295},{\"id\":52219,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52220,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52221,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52222,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52223,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52224,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52225,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52226,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52227,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52228,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52229,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52230,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52231,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52232,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52233,\"type\":\"TEXT_LABEL\",\"formId\":295},{\"id\":52234,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52235,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52236,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52237,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52238,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52565,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52566,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52850,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52852,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52854,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52856,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52942,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":52956,\"type\":\"BOOLEAN_SELECTOR\",\"formId\":295},{\"id\":52957,\"type\":\"BOOLEAN_SELECTOR\",\"formId\":295},{\"id\":52958,\"type\":\"MULTI_SELECTOR\",\"formId\":295},{\"id\":52960,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53431,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53432,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53433,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53434,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53435,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53436,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53437,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53438,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53439,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53440,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53441,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53442,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53443,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53444,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53445,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53446,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53447,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53448,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53449,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53450,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53451,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":53452,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":54484,\"type\":\"RICH_TEXT_LABEL\",\"formId\":295},{\"id\":60706,\"type\":\"TEXT_INPUT\",\"formId\":295}],\"formIcons\":{\"UNLOCKED\":{\"imageUrl\":\"/icons/form/2020/11/17/primaryConsent_9f71f7b9-a47c-4ad6-b76c-6b84071c81d3.png\",\"imageName\":\"primaryConsent.png\",\"imageSize\":29352,\"aspectRatio\":1.2121212121212122},\"LOCKED\":{\"imageUrl\":\"/icons/form/2020/11/17/primaryConsent_9f71f7b9-a47c-4ad6-b76c-6b84071c81d3.png\",\"imageName\":\"primaryConsent.png\",\"imageSize\":29352,\"aspectRatio\":1.2121212121212122},\"COMPLETED\":{\"imageUrl\":\"/icons/form/2020/11/17/primaryConsent_9f71f7b9-a47c-4ad6-b76c-6b84071c81d3.png\",\"imageName\":\"primaryConsent.png\",\"imageSize\":29352,\"aspectRatio\":1.2121212121212122},\"PAUSED\":{\"imageUrl\":\"/icons/form/2020/11/17/primaryConsent_9f71f7b9-a47c-4ad6-b76c-6b84071c81d3.png\",\"imageName\":\"primaryConsent.png\",\"imageSize\":29352,\"aspectRatio\":1.2121212121212122}},\"surveyIcon\":{\"icon\":{\"imageUrl\":\"/icons/form/2020/11/09/img-pc@3x_79be17e4-29c1-41b3-b4c5-257528cb3893.png\",\"imageName\":\"img-pc@3x.png\",\"imageSize\":61199,\"aspectRatio\":1.2048192771084338}},\"isBaseConsent\":true,\"consentType\":\"PII\",\"activeVersionViewModeTemplate\":\"NONE\",\"externalTargetMappings\":[{\"id\":7540,\"formId\":295,\"externalTarget\":\"DRC\"}]},\"formFieldEntries\":[{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":41,\"formEntryId\":2,\"formFieldId\":10783,\"formFieldEntryValues\":[{\"id\":41,\"valueAsString\":\"test2@gmail.com\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":42,\"formEntryId\":2,\"formFieldId\":34053,\"formFieldEntryValues\":[{\"id\":42,\"valueAsString\":\"SOR_OR\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":43,\"formEntryId\":2,\"formFieldId\":9841,\"formFieldEntryValues\":[{\"id\":43,\"valueAsString\":\"18YearsofAge_18YearsYes\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":44,\"formEntryId\":2,\"formFieldId\":21024,\"formFieldEntryValues\":[{\"id\":44,\"valueAsString\":\"PIIStateCare_OR\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":45,\"formEntryId\":2,\"formFieldId\":21025,\"formFieldEntryValues\":[{\"id\":45,\"valueAsString\":\"PIIState_OR\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":46,\"formEntryId\":2,\"formFieldId\":6773,\"formFieldEntryValues\":[{\"id\":46,\"valueAsBoolean\":true}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":47,\"formEntryId\":2,\"formFieldId\":50483,\"formFieldEntryValues\":[{\"id\":47,\"valueAsString\":\"3\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":48,\"formEntryId\":2,\"formFieldId\":6779,\"formFieldEntryValues\":[{\"id\":48,\"valueAsFile\":\"PRVibrent-RgsupIVpNZIpRtxAYjq0WfVsiJeAPENYygU8U6Ie1NnKhQj-Od978tA_GwUWIvYndMEE6fDheq62OeEG-E_l6ooFYL-8t6ezx8pI6z5ypYTPCNpEsc3v4JT5uysaQo5LDazi\",\"fileName\":\"Signature_4397.png\",\"fileSize\":16855}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":49,\"formEntryId\":2,\"formFieldId\":6780,\"formFieldEntryValues\":[{\"id\":49,\"valueAsNumber\":1.644812391153E12}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":50,\"formEntryId\":2,\"formFieldId\":35065,\"formFieldEntryValues\":[{\"id\":50,\"valueAsString\":\"HelpWithConsent_No\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":51,\"formEntryId\":2,\"formFieldId\":9459,\"formFieldEntryValues\":[{\"id\":51,\"valueAsString\":\"Ma\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":52,\"formEntryId\":2,\"formFieldId\":9460,\"formFieldEntryValues\":[{\"id\":52,\"valueAsString\":\"P\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":53,\"formEntryId\":2,\"formFieldId\":9461,\"formFieldEntryValues\":[{\"id\":53,\"valueAsString\":\"Jay\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":54,\"formEntryId\":2,\"formFieldId\":9462,\"formFieldEntryValues\":[{\"id\":54,\"valueAsString\":\"3585 Se 14th Ave\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":55,\"formEntryId\":2,\"formFieldId\":9464,\"formFieldEntryValues\":[{\"id\":55,\"valueAsString\":\"Portland\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":56,\"formEntryId\":2,\"formFieldId\":9503,\"formFieldEntryValues\":[{\"id\":56,\"valueAsString\":\"PIIState_OR\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":57,\"formEntryId\":2,\"formFieldId\":9837,\"formFieldEntryValues\":[{\"id\":57,\"valueAsString\":\"97202\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":58,\"formEntryId\":2,\"formFieldId\":9838,\"formFieldEntryValues\":[{\"id\":58,\"valueAsString\":\"9797979685\"}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":59,\"formEntryId\":2,\"formFieldId\":9469,\"formFieldEntryValues\":[{\"id\":59,\"valueAsNumber\":4.733856E11}]},{\"@class\":\"com.vibrent.acadia.web.rest.dto.form.FormFieldEntryDTO\",\"id\":60,\"formEntryId\":2,\"formFieldId\":60706,\"formFieldEntryValues\":[{\"id\":60,\"valueAsString\":\"Organization/UNSET\"}]}],\"formStateMetaData\":{\"pageNavigationSequence\":[8079,195070,11857,195075,195073,195076,195078,195077,940,195080,195082,195083,195085,195093,195087,195089,195091,195095,762,195098,195100,195102,3869,9235,195104,195105,195106,195107,195110,195111,195115,195119,195123,16798,195127,195109,195129,195130,10817,195131,195132],\"visibleFieldIds\":[10732,52956,52957,34053,9841,21024,10733,10734,10738,10735,10736,10737,10739,45899,10740,10741,45905,45912,6701,6698,6707,6835,47281,6728,6770,6773,6779,35065,9459,9460,9461,9462,9463,9464,9503,9837,9838,9469],\"elementMetaDataMap\":{},\"fieldValueChangeCountMap\":{}},\"externalId\":\"441130405\",\"pdfFileInfoSet\":[{\"en\":{\"url\":\"PRVibrent-BuR-i1JgnFzMyUbUxy0tSmklT5aMVso6UwwgxsSTnuO_QG1Vi8IQN5XdLBOdQt_7yJvDCjYf_8BGXiE6bteqMM9tfEwkFleSK7ALVcno6PsuMK4m7k-1OlpukOr2s9ZHxkRNkzuq_5ugUiL76Sei2o34sRYN3f61NQ\",\"mimeType\":\"application/pdf\",\"fileSize\":0,\"createdDate\":1644812537443,\"fileName\":\"20210524_F1_Primary_Consent_Form-ENG.pdf\"},\"es\":{\"url\":\"PRVibrent-EmZf0uoskCjdJXzSujtCUqy7RKoAUcThv7CI4daEEWAwSNStV1JYcCfyGKDBhC_x_fjP5XOS2BwAccjtX7rBMYEfv1Ssge88YwHIoG44vp_e5yoCvaT_sESGPpNPZNXSoOYN_JsQxiOhZmCq4v0Fo0fDQg\",\"mimeType\":\"application/pdf\",\"fileSize\":0,\"createdDate\":1644812537585,\"fileName\":\"20210524_F1_Consentimiento-ESP.pdf\"}}],\"viewModeTemplate\":\"NONE\",\"autoSave\":false,\"consentProvided\":true}]";
    public static final String FORM_VERSION_ID_RESPONSE = "{\"createdOn\":1644810435399,\"updatedOn\":1644810436316,\"createdById\":87125409,\"updatedById\":87125409,\"isDeleted\":false,\"id\":24950,\"previousFormVersionId\":24890,\"versionId\":2521,\"isActive\":true,\"gradient\":\"GREEN_LIGHT_BLUE_1\",\"questionnaireId\":\"1791513\",\"semanticVersion\":\"V2021.10.21\",\"enableAutoSave\":true,\"breakingChange\":true,\"accessibleWithoutLogin\":false,\"formId\":295,\"name\":\"ConsentPII\",\"displayName\":\"Primary Consent\",\"category\":\"CONSENT_FORM\",\"description\":\"\",\"isHidden\":false,\"consentType\":\"PII\",\"externalTargetMappings\":[{\"id\":7540,\"formId\":295,\"externalTarget\":\"DRC\"}],\"cancelButtonEnabled\":false}";
    public static final String SSN_RESPONSE = "{\"createdOn\":1644810435399,\"updatedOn\":1644810436316,\"createdById\":87125409,\"updatedById\":87125409,\"id\":24950}";
    public static final String ACTIVE_FORM_VERSION_ID_RESPONSE = "{\"formId\":284,\"activeFormVersionId\":25097}";

    @Mock
    private OAuth2RestTemplate keycloakDrcInternalCredentialsRestTemplate;

    @Mock
    private RestClientUtil restClientUtil;

    @Mock
    private OAuth2AccessToken oAuth2AccessToken;

    private ApiService apiService;

    @BeforeEach
    void setUp() {
        apiService = new ApiServiceImpl(apiUrl, keycloakDrcInternalCredentialsRestTemplate, restClientUtil);
    }

    @Test
    void getBioBankAddress() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);

        String bioBankAddress = apiService.getBioBankAddress();
        assertNotNull(bioBankAddress);
        assertEquals(BIOBANK_ADDRESS_API_RESPONSE, bioBankAddress);
    }

    @Test
    void getSalivaryKitDetails() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(SALIVARY_KIT_DETAILS_API_RESPONSE);

        String deviceDetails = apiService.getDeviceDetails();
        assertNotNull(deviceDetails);
        assertEquals(SALIVARY_KIT_DETAILS_API_RESPONSE, deviceDetails);
    }

    @Test
    void getUserDetails() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(USER_DETAILS_API_RESPONSE);

        UserDTO userDetails = apiService.getUserDTO(79871410L);
        assertNotNull(userDetails);
        assertEquals(USER_DETAILS_API_RESPONSE, JacksonUtil.getMapper().writeValueAsString(userDetails));
    }

    @Test
    void getUserFormEntryDTO() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(FORM_ENTRY_LIST_RESPONSE);

        List<FormEntryDTO> userFormEntryDTOList = apiService.getUserFormEntryDTO(87125411L);
        assertNotNull(userFormEntryDTOList);
        assertEquals(FORM_ENTRY_LIST_RESPONSE, JacksonUtil.getMapper().writeValueAsString(userFormEntryDTOList));
    }

    @Test
    void getUserFormEntryDTOOnException() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenThrow(RuntimeException.class);

        Assert.assertThrows("Failed to fetch User FormEntryDTO for user:",
                BusinessProcessingException.class,
                () -> apiService.getUserFormEntryDTO(87125411L));
    }

    @Test
    void getFormVersionById() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(FORM_VERSION_ID_RESPONSE);

        FormVersionDTO formVersionById = apiService.getFormVersionById(295L);
        assertNotNull(formVersionById);
        assertEquals(FORM_VERSION_ID_RESPONSE, JacksonUtil.getMapper().writeValueAsString(formVersionById));
    }


    @Test
    void getFormVersionByIdException() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenThrow(RuntimeException.class);
        Assert.assertThrows("Failed to fetch User FormVersion for id:",
                BusinessProcessingException.class,
                () -> apiService.getFormVersionById(295L));
    }

    @Test
    void getActiveFormVersionByFormId() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(ACTIVE_FORM_VERSION_ID_RESPONSE);

        ActiveFormVersionDTO activeFormVersionByFormId = apiService.getActiveFormVersionByFormId(284L);
        assertNotNull(activeFormVersionByFormId);
        assertEquals(ACTIVE_FORM_VERSION_ID_RESPONSE, JacksonUtil.getMapper().writeValueAsString(activeFormVersionByFormId));
    }


    @Test
    void getActiveFormVersionByFormIdException() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenThrow(RuntimeException.class);
        Assert.assertThrows("Failed to fetch active form version for formId:",
                BusinessProcessingException.class,
                () -> apiService.getActiveFormVersionByFormId(295L));
    }


    @Test
    void getUserSsnByUserId() throws Exception {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
        when(restClientUtil.getRequest(any(), (HttpHeaders) any())).thenReturn(SSN_RESPONSE);

        UserSSNDTO userSsnByUserId = apiService.getUserSsnByUserId(87125409L);
        assertNotNull(userSsnByUserId);
        assertEquals(SSN_RESPONSE, JacksonUtil.getMapper().writeValueAsString(userSsnByUserId));
    }


    @Test
    void getUserSsnByUserIdException() {
        when(keycloakDrcInternalCredentialsRestTemplate.getAccessToken()).thenThrow(RuntimeException.class);
        Assert.assertThrows("Failed to fetch SSN for user id:",
                BusinessProcessingException.class,
                () -> apiService.getUserSsnByUserId(87125409L));
    }


}