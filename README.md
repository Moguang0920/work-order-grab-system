# nameless · 运维工单抢单系统

一个面向设备运维场景的后端系统：工程师从抢单池里抢工单、处理、完成，管理员通过 RBAC 控制谁能做什么。

重点解决了一个具体问题：**多人同时抢同一张工单时，怎么保证只有一个人抢到**。

---

## 核心亮点

| 亮点       | 做法                             | 结果                           |
| -------- | ------------------------------ | ---------------------------- |
| 抢单并发控制   | 数据库条件更新（CAS），靠影响行数判定           | **75 并发抢 1 单，恰好 1 人成功，零超抢**  |
| 幂等       | 业务查重 + 唯一索引 + 状态条件三层           | 重复点击不产生重复流水                  |
| 权限查询     | 三表 JOIN 改为 Redis 缓存 + 事务提交后精准失效 | **194 → 534 RPS，P95 222 → 109ms**，改完权限仍立即生效 |
| 性能定位     | k6 压测 → 找到瓶颈 → 优化 → 再压测        | 50 并发同场对照，吞吐 **+176%**       |
| 并发边界     | 50 与 150 VU 对照，找吞吐拐点 | 150 VU 吞吐反降 44%，拐点在 **50~75** |



---

## 技术栈

- **框架**：Spring Boot 4.1.1 / Java 17（运行时 JDK 21）
- **持久层**：MyBatis-Plus 3.5.17 + MySQL 9.5
- **鉴权**：Sa-Token 1.46（注解式 + 路由式）
- **缓存**：Redis 7.4（会话存储 + 超时延时队列）
- **文档**：SpringDoc OpenAPI（Swagger UI）
- **测试**：JUnit 5（28 个用例）+ k6（压测）

---

## 功能

**权限（RBAC）**

- 用户 → 角色 → 权限三级模型，四张表
- 注解鉴权：`@SaCheckPermission("order:grab")`、`@SaCheckRole("admin")`
- 动态分配：给用户分配角色、给角色分配权限，覆盖式分配
- 权限实时生效，不需要重新登录

**工单与抢单**

- 抢单池分页查询（只返回待抢）
- 抢单 / 完成 / 放弃，状态机约束流转
- 15 分钟未处理自动释放回池（Redis ZSet 延时队列 + 定时兜底扫表）
- 抢单流水记录（审计 + 幂等）

---

## 快速开始

### 方式一：Docker（推荐）

```bash
# 只起 MySQL + Redis，应用在本地跑（改代码不用重建镜像）
docker compose up -d

# 或者连应用一起容器化启动
docker compose --profile full up -d
```

数据库首次启动会自动建表并写入测试数据（`docker/mysql-init.sql`）。

> 如果本机已装 MySQL/Redis 占用 3306/6379，先停掉本机服务，或修改 `docker-compose.yml` 里的端口映射。

### 方式二：本地中间件

```bash
# 1. 导入表结构和测试数据（按顺序）
mysql -uroot -p123123 < nameless/src/main/resources/db/rbac_schema.sql
mysql -uroot -p123123 < nameless/src/main/resources/db/rbac_data.sql
mysql -uroot -p123123 < nameless/src/main/resources/db/order_schema.sql
mysql -uroot -p123123 < nameless/src/main/resources/db/order_data.sql

# 2. 启动
cd nameless
mvn spring-boot:run
```

Windows 下也可以直接双击项目根目录的 `run.bat`（会先检查端口再编译启动）。

启动后：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI：<http://localhost:8080/v3/api-docs>

---

## 测试账号

| 邮箱                   | 密码        | 角色       | 能做什么            |
| -------------------- | --------- | -------- | --------------- |
| <admin@test.com>     | Admin@123 | admin    | 全部权限（含角色权限管理）   |
| <alice@test.com>     | Test@1234 | user     | 查看用户、查看订单       |
| <bob@test.com>       | Test@1234 | guest    | 只能查看订单          |
| <engineer1@test.com> | Test@1234 | engineer | 抢单、完成、放弃        |
| <engineer2@test.com> | Test@1234 | engineer | 同上（用来演示两个人抢同一单） |

登录后把返回的 `data.saTokenInfo.tokenValue` 放到请求头 `satoken` 里。

---

## 接口概览

| 方法   | 路径                     | 权限               | 说明             |
| ---- | ---------------------- | ---------------- | -------------- |
| POST | `/user/register`       | 公开               | 注册             |
| POST | `/user/login/email`    | 公开               | 邮箱登录，返回 token  |
| GET  | `/role/me`             | 登录               | 当前用户 + 角色 + 权限 |
| POST | `/role/assign`         | `role:assign`    | 给用户分配角色        |
| POST | `/permission/assign`   | `role:assign`    | 给角色分配权限        |
| GET  | `/order/pool`          | `order:grab`     | 抢单池（分页）        |
| POST | `/order/{id}/grab`     | `order:grab`     | 抢单             |
| GET  | `/order/my`            | `order:grab`     | 我的工单           |
| POST | `/order/{id}/complete` | `order:complete` | 完成             |
| POST | `/order/{id}/release`  | `order:grab`     | 放弃，回到池子        |

三种返回码的区别：**401** 没登录、**403** 登录了但没权限、**409** 状态冲突（比如单被抢走了）。

---

## 设计说明

### 抢单为什么用条件更新，而不是分布式锁

```sql
UPDATE work_order
   SET assignee_id = #{userId}, status = 'PROCESSING', grab_time = NOW()
 WHERE id = #{orderId} AND status = 'PENDING' AND assignee_id IS NULL
```

单库场景下，这一条 SQL 就够了：数据库行锁天然把并发请求串行化，影响行数为 1 才是抢到。分布式锁要多一次网络往返、要处理锁过期和续期，在这个量级是负收益。

**如果将来分库分表**，同一张单会固定路由到某个库，CAS 仍然成立；只有在"扣减库存"这类跨行场景才需要 Redis 预扣。

### 超时释放为什么用 Redis ZSet

用 `@Scheduled` 全表扫 `status='PROCESSING' AND deadline < NOW()` 在数据量上来后会拖库（即使有索引）。改成 ZSet 存到期时间戳，每次只取到期的，代价是 O(log N)：

```
ZADD work:order:timeout <到期时间戳> <orderId>
ZRANGEBYSCORE work:order:timeout 0 <now> LIMIT 0 100
```

Redis 里的数据可能丢，所以定时任务还会按 DB 的 `deadline` 兜底扫一次——**DB 才是最终事实来源**。

### 权限实时查库的代价

`StpInterfaceImpl` 每次鉴权都查一次三表 JOIN，好处是权限变更立即生效，代价是压测时这部分占了主要耗时（见 `压测报告.md`）。生产环境应该加 30 秒本地缓存 + 变更时主动失效。

---

## 压测结果

用 k6 压测（详细报告见 `压测报告.md`）：

```
场景一：75 个工程师并发抢同一张单
  抢单成功  1
  手慢了    74
  异常      0          → 零超抢

场景二：持续压抢单池 20 秒（权限缓存前后同场对照）
  改前（每次请求三表 JOIN）   194 RPS  avg 304ms  P95 222ms
  改后（缓存命中）           534 RPS  avg  98ms  P95 109ms
  失败率 0%，吞吐 +176%，P95 -51%

场景三：并发扩展性（同机构建，50 vs 150 VU）
  50 VU    1469 RPS   avg  34ms   P95  41ms   失败率 0%
  150 VU    826 RPS   avg 161ms   P95 192ms   失败率 0%
  → 并发翻三倍吞吐反降 44%，拐点在 50~75；150 人抢同一单仍恰好 1 人成功
```

复现：

```bash
k6 run k6/grab-race.js
k6 run k6/pool-throughput.js
```

---

## 项目结构

```
nameless/                       Maven 工程
  src/main/java/com/example/
    config/       Sa-Token、Redis、MyBatis-Plus、定时任务配置
    controller/   接口层
    service/      业务逻辑（抢单核心在 WorkOrderServiceImpl）
    mapper/       MyBatis-Plus Mapper，抢单的 CAS SQL 在 WorkOrderMapper
    entity/       实体
    enums/        工单状态机
    task/         超时释放定时任务
  src/main/resources/db/   建表与初始化脚本
  src/test/java/           单元测试与并发测试
docker/          数据库初始化脚本（容器用）
k6/              压测脚本
docker-compose.yml
run.bat          Windows 一键启动
```

---

## 已知限制

这个项目是奔着"把一件事做透"去的，不是全功能系统，以下是有意留下的边界：

- **没有前端**，用 Swagger UI 演示
- **单机单库**，没有分库分表和多机房
- **抢单成功后的通知**没做，所以没有引入 MQ（这个量级引入是负收益，见 `抢单功能实现指南.md` 的讨论）
- **`grab` 方法里的事务与 Redis 不保证一致**：事务回滚时 ZSet 里的超时任务不会撤销，靠超时任务自己检查 DB 状态兜底
- **权限缓存只有 Redis 一层**，没做本地 Caffeine 二级缓存：多实例下要处理失效广播，收益还不够，所以没上
