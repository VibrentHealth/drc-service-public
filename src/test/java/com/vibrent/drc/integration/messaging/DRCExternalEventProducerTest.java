package com.vibrent.drc.integration.messaging;

import com.vibrent.drc.messaging.producer.DrcExternalEventProducer;
import com.vibrent.drc.service.ParticipantService;
import com.vibrent.drc.service.impl.DrcNotificationRequestServiceImpl;
import com.vibrent.vxp.drc.dto.DrcNotificationRequestDTO;
import com.vibrent.vxp.drc.dto.EventTypes;
import com.vibrent.vxp.push.DRCExternalEventDto;
import com.vibrent.vxp.push.DRCExternalEventDtoWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DRCExternalEventProducerTest {

    private static final String TOPIC_NAME = "event.vxp.push.participant";

    private DrcExternalEventProducer drcExternalEventProducer;

    private DRCExternalEventDto drcExternalEventDto;

    @Mock
    private ListenableFuture future;

    @Mock
    private KafkaTemplate<String, DRCExternalEventDto> kafkaTemplate;

    @Mock
    private ParticipantService participantService;

    @BeforeEach
    void setUp() {
        drcExternalEventDto = new DrcNotificationRequestServiceImpl(participantService, drcExternalEventProducer)
                .buildExternalEventDto(new DrcNotificationRequestDTO().event(EventTypes.INFORMING_LOOP_STARTED).participantId("10100").messageBody("{\"module_type\": \"recreational_genetics\"}"));
        drcExternalEventProducer = new DrcExternalEventProducer(TOPIC_NAME, true, kafkaTemplate);
    }

    @Test
    @DisplayName("when producer is requested with valid data" +
            "then verify data send to kafka")
    void send() {
        when(kafkaTemplate.send((Message<DRCExternalEventDtoWrapper>) any())).thenReturn(future);
        drcExternalEventProducer.send(drcExternalEventDto);
        verify(kafkaTemplate).send(ArgumentMatchers.<Message<DRCExternalEventDtoWrapper>>any());
    }


    @Test
    @DisplayName("when kafka flag is disabled " +
            "when producer is requested with valid data" +
            "then verify kafka send API is not requested")
    void sendKafkaDisabled() {
        drcExternalEventProducer.setKafkaEnabled(false);
        drcExternalEventProducer.send(drcExternalEventDto);
        verify(kafkaTemplate, Mockito.times(0)).send(ArgumentMatchers.<Message<DRCExternalEventDtoWrapper>>any());
    }

    @Test
    @DisplayName("when kafka flag is enabled " +
            "when producer is requested with null data" +
            "then verify kafka send API is not requested")
    void whenDataSharingUpdateEventDtoWrapperIsNullThenReturn() {
        drcExternalEventProducer.setKafkaEnabled(true);
        drcExternalEventProducer.send(null);
        verify(kafkaTemplate, times(0)).send(ArgumentMatchers.<Message<DRCExternalEventDtoWrapper>>any());
    }
}