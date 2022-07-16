package com.hwans.apiserver.service.impl;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.user.UserCreateDto;
import com.hwans.apiserver.dto.user.UserDto;
import com.hwans.apiserver.entity.User;
import com.hwans.apiserver.mapper.UserMapper;
import com.hwans.apiserver.repository.user.UserRepository;
import com.hwans.apiserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserDto signup(UserCreateDto userCreateDto) {
        User user = userMapper.toEntity(userCreateDto);

        // 이미 해당 아이디가 존재할 경우
        if(userRepository.existsById(user.getId())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS, "이미 존재하는 사용자 아이디 입니다.");
        }

        // 새 사용자 정보 저장
        Optional<User> savedUser = Optional.ofNullable(userRepository.save(user));
        return userMapper.toDto(savedUser.orElseThrow(() -> new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR, "사용자 생성에 실패했습니다.")));
    }
}
