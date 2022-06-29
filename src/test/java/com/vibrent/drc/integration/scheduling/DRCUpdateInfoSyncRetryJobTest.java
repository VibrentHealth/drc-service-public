package com.vibrent.drc.integration.scheduling;

import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.integration.IntegrationTest;
import com.vibrent.drc.repository.DRCUpdateInfoSyncRetryRepository;
import com.vibrent.drc.scheduling.DRCUpdateInfoSyncRetryJob;
import com.vibrent.drc.service.AccountInfoUpdateEventService;
import com.vibrent.drc.util.JacksonUtil;
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
public class DRCUpdateInfoSyncRetryJobTest extends IntegrationTest {

    @Autowired
    DRCUpdateInfoSyncRetryJob drcUpdateInfoSyncRetryJob;

    @Autowired
    DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository;

    @MockBean
    AccountInfoUpdateEventService accountInfoUpdateEventService;


    @SneakyThrows
    @Test
    public void testJobExecution() {
        DRCUpdateInfoSyncRetry e1 = buildDrcUpdateInfoSyncRetryEntry(1L);
        DRCUpdateInfoSyncRetry e2 = buildDrcUpdateInfoSyncRetryEntry(2L);
        drcUpdateInfoSyncRetryRepository.save(e1);
        drcUpdateInfoSyncRetryRepository.save(e2);

        drcUpdateInfoSyncRetryJob.execute(null);

        verify(accountInfoUpdateEventService, times(2)).processAccountInfoUpdates(any(AccountInfoUpdateEventDto.class), anyBoolean());
    }

    @SneakyThrows
    @Test
    public void tesJobtExecuteOnlyForRetryCountLessThanMaxValue() {
        DRCUpdateInfoSyncRetry e1 = buildDrcUpdateInfoSyncRetryEntry(1L);
        DRCUpdateInfoSyncRetry e2 = buildDrcUpdateInfoSyncRetryEntry(2L);
        e1.setRetryCount(Long.MAX_VALUE);
        drcUpdateInfoSyncRetryRepository.save(e1);
        drcUpdateInfoSyncRetryRepository.save(e2);

        //Insert SecondContact Entry
        drcUpdateInfoSyncRetryJob.execute(null);

        verify(accountInfoUpdateEventService, times(1)).processAccountInfoUpdates(any(AccountInfoUpdateEventDto.class), anyBoolean());
    }



    @SneakyThrows
    private DRCUpdateInfoSyncRetry buildDrcUpdateInfoSyncRetryEntry(long vibrentId) {
        var entry = new DRCUpdateInfoSyncRetry();
        entry.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);
        entry.setVibrentId(vibrentId);
        entry.setRetryCount(0L);
        var dto = new AccountInfoUpdateEventDto();
        dto.setVibrentID(vibrentId);
        dto.setExternalID("P" + vibrentId);
        entry.setPayload(JacksonUtil.getMapper().writeValueAsString(dto));

        return entry;
    }
}
