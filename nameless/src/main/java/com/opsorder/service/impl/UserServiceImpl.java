package com.opsorder.service.impl;

import com.opsorder.common.Result;
import com.opsorder.dto.UserLoginByEmail;
import com.opsorder.dto.UserLoginByPhone;
import com.opsorder.entity.User;
import com.opsorder.mapper.UserMapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.dao.DuplicateKeyException;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.opsorder.service.UserService;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // public static PasswordEncoderUtil passwordEncoder = new
    // PasswordEncoderUtil();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Result registerUser(String username, String password, String email, String phone, String nickname) {
        if (!StringUtils.hasText(phone)
                || !StringUtils.hasText(password)
                || !StringUtils.hasText(nickname)
                || !StringUtils.hasText(email)) {
            return Result.error("手机号、密码、昵称、邮箱不能为空");
        }

        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }

        if (password.length() < 6 || password.length() > 20) {
            return Result.error("密码长度需为6-20位");
        }

        if (nickname.length() > 20) {
            return Result.error("昵称长度不能超过20");
        }
        if (!email.matches("^[\\w.-]+@[\\w-]+\\.[\\w.-]+$")) {
            return Result.error("邮箱格式不正确");
        }
        // 基本检测
        Long phoneCount = this.count(new QueryWrapper<User>().eq("phone", phone));
        Long emailCount = this.count(new QueryWrapper<User>().eq("email", email));

        if (phoneCount > 0) {
            return Result.error(400, "手机号已注册");
        }
        if (emailCount > 0) {
            return Result.error(400, "邮箱已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        try {
            if (!this.save(user)) {
                return Result.error(500, "注册失败");
            }
        } catch (DuplicateKeyException e) {
            return Result.error(400, "手机号或邮箱已注册");
        }

        user.setPassword(null);
        return Result.success(user);

    }

    public Result loginUserByEmail(String email, String password) {
        if (!StringUtils.hasText(email)
                || !StringUtils.hasText(password)) {

            return Result.error(400, "邮箱或密码不能为空");
        }

        User user = this.getOne(new QueryWrapper<User>().eq("email", email));
        if (user == null||!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(400, "用户不存在或密码错误");
        }

        StpUtil.login(user.getId());
        UserLoginByEmail userLoginByEmail = new UserLoginByEmail();
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        userLoginByEmail.setSaTokenInfo(tokenInfo);
        userLoginByEmail.setEmail(email);
        userLoginByEmail.setPassword(null);
        return Result.success(userLoginByEmail);

    }

    public Result loginUserByPhone(String phone, String password) {
        if (!StringUtils.hasText(phone)
                || !StringUtils.hasText(password)) {

            return Result.error(400, "手机号或密码不能为空");
        }

        User user = this.getOne(new QueryWrapper<User>().eq("phone", phone));
        if (user == null||!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(400, "用户不存在或密码错误");
           
        }

        StpUtil.login(user.getId());                
        UserLoginByPhone userLoginByEmail = new UserLoginByPhone();
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        userLoginByEmail.setSaTokenInfo(tokenInfo);
        userLoginByEmail.setPhone(phone);
        userLoginByEmail.setPassword(null);
       
        return Result.success(userLoginByEmail);
      
    }
}