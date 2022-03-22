package com.vibrent.drc.service;

import com.vibrent.drc.dto.UserSearchResponseDTO;

import java.util.List;
import java.util.Set;
import java.util.Optional;

public interface ParticipantService {

    UserSearchResponseDTO getParticipantsByVibrentIds(List<String> vibrentIds);

    UserSearchResponseDTO getParticipantsByDrcIds(List<String> drcIds);

    Long getVibrentId(String externalId);

    void fetchAndCacheVibrentIds(Set<String> externalIds);

    UserSearchResponseDTO getParticipants(List<String> vibrentIds, List<String> drcIds,
                                                  Optional<String> startDate, Optional<String> endDate,
                                                  Optional<Integer> page, Optional<Integer> pageSize );
}
