package org.modernash.renovatio.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.modernash.renovatio.core.dto.UserDto;
import org.modernash.renovatio.core.entity.UserEntity;

/**
 * Mapper for converting between {@link UserEntity} and {@link UserDto}.
 */
@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDto toDto(UserEntity entity);

    UserEntity toEntity(UserDto dto);
}
