package com.hwans.apiserver.controller;

import com.hwans.apiserver.dto.user.UserCreateDto;
import com.hwans.apiserver.dto.user.UserDto;
import com.hwans.apiserver.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "사용자")
@RequestMapping(value = "/api")
public class UserController {
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @ApiOperation(value = "사용자 생성", notes = "새로운 사용자를 생성한다.", tags = "사용자")
    @PostMapping(value = "/v1/user")
    public UserDto createUser(@ApiParam(value = "새로운 사용자 생성 정보", required = true) @RequestBody UserCreateDto userCreateDto) {
        return new UserDto().setName(userCreateDto.getName());
    }

    @ApiOperation(value = "내 사용자 정보 조회", notes = "내 사용자 정보를 조회합니다.", tags = "사용자")
    @GetMapping(value = "/v1/user/me")
    public UserDto getMyUserInfo() {
        return new UserDto().setName("");
    }
}
