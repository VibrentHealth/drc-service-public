package com.vibrent.drc.util;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.primitive.DecimalDt;
import com.vibrent.acadia.domain.enumeration.*;
import com.vibrent.acadia.web.rest.dto.form.FormComponentFieldDTO;
import com.vibrent.acadia.web.rest.dto.form.FormFieldDTO;
import com.vibrent.acadia.web.rest.dto.form.FormFieldEntryValueDTO;
import com.vibrent.acadia.web.rest.dto.helpers.form.enums.SubFieldType;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.FieldValueRadioOptionsModel;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.FieldValueRadioSelectorModel;
import com.vibrent.acadia.web.rest.dto.helpers.form.fieldValue.OptionsValue;
import com.vibrent.acadia.web.rest.dto.helpers.form.insights.InsightsConfigModel;
import com.vibrent.acadia.web.rest.dto.helpers.form.subfields.SubFieldRadioOptionModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class FHIRQuestionnaireResponseConverterTest {

    private static final String EXTERNAL_ID = "P1232322";

    @Test
    void testCreateLongDecimalDt() {
        DecimalDt dt = FHIRQuestionnaireResponseConverter.createLongDecimalDt(2554d);
        assertNotNull(dt);
    }

    @Test
    void testCreateAnswerValue() throws Exception {
        FormComponentFieldDTO formComponentFieldDTO1 = getFormComponentFieldDTO();
        IDatatype iDatatype = FHIRQuestionnaireResponseConverter.createAnswerValue(formComponentFieldDTO1, getFormFieldEntryValueDTO("5241", 5241d));
        assertNotNull(iDatatype);
    }

    @Test
    void testValueToDataType() throws Exception {
        IDatatype id= FHIRQuestionnaireResponseConverter.valueToIDataType(getFormFieldEntryValueDTO("12",12d),"codingDisplay");
        assertNotNull(id);

        FormFieldEntryValueDTO formFieldEntryValueDTO = getFormFieldEntryValueDTO("56", 56d);
        IDatatype iDatatype = FHIRQuestionnaireResponseConverter.valueToDataType(formFieldEntryValueDTO, "N/A");
        assertNotNull(iDatatype);

        FormFieldEntryValueDTO formFieldEntryValueDTO1 = getFormFieldEntryValueDTO("56", 56d);
        IDatatype iDatatype1 = FHIRQuestionnaireResponseConverter.valueToDataType(formFieldEntryValueDTO1, "NA");
        assertNotNull(iDatatype1);

        FormFieldEntryValueDTO formFieldEntryValueDTO2 = getFormFieldEntryValueDTO("56", 56d);
        IDatatype iDatatype2 = FHIRQuestionnaireResponseConverter.valueToDataType(formFieldEntryValueDTO2, null);
        assertNotNull(iDatatype2);
    }

    // private methods

    FormComponentFieldDTO getFormComponentFieldDTO() {

        FormComponentFieldDTO multiSelectField = new FormComponentFieldDTO();
        multiSelectField.setType(FormComponentFieldType.MULTI_SELECTOR);
        multiSelectField.setName("emsi_feedback");
        FieldValueRadioSelectorModel fv = new FieldValueRadioSelectorModel();
        fv.setValue("5421");
        fv.setValueType(ValueType.NUMBER);

        multiSelectField.setFieldValue(fv);

        FormComponentFieldDTO.SubFieldList subFieldList = new FormComponentFieldDTO.SubFieldList();
        SubFieldRadioOptionModel radioOptions = new SubFieldRadioOptionModel();
        radioOptions.setFieldValue(new FieldValueRadioOptionsModel());
        radioOptions.getFieldValue().setValues(new ArrayList<>());
        radioOptions.getFieldValue().getValues().add(getOption(295L, 1, EmsiFeedbackSelectedOptionEnum.NO_CLINIC_IN_MY_AREA));
        radioOptions.getFieldValue().getValues().add(getOption(295L, 2, EmsiFeedbackSelectedOptionEnum.LACK_OF_TRANSPORTATION));
        radioOptions.getFieldValue().getValues().add(getOption(295L, 3, EmsiFeedbackSelectedOptionEnum.OTHER));
        radioOptions.getFieldValue().getValues().add(getOption(295L, 2, EmsiFeedbackSelectedOptionEnum.LACK_OF_TRANSPORTATION));
        radioOptions.setType(SubFieldType.RADIO_OPTIONS);
        subFieldList.add(radioOptions);
        multiSelectField.setSubFields(subFieldList);
        multiSelectField.setDisplayOrder(1);
        multiSelectField.setFormField(new FormFieldDTO());
        multiSelectField.getFormField().setFormId(295L);
        multiSelectField.getFormField().setId(295L * 10 + 1);
        multiSelectField.getFormField().setType(FormComponentFieldType.RADIO_SELECTOR);
        multiSelectField.setInsightsConfig(new InsightsConfigModel());
        multiSelectField.getInsightsConfig().setVisualizationMode(InsightsVisualizationMode.HORIZONTAL_BAR_CHART);
        multiSelectField.setInsightsEnabled(true);
        multiSelectField.setDisplayOrder(1);
        return multiSelectField;
    }

    private FormFieldEntryValueDTO getFormFieldEntryValueDTO(String fieldValue, Double value) {
        FormFieldEntryValueDTO formFieldEntryValueDTO = new FormFieldEntryValueDTO();
        formFieldEntryValueDTO.setValueAsString(fieldValue);
        formFieldEntryValueDTO.setValueAsNumber(value);

        return formFieldEntryValueDTO;
    }
    private FormFieldEntryValueDTO getFormFieldEntryValueDTO(String value) {
        FormFieldEntryValueDTO formFieldEntryValueDTO = new FormFieldEntryValueDTO();

        formFieldEntryValueDTO.setValueAsString(value);

        return formFieldEntryValueDTO;
    }

    private OptionsValue getOption(Long formId, int index, EmsiFeedbackSelectedOptionEnum optionEnum) {
        OptionsValue optionsValue = new OptionsValue();
        String optionTextEn = String.format("Option%d-%d", formId, index);
        String optionTextEs = String.format("Opcion%d-%d", formId, index);
        optionsValue.setText(optionTextEn);
        optionsValue.setContentDescription(optionTextEn);
        optionsValue.setValue(optionEnum.toString());
        optionsValue.setLocalizationMap(new HashMap<>());
        optionsValue.getLocalizationMap().put(LocaleMaster.es, new HashMap<>());
        optionsValue.getLocalizationMap().get(LocaleMaster.es).put("text", optionTextEs);
        optionsValue.getLocalizationMap().get(LocaleMaster.es).put("contentDescription", optionTextEs);
        return optionsValue;
    }
}