package com.vibrent.drc.repository;

import com.vibrent.drc.domain.SystemProperties;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPropertiesRepository extends JpaRepository<SystemProperties, Long> {

    SystemProperties findByName(SystemPropertiesEnum drcGenomicsReportReadyStatus);
}
