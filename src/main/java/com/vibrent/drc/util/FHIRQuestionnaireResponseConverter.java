package com.vibrent.drc.util;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireResponse;
import ca.uhn.fhir.model.dstu2.valueset.AnswerFormatEnum;
import ca.uhn.fhir.model.primitive.*;
import com.vibrent.acadia.domain.enumeration.FormComponentFieldType;
import com.vibrent.acadia.domain.enumeration.ValueType;
import com.vibrent.acadia.util.CompareUtil;
import com.vibrent.acadia.web.rest.dto.form.*;
import com.vibrent.acadia.web.rest.dto.helpers.form.enums.SubFieldType;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.FieldValueCommonSelectionFieldModel;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.OptionsValue;
import com.vibrent.acadia.web.rest.dto.helpers.form.subfields.*;
import com.vibrent.drc.exception.FHIRConverterException;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
class FHIRQuestionnaireResponseConverter {

    private static final String SKIPPED_VALUE = "PMI_Skip";
    private static final String NA_VALUE = "NA";
    private static final String NA_VALUE_WITH_SEPARATOR = "N/A";
    public static final String FHIR_QUESTIONNAIRE_ROOT_ID = "root_group";

    private static NumberFormat decimalFormatter = new DecimalFormat("0.0");

    static {
        decimalFormatter.setMinimumFractionDigits(1);
        decimalFormatter.setMaximumFractionDigits(10);
    }

    private FHIRQuestionnaireResponseConverter() {
    }

    /**
     * create root group for questionnaire response
     *
     * @return new Questionnaire Response group with root group id
     */
    static QuestionnaireResponse.Group createRootGroup(FormVersionDTO formDTO) {
        QuestionnaireResponse.Group rootGroup = new QuestionnaireResponse.Group();

        rootGroup.setTitle(formDTO.getDisplayName());
        rootGroup.setText(formDTO.getDescription());
        rootGroup.setLinkId(FHIR_QUESTIONNAIRE_ROOT_ID);
        return rootGroup;
    }

    static void addGroupQuestions(QuestionnaireResponse.Group rootGroup, FormEntryDTO formEntryDTO, FormVersionDTO formVersionDTO) throws FHIRConverterException {
        List<FormComponentFieldDTO> allInputFields = FHIRConverterUtility.findAllInputFields(formVersionDTO.getEditMode());
        Map<Long, FormComponentFieldDTO> inputFieldMap = convertListToMap(allInputFields);

        List<FormFieldEntryDTO> formFieldEntries = formEntryDTO.getFormFieldEntries();
        if (formFieldEntries == null)
            return;

        for (FormFieldEntryDTO fieldEntryDTO : formFieldEntries) {
            if (fieldEntryDTO.getFormFieldId() != null) {
                FormComponentFieldDTO formComponentFieldDTO = inputFieldMap.get(fieldEntryDTO.getFormFieldId());
                if (formComponentFieldDTO == null) {
                    throw new FHIRConverterException("Can't find matching field by form field id : " + fieldEntryDTO.getFormFieldId());
                }
                if (!(formComponentFieldDTO.getName().isBlank()) && formComponentFieldDTO.getName()
                        .startsWith(FHIRConverterUtility.IGNORE_QUESTION_IF_STARTS_WITH)) {
                    continue;
                }
                QuestionnaireResponse.GroupQuestion question = createQuestion(fieldEntryDTO, formComponentFieldDTO);

                if (question == null) {
                    throw new FHIRConverterException("Can't create QuestionnaireResponse.GroupQuestion from : " + formEntryDTO);
                }
                rootGroup.addQuestion(question);
            }
        }

        // process skipped question here
        processSkippedQuestions(rootGroup, formEntryDTO, formVersionDTO, inputFieldMap);
    }

    private static void processSkippedQuestions(QuestionnaireResponse.Group rootGroup, FormEntryDTO formEntryDTO, FormVersionDTO formVersionDTO, Map<Long, FormComponentFieldDTO> inputFieldMap) {
        List<Long> skippedQuestionFieldIds = findSkippedQuestionIds(formEntryDTO, formVersionDTO);

        for (Long fieldId : skippedQuestionFieldIds) {
            FormComponentFieldDTO formComponentFieldDTO = inputFieldMap.get(fieldId);
            if ((formComponentFieldDTO.getName().isBlank()) && formComponentFieldDTO.getName()
                    .startsWith(FHIRConverterUtility.IGNORE_QUESTION_IF_STARTS_WITH)) {
                continue;
            }
            rootGroup.addQuestion(createSkippedQuestionAnswer(fieldId, formComponentFieldDTO));
        }
    }

    private static Map<Long, FormComponentFieldDTO> convertListToMap(List<FormComponentFieldDTO> allInputFields) {
        Map<Long, FormComponentFieldDTO> map = new HashMap<>();
        if (allInputFields != null) {
            allInputFields.forEach(f -> map.put(f.getFormField().getId(), f));
        }

        return map;
    }

    /**
     * create a question for skipped question
     *
     * @param fieldId               - field id
     * @param formComponentFieldDTO - form component field dto, used to get question text
     * @return response question represent a skipped value
     */
    private static QuestionnaireResponse.GroupQuestion createSkippedQuestionAnswer(Long fieldId, FormComponentFieldDTO formComponentFieldDTO) {
        QuestionnaireResponse.GroupQuestion question = new QuestionnaireResponse.GroupQuestion();
        question.setLinkId(FHIRConverterUtility.getLinkId(fieldId));

        QuestionnaireResponse.GroupQuestionAnswer answer = question.addAnswer();

        IDatatype value = new CodingDt(FHIRConverterUtility.SYSTEM_PPI, SKIPPED_VALUE);

        answer.setValue(value);

        StringDt questionTextDt = FHIRConverterUtility.createQuestionTextDt(formComponentFieldDTO);
        question.setText(questionTextDt);

        return question;
    }

    /**
     * find the field ids for skipped questions (questions that are on the navigation path and does not have a field entry)
     *
     * @param formEntryDTO   - form entry dto
     * @param formVersionDTO - form version dto
     * @return - list of fields ids that are skipped, if there are no question skipped, return empty list
     */
    private static @NotNull List<Long> findSkippedQuestionIds(FormEntryDTO formEntryDTO, FormVersionDTO formVersionDTO) {
        Set<Long> answeredFieldIds = getAnsweredFieldIds(formEntryDTO);

        FormStateMetaDataDTO formStateMetaData = formEntryDTO.getFormStateMetaData();
        if (formStateMetaData == null) {
            return new ArrayList<>();
        }

        Set<Long> allVisitedFieldIds = listToSet(formStateMetaData.getVisibleFieldIds());

        // old entry, doesn't have this value, then we assume all fields on all page in the navigation history
        // has been visible
        List<Long> nodeHistory = formStateMetaData.getPageNavigationSequence();

        List<FormPageDTO> formPageDTOList = findVisitedPages(formVersionDTO, nodeHistory);

        List<Long> allInputFields = getInputFieldIds(formPageDTOList);

        // return the field ids that are visited and not in the form entry
        return allInputFields.stream()
                .filter(allVisitedFieldIds::contains)
                .filter(fid -> !answeredFieldIds.contains(fid))
                .collect(Collectors.toList());
    }

    private static Set<Long> listToSet(List<Long> list) {
        Set<Long> outSet = new HashSet<>();

        if (list != null) {
            outSet.addAll(list);
        }

        return outSet;
    }

    /**
     * getting all the field id from pages where the field is an input field
     *
     * @param pages - list of pages
     * @return list of input field ids
     */
    private static @NotNull List<Long> getInputFieldIds(@NotNull List<FormPageDTO> pages) {
        List<Long> fieldIds = new ArrayList<>();

        List<FormComponentFieldDTO> allInputFields = FHIRConverterUtility.findAllInputFields(pages);
        if (allInputFields != null) {
            for (FormComponentFieldDTO field : allInputFields) {
                fieldIds.add(field.getFormField().getId());
            }
        }

        return fieldIds;
    }

    /**
     * finding a list of pages that has been visited
     *
     * @param formVersionDTO - form version dto
     * @param nodeHistory    - node history, a list of node ids
     * @return - a list of pages that has been visited, never null
     */
    private static @NotNull List<FormPageDTO> findVisitedPages(FormVersionDTO formVersionDTO, List<Long> nodeHistory) {
        List<FormPageDTO> visitedPages = new ArrayList<>();
        List<FormNavigationNodeDTO> navigationNodes = formVersionDTO.getEditMode().getNavigationNodes();
        List<FormPageDTO> allPages = formVersionDTO.getEditMode().getPages();
        if (nodeHistory != null && !nodeHistory.isEmpty() && navigationNodes != null && !navigationNodes.isEmpty()) {
            for (long nodeId : nodeHistory) {
                FormNavigationNodeDTO nodeDTO = findNodeById(navigationNodes, nodeId);
                if (nodeDTO != null && nodeDTO.getPageLocalId() != null) {
                    visitedPages.add(findPageByLocalId(allPages, nodeDTO.getPageLocalId()));
                }
            }
        }

        return visitedPages;
    }

    private static FormPageDTO findPageByLocalId(@NotNull List<FormPageDTO> allPages, long pageLocalId) {
        for (FormPageDTO page : allPages) {
            if (page.getLocalId() == pageLocalId) {
                return page;
            }
        }

        return null;
    }

    private static FormNavigationNodeDTO findNodeById(@NotNull List<FormNavigationNodeDTO> navigationNodes, long nodeId) {
        for (FormNavigationNodeDTO node : navigationNodes) {
            if (node.getLocalId() == nodeId) {
                return node;
            }
        }

        return null;
    }

    /**
     * getting the field ids for all the answers
     *
     * @param formEntryDTO - form entry dto
     * @return - a set of ids, never return null
     */
    private static Set<Long> getAnsweredFieldIds(FormEntryDTO formEntryDTO) {
        Set<Long> answeredIds = new HashSet<>();

        List<FormFieldEntryDTO> formFieldEntries = formEntryDTO.getFormFieldEntries();
        if (formFieldEntries != null && !formFieldEntries.isEmpty()) {
            formFieldEntries.forEach(ffe -> answeredIds.add(ffe.getFormFieldId()));
        }

        return answeredIds;
    }

    /**
     * create question for questionnaire response object
     *
     * @param formFieldEntry        form field entry object
     * @param formComponentFieldDTO the original form component field
     * @return QuestionnaireResponse.GroupQuestion object based on field entry object
     */
    private static QuestionnaireResponse.GroupQuestion createQuestion(FormFieldEntryDTO formFieldEntry, FormComponentFieldDTO formComponentFieldDTO) throws FHIRConverterException {
        AnswerFormatEnum type = FHIRConverterUtility.getAnswerFormatEnum(formComponentFieldDTO);
        if (type == null)
            return null; // not a valid type for creating questions
        QuestionnaireResponse.GroupQuestion question = new QuestionnaireResponse.GroupQuestion();
        question.setLinkId(FHIRConverterUtility.getLinkId(formFieldEntry.getFormFieldId()));

        List<FormFieldEntryValueDTO> formFieldEntryValues = formFieldEntry.getFormFieldEntryValues();
        for (FormFieldEntryValueDTO formFieldEntryValue : formFieldEntryValues) {
            QuestionnaireResponse.GroupQuestionAnswer answer = question.addAnswer();

            IDatatype value = createAnswerValue(formComponentFieldDTO, formFieldEntryValue);

            answer.setValue(value);
        }

        StringDt questionTextDt = FHIRConverterUtility.createQuestionTextDt(formComponentFieldDTO);
        question.setText(questionTextDt);

        return question;
    }

    /**
     * create answer value for one form field entry value
     *
     * @param formComponentFieldDTO original form field dto
     * @param formFieldEntryValue   one entry value
     * @return an Answer Value object
     */
    static IDatatype createAnswerValue(FormComponentFieldDTO formComponentFieldDTO, FormFieldEntryValueDTO formFieldEntryValue) throws FHIRConverterException {
        IDatatype dataValue = null;

        FormComponentFieldType fieldType = formComponentFieldDTO.getType();

        // construct the correct data type depending on the type of form component and value stored in entry value
        Double valueAsNumber = formFieldEntryValue.getValueAsNumber();

        switch (fieldType) {
            case DAY_SELECTOR:
                dataValue = new DateDt();
                if (valueAsNumber != null) {
                    ((DateDt) dataValue).setValue(new Date(valueAsNumber.longValue()));
                }
                break;
            case RADIO_SELECTOR:
            case DROPDOWN:
            case API_DROPDOWN:
                //case TYPE_AHEAD:
            case MULTI_SELECTOR:
            case IMAGE_CONTAINER:
            case MATRIX_QUESTION:
                dataValue = valueToIDataType(formFieldEntryValue, findDisplayForValue(formComponentFieldDTO, formFieldEntryValue));
                break;
            case SLIDER:
                dataValue = valueToDataType(formFieldEntryValue, findDisplayForValue(formComponentFieldDTO, formFieldEntryValue));
                break;
            default:
                // rest are normal values, we can determine it based on the value stored in entry value
                dataValue = valueToIDataType(formFieldEntryValue, null);
                break;
        }

        return dataValue;
    }

    /**
     * find the display text for value in entry value
     *
     * @param formComponentFieldDTO component field dto that include the field definition
     * @param formFieldEntryValue   value selected
     * @return display text for that value, if nothing is found, return null
     */
    private static String findDisplayForValue(FormComponentFieldDTO formComponentFieldDTO, FormFieldEntryValueDTO formFieldEntryValue) {
        if (formComponentFieldDTO == null || formFieldEntryValue == null) {
            return null;
        }

        ValueType valueType;
        if (formComponentFieldDTO.getType() == FormComponentFieldType.MATRIX_QUESTION) {
            valueType = formComponentFieldDTO.getFormComponentDto().getFormComponentMetaDataDTO()
                    .getMatrixQuestionnaireMetadataDTO().getValueType();
        } else {
            FieldValueCommonSelectionFieldModel fieldValue = (FieldValueCommonSelectionFieldModel) formComponentFieldDTO.getFieldValue();
            valueType = fieldValue.getValueType();
        }

        switch (valueType) {
            case NUMBER:
                return formComponentFieldDTO.getType() == FormComponentFieldType.MATRIX_QUESTION ?
                        findDisplayForValueInMatrix(formComponentFieldDTO, formFieldEntryValue.getValueAsNumber()) :
                        findDisplayForValue(formComponentFieldDTO.getSubFields(), formFieldEntryValue.getValueAsNumber());
            case STRING:
                return formComponentFieldDTO.getType() == FormComponentFieldType.MATRIX_QUESTION ?
                        findDisplayForValueInMatrix(formComponentFieldDTO, formFieldEntryValue.getValueAsString()) :
                        findDisplayForValue(formComponentFieldDTO.getSubFields(),
                                formFieldEntryValue.getValueAsString());
            default:
                return null;
        }
    }

    /**
     * find display for value using a list of subfields and a string or double value
     *
     * @param subFields     list of subfields that contains the answer display
     * @param valueSelected a string or a double value
     * @return display text in subfield that have the matching value, null if nothing is found
     */
    private static String findDisplayForValue(FormComponentFieldDTO.SubFieldList subFields, Object valueSelected) {
        if (subFields == null || valueSelected == null)
            return null;

        List<OptionsValue> optionValues;
        for (SubFieldModelBase subFieldModelBase : subFields) {
            SubFieldType subFieldType = subFieldModelBase.getType();

            switch (subFieldType) {
                case RADIO_OPTIONS:
                    optionValues = ((SubFieldRadioOptionModel) subFieldModelBase).getFieldValue().getValues();
                    break;
                case MULTI_SELECT_OPTIONS:
                    optionValues = ((SubFieldMultiSelectModel) subFieldModelBase).getFieldValue().getValues();
                    break;
                case DROPDOWN_OPTIONS:
                    optionValues = ((SubFieldDropdownOptionModel) subFieldModelBase).getFieldValue().getValues();
                    break;
                case SLIDER_OPTIONS:
                    optionValues = ((SubFieldSliderOptionModel) subFieldModelBase).getFieldValue().getValues();
                    break;
                default:
                    optionValues = null;
            }

            if (optionValues != null) {
                return findDisplayForValueFromOptionValues(optionValues, valueSelected);
            }
        }

        return null;
    }

    /**
     * find display for value using a list of options and a string or double value
     *
     * @param field         Matrix questionnaire field
     * @param valueSelected a string or a double value
     * @return display text in options that have the matching value, null if nothing is found
     */
    private static String findDisplayForValueInMatrix(FormComponentFieldDTO field, Object valueSelected) {
        List<OptionsValue> optionValues = field.getFormComponentDto().getFormComponentMetaDataDTO()
                .getMatrixQuestionnaireMetadataDTO().getValues();
        if (optionValues != null) {
            return findDisplayForValueFromOptionValues(optionValues, valueSelected);
        }
        return null;
    }

    /**
     * find the display for value from a list of option values
     *
     * @param optionValues  list of option values
     * @param valueSelected a string or a double value
     * @return display text in option value that have the matching value, null if nothing is found
     */
    private static String findDisplayForValueFromOptionValues(List<OptionsValue> optionValues, Object valueSelected) {
        if (valueSelected instanceof Double) {
            double dValueSelected = (Double) valueSelected;
            for (OptionsValue ov : optionValues) {
                try {
                    double currentValue = Double.parseDouble(ov.getValue());
                    if (CompareUtil.equalDouble(currentValue, dValueSelected)) {
                        return ov.getText();
                    }
                } catch (Exception e) {
                    log.warn("found value for double that can not be convereted : {}", ov.getValue());
                }
            }
        } else {
            String sValueSelected = (String) valueSelected;
            for (OptionsValue ov : optionValues) {
                if (CompareUtil.equalString(ov.getValue(), sValueSelected)) {
                    return ov.getText();
                }
            }

        }
        return null;
    }

    /**
     * convert non-date/time field entry value to IDataType
     * warning: this can not handle date/time value that are stored as numbers, those need to be
     * handled outside of this function
     *
     * @param formFieldEntryValue field entry value
     * @param codingDisplay       display value for the answer code, if this is null, then the answer is not a code
     * @return iDataType object
     */
    static IDatatype valueToIDataType(FormFieldEntryValueDTO formFieldEntryValue, String codingDisplay) throws FHIRConverterException {
        IDatatype dataValue = null;
        if (formFieldEntryValue.getValueAsNumber() != null) {
            Double valueAsNumber = formFieldEntryValue.getValueAsNumber();
            if (valueAsNumber == valueAsNumber.intValue()) {
                dataValue = new IntegerDt(valueAsNumber.intValue());
            } else if (valueAsNumber == valueAsNumber.longValue()) {
                dataValue = createLongDecimalDt(valueAsNumber);
            } else {
                dataValue = new DecimalDt(valueAsNumber);
            }

            if (formFieldEntryValue.getUnitId() != null) {
                // need to handle unit here when we have DRC code book for unit
                log.warn("units are not being converted at this time because we don't not have the code book for unit : {}", formFieldEntryValue);
            }
        } else if (formFieldEntryValue.getValueAsString() != null) {
            if (codingDisplay != null) {
                dataValue = new CodingDt(FHIRConverterUtility.SYSTEM_PPI, formFieldEntryValue.getValueAsString())
                        .setDisplay(codingDisplay);
            } else {
                dataValue = new StringDt(formFieldEntryValue.getValueAsString());
            }
        } else if (formFieldEntryValue.getValueAsBoolean() != null) {
            dataValue = new BooleanDt(formFieldEntryValue.getValueAsBoolean());
        }

        return dataValue;
    }


    static DecimalDt createLongDecimalDt(double decimalNumber) {
        DecimalDt dt = new DecimalDt(decimalNumber);
        dt.setValueAsString(decimalFormatter.format(decimalNumber));

        return dt;
    }

    static IDatatype valueToDataType(FormFieldEntryValueDTO formFieldEntryValue, String displayValue)
            throws FHIRConverterException {
        if (isNAValue(displayValue)) {
            return new CodingDt(FHIRConverterUtility.SYSTEM_PPI, SKIPPED_VALUE);
        } else {
            return valueToIDataType(formFieldEntryValue, displayValue);
        }
    }

    private static boolean isNAValue(String displayValue) {
        return displayValue != null &&
                (displayValue.equalsIgnoreCase(NA_VALUE) || displayValue.equalsIgnoreCase(NA_VALUE_WITH_SEPARATOR));
    }

}
