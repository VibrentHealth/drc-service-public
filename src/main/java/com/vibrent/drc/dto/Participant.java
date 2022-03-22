package com.vibrent.drc.dto;

import com.vibrent.drc.enumeration.ConsentSuspensionStatus;
import com.vibrent.drc.enumeration.ConsentWithdrawStatus;
import com.vibrent.drc.enumeration.WithdrawalReason;
import lombok.Data;

import java.util.Map;

/**
 * this is a dto class matching what DRC is expecting, do not change the name unless you know DRC has changed it or
 * our communication will fail
 */
@Data
public class Participant {

    private String participantId;

    // structure when that information is known
    /**
     * "meta":
     * {
     * "versionId": "W/"1481733435084""
     * },
     */
    private Map<String, String> meta;

    private long externalId;

    private String biobankId;

    private String site;

    private String organization;

    private String awardee;

    private ConsentSuspensionStatus suspensionStatus;

    private ConsentWithdrawStatus withdrawalStatus;

    private WithdrawalReason withdrawalReason;

    private String withdrawalReasonJustification;

    private Long withdrawalTimeStamp;

    private Boolean testParticipant;
}
