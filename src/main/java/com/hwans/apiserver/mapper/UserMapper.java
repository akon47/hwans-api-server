package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.user.UserCreateDto;
import com.hwans.apiserver.dto.user.UserDto;
import com.hwans.apiserver.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    @Mapping(source = "password", target = "password", ignore = true)
    public abstract User toEntity(UserCreateDto userCreateDtoDto);

    @AfterMapping
    public void afterMapping(@MappingTarget User target, UserCreateDto source) {
        target.setPassword(source.getPassword()); // TODO: 변환코드 필요
    }

    public abstract UserDto toDto(User user);
}
