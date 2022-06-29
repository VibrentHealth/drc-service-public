package com.vibrent.drc.domain;

import com.vibrent.drc.domain.common.AbstractEntity;
import com.vibrent.drc.enumeration.DataTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;


@Entity
@Table(name = "update_info_sync_retry_entry")
@Getter
@Setter
@ToString
public class DRCUpdateInfoSyncRetry extends AbstractEntity {

    private static final long serialVersionUID = -852986324594350588L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "IdOrGenerated")
    @GenericGenerator(name = "IdOrGenerated", strategy = "com.vibrent.drc.domain.UseIdOrGenerate")
    private Long id;

    @Column(name = "vibrent_id")
    private Long vibrentId;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private DataTypeEnum type;

    @Column(name = "payload")
    private String payload;

    @Column(name = "retry_count")
    private Long retryCount;

    @Column(name = "error_details")
    private String errorDetails;
}