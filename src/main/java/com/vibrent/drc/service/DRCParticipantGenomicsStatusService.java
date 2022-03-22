package com.vibrent.drc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;

public interface DRCParticipantGenomicsStatusService {
    void retrieveParticipantGenomicsStatusFromDrc() throws DrcConnectorException, JsonProcessingException;
}
