package com.opsorder.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID，主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 密码（加密后） */
    @TableField("password")
    private String password;

    /** 邮箱 */
    @TableField("email")
    private String email;

    /** 性别：0-未知，1-男，2-女 */
    @TableField("gender")
    private Integer gender;

    /** 生日 */
    @TableField("birthday")
    private LocalDate birthday;

    /** 状态：0-禁用，1-正常 */
    @TableField("status")
    private Integer status;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 最后登录时间 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    


}
