package com.vibrent.drc.repository;

import com.vibrent.drc.domain.DrcSyncedStatus;
import com.vibrent.drc.enumeration.DataTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrcSyncedStatusRepository extends JpaRepository<DrcSyncedStatus, Long> {
    DrcSyncedStatus findByVibrentIdAndType(Long vibrentId, DataTypeEnum type);
}
