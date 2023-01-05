package com.vibrent.drc.domain;

import com.vibrent.drc.domain.common.IdGeneratorAbstract;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table(name = "order_tracking_details")
@Getter
@Setter
@ToString
public class OrderTrackingDetails extends IdGeneratorAbstract {

    private static final long serialVersionUID = 5640737975537993933L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "IdOrGenerated")
    @GenericGenerator(name = "IdOrGenerated", strategy = "com.vibrent.drc.domain.UseIdOrGenerate")
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "participant_id")
    private String participantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "identifier_type")
    @Enumerated(EnumType.STRING)
    private IdentifierType identifierType;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "last_message_status")
    private String lastMessageStatus;

    public enum IdentifierType {
        NONE,
        ORDER_ID,
        PARTICIPANT_TRACKING_ID,
        RETURN_TRACKING_ID,
        FULFILLMENT_ID
    }
}