package com.opsorder.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsorder.common.Result;
import com.opsorder.dto.UserLoginByEmail;
import com.opsorder.dto.UserLoginByPhone;
import com.opsorder.dto.UserRegisterDTO;
import com.opsorder.service.UserService;

import org.springframework.web.bind.annotation.RequestBody;
// 注意：原来这里 import 的是 Swagger 的 io.swagger.v3.oas.annotations.parameters.RequestBody，
// 它是给 OpenAPI 文档用的注解，不会触发 Spring MVC 的消息体转换。
// 结果就是 POST 的 JSON body 完全收不到，参数会被当成表单字段绑定、全部为 null。
// 必须使用 Spring 的 @RequestBody 才能解析请求体。
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

@RestController 
@RequestMapping("/user")
public class UserController {

    @Resource 
    private UserService userService;

    @PostMapping("/register")
    public Result registerUser(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.registerUser(userRegisterDTO.getUsername(), userRegisterDTO.getPassword(), userRegisterDTO.getEmail(), userRegisterDTO.getPhone(), userRegisterDTO.getNickname());
       // return userService.registerUser(username, password, email, phone, nickname);
    }

    @PostMapping("/login/email")
    public Result loginUser(@Valid @RequestBody UserLoginByEmail userLoginDTO) {
        return userService.loginUserByEmail(userLoginDTO.getEmail(), userLoginDTO.getPassword());
    }

    @PostMapping("/login/phone")
    public Result loginUserByPhone(@Valid @RequestBody UserLoginByPhone userLoginDTO) {
        return userService.loginUserByPhone(userLoginDTO.getPhone(), userLoginDTO.getPassword());
    }
}