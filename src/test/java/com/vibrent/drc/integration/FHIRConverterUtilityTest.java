package com.vibrent.drc.integration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.BaseResource;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.acadia.web.rest.dto.form.FormEntryDTO;
import com.vibrent.acadia.web.rest.dto.form.FormVersionDTO;
import com.vibrent.drc.util.FHIRConverterUtility;
import com.vibrent.drc.util.JacksonUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@Category(IntegrationTest.class)
@Transactional
public class FHIRConverterUtilityTest extends IntegrationTest {
    private boolean drcImpersonation = false;
    public static String FORM_ENTRY_DTO_JSON_INPUT = "testData/form-entry-dto.json";
    public static String FORM_VERSION_DTO_JSON_INPUT = "testData/form-version-dto.json";
    public static String FORM_ENTRY_DTO_BASICS_JSON_INPUT = "testData/form-entry-dto-basics.json";
    public static String FORM_VERSION_DTO_BASICS_JSON_INPUT = "testData/form-version-dto-basics.json";
    public static String QR_RESOURCE_PATH_BASICS = "testData/quetionnaireResponseBasics.json";
    public static String QR_RESOURCE_PATH_PRIMARY = "testData/quetionnaireResponsePrimary.json";

    private FormEntryDTO formEntryDTO;
    private FormVersionDTO formVersionDTO;
    private FhirContext fhirContext;

    @Before
    public void setUp() throws Exception {
        fhirContext = FhirContext.forDstu2();
        //To run test locally
       // TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void convertFormEntryToQuestionnaireResponse() throws Exception {
        formVersionDTO = formVersionDTO(FORM_VERSION_DTO_JSON_INPUT);
        formEntryDTO = formEntryDTO(FORM_ENTRY_DTO_JSON_INPUT);
        String expectedQR = getQrString(QR_RESOURCE_PATH_PRIMARY);

        Logger logger = (Logger) LoggerFactory.getLogger(FHIRConverterUtility.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        QuestionnaireResponse questionnaireResponse = FHIRConverterUtility.convertFormEntryToQuestionnaireResponse(formEntryDTO, formVersionDTO, formEntryDTO.getExternalId(), "en", drcImpersonation);
        String questionnaireResponseString = getString(questionnaireResponse);
        assertNotNull(questionnaireResponse);
        assertEquals(expectedQR, questionnaireResponseString);

        List<ILoggingEvent> logsList = listAppender.list;
        int index = Math.max(0, logsList.size() - 1);

        assertEquals(Level.INFO, logsList.get(index).getLevel());
        assertEquals(String.format("DRC-Service: QuestionnaireResponse is generated for Participant Id - %s, formEntryId - %d, formVersion(Id - %d, formId - %d, Version - %d)",
                formEntryDTO.getExternalId(), formEntryDTO.getId(), formVersionDTO.getId(), formVersionDTO.getFormId(), formVersionDTO.getVersionId()),
                logsList.get(index).getFormattedMessage());
    }

    @Test
    public void convertFormEntryToQuestionnaireResponse1() throws Exception {
        formVersionDTO = formVersionDTO(FORM_VERSION_DTO_BASICS_JSON_INPUT);
        formEntryDTO = formEntryDTO(FORM_ENTRY_DTO_BASICS_JSON_INPUT);
        String expectedQR = getQrString(QR_RESOURCE_PATH_BASICS);

        Logger logger = (Logger) LoggerFactory.getLogger(FHIRConverterUtility.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        QuestionnaireResponse questionnaireResponse = FHIRConverterUtility.convertFormEntryToQuestionnaireResponse(formEntryDTO, formVersionDTO, formEntryDTO.getExternalId(), "en", false);
        String questionnaireResponseString = getString(questionnaireResponse);
        assertNotNull(questionnaireResponse);
        assertEquals(expectedQR, questionnaireResponseString);

        List<ILoggingEvent> logsList = listAppender.list;
        int index = Math.max(0, logsList.size() - 1);

        assertEquals(Level.INFO, logsList.get(index).getLevel());
        assertEquals(String.format("DRC-Service: QuestionnaireResponse is generated for Participant Id - %s, formEntryId - %d, formVersion(Id - %d, formId - %d, Version - %d)",
                formEntryDTO.getExternalId(), formEntryDTO.getId(), formVersionDTO.getId(), formVersionDTO.getFormId(), formVersionDTO.getVersionId()),
                logsList.get(index).getFormattedMessage());
    }

    FormVersionDTO formVersionDTO(String resourcePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedInputStream bufferedInputStream = (BufferedInputStream) classLoader.getResource(resourcePath).getContent();
        FormVersionDTO formVersionDTO = JacksonUtil.getMapper().readValue(bufferedInputStream, FormVersionDTO.class);
        bufferedInputStream.close();
        return formVersionDTO;
    }

    FormEntryDTO formEntryDTO(String resourcePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedInputStream bufferedInputStream = (BufferedInputStream) classLoader.getResource(resourcePath).getContent();
        FormEntryDTO formEntryDTO = JacksonUtil.getMapper().readValue(bufferedInputStream, FormEntryDTO.class);
        bufferedInputStream.close();
        return formEntryDTO;
    }

    String getQrString(String resourcePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        BufferedInputStream bufferedInputStream = (BufferedInputStream) classLoader.getResource(resourcePath).getContent();
        String questionnaireResponse = new String(bufferedInputStream.readAllBytes(), StandardCharsets.UTF_8);
        bufferedInputStream.close();
        return questionnaireResponse;
    }


    private <T extends BaseResource> String getString(T resource) {
        return fhirContext.newJsonParser().encodeResourceToString(resource);
    }

}
