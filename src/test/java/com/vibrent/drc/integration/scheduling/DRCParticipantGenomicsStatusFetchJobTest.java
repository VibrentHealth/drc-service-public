package com.vibrent.drc.integration.scheduling;

import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import com.vibrent.drc.integration.IntegrationTest;
import com.vibrent.drc.scheduling.DRCParticipantGenomicsStatusFetchJob;
import com.vibrent.drc.service.DRCParticipantGenomicsStatusService;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@EnableAutoConfiguration(exclude = {FlywayAutoConfiguration.class})
@Category(IntegrationTest.class)
@Transactional
public class DRCParticipantGenomicsStatusFetchJobTest extends IntegrationTest {

    @Autowired
    private DRCParticipantGenomicsStatusFetchJob drcParticipantGenomicsStatusFetchJob;

    @MockBean
    DRCParticipantGenomicsStatusService drcParticipantGenomicsStatusService;

    boolean genomicSchedulingWorkflow = true;

    @SneakyThrows
    @Test
    public void testJobExecution() {
        drcParticipantGenomicsStatusFetchJob = new DRCParticipantGenomicsStatusFetchJob(drcParticipantGenomicsStatusService,genomicSchedulingWorkflow);
        drcParticipantGenomicsStatusFetchJob.execute(null);
        verify(drcParticipantGenomicsStatusService, times(1)).retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_STATUS, ExternalEventType.DRC_GENOMICS_RESULT_STATUS, SystemPropertiesEnum.DRC_GENOMICS_REPORT_READY_STATUS);
        verify(drcParticipantGenomicsStatusService, times(1)).retrieveParticipantGenomicsStatusFromDrc(DrcConstant.URL_GENOMICS_PARTICIPANT_SCHEDULING, ExternalEventType.DRC_GENOMICS_SCHEDULING_STATUS, SystemPropertiesEnum.DRC_GENOMICS_SCHEDULING_STATUS);
    }
}
