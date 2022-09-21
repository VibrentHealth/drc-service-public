package com.vibrent.drc.converter;

import com.vibrent.drc.util.ParticipantDataUtil;
import com.vibrent.drc.vo.AddressElementVo;
import com.vibrent.drc.vo.ParticipantVo;
import com.vibrent.drc.vo.SecondaryContactVo;
import com.vibrent.vxp.push.AddressElementDto;
import com.vibrent.vxp.push.ParticipantDto;
import com.vibrent.vxp.push.SecondaryContactDto;
import com.vibrent.vxp.push.TypeEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface ParticipantConverter {


    @Mapping(target = "verifiedPhoneNumber", source = "phoneNumber")
    @Mapping(target = "accountAddress", expression = "java(getAccountAddress(dto))")
    @Mapping(target = "secondaryContacts", expression = "java(buildSecondaryContactMap(dto))")
    void updateParticipant(ParticipantDto dto, @MappingTarget ParticipantVo vo);


    @Mapping(target = "emailAddress", expression = "java(com.vibrent.drc.util.ParticipantDataUtil.getContactByTypeAndVerification(dto, com.vibrent.vxp.push.TypeEnum.EMAIL, Boolean.TRUE))")
    @Mapping(target = "verifiedPhoneNumber", expression = "java(com.vibrent.drc.util.ParticipantDataUtil.getContactByTypeAndVerification(dto, com.vibrent.vxp.push.TypeEnum.PHONE, Boolean.TRUE))")
    @Mapping(target = "phoneNumber", expression = "java(com.vibrent.drc.util.ParticipantDataUtil.getContactByTypeAndVerification(dto, com.vibrent.vxp.push.TypeEnum.PHONE, Boolean.FALSE))")
    @Mapping(target = "accountAddress", expression = "java(getAccountAddress(dto))")
    @Mapping(ignore = true, target = "secondaryContacts")
    @Mapping(ignore = true, target = "testUser")
    @Mapping(ignore = true, target = "ssn")
    void updateUserAccountFields(ParticipantDto dto, @MappingTarget ParticipantVo vo);

    @Mapping(ignore = true, target = "emailAddress")
    @Mapping(ignore = true, target = "verifiedPhoneNumber")
    @Mapping(ignore = true, target = "phoneNumber")
    @Mapping(ignore = true, target = "accountAddress")
    @Mapping(ignore = true, target = "firstName")
    @Mapping(ignore = true, target = "middleInitial")
    @Mapping(ignore = true, target = "lastName")
    @Mapping(ignore = true, target = "dateOfBirth")
    @Mapping(target = "secondaryContacts", expression = "java(buildSecondaryContactMap(dto))")
    @Mapping(ignore = true, target = "testUser")
    void updateSecondaryContactsAndSSNFields(ParticipantDto dto, @MappingTarget ParticipantVo vo, String ssn);

    @Mapping(target = "address", expression = "java(getSecondaryContactAddress(secondaryContactDto))")
    @Mapping(target = "emailAddress", expression = "java(getContactFromSecondaryContact(secondaryContactDto, com.vibrent.vxp.push.TypeEnum.EMAIL))")
    @Mapping(target = "phoneNumber", expression = "java(getContactFromSecondaryContact(secondaryContactDto, com.vibrent.vxp.push.TypeEnum.PHONE))")
    SecondaryContactVo toSecondaryContact(SecondaryContactDto secondaryContactDto);

    AddressElementVo toAddress(AddressElementDto addressElementDto);

    @Named("SecondaryContactAddress")
    default AddressElementVo getSecondaryContactAddress(SecondaryContactDto dto) {
        if (CollectionUtils.isEmpty(dto.getAddresses())) {
            return null;
        }
        return toAddress(dto.getAddresses().get(0));
    }

    @Named("GetSecondaryContactOfType")
    default String getContactFromSecondaryContact(SecondaryContactDto dto, TypeEnum typeEnum) {
        return ParticipantDataUtil.getContact(dto, typeEnum);
    }

    @Named("GetAccountAddress")
    default AddressElementVo getAccountAddress(ParticipantDto dto) {
        return toAddress(ParticipantDataUtil.getAccountAddress(dto));
    }

    @Named("GetSecondaryContact")
    default Map<String, SecondaryContactVo> buildSecondaryContactMap(ParticipantDto dto) {
        Map<String, SecondaryContactVo> secondaryContactVoMap = new HashMap<>();
        if (dto != null && dto.getSecondaryContacts() != null) {
            for (var contact : dto.getSecondaryContacts()) {
                if(contact != null && contact.getPreference() != null) {
                    secondaryContactVoMap.put(contact.getPreference(), toSecondaryContact(contact));
                }
            }
        }
        return secondaryContactVoMap;
    }
}
