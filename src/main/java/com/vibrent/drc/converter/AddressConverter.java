package com.vibrent.drc.converter;

import com.vibrent.fulfillment.dto.AddressDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.util.ObjectUtils;

/**
 * @author jigar.patel
 */
@Mapper(componentModel = "spring")
public interface AddressConverter {

    @Mapping(target = "postalCode", expression = "java(parsePostalCode(addressDto))")
    void convertToAddressDto(@MappingTarget com.vibrent.vxp.workflow.AddressDto dto, AddressDto addressDto);

    default String parsePostalCode(AddressDto addressDto) {
        if(ObjectUtils.isEmpty(addressDto.getPostalCode())){
            return null;
        }
        return String.valueOf(addressDto.getPostalCode());
    }
}
