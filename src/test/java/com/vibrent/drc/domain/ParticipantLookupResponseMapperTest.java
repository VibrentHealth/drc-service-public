package com.vibrent.drc.domain;

import com.vibrent.drc.dto.UserSearchResponseDTO;
import com.vibrent.drc.enumeration.UserInfoType;
import com.vibrent.vxp.drc.dto.Participant;
import com.vibrent.vxp.drc.dto.ParticipantLookupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ParticipantLookupResponseMapperTest {

    private ParticipantLookupResponseMapper participantLookupResponseMapper;

    private UserSearchResponseDTO userSearchResponseDTO;

    @BeforeEach
    void setUp() {
        participantLookupResponseMapper = new ParticipantLookupResponseMapper();
        initializeUserSearchResponseDTO();
    }

    @Test
    void whenInputIsNullThenVerify() {
        assertNull(participantLookupResponseMapper.convertUserSearchResponse(null));
    }

    @Test
    void whenInputIsValidThenVerify() {
        ParticipantLookupResponse participantLookupResponse = participantLookupResponseMapper.convertUserSearchResponse(userSearchResponseDTO);
        assertNotNull(participantLookupResponse);

        List<Participant> participants = participantLookupResponse.getParticipants();
        assertNotNull(participants);
        assertEquals(1, participants.size());

        Participant participant = participants.get(0);
        assertEquals("V1", participant.getVibrentId());
        assertEquals("P1", participant.getDrcId());
        assertFalse(participant.getTestParticipant());
    }

    @Test
    void whenUserSearchResponseListIsNullThenVerify() {
        userSearchResponseDTO.setResults(null);
        assertNotNull(participantLookupResponseMapper.convertUserSearchResponse(userSearchResponseDTO));
    }

    private void initializeUserSearchResponseDTO() {
        List<Map<UserInfoType, Object>> responseList = new ArrayList<>();
        Map<UserInfoType, Object> user1Info = new HashMap<>();
        user1Info.put(UserInfoType.VIBRENT_ID, "V1");
        user1Info.put(UserInfoType.EXTERNAL_ID, "P1");
        responseList.add(user1Info);

        userSearchResponseDTO = new UserSearchResponseDTO();
        userSearchResponseDTO.setResults(responseList);
    }
}