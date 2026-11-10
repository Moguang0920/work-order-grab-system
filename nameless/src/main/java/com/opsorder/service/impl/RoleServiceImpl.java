package com.opsorder.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.opsorder.cache.UserAuthCache;
import com.opsorder.common.Result;
import com.opsorder.entity.Role;
import com.opsorder.entity.User;
import com.opsorder.mapper.PermissionMapper;
import com.opsorder.mapper.RoleMapper;
import com.opsorder.mapper.UserMapper;
import com.opsorder.service.RoleService;

import jakarta.annotation.Resource;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    /** 内置角色不允许删除，避免把系统管理员角色误删 */
    private static final List<String> BUILT_IN_ROLES = Arrays.asList("admin", "user", "guest");

    @Resource
    private UserMapper userMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private UserAuthCache userAuthCache;

    @Override
    public Result listRoles() {
        List<Role> roles = this.list(new QueryWrapper<Role>().orderByAsc("sort"));
        return Result.success(roles);
    }

    @Override
    public Result addRole(Role role) {
        if (role == null
                || !StringUtils.hasText(role.getRoleCode())
                || !StringUtils.hasText(role.getRoleName())) {
            return Result.error(400, "角色标识与角色名称不能为空");
        }
        Long exists = this.baseMapper.selectIdByRoleCode(role.getRoleCode());
        if (exists != null) {
            return Result.error(400, "角色标识已存在：" + role.getRoleCode());
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getSort() == null) {
            role.setSort(0);
        }
        role.setDeleted(0);
        return this.save(role) ? Result.success(role) : Result.error(500, "新增角色失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteRole(Long id) {
        Role role = this.getById(id);
        if (role == null) {
            return Result.error(400, "角色不存在");
        }
        if (BUILT_IN_ROLES.contains(role.getRoleCode())) {
            return Result.error(400, "内置角色不允许删除：" + role.getRoleCode());
        }
        // 删除前先把受影响的用户取出来：删完关联表就查不到是谁了
        List<Long> affectedUserIds = this.baseMapper.selectUserIdsByRoleId(id);

        // 物理删除角色，并清理用户关联和权限关联。
        // 不能用逻辑删除：role_code 带唯一索引，逻辑删除后同一个标识再也建不出来。
        this.baseMapper.deleteUserRolesByRoleId(id);
        permissionMapper.deleteRolePermissionsByRoleId(id);
        boolean removed = this.baseMapper.deleteRolePhysically(id) > 0;
        if (!removed) {
            return Result.error(500, "删除失败");
        }
        // 这些人的角色没了，缓存必须失效，否则最长 30 分钟内仍带着旧角色
        userAuthCache.evictByIdsAfterCommit(affectedUserIds);
        return Result.success("删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result assignRolesToUser(Long userId, List<String> roleCodes) {
        if (userId == null || roleCodes == null || roleCodes.isEmpty()) {
            return Result.error(400, "用户ID与角色标识不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(400, "用户不存在：" + userId);
        }

        List<Long> roleIds = new ArrayList<>();
        for (String code : roleCodes) {
            Long roleId = this.baseMapper.selectIdByRoleCode(code);
            if (roleId == null) {
                return Result.error(400, "角色不存在：" + code);
            }
            roleIds.add(roleId);
        }

        // 覆盖式分配：先清空旧角色，再绑定新角色
        this.baseMapper.deleteUserRolesByUserId(userId);
        for (Long roleId : roleIds) {
            this.baseMapper.insertUserRole(userId, roleId);
        }
        // 必须在事务提交后删缓存，否则并发请求会读到旧角色又写回缓存
        userAuthCache.evictAfterCommit(userId);
        return Result.success("分配成功");
    }

    @Override
    public Result getUserRoles(Long userId) {
        return Result.success(this.baseMapper.selectRolesByUserId(userId));
    }
}
