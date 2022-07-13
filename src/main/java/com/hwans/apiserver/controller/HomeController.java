package com.hwans.apiserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.annotations.*;

@Controller
@ApiIgnore
public class HomeController {

    @RequestMapping("/")
    public String index() {
        return "redirect:/swagger-ui/index.html";
    }

}
