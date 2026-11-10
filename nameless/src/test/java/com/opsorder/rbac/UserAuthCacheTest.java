package com.opsorder.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.opsorder.cache.UserAuthCache;
import com.opsorder.entity.User;
import com.opsorder.mapper.RoleMapper;
import com.opsorder.mapper.UserMapper;
import com.opsorder.nameless.NamelessApplication;

/**
 * 权限缓存行为测试。
 *
 * 重点验证两件事：
 *  1. 命中缓存时确实不再回源——绕过 Service 直接改库，load() 仍返回旧值，说明读的是缓存；
 *  2. 删除缓存后能回源拿到新值——这是「改完权限立即生效」的底层保证。
 *
 * 注：测试事务会回滚，不会触发 afterCommit 里的自动失效，所以这里手动调用 evict 模拟提交后的效果。
 */
@SpringBootTest(classes = NamelessApplication.class)
@Transactional
class UserAuthCacheTest {

    @Autowired
    private UserAuthCache userAuthCache;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 数据库回滚了，但缓存在 Redis 里不会跟着回滚。
     * 用例结束后必须清掉，否则会把「bob 是 user 角色」这个假状态留给后面的用例。
     */
    @AfterEach
    void cleanCache() {
        User bob = userMapper.selectOne(new QueryWrapper<User>().eq("email", "bob@test.com"));
        if (bob != null) {
            userAuthCache.evict(bob.getId());
        }
    }

    @Test
    @DisplayName("缓存命中读旧值，evict 后回源读新值")
    void cacheHitThenEvict() {
        User bob = userMapper.selectOne(new QueryWrapper<User>().eq("email", "bob@test.com"));
        assertNotNull(bob, "测试前置数据缺失，请先执行 rbac_data.sql");
        Long bobId = bob.getId();

        // 从干净状态开始：先清缓存，再读一次把 guest 角色写进缓存
        userAuthCache.evict(bobId);
        assertEquals(List.of("guest"), userAuthCache.load(bobId).roles());

        // 绕过 Service 直接改库（也就绕过了主动失效），模拟「缓存还没过期但库里已经变了」
        Long userRoleId = roleMapper.selectIdByRoleCode("user");
        assertNotNull(userRoleId, "角色 user 不存在");
        roleMapper.deleteUserRolesByUserId(bobId);
        roleMapper.insertUserRole(bobId, userRoleId);

        // 缓存还在：读到的仍是 guest —— 这一步证明 cache 真的生效了，不是每次都查库
        assertEquals(List.of("guest"), userAuthCache.load(bobId).roles(),
                "缓存未失效时应返回旧角色");
        assertTrue(userAuthCache.load(bobId).perms().contains("order:read"));

        // 删掉缓存后回源，读到新的 user 角色
        userAuthCache.evict(bobId);
        assertEquals(List.of("user"), userAuthCache.load(bobId).roles(),
                "缓存失效后应回源读到新角色");
        assertTrue(userAuthCache.load(bobId).perms().contains("user:read"));
    }
}
