package com.opsorder.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsorder.common.Result;
import com.opsorder.dto.AssignPermissionDTO;
import com.opsorder.service.PermissionService;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.Resource;

/**
 * 权限管理接口
 */
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    /** 全部权限列表（用于后台权限勾选树） */
    @SaCheckPermission("role:read")
    @GetMapping("/list")
    public Result listPermissions() {
        return permissionService.listPermissions();
    }

    /** 查询某个角色已拥有的权限 */
    @SaCheckPermission("role:read")
    @GetMapping("/role/{roleId}")
    public Result permissionsByRole(@PathVariable Long roleId) {
        return permissionService.getPermissionsByRole(roleId);
    }

    /** 给角色分配权限 */
    @SaCheckPermission("role:assign")
    @PostMapping("/assign")
    public Result assignPermissions(@RequestBody AssignPermissionDTO dto) {
        return permissionService.assignPermissionsToRole(dto.getRoleId(), dto.getPermCodes());
    }

    /** 查询某个用户最终继承到的权限 */
    @SaCheckPermission("role:read")
    @GetMapping("/user/{userId}")
    public Result permissionsByUser(@PathVariable Long userId) {
        return permissionService.getPermissionsByUser(userId);
    }

    /** 当前登录人自己的权限标识列表 */
    @GetMapping("/mine")
    public Result myPermissions() {
        return Result.success(StpUtil.getPermissionList());
    }
}
