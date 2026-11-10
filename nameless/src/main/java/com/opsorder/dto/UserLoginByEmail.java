package com.opsorder.dto;

import java.io.Serializable;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;

@Data 
public class UserLoginByEmail implements Serializable{

    private static final long serialVersionUID = 1L;

    private String email;
    private String password;
    private SaTokenInfo saTokenInfo;
    
}
