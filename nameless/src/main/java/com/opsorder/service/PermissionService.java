package com.opsorder.service;

import java.util.List;

import com.opsorder.common.Result;

/**
 * 权限与「角色-权限」关系管理
 */
public interface PermissionService {

    /** 全部权限列表 */
    Result listPermissions();

    /** 查询某个角色拥有的权限 */
    Result getPermissionsByRole(Long roleId);

    /** 给角色分配权限（覆盖式） */
    Result assignPermissionsToRole(Long roleId, List<String> permCodes);

    /** 查询某个用户最终拥有的权限（经角色聚合） */
    Result getPermissionsByUser(Long userId);
}
