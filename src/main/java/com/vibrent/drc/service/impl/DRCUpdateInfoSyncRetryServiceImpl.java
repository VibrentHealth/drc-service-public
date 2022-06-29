package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.repository.DRCUpdateInfoSyncRetryRepository;
import com.vibrent.drc.service.AccountInfoUpdateEventService;
import com.vibrent.drc.service.DRCUpdateInfoSyncRetryService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Slf4j
@Service
public class DRCUpdateInfoSyncRetryServiceImpl implements DRCUpdateInfoSyncRetryService {

    private final long maxRetryCount;
    private final AccountInfoUpdateEventService accountInfoUpdateEventService;
    private final DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository;

    public DRCUpdateInfoSyncRetryServiceImpl(AccountInfoUpdateEventService accountInfoUpdateEventService,
                                             DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository,
                                             @Value("${vibrent.drc.sync.retry.maxRetryCount}") long maxRetryCount) {
        this.accountInfoUpdateEventService = accountInfoUpdateEventService;
        this.drcUpdateInfoSyncRetryRepository = drcUpdateInfoSyncRetryRepository;
        this.maxRetryCount = maxRetryCount;
    }

    @Override
    public Stream<DRCUpdateInfoSyncRetry> getEligibleEntries() {
        return drcUpdateInfoSyncRetryRepository.findByRetryCountLessThanOrderByUpdatedOnAsc(maxRetryCount);
    }

    @Override
    public void retryUpdateInfoSync(@NonNull DRCUpdateInfoSyncRetry entry) {
        if (!drcUpdateInfoSyncRetryRepository.existsById(entry.getId())) {
            return;
        }

        if (DataTypeEnum.ACCOUNT_UPDATE_DATA == entry.getType()) {
            sendAccountInfoUpdateSync(entry);
        }
    }

    private void sendAccountInfoUpdateSync(@NonNull DRCUpdateInfoSyncRetry entry) {

        AccountInfoUpdateEventDto accountUpdateEvent = getAccountInfoUpdateEventDto(entry);

        if (accountUpdateEvent == null) {
            log.info("DRC-SERVICE: DRCUpdateInfoSyncRetry entry with invalid AccountInfoUpdateEventDto payload.");
            drcUpdateInfoSyncRetryRepository.delete(entry);
        }

        accountInfoUpdateEventService.processAccountInfoUpdates(accountUpdateEvent, false);
    }

    private AccountInfoUpdateEventDto getAccountInfoUpdateEventDto(DRCUpdateInfoSyncRetry entry) {
        AccountInfoUpdateEventDto accountUpdateEvent = null;
        try {
            accountUpdateEvent = JacksonUtil.getMapper().readValue(entry.getPayload(), AccountInfoUpdateEventDto.class);
        } catch (JsonProcessingException e) {
            entry.setRetryCount(Long.MAX_VALUE);
            log.warn("DRC-SERVICE: Retry {} failed. Error Msg: {} for user Id: {}", entry.getType(), e.getMessage(), entry.getVibrentId());
        }
        return accountUpdateEvent;
    }
}
