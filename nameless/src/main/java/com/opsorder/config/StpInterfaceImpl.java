package com.opsorder.config;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.opsorder.cache.UserAuthCache;

import cn.dev33.satoken.stp.StpInterface;
import jakarta.annotation.Resource;

/**
 * Sa-Token 权限数据源。
 *
 * Sa-Token 在每次进行 @SaCheckRole / @SaCheckPermission 校验时，
 * 会通过这个接口拿到当前用户拥有的角色和权限集合。
 *
 * 这里走 Redis 缓存（UserAuthCache）：缓存命中直接返回，未命中回源查库并回填。
 * 管理员改完角色/权限后由写操作主动删 key，所以依然「改完即生效、无需重新登录」。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserAuthCache userAuthCache;

    /** 返回某个账号拥有的权限标识集合 */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        return userAuthCache.load(userId).perms();
    }

    /** 返回某个账号拥有的角色标识集合 */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        if (userId == null) {
            return Collections.emptyList();
        }
        return userAuthCache.load(userId).roles();
    }

    /** 登录时 StpUtil.login(user.getId()) 存的是用户ID，这里统一转成 Long */
    private Long toUserId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
