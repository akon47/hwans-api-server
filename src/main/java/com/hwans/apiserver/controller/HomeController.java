package com.hwans.apiserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.annotations.*;

@Controller
@ApiIgnore
public class HomeController {

    /**
     * root 페이지 접근 시 스웨거 페이지로 리다이렉트 시켜준다.
     *
     * @return 응답
     */
    @RequestMapping("/")
    public String index() {
        return "redirect:/swagger-ui/index.html";
    }

}
