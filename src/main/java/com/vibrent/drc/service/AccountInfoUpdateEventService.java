package com.vibrent.drc.service;

import com.vibrent.vxp.push.AccountInfoUpdateEventDto;

import java.util.Set;

public interface AccountInfoUpdateEventService {

    /**
     * Process Account Info updates
     */
    void processAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto);

    /**
     * Send Account Info updates to DRC
     */
    boolean sendAccountInfoUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto);

    /**
     * Send Secondary Contact or SSN updates to DRC
     */
    boolean sendSecondaryContactInfoAndSsnUpdates(AccountInfoUpdateEventDto accountInfoUpdateEventDto, String ssn, Set<String> secondaryContactTypes);

}
