package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.exception.BusinessProcessingException;
import com.vibrent.drc.repository.DRCUpdateInfoSyncRetryRepository;
import com.vibrent.drc.service.SyncRetryHelperService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SyncRetryHelperServiceImpl implements SyncRetryHelperService {

    private final DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository;

    public SyncRetryHelperServiceImpl(DRCUpdateInfoSyncRetryRepository drcUpdateInfoSyncRetryRepository) {
        this.drcUpdateInfoSyncRetryRepository = drcUpdateInfoSyncRetryRepository;
    }

    @Override
    @Transactional
    public void addToRetryQueue(AccountInfoUpdateEventDto accountInfoUpdateEventDto, boolean incrementRetryCounter, String reason) {

        DRCUpdateInfoSyncRetry entry = drcUpdateInfoSyncRetryRepository.findByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        if (entry == null) {
            entry = new DRCUpdateInfoSyncRetry();
            entry.setVibrentId(accountInfoUpdateEventDto.getVibrentID());
            entry.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);
            entry.setRetryCount(0L);
            entry.setErrorDetails(reason);
        }

        if (incrementRetryCounter) {
            entry.setRetryCount(entry.getRetryCount() == null ? 1L : entry.getRetryCount() + 1L);
        }

        entry.setErrorDetails(reason);

        try {
            entry.setPayload(JacksonUtil.getMapper().writeValueAsString(accountInfoUpdateEventDto));
            drcUpdateInfoSyncRetryRepository.save(entry);
        } catch (JsonProcessingException e) {
            throw new BusinessProcessingException("Failed to add entry to retry queue ");
        }
    }

    @Override
    public void deleteByVibrentIdAndType(long vibrentId, DataTypeEnum dataTypeEnum) {
        drcUpdateInfoSyncRetryRepository.deleteByVibrentIdAndType(vibrentId, dataTypeEnum);
    }

}
