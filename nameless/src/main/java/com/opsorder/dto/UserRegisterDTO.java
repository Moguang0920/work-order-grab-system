package com.opsorder.dto;

import java.io.Serializable;

import lombok.Data;

@Data 
public class UserRegisterDTO implements Serializable{

   private static final long serialVersionUID = 1L;
   
   private String username;
  private String nickname;
  private String password;
  private String email;
  private String phone;
}
