package com.hwans.apiserver.service;

import com.hwans.apiserver.dto.user.UserCreateDto;
import com.hwans.apiserver.dto.user.UserDto;
import com.hwans.apiserver.entity.User;

public interface UserService {
    UserDto signup(UserCreateDto userCreateDto);
}
