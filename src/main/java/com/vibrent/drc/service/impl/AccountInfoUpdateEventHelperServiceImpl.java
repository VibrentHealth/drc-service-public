package com.vibrent.drc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.acadia.web.rest.dto.UserSSNDTO;
import com.vibrent.drc.converter.ParticipantConverter;
import com.vibrent.drc.domain.DrcSyncedStatus;
import com.vibrent.drc.enumeration.DataTypeEnum;
import com.vibrent.drc.repository.DrcSyncedStatusRepository;
import com.vibrent.drc.service.AccountInfoUpdateEventHelperService;
import com.vibrent.drc.service.ApiService;
import com.vibrent.drc.util.JacksonUtil;
import com.vibrent.drc.util.ParticipantDataUtil;
import com.vibrent.drc.vo.ParticipantVo;
import com.vibrent.vxp.push.AccountInfoUpdateEventDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor
@Service
public class AccountInfoUpdateEventHelperServiceImpl implements AccountInfoUpdateEventHelperService {
    DrcSyncedStatusRepository drcSyncedStatusRepository;
    ParticipantConverter participantConverter;
    ApiService apiService;

    @Override
    public void processIfUserAccountUpdated(AccountInfoUpdateEventDto accountInfoUpdateEventDto, BooleanSupplier sendUserInfo) {

        DrcSyncedStatus drcSyncedStatus = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        ParticipantVo participantVo = extractParticipantVo(drcSyncedStatus);

        if (accountInfoUpdateEventDto.getParticipant() != null
                && ParticipantDataUtil.isUserAccountUpdated(accountInfoUpdateEventDto.getParticipant(), participantVo)
                && sendUserInfo.getAsBoolean()) {
            saveSyncedUserAccountFields(accountInfoUpdateEventDto);
        }

    }

    @Override
    public void processIfUserSecondaryContactOrSSNUpdated(AccountInfoUpdateEventDto accountInfoUpdateEventDto, BiPredicate<String, Set<String>> sendSecondaryContactAndSSNInfo) {
        Set<String> secondaryContactAndSSNChanges = Collections.emptySet();
        ParticipantVo participantVo = null;
        String ssn = null;

        if (accountInfoUpdateEventDto.getParticipant() != null
                && Boolean.TRUE.equals(accountInfoUpdateEventDto.getParticipant().getHasSSN())) {
            UserSSNDTO userSSNDTO = apiService.getUserSsnByUserId(accountInfoUpdateEventDto.getVibrentID());
            if (userSSNDTO != null) {
                ssn = userSSNDTO.getSsn();
            }
        }

        if (accountInfoUpdateEventDto.getParticipant() != null &&
                !CollectionUtils.isEmpty(accountInfoUpdateEventDto.getParticipant().getSecondaryContacts())) {
            DrcSyncedStatus drcSyncedStatus = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
            participantVo = extractParticipantVo(drcSyncedStatus);
            secondaryContactAndSSNChanges = ParticipantDataUtil.findSecondaryContactAndSsnChanges(participantVo, accountInfoUpdateEventDto.getParticipant(), ssn);
        }

        if ((!CollectionUtils.isEmpty(secondaryContactAndSSNChanges) || participantVo == null) && sendSecondaryContactAndSSNInfo.test(ssn, secondaryContactAndSSNChanges)) {
            saveSecondaryContactAndSSNFields(accountInfoUpdateEventDto, ssn);
        }

    }

    @Override
    public void processIfTestParticipantUpdated(AccountInfoUpdateEventDto accountInfoUpdateEventDto, BooleanSupplier sendUserInfo) {
        DrcSyncedStatus drcSyncedStatus = drcSyncedStatusRepository.findByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(), DataTypeEnum.ACCOUNT_UPDATE_DATA);
        ParticipantVo participantVo = extractParticipantVo(drcSyncedStatus);
        if (accountInfoUpdateEventDto.getParticipant() != null
                && ParticipantDataUtil.isTestFlagUpdated(accountInfoUpdateEventDto.getParticipant(), participantVo)
                && sendUserInfo.getAsBoolean()) {
            saveSyncedUserTestParticipantFlag(accountInfoUpdateEventDto);
        }
    }

    void saveSyncedUserAccountFields(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        saveDrcSyncedStatus(accountInfoUpdateEventDto, participantVo ->
                participantConverter.updateUserAccountFields(accountInfoUpdateEventDto.getParticipant(), participantVo));
    }

    public DrcSyncedStatus saveSecondaryContactAndSSNFields(AccountInfoUpdateEventDto accountInfoUpdateEventDto, String ssn) {
        return saveDrcSyncedStatus(accountInfoUpdateEventDto, participantVo ->
                participantConverter.updateSecondaryContactsAndSSNFields(accountInfoUpdateEventDto.getParticipant(), participantVo, ssn));
    }

    public DrcSyncedStatus saveSsnFields(AccountInfoUpdateEventDto accountInfoUpdateEventDto, String ssn) {
        return saveDrcSyncedStatus(accountInfoUpdateEventDto, participantVo ->
                participantVo.setSsn(ssn));
    }

    void saveSyncedUserTestParticipantFlag(AccountInfoUpdateEventDto accountInfoUpdateEventDto) {
        saveDrcSyncedStatus(accountInfoUpdateEventDto, participantVo ->
                participantVo.setTestUser(accountInfoUpdateEventDto.getParticipant().getTestUser()));
    }

    protected DrcSyncedStatus saveDrcSyncedStatus(AccountInfoUpdateEventDto accountInfoUpdateEventDto, Consumer<ParticipantVo> updateParticipant) {
        DrcSyncedStatus drcSyncedStatus = drcSyncedStatusRepository.
                findByVibrentIdAndType(accountInfoUpdateEventDto.getVibrentID(),
                        DataTypeEnum.ACCOUNT_UPDATE_DATA);

        if (drcSyncedStatus == null) {
            drcSyncedStatus = new DrcSyncedStatus();
            drcSyncedStatus.setType(DataTypeEnum.ACCOUNT_UPDATE_DATA);
            drcSyncedStatus.setVibrentId(accountInfoUpdateEventDto.getVibrentID());
        }

        ParticipantVo participantVo = extractParticipantVo(drcSyncedStatus);
        if (participantVo == null) {
            participantVo = new ParticipantVo();
            participantVo.setVibrentID(accountInfoUpdateEventDto.getVibrentID());
        }

        updateParticipant.accept(participantVo);
        try {
            drcSyncedStatus.setData(JacksonUtil.getMapper().writeValueAsString(participantVo));
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert ParticipantVo to String", e);
            return null;
        }

        return drcSyncedStatusRepository.save(drcSyncedStatus);
    }

    private ParticipantVo extractParticipantVo(DrcSyncedStatus syncedData) {
        ParticipantVo vo = null;
        if (syncedData != null && !StringUtils.isEmpty(syncedData.getData())) {
            try {
                vo = JacksonUtil.getMapper().readValue(syncedData.getData(), ParticipantVo.class);
            } catch (JsonProcessingException e) {
                log.warn("drc-service: failed to parse the participant data for Account update event. Entry Id: {}", syncedData.getId());
            }
        }

        return vo;
    }

}
