package com.opsorder.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 给用户分配角色的请求体
 */
@Data
public class AssignRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标用户ID */
    private Long userId;

    /** 角色标识列表，如 ["admin", "user"] */
    private List<String> roleCodes;
}
