package com.opsorder.service;

import java.util.List;

import com.opsorder.common.Result;
import com.opsorder.entity.Role;

/**
 * 角色与「用户-角色」关系管理
 */
public interface RoleService {

    /** 角色列表 */
    Result listRoles();

    /** 新增角色 */
    Result addRole(Role role);

    /** 删除角色（内置角色受保护） */
    Result deleteRole(Long id);

    /** 给用户分配角色（覆盖式：先清空再绑定） */
    Result assignRolesToUser(Long userId, List<String> roleCodes);

    /** 查询某个用户的角色 */
    Result getUserRoles(Long userId);
}
