package com.vibrent.drc.service;

import com.vibrent.vxp.push.AccountInfoUpdateEventDto;

import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;

public interface AccountInfoUpdateEventHelperService {
    void processIfUserAccountUpdated(AccountInfoUpdateEventDto accountInfoUpdateEventDto, BooleanSupplier sendUserInfo);

    void processIfUserSecondaryContactOrSSNUpdated(@NotNull AccountInfoUpdateEventDto accountInfoUpdateEventDto, BiPredicate<String, Set<String>> sendSecondaryContactAndSSNInfo);

    void processIfTestParticipantUpdated(AccountInfoUpdateEventDto accountInfoUpdateEventDto, BooleanSupplier sendUserInfo);
}
