package com.opsorder.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.opsorder.cache.UserAuthCache;
import com.opsorder.common.Result;
import com.opsorder.entity.Permission;
import com.opsorder.entity.Role;
import com.opsorder.mapper.PermissionMapper;
import com.opsorder.mapper.RoleMapper;
import com.opsorder.service.PermissionService;

import jakarta.annotation.Resource;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserAuthCache userAuthCache;

    @Override
    public Result listPermissions() {
        List<Permission> list = this.list(new QueryWrapper<Permission>().orderByAsc("sort"));
        return Result.success(list);
    }

    @Override
    public Result getPermissionsByRole(Long roleId) {
        return Result.success(this.baseMapper.selectPermsByRoleId(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result assignPermissionsToRole(Long roleId, List<String> permCodes) {
        if (roleId == null || permCodes == null || permCodes.isEmpty()) {
            return Result.error(400, "角色ID与权限标识不能为空");
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            return Result.error(400, "角色不存在：" + roleId);
        }

        List<Long> permIds = new ArrayList<>();
        for (String code : permCodes) {
            Long permId = this.baseMapper.selectIdByPermCode(code);
            if (permId == null) {
                return Result.error(400, "权限不存在：" + code);
            }
            permIds.add(permId);
        }

        // 这个角色下所有人的权限都变了，先把人取出来，提交后统一失效
        List<Long> affectedUserIds = roleMapper.selectUserIdsByRoleId(roleId);

        // 覆盖式分配：先清空旧权限，再绑定新权限
        this.baseMapper.deleteRolePermissionsByRoleId(roleId);
        for (Long permId : permIds) {
            this.baseMapper.insertRolePermission(roleId, permId);
        }
        userAuthCache.evictByIdsAfterCommit(affectedUserIds);
        return Result.success("分配成功");
    }

    @Override
    public Result getPermissionsByUser(Long userId) {
        return Result.success(this.baseMapper.selectPermsByUserId(userId));
    }
}
