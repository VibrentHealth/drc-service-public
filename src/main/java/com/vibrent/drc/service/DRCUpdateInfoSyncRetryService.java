package com.vibrent.drc.service;

import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import lombok.NonNull;

import java.util.stream.Stream;

public interface DRCUpdateInfoSyncRetryService {
    Stream<DRCUpdateInfoSyncRetry> getEligibleEntries();

    void retryUpdateInfoSync(@NonNull DRCUpdateInfoSyncRetry entry);
}
