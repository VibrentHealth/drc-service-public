package com.vibrent.drc.constants;

public final class DrcConstant {

    private DrcConstant() {
        //private constructor
    }

    public static final String DEFAULT_START_DATE = "1970-01-01T00:00:00-00:00";
    public static final String URL_GENOMICS_PARTICIPANT_STATUS = "/GenomicOutreachV2";
    public static final String DRC_EXTERNAL_EVENT = "DRC_GENOMICS_PARTICIPANT_STATUS";

    public static final String INTERNAL_ID = "internalId";
    public static final String EXTERNAL_ID = "externalId";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_DESCRIPTION = "eventDescription";

    public static final String USER_INFO_SEARCH_API = "/api/userInfo/search";
    public static final String VIBRENTID_CACHE = "DRC_VIBRENTID_CACHE";
    public static final String SALIVERY_ORDER_DEVICE_CACHE = "SALIVERY_ORDER_DEVICE_CACHE";
    public static final String SALIVERY_BIOBANK_ADDRESS_CACHE = "SALIVERY_BIOBANK_ADDRESS_CACHE";

    public static final String TYPE = "type";
    public static final String PARTICIPANT_ID = "participant_id";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final String URL_SUPPLYDELIVERY = "/SupplyDelivery";

    public static final String BIOBANK_ADDRESS_DETAILS_API = "/api/salivary/biobank/address";
    public static final String SALIVARY_KIT_DETAILS_API = "/api/salivary/order/device";
    public static final String USER_DETAILS_API = "/api/user";
    public static final String GET_FORM_VERSION_API = "/api/formVersion/";
    public static final String GET_FORM_ENTRY_API = "/api/formEntryAdmin/user";
    public static final String URL_PARTICIPANT = "/Participant";
    public static final String GET_ACTIVE_FORM_VERSION_API = "/api/forms/active/";
    public static final String GET_SSN_INFO_API ="/api/user/ssn/";

    public static final String ROLE_CATI = "ROLE_MC_CATI_INTERVIEWER";

    public static final String META_VERSION_ID = "versionId";
    // header fields
    public static final String HEADER_KEY_IF_MATCH = "If-Match";
    public static final String CONTACT_ONE = "CONTACT_ONE";
    public static final String CONTACT_TWO = "CONTACT_TWO";
    public static final String SSN = "SSN";

}
