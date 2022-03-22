package com.vibrent.drc.domain;

import com.vibrent.drc.domain.common.AbstractEntity;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "SYSTEM_PROPERTIES")
@Data
public class SystemProperties extends AbstractEntity {

    private static final long serialVersionUID = 10834294759265643L;

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY, generator="IdOrGenerated")
    @GenericGenerator(name="IdOrGenerated", strategy="com.vibrent.drc.domain.UseIdOrGenerate")
    private Long id;

    @Enumerated(EnumType.STRING)
    private SystemPropertiesEnum name;

    @Column(name = "value", length = 1024)
    private String value;

    @Column(name = "text_value")
    private String textValue;

}
