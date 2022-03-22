package com.vibrent.drc.util;

import com.vibrent.acadia.domain.enumeration.SecondaryContactType;
import com.vibrent.drc.vo.AddressElementVo;
import com.vibrent.drc.vo.ParticipantVo;
import com.vibrent.drc.vo.SecondaryContactVo;
import com.vibrent.vxp.push.*;
import lombok.NonNull;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.*;

import static com.vibrent.drc.constants.DrcConstant.*;

public final class ParticipantDataUtil {

    private ParticipantDataUtil() {
    }

    public static boolean isSSNUpdated(String ssn, ParticipantVo vo) {
        if (vo == null && ssn == null) {
            return false;
        }

        if (vo == null) {
            return true;
        }

        return isDifferent(ssn, vo.getSsn());
    }

    public static boolean isUserAccountUpdated(@NonNull ParticipantDto dto, ParticipantVo vo) {
        if (vo == null) {
            return true;
        }

        return isDifferent(vo.getEmailAddress(), getVerifiedContact(dto, TypeEnum.EMAIL))
                || isDifferent(vo.getVerifiedPhoneNumber(), getVerifiedContact(dto, TypeEnum.PHONE))
                || isDifferent(vo.getAccountAddress(), getAccountAddress(dto))
                || isDifferent(vo.getFirstName(), dto.getFirstName())
                || isDifferent(vo.getMiddleInitial(), dto.getMiddleInitial())
                || isDifferent(vo.getLastName(), dto.getLastName())
                || isDifferent(vo.getDateOfBirth(), dto.getDateOfBirth());
    }


    public static Set<String> findSecondaryContactAndSsnChanges(ParticipantVo participantVo, ParticipantDto participantDto, String ssn) {
        Map<String, SecondaryContactVo> secondaryContactsVo = participantVo.getSecondaryContacts();
        List<SecondaryContactDto> secondaryContactsDto = participantDto.getSecondaryContacts();
        Set<String> changes = new HashSet<>();
        if (CollectionUtils.isEmpty(secondaryContactsVo) && CollectionUtils.isEmpty(secondaryContactsDto)) {
            return changes;
        }

        if (secondaryContactsVo == null) {
            secondaryContactsVo = Collections.emptyMap();
        }

        if (secondaryContactsDto == null) {
            secondaryContactsDto = Collections.emptyList();
        }

        SecondaryContactDto secondaryContactOne = secondaryContactsDto.stream().filter(secondaryContactDto -> SecondaryContactType.CONTACT_ONE.toString().equals(secondaryContactDto.getPreference())).findFirst().orElse(null);
        SecondaryContactDto secondaryContactTwo = secondaryContactsDto.stream().filter(secondaryContactDto -> SecondaryContactType.CONTACT_TWO.toString().equals(secondaryContactDto.getPreference())).findFirst().orElse(null);
        if (isDifferent(secondaryContactsVo.get(SecondaryContactType.CONTACT_ONE.toString()), secondaryContactOne)) {
            changes.add(CONTACT_ONE);
        }
        if (isDifferent(secondaryContactsVo.get(SecondaryContactType.CONTACT_TWO.toString()), secondaryContactTwo)) {
            changes.add(CONTACT_TWO);
        }
        if (isSSNUpdated(ssn, participantVo)) {
            changes.add(SSN);
        }
        return changes;
    }

    public static boolean isDifferent(AddressElementVo accountAddressVo, AddressElementDto accountAddressDto) {

        if (accountAddressVo == null && accountAddressDto == null) {
            return false;
        }

        if (accountAddressVo != null && accountAddressDto != null) {
            return isDifferent(accountAddressVo.getCity(), accountAddressDto.getCity()) ||
                    isDifferent(accountAddressVo.getCountry(), accountAddressDto.getCountry()) ||
                    isDifferent(accountAddressVo.getLine1(), accountAddressDto.getLine1()) ||
                    isDifferent(accountAddressVo.getLine2(), accountAddressDto.getLine2()) ||
                    isDifferent(accountAddressVo.getPostalCode(), accountAddressDto.getPostalCode()) ||
                    isDifferent(accountAddressVo.getState(), accountAddressDto.getState()) ||
                    !Objects.equals(accountAddressVo.getValidated(), accountAddressDto.getValidated());
        }

        return true;
    }

    public static boolean isDifferent(SecondaryContactVo secondaryContactVo, SecondaryContactDto secondaryContactDto) {
        if (secondaryContactVo == null && secondaryContactDto == null) {
            return false;
        }

        if (secondaryContactVo != null && secondaryContactDto != null) {
            return
                    isDifferent(secondaryContactVo.getAddress(), CollectionUtils.firstElement(secondaryContactDto.getAddresses())) ||
                            isDifferent(secondaryContactVo.getFirstName(), secondaryContactDto.getFirstName()) ||
                            isDifferent(secondaryContactVo.getLastName(), secondaryContactDto.getLastName()) ||
                            isDifferent(secondaryContactVo.getMiddleInitial(), secondaryContactDto.getMiddleInitial()) ||
                            isDifferent(secondaryContactVo.getEmailAddress(), getContact(secondaryContactDto, TypeEnum.EMAIL)) ||
                            isDifferent(secondaryContactVo.getPhoneNumber(), getContact(secondaryContactDto, TypeEnum.PHONE)) ||
                            isDifferent(secondaryContactVo.getPreference(), secondaryContactDto.getPreference()) ||
                            isDifferent(secondaryContactVo.getRelationship(), secondaryContactDto.getRelationship());
        }

        return true;
    }

    public static String getContact(SecondaryContactDto secondaryContactDto, TypeEnum contactType) {
        if (secondaryContactDto != null && secondaryContactDto.getContacts() != null) {
            return secondaryContactDto.getContacts().stream()
                    .filter(contactElementDto -> contactElementDto.getContactType() == contactType)
                    .map(ContactElementDto::getContact)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        }
        return null;
    }

    public static AddressElementDto getAccountAddress(ParticipantDto dto) {
        if (dto != null && dto.getAddresses() != null) {
            return dto.getAddresses().stream()
                    .filter(address -> address.getAddressType() == AddressTypeEnum.ACCOUNT_ADDRESS)
                    .findFirst().orElse(null);
        }

        return null;
    }

    public static String getVerifiedContact(ParticipantDto dto, TypeEnum type) {
        if (dto != null && dto.getContacts() != null) {
            return dto.getContacts().stream()
                    .filter(contact -> contact.getContactType() == type && Boolean.TRUE.equals(contact.getVerified()))
                    .map(ContactElementDto::getContact)
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }

        return null;
    }

    public static boolean isTestFlagUpdated(@NotNull ParticipantDto dto, ParticipantVo vo) {
        if (vo == null) {
            return true;
        }
        if (dto.getTestUser() == null && vo.getTestUser() == null) {
            return false;
        }
        return !Objects.equals(dto.getTestUser(), vo.getTestUser());
    }

    private static boolean isDifferent(String left, String right) {
        return !Objects.equals(left, right);
    }

}
