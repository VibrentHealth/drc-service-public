package com.vibrent.drc.util;

import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.dto.Participant;

public class ParticipantUtil {

    private ParticipantUtil() {
        //private constructor
    }

    public static String findMetaVersion(Participant retrievedParticipant) {
        if (retrievedParticipant == null || retrievedParticipant.getMeta() == null) {
            return null;
        } else {
            return retrievedParticipant.getMeta().get(DrcConstant.META_VERSION_ID);
        }
    }
}
