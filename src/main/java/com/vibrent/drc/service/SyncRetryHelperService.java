package com.vibrent.drc.service;

import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;

public interface SyncRetryHelperService {

    void addToRetryQueue(AccountInfoUpdateEventDto accountInfoUpdateEventDto,
                         boolean incrementRetryCounter,
                         String reason);

    void deleteByVibrentIdAndType(long vibrentId, DataTypeEnum dataTypeEnum);
}