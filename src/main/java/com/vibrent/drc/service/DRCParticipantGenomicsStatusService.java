package com.vibrent.drc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.drc.enumeration.ExternalEventType;
import com.vibrent.drc.enumeration.SystemPropertiesEnum;
import com.vibrenthealth.drcutils.exception.DrcConnectorException;

public interface DRCParticipantGenomicsStatusService {
    void retrieveParticipantGenomicsStatusFromDrc(String url, ExternalEventType externalEventType, SystemPropertiesEnum systemPropertiesEnum) throws DrcConnectorException, JsonProcessingException;
}
