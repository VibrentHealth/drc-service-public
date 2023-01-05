package com.vibrent.drc.service.impl;

import ca.uhn.fhir.context.FhirContext;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.drc.configuration.DrcProperties;
import com.vibrent.drc.domain.OrderTrackingDetails;
import com.vibrent.drc.dto.ExternalApiRequestLog;
import com.vibrent.drc.enumeration.ExternalEventSource;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.ExternalServiceType;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.exception.BusinessValidationException;
import com.vibrent.drc.service.*;
import com.vibrent.fulfillment.dto.OrderDetailsDTO;
import com.vibrent.fulfillment.dto.ProductDTO;
import com.vibrent.fulfillment.dto.TrackingDetailsDTO;
import com.vibrent.fulfillment.dto.TrackingTypeEnum;
import com.vibrent.genotek.vo.OrderInfoDTO;
import com.vibrent.vxp.workflow.*;
import com.vibrenthealth.drcutils.connector.HttpResponseWrapper;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;
import com.vibrenthealth.drcutils.service.DRCBackendProcessorService;
import com.vibrenthealth.drcutils.service.DRCConfigService;
import com.vibrenthealth.drcutils.service.impl.DRCConfigServiceImpl;
import com.vibrenthealth.drcutils.service.impl.DRCRetryServiceImpl;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DRCSalivaryOrderServiceImplTest {

    public static final String BIOBANK_ADDRESS_API_RESPONSE = "{\"city\": \"Rochester\", \"line\": [\"Mayo Clinic Laboratories\", \"3050 Superior Drive NW\"], \"state\": \"MN\", \"postalCode\": \"55901\"}";
    private String testWorkflowId = "1D2L3F4G";
    private String testTrackingId = "PARTTRACK123";
    private Long testTime = 1554399635000L;
    private String testOrderIdForFulfillment= "100";
    private String testSupplierOrderId= "100";
    private String testOrderId = "123456";
    private String testFulfillmentId = "FULLID123";
    private String testBarcode = "BARCODE123";
    private String testBiobankTrackingId = "BIOTRACK123";
    private String testParticipantTrackingId = "PARTTRACK123";
    private String testMessageId = "vxpmessage123";
    private String testParticipantId = "P12345676534";
    private Long testUserId = 132434L;

    private FhirContext fhirContext;

    private DRCSalivaryOrderServiceImpl drcSalivaryOrderService;

    private FHIRSalivaryConverterUtility fhirSalivaryConverterUtility;

    @Mock
    private ApiService apiService;

    @Mock
    private GenotekService genotekService;

    @Mock
    private DRCConfigService drcConfigService;

    @Mock
    private DRCSupplyStatusService drcSupplyStatusService;

    @Mock
    private DrcProperties drcProperties;

    @Mock
    private OrderTrackingDetailsService orderTrackingDetailsService;

    @Mock
    private DRCBackendProcessorWrapperImpl drcBackendProcessorWrapper;

    @Mock
    private ExternalApiRequestLogsService externalApiRequestLogsService;

    @Mock
    private DRCBackendProcessorService drcBackendProcessorService;

    @Captor
    ArgumentCaptor<String> fhirMessage;

    @BeforeEach
    public void setUp() throws Exception {
        drcBackendProcessorWrapper = new DRCBackendProcessorWrapperImpl(externalApiRequestLogsService, drcBackendProcessorService);
        drcConfigService = new DRCConfigServiceImpl(false, "https://pmi-drc-api-test.appspot.com");
        fhirSalivaryConverterUtility = new FHIRSalivaryConverterUtility("http://joinallofus.org/fhir/", apiService, genotekService);
        drcSalivaryOrderService = new DRCSalivaryOrderServiceImpl(fhirSalivaryConverterUtility, drcConfigService, drcSupplyStatusService, drcProperties, orderTrackingDetailsService);

        //Set Value attributes
        ReflectionTestUtils.setField(drcConfigService, "runPostProcessing", false);
        initializeContext();
        ReflectionTestUtils.setField(drcSalivaryOrderService, "fhirContext", fhirContext);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    private void initializeContext() {
        //set context initialization
        fhirContext = FhirContext.forR4();
    }

    @Test
    public void testVerifyAndSendSalivaryRequestMissingRequestCreateTrackOrderMessage() throws Exception {
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        Assert.assertThrows("Unable to continue DRC supply request due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse((CreateTrackOrderResponseDto) null, messageHeaderDto));
    }

    @Test
    public void testVerifyAndSendSalivaryRequestMissingRequestCreateTrackOrderHeader() throws Exception {
        //EXPECT Business validation on missing vxp requests
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        Assert.assertThrows("Unable to continue DRC supply request due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, null));
    }

    @Test
    public void testVerifyAndSendSalivaryRequestCreateTrackOrderSendDRC_SupplyRequestCreated() throws Exception {
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.CREATED);
        createTrackOrderResponseDto.setIdentifiers(Collections.singletonList(getOrderIdIdentifier()));

        drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(Matchers.anyList());

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"CREATED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}],\"status\":\"active\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1},\"authoredOn\":\"2019-04-04T17:40:35+00:00\",\"requester\":{\"reference\":\"#patient-1\"},\"supplier\":[{\"reference\":\"#supplier-1\"}],\"deliverFrom\":{\"reference\":\"#supplier-1\"},\"deliverTo\":{\"reference\":\"#patient-1\"}}",
                drcPayload);
    }

    @Test
    public void testVerifyAndSendSalivaryRequestCreateTrackOrderSendDRC_SupplyRequestFulfilled() throws Exception {
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT on Fulfilled workflow supplyRequest has proper values
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.PENDING_SHIPMENT);
        createTrackOrderResponseDto.setIdentifiers(Arrays.asList(getOrderIdIdentifier(), getFulfillmentIdentifier()));

        drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(Matchers.anyList());

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"PENDING_SHIPMENT\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"},{\"system\":\"http://joinallofus.org/fhir/fulfillmentId\",\"value\":\"FULLID123\"}],\"status\":\"active\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1},\"authoredOn\":\"2019-04-04T17:40:35+00:00\",\"requester\":{\"reference\":\"#patient-1\"},\"supplier\":[{\"reference\":\"#supplier-1\"}],\"deliverFrom\":{\"reference\":\"#supplier-1\"},\"deliverTo\":{\"reference\":\"#patient-1\"}}",
                drcPayload);
    }

    @Test
    public void testVerifyAndSendSalivaryRequestCreateTrackOrderSendDRC_SupplyRequestShipped() throws Exception {
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT on Shipped workflow supplyRequest has proper values
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.SHIPPED);
        createTrackOrderResponseDto.setIdentifiers(Arrays.asList(getOrderIdIdentifier(), getFulfillmentIdentifier(), getBarcodeIdentifier(),
                getTrackingToParticipantIdentifier(), getTrackingToBioBankIdentifier()));
        drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(Matchers.anyList());

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/barcode\",\"valueString\":\"BARCODE123\"},{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"SHIPPED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"},{\"system\":\"http://joinallofus.org/fhir/fulfillmentId\",\"value\":\"FULLID123\"}],\"status\":\"completed\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1},\"authoredOn\":\"2019-04-04T17:40:35+00:00\",\"requester\":{\"reference\":\"#patient-1\"},\"supplier\":[{\"reference\":\"#supplier-1\"}],\"deliverFrom\":{\"reference\":\"#supplier-1\"},\"deliverTo\":{\"reference\":\"#patient-1\"}}",
                drcPayload);
    }

    @Test
    public void testVerifyAndSendSalivaryRequestCreateTrackOrderSendDRC_SupplyRequestCancelled() throws Exception {
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT on Cancelled workflow supplyRequest has proper values
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.ERROR);
        createTrackOrderResponseDto.setIdentifiers(Collections.singletonList(getOrderIdIdentifier()));
        drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(Matchers.anyList());
    }

    @Test
    public void testVerifyAndSendTrackDeliveryMissingRequestTrackDeliveryMessage() {
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        Assert.assertThrows("Unable to continue supply delivery due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse((TrackDeliveryResponseDto) null, messageHeaderDto));
    }

    @Test
    public void testVerifyAndSendTrackDeliveryMissingHeaderDto() {
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("Unable to continue supply delivery due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(createTrackDeliveryResponseDto(), null));
    }

    @Test
    public void testVerifyAndSendTrackDeliveryMissingOrderDetails() {
        //EXPECT Business validation on missing vxp requests
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(null);
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        Assert.assertThrows("DRCSupplyStatusServiceImpl: Unable to continue supply delivery, salivary order status is null", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(createTrackDeliveryResponseDto(), messageHeaderDto));
    }

    @Test
    public void testVerifyAndSendSalivaryRequestTrackDeliverySendDRC_SupplyDeliveryParticipantShipped() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getParticipantTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);

        drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"IN_TRANSIT\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"status\":\"in-progress\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
                drcPayload);
    }

    @Test
    public void testNullDeviceDetailsAndSalivaryRequestTrackDelivery_SupplyDeliveryParticipantShipped() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getParticipantTrackingOrderDetails());
        doThrow(new BusinessProcessingException("DRC request failed")).when(this.genotekService).getDeviceDetails(anyLong());
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);

        var exception = assertThrows(BusinessProcessingException.class, () -> drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto));
        assertTrue(exception.getMessage().contains("DRC request failed"));
        Mockito.verify(drcSupplyStatusService, times(0)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(0)).save(any(OrderTrackingDetails.class));
        Mockito.verify(drcSupplyStatusService, times(0)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
    }

    @Test
    public void testVerifyAndSendSalivaryRequestTrackDeliverySendDRC_SupplyDeliveryParticipantDelivery() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getParticipantTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());

        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"DELIVERED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
                 drcPayload);
    }

    @Test
    public void testVerifyAndSendSalivaryRequestTrackDeliverySendDRC_SupplyDeliveryBioBankShipped() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.IN_TRANSIT);

        drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"IN_TRANSIT\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}}],\"status\":\"in-progress\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
                drcPayload);
    }

    @Test
    public void testVerifyAndSendSalivaryRequestTrackDeliverySendDRC_SupplyDeliveryBioBankDelivery() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);
        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"DELIVERED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
                drcPayload);
    }


    @Test
    public void testVerifyAndSendSalivaryRequestCreateTrackOrderSendExternalEventLogFailedLog() throws Exception {

        DRCSupplyStatusServiceImpl supplyStatusService = new DRCSupplyStatusServiceImpl(new DRCRetryServiceImpl(drcConfigService), drcBackendProcessorWrapper);
        drcSalivaryOrderService = new DRCSalivaryOrderServiceImpl(fhirSalivaryConverterUtility, drcConfigService, supplyStatusService, drcProperties, orderTrackingDetailsService);

        doThrow(new DrcConnectorException("400 bad request")).when(this.drcBackendProcessorService).sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class));
        when(drcBackendProcessorWrapper.isInitialized()).thenReturn(true);

        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.CREATED);
        createTrackOrderResponseDto.setIdentifiers(Collections.singletonList(getOrderIdIdentifier()));

        drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);

        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);
        verify(externalApiRequestLogsService, times(1)).send(captor.capture());

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.POST, actual.getHttpMethod());
        assertEquals("https://pmi-drc-api-test.appspot.com/SupplyRequest", actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNotNull(actual.getRequestBody());
        assertNotNull(actual.getResponseBody());
        assertEquals(400, actual.getResponseCode());
        assertNotEquals(0L, actual.getRequestTimestamp());
        assertNotEquals(0L, actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(ExternalEventType.DRC_SUPPLY_STATUS, actual.getEventType());
        assertNotNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE, actual.getEventSource());

    }

    @Test
    public void testVerifyAndSendSalivaryRequestCreateTrackOrderSendExternalEventLog() throws Exception {

        DRCSupplyStatusServiceImpl supplyStatusService = new DRCSupplyStatusServiceImpl(new DRCRetryServiceImpl(drcConfigService), drcBackendProcessorWrapper);
        drcSalivaryOrderService = new DRCSalivaryOrderServiceImpl(fhirSalivaryConverterUtility, drcConfigService, supplyStatusService, drcProperties, orderTrackingDetailsService);

        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenReturn(new HttpResponseWrapper(200, "response"));
        when(drcBackendProcessorWrapper.isInitialized()).thenReturn(true);

        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        CreateTrackOrderResponseDto createTrackOrderResponseDto = createTrackOrderResponseDTO();
        createTrackOrderResponseDto.setStatus(StatusEnum.CREATED);
        createTrackOrderResponseDto.setIdentifiers(Collections.singletonList(getOrderIdIdentifier()));

        drcSalivaryOrderService.verifyAndSendCreateTrackOrderResponse(createTrackOrderResponseDto, messageHeaderDto);

        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);
        verify(externalApiRequestLogsService, times(1)).send(captor.capture());

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.POST, actual.getHttpMethod());
        assertEquals("https://pmi-drc-api-test.appspot.com/SupplyRequest", actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNotNull(actual.getRequestBody());
        assertNotNull(actual.getResponseBody());
        assertEquals(200, actual.getResponseCode());
        assertNotEquals(0L, actual.getRequestTimestamp());
        assertNotEquals(0L, actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(ExternalEventType.DRC_SUPPLY_STATUS, actual.getEventType());
        assertNotNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE, actual.getEventSource());

    }


    @Test
    public void testVerifyAndSendSalivaryRequestTrackDeliverySendExternalEventLogWithSuccessResponse() throws Exception {
        DRCSupplyStatusServiceImpl supplyStatusService = new DRCSupplyStatusServiceImpl(new DRCRetryServiceImpl(drcConfigService), drcBackendProcessorWrapper);
        drcSalivaryOrderService = new DRCSalivaryOrderServiceImpl(fhirSalivaryConverterUtility, drcConfigService, supplyStatusService, drcProperties, orderTrackingDetailsService);

        when(drcProperties.getDrcApiBaseUrl()).thenReturn("https://pmi-drc-api-test.appspot.com");
        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenReturn(new HttpResponseWrapper(200, "response"));
        when(drcBackendProcessorWrapper.isInitialized()).thenReturn(true);

        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId)).thenReturn(getBiobankTrackingOrderDetails1());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);

        //EXPECT save and drc call to be made on supplyRequest
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        TrackDeliveryResponseDto trackDeliveryResponseDto = createTrackDeliveryResponseDto();
        trackDeliveryResponseDto.setStatus(StatusEnum.DELIVERED);

        drcSalivaryOrderService.verifyAndSendTrackDeliveryResponse(trackDeliveryResponseDto, messageHeaderDto);

//        Assertions.assertEquals("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"DELIVERED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"USPS\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"123456\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}},\"occurrenceDateTime\":\"2019-04-04T17:40:35+00:00\",\"supplier\":{\"reference\":\"#supplier-1\"},\"destination\":{\"reference\":\"#location-1\"}}",
//                drcPayload);

        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);
        verify(externalApiRequestLogsService, times(1)).send(captor.capture());

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.PUT, actual.getHttpMethod());
        assertEquals("https://pmi-drc-api-test.appspot.com/SupplyDelivery/" + testOrderId, actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNotNull(actual.getRequestBody());
        assertNotNull(actual.getResponseBody());
        assertEquals(200, actual.getResponseCode());
        assertNotEquals(0L, actual.getRequestTimestamp());
        assertNotEquals(0L, actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(ExternalEventType.DRC_SUPPLY_STATUS, actual.getEventType());
        assertNotNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE, actual.getEventSource());
    }

    @Test
    @DisplayName("Test failed External event log " +
            "when received Fulfillment order response ")
    void testVerifyAndSendSalivaryRequestCreateFulfillmentTrackOrderSendExternalEventLogFailedLog() throws Exception {

        DRCSupplyStatusServiceImpl supplyStatusService = new DRCSupplyStatusServiceImpl(new DRCRetryServiceImpl(drcConfigService), drcBackendProcessorWrapper);
        drcSalivaryOrderService = new DRCSalivaryOrderServiceImpl(fhirSalivaryConverterUtility, drcConfigService, supplyStatusService, drcProperties, orderTrackingDetailsService);
        doThrow(new DrcConnectorException("400 bad request")).when(this.drcBackendProcessorService).sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class));
        when(drcBackendProcessorWrapper.isInitialized()).thenReturn(true);
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());

        //EXPECT save and drc call to be made on supplyRequest
        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(createFulfillmentResponseDto(), createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);
        verify(externalApiRequestLogsService, times(1)).send(captor.capture());

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.POST, actual.getHttpMethod());
        assertEquals("https://pmi-drc-api-test.appspot.com/SupplyRequest", actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNotNull(actual.getRequestBody());
        assertNotNull(actual.getResponseBody());
        assertEquals(400, actual.getResponseCode());
        assertNotEquals(0L, actual.getRequestTimestamp());
        assertNotEquals(0L, actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(ExternalEventType.DRC_SUPPLY_STATUS, actual.getEventType());
        assertNotNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE, actual.getEventSource());
    }

    @Test
    @DisplayName("Test success External event log " +
            "when received Fulfillment order response ")
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendExternalEventLogWithSuccessResponse() throws Exception {
        DRCSupplyStatusServiceImpl supplyStatusService = new DRCSupplyStatusServiceImpl(new DRCRetryServiceImpl(drcConfigService), drcBackendProcessorWrapper);
        drcSalivaryOrderService = new DRCSalivaryOrderServiceImpl(fhirSalivaryConverterUtility, drcConfigService, supplyStatusService, drcProperties, orderTrackingDetailsService);

        when(drcProperties.getDrcApiBaseUrl()).thenReturn("https://pmi-drc-api-test.appspot.com");
        when(this.drcBackendProcessorService.sendRequestReturnDetails(anyString(), anyString(), any(RequestMethod.class), nullable(Map.class))).thenReturn(new HttpResponseWrapper(200, "response"));
        when(drcBackendProcessorWrapper.isInitialized()).thenReturn(true);
        when(this.orderTrackingDetailsService.getOrderDetails(testBiobankTrackingId, OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID)).thenReturn(getBiobankTrackingOrderDetails1());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);

        //EXPECT save and drc call to be made on supplyRequest
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.RETURN_DELIVERED);
        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        ArgumentCaptor<ExternalApiRequestLog> captor = ArgumentCaptor.forClass(ExternalApiRequestLog.class);
        verify(externalApiRequestLogsService, times(1)).send(captor.capture());

        //Check external Event
        ExternalApiRequestLog actual = captor.getValue();

        assertEquals(ExternalServiceType.DRC, actual.getService());
        assertEquals(RequestMethod.PUT, actual.getHttpMethod());
        assertEquals("https://pmi-drc-api-test.appspot.com/SupplyDelivery/" + testOrderId, actual.getRequestUrl());
        assertNull(actual.getRequestHeaders());
        assertNotNull(actual.getRequestBody());
        assertNotNull(actual.getResponseBody());
        assertEquals(200, actual.getResponseCode());
        assertNotEquals(0L, actual.getRequestTimestamp());
        assertNotEquals(0L, actual.getResponseTimestamp());
        assertEquals(testUserId, actual.getInternalId());
        assertEquals(ExternalEventType.DRC_SUPPLY_STATUS, actual.getEventType());
        assertNotNull(actual.getDescription());
        assertEquals(ExternalEventSource.DRC_SERVICE, actual.getEventSource());
    }

    @Test
    @DisplayName("When null Fulfillment order response received " +
            "then throw BusinessValidationException ")
    void testVerifyAndSendSalivaryRequestMissingRequestCreateFulfillmentTrackOrderMessage(){
        //EXPECT Business validation on missing vxp requests
        MessageHeaderDto messageHeaderDto = createFulfillmentMessageHeaderDTO();
        Assert.assertThrows("Unable to continue DRC supply request due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(null, messageHeaderDto, getOrderDetailsDTO(), getParticipantDto()));
    }

    @Test
    @DisplayName("When Fulfillment order response received with empty message header " +
            "then throw BusinessValidationException ")
    void testVerifyAndSendSalivaryRequestMissingRequestCreateFulfillmentTrackOrderHeader() {
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("Unable to continue DRC supply request due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(createFulfillmentResponseDto(), null, getOrderDetailsDTO(), getParticipantDto()));
    }

    @Test
    @DisplayName("When Fulfillment order response received with null order details " +
            "then throw BusinessValidationException ")
    void testVerifyAndSendSalivaryRequestMissingTrackingDetails() {
        //EXPECT Business validation on missing vxp requests
        Assert.assertThrows("Unable to continue supply delivery due to missing request", BusinessValidationException.class,
                () -> drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(createFulfillmentResponseDto(), createFulfillmentMessageHeaderDTO(), null, getParticipantDto()));
    }

    @Test
    @DisplayName("When Fulfillment order response received but genotek service is down " +
            "then throw Exception")
    void testNullDeviceDetailsAndSalivaryRequestFulfillmentTrackDelivery_SupplyDeliveryParticipantShipped() throws Exception {
        when(this.orderTrackingDetailsService.getOrderDetails(testTrackingId, OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID)).thenReturn(getParticipantTrackingOrderDetails());
        doThrow(new BusinessProcessingException("DRC request failed")).when(this.genotekService).getDeviceDetails(anyLong());
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.PARTICIPANT_IN_TRANSIT);
        Logger logger = (Logger) LoggerFactory.getLogger(DRCSalivaryOrderServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        var logList = listAppender.list;
        assertTrue(logList.get(1).getMessage().contains("DRC request failed"));
        assertEquals("ERROR",logList.get(1).getLevel().toString());
        Mockito.verify(drcSupplyStatusService, times(0)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(0)).save(any(OrderTrackingDetails.class));
    }

    @Test
    @DisplayName("When Fulfillment order response received with participant delivery status " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendDRC_SupplyDeliveryParticipantDelivery() {
        when(this.orderTrackingDetailsService.getOrderDetails(testParticipantTrackingId,OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID)).thenReturn(getParticipantTrackingOrderDetails());
        when(this.orderTrackingDetailsService.getOrderDetails(testParticipantTrackingId)).thenReturn(getParticipantTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        //EXPECT save and drc call to be made on supplyRequest
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.PARTICIPANT_DELIVERED);

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        //Verify order details saved to DB
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        assertTrue(drcPayload.contains("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"usps\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"PARTICIPANT_DELIVERED\"},{\"url\":\"http://joinallofus.org/fhir/expected-delivery-date\",\"valueDateTime\":\"1970-01-03T16:32:04+00:00\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"usps\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"PARTTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"100\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}}"));
    }

    @Test
    @DisplayName("When Fulfillment order response received with return shipped status " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendDRC_SupplyDeliveryBioBankShipped() {
        when(this.orderTrackingDetailsService.getOrderDetails(testBiobankTrackingId, OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.orderTrackingDetailsService.getOrderDetails(testBiobankTrackingId)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.RETURN_IN_TRANSIT);

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        assertTrue(drcPayload.contains("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"usps\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"RETURN_IN_TRANSIT\"},{\"url\":\"http://joinallofus.org/fhir/expected-delivery-date\",\"valueDateTime\":\"1970-01-01T18:14:16+00:00\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"usps\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"BIOTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"100\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"BIOTRACK123\"}}],\"status\":\"in-progress\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}}"));
    }

    @Test
    @DisplayName("When Fulfillment order response received with return delivery status " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendDRC_SupplyDeliveryBioBankDelivery() {
        when(this.orderTrackingDetailsService.getOrderDetails(testBiobankTrackingId, OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.orderTrackingDetailsService.getOrderDetails(testBiobankTrackingId)).thenReturn(getBiobankTrackingOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.apiService.getBioBankAddress()).thenReturn(BIOBANK_ADDRESS_API_RESPONSE);
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.RETURN_DELIVERED);

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        assertTrue(drcPayload.contains("{\"resourceType\":\"SupplyDelivery\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"usps\"},{\"resourceType\":\"Location\",\"id\":\"location-1\",\"address\":{\"use\":\"work\",\"type\":\"postal\",\"line\":[\"3050 Superior Drive NW\"],\"city\":\"Rochester\",\"state\":\"MN\",\"postalCode\":\"55901\"}}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/tracking-status\",\"valueString\":\"RETURN_IN_TRANSIT\"},{\"url\":\"http://joinallofus.org/fhir/expected-delivery-date\",\"valueDateTime\":\"1970-01-01T18:14:16+00:00\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/carrier\",\"valueString\":\"usps\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"BIOTRACK123\"}],\"basedOn\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"100\"}}],\"partOf\":[{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/trackingId\",\"value\":\"BIOTRACK123\"}}],\"status\":\"completed\",\"patient\":{\"identifier\":{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}},\"suppliedItem\":{\"quantity\":{\"value\":1},\"itemReference\":{\"reference\":\"#device-1\"}}"));
    }


    @Test
    @DisplayName("When Fulfillment order response received with SHIPPED status " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendDRC_SupplyOrderShipped() {
        when(this.orderTrackingDetailsService.getOrderDetails(testSupplierOrderId, OrderTrackingDetails.IdentifierType.ORDER_ID)).thenReturn(getOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();
        fulfillmentResponseDto.setStatus(OrderStatusEnum.SHIPPED);

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        assertTrue(drcPayload.contains("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}],\"address\":[{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"SHIPPED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"},{\"url\":\"http://joinallofus.org/fhir/barcode\",\"valueString\":\"BARCODE123\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"100\"},{\"system\":\"http://joinallofus.org/fhir/fulfillmentId\",\"value\":\"FULLID123\"}],\"status\":\"completed\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1}"));
    }

    @Test
    @DisplayName("When Fulfillment order response received with CREATED status " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendDRC_SupplyOrderCreated() {
        when(this.orderTrackingDetailsService.getOrderDetails(testOrderIdForFulfillment, OrderTrackingDetails.IdentifierType.ORDER_ID)).thenReturn(getOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();

        fulfillmentResponseDto.setAttributes(Map.of("FULFILLMENT_ID", testFulfillmentId));
        fulfillmentResponseDto.setStatus(OrderStatusEnum.CREATED);

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        assertTrue(drcPayload.contains("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}],\"address\":[{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"CREATED\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"100\"},{\"system\":\"http://joinallofus.org/fhir/fulfillmentId\",\"value\":\"FULLID123\"}],\"status\":\"active\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1}"));
    }


    @Test
    @DisplayName("When Fulfillment order response received with PENDING_SHIPMENT status " +
            "then process request and provide supply delivery")
    @SneakyThrows
    void testVerifyAndSendSalivaryRequestFulfillmentTrackDeliverySendDRC_SupplyOrderPendingShipment() {
        when(this.orderTrackingDetailsService.getOrderDetails(testSupplierOrderId, OrderTrackingDetails.IdentifierType.ORDER_ID)).thenReturn(getOrderDetails());
        when(this.genotekService.getDeviceDetails(anyLong())).thenReturn(buildOrderInfoDTO());
        when(this.drcSupplyStatusService.sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList())).thenReturn(getHttpResponseWrapper());
        FulfillmentResponseDto fulfillmentResponseDto = createFulfillmentResponseDto();

        fulfillmentResponseDto.setAttributes(Map.of("FULFILLMENT_ID", testFulfillmentId));
        fulfillmentResponseDto.setStatus(OrderStatusEnum.PENDING_SHIPMENT);

        drcSalivaryOrderService.verifyAndSendFulfillmentOrderResponse(fulfillmentResponseDto, createFulfillmentMessageHeaderDTO(), getOrderDetailsDTO(), getParticipantDto());

        Mockito.verify(drcSupplyStatusService, times(1)).sendSupplyStatus(fhirMessage.capture(), anyLong(), anyString(), any(RequestMethod.class), anyString(), anyString(), anyList());
        Mockito.verify(orderTrackingDetailsService, times(1)).save(any(OrderTrackingDetails.class));

        String drcPayload = fhirMessage.getValue();
        assertTrue(drcPayload.contains("{\"resourceType\":\"SupplyRequest\",\"text\":{\"status\":\"generated\",\"div\":\"<div xmlns=\\\"http://www.w3.org/1999/xhtml\\\">default narrative text</div>\"},\"contained\":[{\"resourceType\":\"Device\",\"id\":\"device-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/SKU\",\"value\":\"4081\"}],\"deviceName\":[{\"name\":\"OGD-500.015\",\"type\":\"manufacturer-name\"}]},{\"resourceType\":\"Patient\",\"id\":\"patient-1\",\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/participantId\",\"value\":\"P12345676534\"}],\"address\":[{\"use\":\"home\",\"type\":\"postal\",\"line\":[\"6644 E Baywood Ave\"],\"city\":\"Mesa\",\"state\":\"AZ\",\"postalCode\":\"85206\"}]},{\"resourceType\":\"Organization\",\"id\":\"supplier-1\",\"name\":\"Genotek\"}],\"extension\":[{\"url\":\"http://joinallofus.org/fhir/fulfillment-status\",\"valueString\":\"PENDING_SHIPMENT\"},{\"url\":\"http://joinallofus.org/fhir/order-type\",\"valueString\":\"Salivary Order\"}],\"identifier\":[{\"system\":\"http://joinallofus.org/fhir/orderId\",\"value\":\"100\"},{\"system\":\"http://joinallofus.org/fhir/fulfillmentId\",\"value\":\"FULLID123\"}],\"status\":\"active\",\"itemReference\":{\"reference\":\"#device-1\"},\"quantity\":{\"value\":1}"));
    }

    private OrderDetailsDTO getOrderDetailsDTO() {

        OrderDetailsDTO orderDetailsDTO = new OrderDetailsDTO();
        orderDetailsDTO.setId(Long.valueOf(testOrderIdForFulfillment));
        orderDetailsDTO.setProduct(new ProductDTO());
        orderDetailsDTO.setQuantity(1L);
        orderDetailsDTO.setStatus("SHIPPED");
        orderDetailsDTO.setProgramId(106L);
        orderDetailsDTO.setParticipantId(22L);
        orderDetailsDTO.setSupplierOrderId(testSupplierOrderId);

        Map<String, TrackingDetailsDTO> trackingMap = Map.of(
                "PARTICIPANT_TRACKING",getTrackingDetailsDto(testParticipantTrackingId,"PARTICIPANT_DELIVERED", TrackingTypeEnum.PARTICIPANT_TRACKING,"usps",232324334L,7676759L),
                "RETURN_TRACKING",getTrackingDetailsDto(testBiobankTrackingId,"RETURN_IN_TRANSIT",TrackingTypeEnum.RETURN_TRACKING,"usps",65656634L,32123259L)
        );
        orderDetailsDTO.setTrackings(trackingMap);
        return orderDetailsDTO;
    }

    private TrackingDetailsDTO getTrackingDetailsDto(String trackingId, String status, TrackingTypeEnum trackingType, String carrierCode, Long deliveredOn, Long shippedOn) {
        TrackingDetailsDTO trackingDetailsDTO = new TrackingDetailsDTO();
        trackingDetailsDTO.setTrackingId(trackingId);
        trackingDetailsDTO.setStatus(status);
        trackingDetailsDTO.setTrackingType(trackingType);
        trackingDetailsDTO.setCarrierCode(carrierCode);
        trackingDetailsDTO.setDeliveredOn(deliveredOn);
        trackingDetailsDTO.setShippedOn(shippedOn);
        return trackingDetailsDTO;
    }

    private ParticipantDto getParticipantDto() {
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setVibrentID(testUserId);
        participantDto.setExternalID(testParticipantId);
        participantDto.setAddresses(getParticipantAddress());
        return participantDto;
    }

    private List<AddressDto> getParticipantAddress() {
        List<AddressDto> addressDtoList = new ArrayList<>();
        AddressDto addressDto = new AddressDto();
        addressDto.setCity("Mesa");
        addressDto.setLine1("6644 E Baywood Ave");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        addressDto.setAddressType(TypeOfAddressEnum.HOME_ADDRESS);
        addressDtoList.add(addressDto);
        return addressDtoList;
    }

    private FulfillmentResponseDto createFulfillmentResponseDto() {
        FulfillmentResponseDto fulfillmentResponseDto = new FulfillmentResponseDto();

        fulfillmentResponseDto.setStatus(OrderStatusEnum.CREATED);
        fulfillmentResponseDto.setProgramID(123654L);
        fulfillmentResponseDto.setVibrentID(136524L);

        OrderDto orderDto = new OrderDto();
        orderDto.setFulfillmentOrderID(100L);
        orderDto.setQuantity(1L);
        fulfillmentResponseDto.setOrder(orderDto);

        Map<String,String> attributeMap = Map.of(
                "FULFILLMENT_ID",testFulfillmentId,
                "BARCODE_1D",testBarcode
        );
        fulfillmentResponseDto.setAttributes(attributeMap);
        return fulfillmentResponseDto;
    }

    private OrderTrackingDetails getParticipantTrackingOrderDetails() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testTrackingId);
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.PARTICIPANT_TRACKING_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderId));
        orderTrackingDetails.setUserId(testUserId);
        return orderTrackingDetails;
    }

    private OrderTrackingDetails getBiobankTrackingOrderDetails() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testTrackingId);
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderId));
        orderTrackingDetails.setUserId(testUserId);
        return orderTrackingDetails;
    }
    private OrderTrackingDetails getBiobankTrackingOrderDetails1() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testTrackingId);
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.RETURN_TRACKING_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderId));
        orderTrackingDetails.setUserId(testUserId);
        orderTrackingDetails.setLastMessageStatus("BIOBANK_SHIPPED");
        return orderTrackingDetails;
    }

    private MessageHeaderDto createMessageHeaderDTO() {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpWorkflowInstanceID(testWorkflowId);
        messageHeaderDto.setVxpMessageID(testMessageId);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        return messageHeaderDto;
    }

    private MessageHeaderDto createFulfillmentMessageHeaderDTO(){
        MessageHeaderDto messageHeaderDto = createMessageHeaderDTO();
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.FULFILLMENT_KIT_ORDER);
        return messageHeaderDto;
    }

    private CreateTrackOrderResponseDto createTrackOrderResponseDTO() {
        //SET request and mock global database object
        CreateTrackOrderResponseDto createTrackOrderResponseDto = new CreateTrackOrderResponseDto();

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();

        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        createTrackOrderResponseDto.setParticipant(participant);

        createTrackOrderResponseDto.setProvider(ProviderEnum.GENOTEK);
        createTrackOrderResponseDto.setDateTime(testTime);
        return createTrackOrderResponseDto;
    }

    private IdentifierDto getOrderIdIdentifier() {
        IdentifierDto identifier = new IdentifierDto();
        identifier.setId(testOrderId);
        identifier.setProvider(ProviderEnum.GENOTEK);
        identifier.setType(IdentifierTypeEnum.ORDER_ID);
        return identifier;
    }

    private IdentifierDto getFulfillmentIdentifier() {
        IdentifierDto identifier = new IdentifierDto();
        identifier.setId(testFulfillmentId);
        identifier.setProvider(ProviderEnum.GENOTEK);
        identifier.setType(IdentifierTypeEnum.FULFILLMENT_ID);
        return identifier;
    }

    private IdentifierDto getBarcodeIdentifier() {
        IdentifierDto identifier = new IdentifierDto();
        identifier.setId(testBarcode);
        identifier.setProvider(ProviderEnum.GENOTEK);
        identifier.setType(IdentifierTypeEnum.BARCODE_1_D);
        return identifier;
    }

    private IdentifierDto getTrackingToBioBankIdentifier() {
        IdentifierDto identifier = new IdentifierDto();
        identifier.setId(testBiobankTrackingId);
        identifier.setProvider(ProviderEnum.GENOTEK);
        identifier.setType(IdentifierTypeEnum.TRACKING_TO_BIOBANK);
        return identifier;
    }

    private IdentifierDto getTrackingToParticipantIdentifier() {
        IdentifierDto identifier = new IdentifierDto();
        identifier.setId(testParticipantTrackingId);
        identifier.setProvider(ProviderEnum.GENOTEK);
        identifier.setType(IdentifierTypeEnum.TRACKING_TO_PARTICIPANT);
        return identifier;
    }

    private TrackDeliveryResponseDto createTrackDeliveryResponseDto() {
        //SET request and mock global database object
        TrackDeliveryResponseDto trackDeliveryResponseDto = new TrackDeliveryResponseDto();
        trackDeliveryResponseDto.setOperation(OperationEnum.TRACK_DELIVERY);

        //if statement to check if supplyRequest or Delivery based on status
        ParticipantDto participant = new ParticipantDto();
        participant.setExternalID(testParticipantId);
        participant.setVibrentID(testUserId);
        participant.setAddresses(getMailingAddress());
        trackDeliveryResponseDto.setParticipant(participant);

        trackDeliveryResponseDto.setTrackingID(testTrackingId);
        trackDeliveryResponseDto.setDateTime(testTime);
        trackDeliveryResponseDto.setProvider(ProviderEnum.USPS);

        return trackDeliveryResponseDto;
    }

    private List<AddressDto> getMailingAddress() {
        List<AddressDto> mailingAddress = new ArrayList<>();
        AddressDto addressDto = new AddressDto();
        addressDto.setAddressType(TypeOfAddressEnum.MAILING_ADDRESS);
        addressDto.setCity("Mesa");
        addressDto.setCountry("US");
        addressDto.setLine1("6644 E Baywood Ave");
        addressDto.setPostalCode("85206");
        addressDto.setState("AZ");
        mailingAddress.add(addressDto);
        return mailingAddress;
    }
    private  HttpResponseWrapper getHttpResponseWrapper(){
        return new HttpResponseWrapper(201,"response");
    }

    private OrderInfoDTO buildOrderInfoDTO() {
        OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
        orderInfoDTO.setOrderType("Salivary Order");
        orderInfoDTO.setItemCode("4081");
        orderInfoDTO.setItemName("OGD-500.015");
        return orderInfoDTO;
    }

    private OrderTrackingDetails getFulfillmentOrderDetails() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testFulfillmentId);
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.FULFILLMENT_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderIdForFulfillment));
        orderTrackingDetails.setUserId(testUserId);
        return orderTrackingDetails;
    }

    private OrderTrackingDetails getOrderDetails() {
        OrderTrackingDetails orderTrackingDetails = new OrderTrackingDetails();
        orderTrackingDetails.setIdentifier(testOrderIdForFulfillment);
        orderTrackingDetails.setIdentifierType(OrderTrackingDetails.IdentifierType.ORDER_ID);
        orderTrackingDetails.setOrderId(Long.valueOf(testOrderIdForFulfillment));
        orderTrackingDetails.setUserId(testUserId);
        return orderTrackingDetails;
    }
}