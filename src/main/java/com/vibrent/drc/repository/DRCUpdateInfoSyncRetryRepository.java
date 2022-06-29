package com.vibrent.drc.repository;

import com.vibrent.drc.domain.DRCUpdateInfoSyncRetry;
import com.vibrent.drc.enumeration.DataTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

@Repository
public interface DRCUpdateInfoSyncRetryRepository extends JpaRepository<DRCUpdateInfoSyncRetry, Long> {

    Stream<DRCUpdateInfoSyncRetry> findByRetryCountLessThanOrderByUpdatedOnAsc(Long retryCount);
    DRCUpdateInfoSyncRetry findByVibrentIdAndType(long vibrentId, DataTypeEnum type);
    void deleteByVibrentIdAndType(long vibrentId, DataTypeEnum updateEntryType);
}