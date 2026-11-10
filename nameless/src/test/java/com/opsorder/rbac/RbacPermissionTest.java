package com.opsorder.rbac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.opsorder.cache.UserAuthCache;
import com.opsorder.common.Result;
import com.opsorder.config.StpInterfaceImpl;
import com.opsorder.entity.Role;
import com.opsorder.entity.User;
import com.opsorder.mapper.RoleMapper;
import com.opsorder.mapper.UserMapper;
import com.opsorder.nameless.NamelessApplication;
import com.opsorder.service.RoleService;

/**
 * RBAC 权限模块测试用例。
 *
 * 说明：
 *  - 依赖 rbac_schema.sql / rbac_data.sql 已经执行，测试账号为 admin@test.com、alice@test.com、bob@test.com
 *  - 类上加 @Transactional，每个用例结束后自动回滚，不会污染数据库
 *  - 启动类在 com.opsorder.nameless 包下，测试类在 com.opsorder.rbac，需显式指定 classes
 */
@SpringBootTest(classes = NamelessApplication.class)
@Transactional
class RbacPermissionTest {

    private static final String LOGIN_TYPE = "login";

    @Autowired
    private StpInterfaceImpl stpInterface;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserAuthCache userAuthCache;

    /**
     * 权限现在走 Redis 缓存，而测试事务最终会回滚、不会触发 afterCommit 里的缓存失效，
     * 缓存会跨用例残留。所以每个用例开始前先清掉测试账号的缓存，
     * 保证用例从数据库的真实状态出发（而不是上一个用例留下的缓存）。
     */
    @BeforeEach
    void clearAuthCache() {
        userAuthCache.evict(userIdOf("alice@test.com"));
        userAuthCache.evict(userIdOf("bob@test.com"));
    }

    /**
     * 同样要清理：用例里读到的可能是「未提交的角色变更」，会被写进缓存，
     * 而事务回滚只回滚数据库、不会回滚 Redis。不清理会把假状态留给后面的人和正在跑的应用。
     */
    @AfterEach
    void cleanAuthCache() {
        userAuthCache.evict(userIdOf("alice@test.com"));
        userAuthCache.evict(userIdOf("bob@test.com"));
    }

    /** 按邮箱取用户ID，取不到直接让用例失败 */
    private Long userIdOf(String email) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
        assertNotNull(user, "测试前置数据缺失，请先执行 rbac_data.sql，邮箱：" + email);
        return user.getId();
    }

    @Test
    @DisplayName("管理员：拥有 8 个权限，角色为 admin")
    void adminHasAllPermissions() {
        Long adminId = userIdOf("admin@test.com");

        List<String> perms = stpInterface.getPermissionList(adminId, LOGIN_TYPE);
        assertEquals(8, perms.size(), "管理员应有 8 个权限");
        assertTrue(perms.contains("role:assign"), "管理员应有分配角色权限");
        assertTrue(perms.contains("user:delete"), "管理员应有删除用户权限");

        assertEquals(List.of("admin"), stpInterface.getRoleList(adminId, LOGIN_TYPE));
    }

    @Test
    @DisplayName("普通用户：只有 user:read 和 order:read，没有管理权限")
    void normalUserHasLimitedPermissions() {
        Long aliceId = userIdOf("alice@test.com");

        List<String> perms = stpInterface.getPermissionList(aliceId, LOGIN_TYPE);
        assertEquals(2, perms.size());
        assertTrue(perms.contains("user:read"));
        assertTrue(perms.contains("order:read"));
        assertFalse(perms.contains("role:assign"), "普通用户不应有分配角色权限");
        assertFalse(perms.contains("user:delete"), "普通用户不应有删除用户权限");
    }

    @Test
    @DisplayName("访客：只有 order:read")
    void guestOnlyHasOrderRead() {
        Long bobId = userIdOf("bob@test.com");

        assertEquals(List.of("order:read"), stpInterface.getPermissionList(bobId, LOGIN_TYPE));
        assertEquals(List.of("guest"), stpInterface.getRoleList(bobId, LOGIN_TYPE));
    }

    @Test
    @DisplayName("给用户重新分配角色后，权限应即时变化")
    void assignRoleTakesEffectImmediately() {
        Long bobId = userIdOf("bob@test.com");

        Result result = roleService.assignRolesToUser(bobId, Arrays.asList("user"));
        assertTrue(result.getSuccess(), "分配角色应成功");

        // 线上是「事务提交后」删缓存，而测试事务注定回滚、afterCommit 不会执行，
        // 这里手动删一次，等价于提交后的效果，否则会读到上一次缓存的旧权限。
        userAuthCache.evict(bobId);

        List<String> perms = stpInterface.getPermissionList(bobId, LOGIN_TYPE);
        assertEquals(2, perms.size());
        assertTrue(perms.contains("user:read"), "升级为 user 角色后应获得 user:read");
        assertEquals(List.of("user"), stpInterface.getRoleList(bobId, LOGIN_TYPE));
    }

    @Test
    @DisplayName("角色标识重复时不允许新增")
    void addRoleWithDuplicateCodeShouldFail() {
        Role role = new Role();
        role.setRoleCode("admin");
        role.setRoleName("重复的管理员");

        Result result = roleService.addRole(role);
        assertFalse(result.getSuccess());
        assertTrue(result.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("新增自定义角色成功，且可重复查询到")
    void addCustomRoleShouldSucceed() {
        Role role = new Role();
        role.setRoleCode("auditor");
        role.setRoleName("审计员");
        role.setDescription("只读审计角色");

        Result result = roleService.addRole(role);
        assertTrue(result.getSuccess(), "新增角色应成功");

        Long id = roleMapper.selectIdByRoleCode("auditor");
        assertNotNull(id, "新增后应能按角色标识查到");
        assertEquals("审计员", roleMapper.selectById(id).getRoleName());
    }

    @Test
    @DisplayName("内置角色 admin 不允许删除")
    void builtInRoleCanNotBeDeleted() {
        Long adminRoleId = roleMapper.selectIdByRoleCode("admin");

        Result result = roleService.deleteRole(adminRoleId);
        assertFalse(result.getSuccess());
        assertTrue(result.getMessage().contains("内置角色"));
    }
}
