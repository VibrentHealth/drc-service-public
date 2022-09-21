package com.vibrent.drc.util;

import ca.uhn.fhir.model.api.ExtensionDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import ca.uhn.fhir.model.dstu2.valueset.AnswerFormatEnum;
import ca.uhn.fhir.model.dstu2.valueset.QuestionnaireResponseStatusEnum;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.StringDt;
import com.vibrent.acadia.domain.enumeration.FormComponentFieldType;
import com.vibrent.acadia.domain.enumeration.LocaleMaster;
import com.vibrent.acadia.domain.enumeration.ValueType;
import com.vibrent.acadia.web.rest.dto.form.*;
import com.vibrent.acadia.web.rest.dto.helpers.form.enums.SubmitButtonInputType;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.*;
import com.vibrent.acadia.web.rest.dto.helpers.form.subfields.SubFieldModelBase;
import com.vibrent.acadia.web.rest.dto.helpers.form.subfields.SubFieldTitleModel;
import com.vibrent.drc.exception.FHIRConverterException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;


@Slf4j
public class FHIRConverterUtility {

    static final String SYSTEM_PPI = "http://terminology.pmi-ops.org/CodeSystem/ppi";
    private static final String LANGUAGE_URL = "http://hl7.org/fhir/StructureDefinition/iso21090-ST-language";
    private static final String VALUE_LABEL = "value";
    private static final String QUESTION_LABEL = "questionLabel";
    private static final String TRANSLATION_URL = "http://hl7.org/fhir/StructureDefinition/translation";
    private static final String TRANSLATION_INNER_URL = "content";
    private static final String DEFAULT_UNDEFINED_QUESTION_TEXT = "QUESTION LABEL WAS NOT DEFINED, PLEASE CONTACT ADMINISTRATOR";
    public static final String IGNORE_QUESTION_IF_STARTS_WITH = "_";
    private static final String NON_PARTICIPANT_AUTHOR_URL = "http://all-of-us.org/fhir/forms/non-participant-author";
    private static final String NON_PARTICIPANT_AUTHOR = "CATI";

    private static Set<FormComponentFieldType> fieldTypesForNormalInput;
    private static Set<FormComponentFieldType> fieldTypesForConditionalInput;
    private static Set<FormComponentFieldType> fieldTypeForFileUpload;

    private static Set<String> questionLabelsAsEmpty;
    private static Map<FormComponentFieldType, AnswerFormatEnum> fieldTypeToAnswerFormatMap;

    static {
        initFieldTypes();
        initEmptyQuestionLabels();
    }

    private FHIRConverterUtility() {
    }

    private static void initFieldTypes() {
        fieldTypesForNormalInput = new HashSet<>();
        fieldTypesForNormalInput.add(FormComponentFieldType.RADIO_SELECTOR);
        fieldTypesForNormalInput.add(FormComponentFieldType.BOOLEAN_SELECTOR);
        fieldTypesForNormalInput.add(FormComponentFieldType.DAY_SELECTOR);
        fieldTypesForNormalInput.add(FormComponentFieldType.SLIDER);
        fieldTypesForNormalInput.add(FormComponentFieldType.TEXT_INPUT);
        fieldTypesForNormalInput.add(FormComponentFieldType.PHONE_INPUT);
        fieldTypesForNormalInput.add(FormComponentFieldType.NUMBER_INPUT);
        fieldTypesForNormalInput.add(FormComponentFieldType.DROPDOWN);
        fieldTypesForNormalInput.add(FormComponentFieldType.API_DROPDOWN);
        fieldTypesForNormalInput.add(FormComponentFieldType.TYPE_AHEAD);
        fieldTypesForNormalInput.add(FormComponentFieldType.MULTI_SELECTOR);
        fieldTypesForNormalInput.add(FormComponentFieldType.WHEEL);
        fieldTypesForNormalInput.add(FormComponentFieldType.TIME_PICKER);
        fieldTypesForNormalInput.add(FormComponentFieldType.EMBEDDED_TEXT_INPUT);
        fieldTypesForNormalInput.add(FormComponentFieldType.IMAGE_CONTAINER);
        fieldTypesForNormalInput.add(FormComponentFieldType.CALCULATED_SCORE);
        fieldTypesForNormalInput.add(FormComponentFieldType.MATRIX_QUESTION);

        fieldTypeForFileUpload = new HashSet<>();
        fieldTypeForFileUpload.add(FormComponentFieldType.SIGNATURE_BOX);
        fieldTypeForFileUpload.add(FormComponentFieldType.TAKE_PICTURE);

        fieldTypesForConditionalInput = new HashSet<>();
        fieldTypesForConditionalInput.add(FormComponentFieldType.SUBMIT);
        fieldTypesForConditionalInput.add(FormComponentFieldType.VIDEO_PLAYER);


        fieldTypeToAnswerFormatMap = new EnumMap<>(FormComponentFieldType.class);

        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.BOOLEAN_SELECTOR, AnswerFormatEnum.BOOLEAN);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.DAY_SELECTOR, AnswerFormatEnum.DATE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.TIME_PICKER, AnswerFormatEnum.TIME);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.TEXT_INPUT, AnswerFormatEnum.TEXT);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.PHONE_INPUT, AnswerFormatEnum.TEXT);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.NUMBER_INPUT, AnswerFormatEnum.DECIMAL);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.CALCULATED_SCORE, AnswerFormatEnum.DECIMAL);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.RADIO_SELECTOR, AnswerFormatEnum.CHOICE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.DROPDOWN, AnswerFormatEnum.CHOICE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.API_DROPDOWN, AnswerFormatEnum.CHOICE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.TYPE_AHEAD, AnswerFormatEnum.CHOICE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.IMAGE_CONTAINER, AnswerFormatEnum.CHOICE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.MULTI_SELECTOR, AnswerFormatEnum.CHOICE);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.WHEEL, AnswerFormatEnum.DECIMAL);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.EMBEDDED_TEXT_INPUT, AnswerFormatEnum.TEXT);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.MATRIX_QUESTION, AnswerFormatEnum.CHOICE);

        // attachment types
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.SIGNATURE_BOX, AnswerFormatEnum.ATTACHMENT);
        fieldTypeToAnswerFormatMap.put(FormComponentFieldType.TAKE_PICTURE, AnswerFormatEnum.ATTACHMENT);
    }

    private static void initEmptyQuestionLabels() {
        questionLabelsAsEmpty = new HashSet<>();
        questionLabelsAsEmpty.add("");
        questionLabelsAsEmpty.add("Add Question Label Here");
        questionLabelsAsEmpty.add("Please Specify Value Here");
    }

    public static QuestionnaireResponse convertFormEntryToQuestionnaireResponse(FormEntryDTO formEntryDTO,
                                                                                FormVersionDTO formVersionDTO,
                                                                                String participantId,
                                                                                String langKey,
                                                                                boolean setImpersonationInfo) throws FHIRConverterException {
        QuestionnaireResponse response = new QuestionnaireResponse();
        // set identifier using form entry id
        String id = formEntryDTO.getId() == null ? System.currentTimeMillis() + "" : formEntryDTO.getId().toString();
        response.setIdentifier(new IdentifierDt(null, id));

        // Set the status - for now, set it to a constant of 'COMPLETED' or 'IN_PROGRESS' depending on if it's a draft entry or not
        response.setStatus(formEntryDTO.isDraft() ? QuestionnaireResponseStatusEnum.IN_PROGRESS : QuestionnaireResponseStatusEnum.COMPLETED);

        // Set the author date
        DateTimeDt dateTimeDt = new DateTimeDt();
        Long createdTime;
        if (formEntryDTO.getEntryRecordedTime() != null) {
            createdTime = formEntryDTO.getEntryRecordedTime();
        } else if (formEntryDTO.getMeasurementTime() != null) {
            createdTime = formEntryDTO.getMeasurementTime();
        } else if (formEntryDTO.getCreatedOn() != null) {
            createdTime = formEntryDTO.getCreatedOn();
        } else {
            // use current time as all other timestamps are null
            createdTime = System.currentTimeMillis();
        }

        dateTimeDt.setValue(new Date(createdTime));
        response.setAuthored(dateTimeDt);

        // Set "questionnaire" element
        ResourceReferenceDt qDt = new ResourceReferenceDt();
        qDt.setReference(new IdDt("Questionnaire/" + formVersionDTO.getQuestionnaireId()));
        response.setQuestionnaire(qDt);

        // Set "subject" element
        ResourceReferenceDt sDt = new ResourceReferenceDt();
        sDt.setReference(new IdDt("Patient/" + participantId));
        response.setSubject(sDt);

        // Add Language extension
        ExtensionDt languageExtension = new ExtensionDt();
        response.addUndeclaredExtension(languageExtension);
        languageExtension.setUrl(LANGUAGE_URL);

        // Add impersonation extension
        if (setImpersonationInfo) {
            ExtensionDt impersonationExtension = new ExtensionDt();
            response.addUndeclaredExtension(impersonationExtension);
            impersonationExtension.setUrl(NON_PARTICIPANT_AUTHOR_URL);
            // set the role of the user who completed the form
            impersonationExtension.setValue(new StringDt(NON_PARTICIPANT_AUTHOR));
        }


        // set the local of the user who completed the form
        languageExtension.setValue(new CodeDt(langKey));


        // add the root group
        QuestionnaireResponse.Group rootGroup = FHIRQuestionnaireResponseConverter.createRootGroup(formVersionDTO);
        response.setGroup(rootGroup);

        // create GroupQuestion under rootGroup
        FHIRQuestionnaireResponseConverter.addGroupQuestions(rootGroup, formEntryDTO, formVersionDTO);


        log.info("DRC-Service: QuestionnaireResponse is generated for Participant Id - {}, formEntryId - {}, formVersion(Id - {}, formId - {}, Version - {})", participantId, formEntryDTO.getId(), formVersionDTO.getId(), formEntryDTO.getFormId(), formVersionDTO.getVersionId());

        return response;
    }


    public static List<FormComponentFieldDTO> findAllInputFields(FormModeDTO formMode) {
        return findAllInputFields(formMode.getPages());
    }

    static List<FormComponentFieldDTO> findAllInputFields(List<FormPageDTO> formPageDTOS) {
        List<FormComponentFieldDTO> inputFields = new ArrayList<>();

        if (formPageDTOS == null || formPageDTOS.isEmpty()) {
            return inputFields;
        }

        // collecting the sections
        List<FormSectionDTO> sections = new ArrayList<>();

        for (FormPageDTO pageDTO : formPageDTOS) {
            if (pageDTO.getSections() != null) {
                sections.addAll(pageDTO.getSections());
            }
        }

        sections.stream()
                .filter(section -> section.getFormComponents() != null && !section.getFormComponents().isEmpty())
                .forEach(section -> section.getFormComponents().stream()
                        .filter(formComponentDTO -> formComponentDTO.getFormComponentFields() != null && !formComponentDTO.getFormComponentFields().isEmpty())
                        .forEach(formComponentDTO -> formComponentDTO.getFormComponentFields().stream()
                                .filter(FHIRConverterUtility::isInputFieldType)
                                .forEach(formComponentFieldDTO -> {
                                    formComponentFieldDTO.setFormComponentDto(formComponentDTO);
                                    inputFields.add(formComponentFieldDTO);
                                })));

        return inputFields;
    }

    public static boolean isInputFieldType(FormComponentFieldDTO formComponentFieldDTO) {
        FormComponentFieldType type = formComponentFieldDTO.getType();
        if (fieldTypesForNormalInput.contains(type) || fieldTypeForFileUpload.contains(type)) {
            return true;
        } else if (fieldTypesForConditionalInput.contains(type)) {
            FieldValueStringModel fieldValue = formComponentFieldDTO.getFieldValue();
            // variable input type, require additional check
            switch (type) {
                case SUBMIT:
                    return ((FieldValueSubmitButtonModel) fieldValue).isAsInput();
                case VIDEO_PLAYER:
                    return ((FieldValueVideoDisplayModel) fieldValue).isAsInput();
                default:
                    return false;
            }
        }

        return false;
    }

    static AnswerFormatEnum getAnswerFormatEnum(FormComponentFieldDTO formComponentFieldDTO) {
        if (formComponentFieldDTO == null) {
            throw new IllegalArgumentException("input can not be null");
        }

        FormComponentFieldType fieldType = formComponentFieldDTO.getType();

        AnswerFormatEnum answerFormatEnum = fieldTypeToAnswerFormatMap.get(fieldType);
        if (answerFormatEnum == null) { // it is not defined in the map, probably a variable format or someone has made a mistake
            if (fieldTypesForConditionalInput.contains(fieldType)) {
                FieldValueStringModel fieldValue = formComponentFieldDTO.getFieldValue();
                // variable input type, require additional check
                if (FormComponentFieldType.SUBMIT == fieldType) {
                    answerFormatEnum = getAnswerFormatEnumForSubmit((FieldValueSubmitButtonModel) fieldValue);
                } else {
                    log.info("Unknown value passed to switch: {}", fieldType);
                }

                if (answerFormatEnum == null) {
                    throw new UnsupportedOperationException("This conditional input field is not enabled for user input : " + formComponentFieldDTO);
                }
            } else if (fieldType == FormComponentFieldType.SLIDER) {
                FieldValueSliderModel fieldValue = (FieldValueSliderModel) formComponentFieldDTO.getFieldValue();
                if (fieldValue.getValueType() == ValueType.STRING) {
                    answerFormatEnum = AnswerFormatEnum.TEXT;
                } else if (fieldValue.getValueType() == ValueType.NUMBER) {
                    answerFormatEnum = AnswerFormatEnum.DECIMAL;
                }
            }
        }

        if (answerFormatEnum == null) {
            // this exception is more for developer than anything else, it will likely be throw when developer has made a
            // logic error or when a new field type has been introduced and forgot to be put into the map or in this function
            throw new UnsupportedOperationException("could not find valid answer format for field : " + formComponentFieldDTO);
        }
        return answerFormatEnum;
    }

    private static AnswerFormatEnum getAnswerFormatEnumForSubmit(FieldValueSubmitButtonModel fieldValue) {
        if (Boolean.TRUE.equals(fieldValue.isAsInput())) {
            SubmitButtonInputType inputType = fieldValue.getInputType();
            switch (inputType) {
                case AS_NUMBER:
                    return AnswerFormatEnum.DECIMAL;
                case AS_STRING:
                    return AnswerFormatEnum.TEXT;
                case ON_CLICK_TIME:
                    return AnswerFormatEnum.DATE_TIME;
                default:
                    log.info("Unknown value passed to switch: {}", inputType);
                    return null;
            }
        }
        return null;
    }


    static String getLinkId(Long unVersionedFieldId) {
        return "" + unVersionedFieldId;
    }

    static StringDt createQuestionTextDt(FormComponentFieldDTO formComponentFieldDTO) {
        String questionText = null;
        Map<LocaleMaster, Map<String, Serializable>> localizationMap = null;

        if (formComponentFieldDTO.getType() == FormComponentFieldType.BOOLEAN_SELECTOR) {
            // boolean selector is different than other input type, it doesn't extend from input type
            FieldValueBooleanSelectorModel fieldValue = (FieldValueBooleanSelectorModel) formComponentFieldDTO.getFieldValue();
            if (fieldValue != null) {
                questionText = fieldValue.getQuestionLabel();
                localizationMap = fieldValue.getLocalizationMap();
            }
        } else if (formComponentFieldDTO.getFieldValue() instanceof FieldValueInputBaseModel) {
            FieldValueInputBaseModel fieldValue = (FieldValueInputBaseModel) formComponentFieldDTO.getFieldValue();
            if (fieldValue != null) {
                questionText = fieldValue.getQuestionLabel();
                localizationMap = fieldValue.getLocalizationMap();
            }
        } else {
            log.error("invalid form field encountered, probably coming from very old configuration that hasn't been fixed, form component field id: {}, type: {}",
                    formComponentFieldDTO.getId(), formComponentFieldDTO.getType());
            // error handling, just get it from value
            FieldValueStringModel fieldValue = formComponentFieldDTO.getFieldValue();
            questionText = "No Question Text Provided";
            if (fieldValue != null && !questionTextIsEmpty(fieldValue.getValue())) {
                questionText = fieldValue.getValue();
            }
        }

        if (questionTextIsEmpty(questionText)) { // if question text is empty, then we will try several different ways to get question text
            SubFieldTitleModel titleModel = getTitleModelFromComponentField(formComponentFieldDTO);
            if (titleModel != null) {  // if title model is not null, then we can will try to get question label from that subfield
                questionText = titleModel.getFieldValue().getValue();
                Map<LocaleMaster, Map<String, Serializable>> titleLocalizationMap = titleModel.getFieldValue().getLocalizationMap();

                localizationMap = reCreateLocalizationMap(titleLocalizationMap, VALUE_LABEL, QUESTION_LABEL);
            } else if (hasValueText(formComponentFieldDTO)) {
                // get question label from hint text
                questionText = formComponentFieldDTO.getFieldValue().getValue().trim();
                localizationMap = reCreateLocalizationMap(formComponentFieldDTO.getFieldValue().getLocalizationMap(), VALUE_LABEL, QUESTION_LABEL);
            }

        }

        // english is the default locale
        if (questionTextIsEmpty(questionText)) {
            questionText = DEFAULT_UNDEFINED_QUESTION_TEXT;
        }
        StringDt textDt = new StringDt(questionText);
        addLocalizationExtension(textDt, QUESTION_LABEL, localizationMap);

        return textDt;
    }

    private static boolean hasValueText(FormComponentFieldDTO formComponentFieldDTO) {
        String value = formComponentFieldDTO.getFieldValue() != null ?
                formComponentFieldDTO.getFieldValue().getValue() :
                null;
        return !(StringUtils.isEmpty(value));
    }

    private static boolean questionTextIsEmpty(String questionText) {
        if (questionText == null)
            return true;

        questionText = questionText.trim();
        return questionText.length() == 0 || questionLabelsAsEmpty.contains(questionText);
    }

    private static SubFieldTitleModel getTitleModelFromComponentField(FormComponentFieldDTO formComponentFieldDTO) {
        FormComponentFieldDTO.SubFieldList subFields = formComponentFieldDTO.getSubFields();

        if (subFields != null) {
            for (SubFieldModelBase sf : subFields) {
                if (sf instanceof SubFieldTitleModel) {
                    return (SubFieldTitleModel) sf;
                }
            }
        }

        return null;
    }

    private static Map<LocaleMaster, Map<String, Serializable>> reCreateLocalizationMap(
            Map<LocaleMaster, Map<String, Serializable>> originalMap, String originalLabel, String newLabel) {
        Map<LocaleMaster, Map<String, Serializable>> localizationMap = new EnumMap<>(LocaleMaster.class);
        if (originalMap != null) {
            for (Map.Entry<LocaleMaster, Map<String, Serializable>> entry : originalMap.entrySet()) {
                LocaleMaster locale = entry.getKey();
                Map<String, Serializable> oldMap = originalMap.get(locale);
                String langStringQuestionLabel = (String) oldMap.get(originalLabel);
                if (langStringQuestionLabel != null) {
                    Map<String, Serializable> newMap = new HashMap<>();
                    newMap.put(newLabel, langStringQuestionLabel);
                    localizationMap.put(locale, newMap);
                }
            }
        }

        return localizationMap;
    }

    static void addLocalizationExtension(StringDt textDt, String key, Map<LocaleMaster, Map<String, Serializable>> localizationMap) {
        if (localizationMap == null)
            return;

        boolean foundLabel = false; // use this flag to determine if any useful label for other language has been found
        ExtensionDt extension = new ExtensionDt();
        extension.setUrl(TRANSLATION_URL);

        for (Map.Entry e : localizationMap.entrySet()) {
            Map<String, String> mapValue = (Map<String, String>) e.getValue();
            String label = mapValue.get(key);
            if (label != null && label.trim().length() > 0) {
                ExtensionDt langExtension = new ExtensionDt();
                extension.addUndeclaredExtension(langExtension);
                langExtension.setUrl("lang");
                langExtension.setValue(new CodeDt(e.getKey().toString()));

                ExtensionDt contentExtension = new ExtensionDt();
                extension.addUndeclaredExtension(contentExtension);
                contentExtension.setUrl(TRANSLATION_INNER_URL);
                contentExtension.setValue(new StringDt(label));

                foundLabel = true;
            }
        }

        if (foundLabel) {
            textDt.addUndeclaredExtension(extension);
        }
    }


}
