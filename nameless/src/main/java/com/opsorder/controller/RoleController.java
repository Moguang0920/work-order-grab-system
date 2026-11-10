package com.opsorder.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsorder.common.Result;
import com.opsorder.dto.AssignRoleDTO;
import com.opsorder.entity.Role;
import com.opsorder.service.RoleService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;

/**
 * 角色管理接口。
 *
 * 鉴权说明：
 *  - /role/me 只需登录（全局过滤器已负责）
 *  - 其余接口按权限标识校验，admin 拥有全部权限
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    @Resource
    private RoleService roleService;

    /** 角色列表 */
    @SaCheckPermission("role:read")
    @GetMapping("/list")
    public Result listRoles() {
        return roleService.listRoles();
    }

    /** 新增角色 */
    @SaCheckPermission("role:assign")
    @PostMapping("/add")
    public Result addRole(@RequestBody Role role) {
        return roleService.addRole(role);
    }

    /** 删除角色（内置 admin/user/guest 不允许删除） */
    @SaCheckPermission("role:assign")
    @DeleteMapping("/{id}")
    public Result deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id);
    }

    /** 给用户分配角色 */
    @SaCheckPermission("role:assign")
    @PostMapping("/assign")
    public Result assignRoles(@RequestBody AssignRoleDTO dto) {
        return roleService.assignRolesToUser(dto.getUserId(), dto.getRoleCodes());
    }

    /** 查询某个用户的角色 */
    @SaCheckPermission("role:read")
    @GetMapping("/user/{userId}")
    public Result userRoles(@PathVariable Long userId) {
        return roleService.getUserRoles(userId);
    }

    /** 当前登录人信息（登录ID + 角色 + 权限），前端登录后可直接调它渲染菜单 */
    @GetMapping("/me")
    public Result me() {
        Map<String, Object> data = new HashMap<>();
        data.put("loginId", StpUtil.getLoginId());
        data.put("roles", StpUtil.getRoleList());
        data.put("permissions", StpUtil.getPermissionList());
        return Result.success(data);
    }

    /** 仅管理员可访问，用于验证角色鉴权 */
    @SaCheckRole("admin")
    @GetMapping("/admin-only")
    public Result adminOnly() {
        return Result.success("你是管理员，可以访问该接口");
    }
}
