package com.opsorder.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderUtil {

    // 推荐工作因子设为 12（默认是 10），安全性更高
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    /** 加密密码（注册、修改密码时使用） */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /** 校验密码（登录时使用） */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}