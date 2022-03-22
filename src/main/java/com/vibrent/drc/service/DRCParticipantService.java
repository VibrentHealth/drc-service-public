package com.vibrent.drc.service;

import com.vibrent.drc.exception.DrcException;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;

import javax.validation.constraints.NotNull;

public interface DRCParticipantService {

    /**
     * Patching the participant, usually used after user test participant flag is updated
     * @param accountInfoUpdateEventDto AccountInfoUpdateEventDto
     */
    void patchTestParticipant(@NotNull AccountInfoUpdateEventDto accountInfoUpdateEventDto);

}
