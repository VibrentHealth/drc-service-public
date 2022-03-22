package com.vibrent.drc.domain;

import com.vibrent.drc.domain.common.AbstractEntity;
import com.vibrent.drc.enumeration.DataTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "drc_synced_status")
@Getter
@Setter
@ToString
public class DrcSyncedStatus extends AbstractEntity {

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

    @Column(name = "data")
    private String data;

}