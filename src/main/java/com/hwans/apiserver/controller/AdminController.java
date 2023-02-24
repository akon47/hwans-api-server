package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 Controller
 */
@Validated
@RestController
@Api(tags = "어드민")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AdminController {

}
