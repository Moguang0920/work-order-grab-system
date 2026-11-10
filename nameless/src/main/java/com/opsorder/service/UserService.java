package com.opsorder.service;
import com.baomidou.mybatisplus.extension.repository.IRepository;

import com.opsorder.common.Result;
import com.opsorder.entity.User;

import cn.dev33.satoken.stp.SaTokenInfo;

public interface UserService extends IRepository<User> {
    
    /**
     * 注册
     * @param username
     * @param password
     * @param email
     * @param phone
     * @param nickname
     * @return
     */
        public Result registerUser(String username, String password, String email, String phone, String nickname);

        /**     
         * 登录
         * 
         * 
         */
        public Result loginUserByEmail(String email, String password);

        public Result loginUserByPhone(String phone, String password);

    
}
