package com.vibrent.drc.domain;

import com.vibrent.drc.dto.UserSearchResponseDTO;
import com.vibrent.drc.enumeration.UserInfoType;
import com.vibrent.vxp.drc.dto.Participant;
import com.vibrent.vxp.drc.dto.ParticipantLookupResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Component
public class ParticipantLookupResponseMapper {

    public ParticipantLookupResponse convertUserSearchResponse(UserSearchResponseDTO userSearchResponseDTO) {
        if (userSearchResponseDTO == null) {
            return null;
        }

        ParticipantLookupResponse response = new ParticipantLookupResponse();
        response.setParticipants(buildParticipants(userSearchResponseDTO.getResults()));
        response.setTotal(userSearchResponseDTO.getTotal());
        response.setPage(userSearchResponseDTO.getPage());
        response.setLink(userSearchResponseDTO.getLink());
        return response;
    }

    private static List<Participant> buildParticipants(List<Map<UserInfoType, Object>> responseList) {
        if(CollectionUtils.isEmpty(responseList)) {
            return Collections.emptyList();
        }
        List<Participant> participants = new ArrayList<>(responseList.size());
        for(Map<UserInfoType, Object> values : responseList) {
            Participant participant = new Participant();
            participant.setVibrentId(values.get(UserInfoType.VIBRENT_ID) != null ? values.get(UserInfoType.VIBRENT_ID).toString() : null);
            participant.setDrcId(values.get(UserInfoType.EXTERNAL_ID) != null ? values.get(UserInfoType.EXTERNAL_ID).toString() : null);
            if(values.containsKey(UserInfoType.TEST_PARTICIPANT) && values.get(UserInfoType.TEST_PARTICIPANT) != null) {
                participant.setTestParticipant(Boolean.valueOf(values.get(UserInfoType.TEST_PARTICIPANT).toString()));
            }
            participants.add(participant);
        }
        return participants;
    }
}
