# RBAC 权限模块说明

实现时间：2026-10-17
技术栈：Spring Boot 4.1.1 + Sa-Token 1.46.0 + MyBatis-Plus 3.5.17 + Redis（会话存储）

---

## 一、数据模型

```
user ──< sys_user_role >── sys_role ──< sys_role_permission >── sys_permission
```

| 表 | 作用 | 关键字段 |
|---|---|---|
| `sys_role` | 角色 | `role_code`（唯一，鉴权用的标识）、`role_name`、`status`、`deleted` |
| `sys_permission` | 权限 | `perm_code`（唯一，如 `user:read`）、`perm_name`、`perm_type`、`path` |
| `sys_user_role` | 用户-角色 | `user_id` + `role_id`（唯一键） |
| `sys_role_permission` | 角色-权限 | `role_id` + `perm_id`（唯一键） |

建表脚本：`src/main/resources/db/rbac_schema.sql`
测试数据：`src/main/resources/db/rbac_data.sql`（幂等，可重复执行）

> 注意：`role` 在 MySQL 8+ 是保留字，所以表名统一加了 `sys_` 前缀。

---

## 二、测试账号

| 邮箱 | 密码 | 角色 | 权限 |
|---|---|---|---|
| admin@test.com | Admin@123 | admin | 全部 8 个 |
| alice@test.com | Test@1234 | user | user:read、order:read |
| bob@test.com | Test@1234 | guest | order:read |

权限清单（8 个）：
`user:read`、`user:create`、`user:update`、`user:delete`、`role:read`、`role:assign`、`order:read`、`order:manage`

---

## 三、接口清单

### 角色 `/role`

| 方法 | 路径 | 需要的权限 | 说明 |
|---|---|---|---|
| GET | `/role/me` | 仅需登录 | 当前登录人：loginId + 角色 + 权限，前端可直接渲染菜单 |
| GET | `/role/list` | `role:read` | 角色列表 |
| POST | `/role/add` | `role:assign` | 新增角色 |
| DELETE | `/role/{id}` | `role:assign` | 删除角色（内置 admin/user/guest 禁删） |
| POST | `/role/assign` | `role:assign` | 给用户分配角色（覆盖式） |
| GET | `/role/user/{userId}` | `role:read` | 查某用户的角色 |
| GET | `/role/admin-only` | `admin` 角色 | 演示角色鉴权 |

### 权限 `/permission`

| 方法 | 路径 | 需要的权限 | 说明 |
|---|---|---|---|
| GET | `/permission/list` | `role:read` | 全部权限（做权限树用） |
| GET | `/permission/role/{roleId}` | `role:read` | 某角色已有权限 |
| GET | `/permission/user/{userId}` | `role:read` | 某用户最终继承的权限 |
| POST | `/permission/assign` | `role:assign` | 给角色分配权限（覆盖式） |
| GET | `/permission/mine` | 仅需登录 | 我自己的权限标识列表 |

### 请求示例

```bash
# 登录拿 token
curl -X POST http://localhost:8080/user/login/email \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"Admin@123"}'

# 后续请求带上 token（header 名就是 sa-token.token-name 配的 satoken）
curl http://localhost:8080/role/list -H "satoken: <tokenValue>"

# 给用户分配角色
curl -X POST http://localhost:8080/role/assign \
  -H "satoken: <token>" -H "Content-Type: application/json" \
  -d '{"userId":9,"roleCodes":["user","guest"]}'

# 给角色分配权限
curl -X POST http://localhost:8080/permission/assign \
  -H "satoken: <token>" -H "Content-Type: application/json" \
  -d '{"roleId":2,"permCodes":["user:read","order:read"]}'
```

---

## 四、鉴权是怎么串起来的

1. **登录**：`UserServiceImpl` 里 `StpUtil.login(user.getId())`，token 存 Redis（`SaTokenDaoForRedisTemplate`），重启不丢。
2. **第一道**：`SaTokenConfig` 注册的 `SaServletFilter` 拦截 `/**`，未登录直接 401。
   放行：`/user/register`、`/user/login/**`、`/v3/api-docs`、`/swagger-ui/**`。
3. **第二道**：`SaTokenConfig.addInterceptors` 注册 `SaInterceptor`，开启注解鉴权。
4. **权限数据源**：`StpInterfaceImpl` 实时查库（用户 → 角色 → 权限），所以改完角色权限**不用重新登录**就生效。
5. **注解**：Controller 上 `@SaCheckPermission("role:read")`、`@SaCheckRole("admin")`。
6. **异常统一**：`GlobalExceptionHandler` 把 `NotPermissionException` / `NotRoleException` 转成 403 的 `Result`；
   `SaServletFilter.setError` 把未登录转成 401 的 `Result`（已在配置里手动 JSON 序列化，否则会输出 `Result@xxxx`）。

---

## 五、测试用例

`src/test/java/com/example/rbac/RbacPermissionTest.java`，共 7 个用例，`@Transactional` 自动回滚不污染库：

| 用例 | 验证点 |
|---|---|
| adminHasAllPermissions | 管理员 8 个权限、admin 角色 |
| normalUserHasLimitedPermissions | 普通用户只有 2 个权限，没有管理权限 |
| guestOnlyHasOrderRead | 访客只有 order:read |
| assignRoleTakesEffectImmediately | 重新分配角色后权限立即变化 |
| addRoleWithDuplicateCodeShouldFail | 角色标识重复被拒绝 |
| addCustomRoleShouldSucceed | 新增自定义角色成功 |
| builtInRoleCanNotBeDeleted | 内置角色禁止删除 |

运行：

```bash
mvn test
# 结果：Tests run: 8, Failures: 0, Errors: 0  （含原有 contextLoads）
```

HTTP 层实测结果（均通过）：

| 场景 | 结果 |
|---|---|
| admin 访问 `/role/list` | 200，返回 3 个角色 |
| alice 访问 `/role/list` | 403 `无此权限：role:read` |
| alice 访问 `/role/admin-only` | 403 `无此角色：admin` |
| 无 token 访问 `/role/list` | 401 `未登录或登录已过期：未能读取到有效 token` |
| admin 给 bob 分配 [user,guest] | 成功，bob 权限从 `[order:read]` 变为 `[user:read, order:read]` |
| 新增/删除 auditor 角色 + 分配权限 | 全部成功 |

---

## 六、几个设计决定（踩过的坑）

1. **角色删除用物理删除**，不是逻辑删除。
   `role_code` 上有唯一索引，逻辑删除后同一个标识永远建不出来（唯一索引不认 `deleted`），所以删除时直接物理删除，并清理 `sys_user_role`、`sys_role_permission` 关联。
2. **分配是覆盖式**：`/role/assign` 先清空该用户所有角色再绑定，避免增量叠加导致权限越滚越大。
3. **权限表只做标识，不做继承**：目前是扁平的 `perm_code` 列表，`parent_id` 预留了树形结构，需要菜单树时可以直接用。
4. **Sa-Token 的 `setError` 只能输出字符串**：直接返回对象会变成 `com.opsorder.common.Result@32f5551a`，必须在里面手动序列化。

---

## 八、后续可扩展

- 数据权限（只能看本部门数据）：加 `dept_id` + 自定义注解
- 权限本地二级缓存（Caffeine）：省掉 Redis 往返，但多实例要靠 pub/sub 广播失效
- 操作日志：`@SaCheckPermission` + AOP 记录谁在什么时候改了谁的权限
- 前端菜单：`/role/me` 返回的 `permissions` 直接过滤路由表
