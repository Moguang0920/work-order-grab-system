package com.opsorder.dto;

import java.io.Serializable;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;

@Data
public class UserLoginByPhone implements Serializable{

    private static final long serialVersionUID = 1L;

    private String phone;
    private String password;
    private SaTokenInfo saTokenInfo;
    
}
