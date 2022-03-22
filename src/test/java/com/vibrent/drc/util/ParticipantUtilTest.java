package com.vibrent.drc.util;

import com.vibrent.drc.constants.DrcConstant;
import com.vibrent.drc.dto.Participant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ParticipantUtilTest {

    @Test
    void testFindMetaVersion() {
        assertNull(ParticipantUtil.findMetaVersion(null));
        assertNull(ParticipantUtil.findMetaVersion(new Participant()));

        Participant participant = new Participant();
        participant.setMeta(new HashMap<>());
        assertNull(ParticipantUtil.findMetaVersion(participant));

        Map<String, String> meta = new HashMap<>();
        meta.put(DrcConstant.META_VERSION_ID, "1.0");
        participant.setMeta(meta);
        assertEquals("1.0", ParticipantUtil.findMetaVersion(participant));
    }
}