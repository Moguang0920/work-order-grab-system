package com.opsorder.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 给角色分配权限的请求体
 */
@Data
public class AssignPermissionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标角色ID */
    private Long roleId;

    /** 权限标识列表，如 ["user:read", "order:read"] */
    private List<String> permCodes;
}
