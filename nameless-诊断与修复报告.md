# nameless 项目：诊断 + 修复报告

更新时间：2026-10-15 22:20

---

## 一、真正的根因：组件扫描没覆盖到任何一个业务类

这是"跑不起来"的核心原因，之前的诊断漏了。

`NamelessApplication` 位于 `com.example.nameless` 包，而所有业务组件都在**平级包**里：

```
com.example
├── controller   UserController        ← 扫不到
├── service      UserService / Impl    ← 扫不到
├── config       MyBatisPlusConfig     ← 扫不到
├── mapper       UserMapper            ← 靠主类的 @MapperScan 才注册上
└── nameless     NamelessApplication   ← @SpringBootApplication 在这里
                 └─ 默认只扫 com.example.nameless.**
```

**实测证据**（修复前）

| 检查 | 结果 |
|---|---|
| `POST /user/register` | `404 Not Found` |
| `GET /v3/api-docs` | `"paths":{}` —— 一个接口都没有 |
| 应用启动日志 | 正常 Started，零报错（连数据库都没连，因为连接池是懒加载） |

所以现象是"控制台一切正常、服务起来了，但接口全是 404"。

**修复**：主类放宽扫描根
```java
@SpringBootApplication(scanBasePackages = "com.example")
```

**修复后实测**
```
paths: ['/user/register', '/user/login/phone', '/user/login/email']
POST /user/register → 200 {"success":true,"id":1,...}
POST /user/login/email → 200 {"success":true,...}
数据库落库正常，create_time / update_time 已填充
```

---

## 二、第二处代码问题：`@RequestBody` 导错包

`UserController` 里 import 的是：

```java
import io.swagger.v3.oas.annotations.parameters.RequestBody;   // ← Swagger 的
```

这是给 OpenAPI 文档生成用的注解，**不会触发 Spring MVC 的消息体转换**。
少了 Spring 的 `@RequestBody`，参数会被当成表单字段绑定，POST 的 JSON body 完全收不到，字段全是 null。

已改为：
```java
import org.springframework.web.bind.annotation.RequestBody;   // ← Spring 的
```

---

## 三、Redis / RocketMQ：已停用（本次操作）

按你的要求注释掉，不是删除，随时可恢复。

**pom.xml** 注释了 4 个依赖：
- `sa-token-redis-template`
- `rocketmq-spring-boot-starter`
- `commons-pool2`（Redis 连接池）
- `spring-boot-starter-data-redis`

**application.properties** 注释了：Redis 全部配置、RocketMQ 全部配置、`sa-token.dao-type=redis`。

> 提醒：RocketMQ 这个依赖删掉是对的，不只是"暂时不用"。rocketmq-spring-boot 2.2.3 属于 Spring Boot 2 时代，
> 仅通过 `META-INF/spring.factories` 注册自动配置，而 Spring Boot 3+ 已不再读取该文件。
> 在 Boot 4.1.1 下它 100% 失效（`--debug` 报告里搜不到任何 rocketmq 条目，`RocketMQTemplate` 无法注入）。
> 以后真要用，得换提供 `META-INF/spring/...AutoConfiguration.imports` 的新版本。

Sa-Token 现在退回内存 DAO，**服务重启后登录态会丢失**，正式用之前记得把 Redis 开回来。

---

## 四、其余修复项

| 项 | 处理 |
|---|---|
| **VSCode 认不到 Maven 工程** | 新增 `nameless.code-workspace`，工作区根直接指向 `nameless/` 子目录（原来 VSCode 打开的是外层目录，而 pom.xml 在子目录里，Java 扩展找不到工程、没有 Run/Debug） |
| **`SERVER__PORT=0` 覆盖端口** | 该文件在 `terminal.integrated.env.windows` 里把 `SERVER__PORT`、`SERVER__HOST` 设为 null 兜底 |
| **MetaObjectHandler 缺失** | 新增 `com/example/config/MyMetaObjectHandler.java`，`create_time`/`update_time` 自动填充生效（已实测） |
| **mapper 目录不存在** | 新建 `src/main/resources/mapper/`，并放了说明文件 |
| **`id-type` 与实体冲突** | 全局 `assign_id` 改为 `auto`，与 `User` 的 `@TableId(IdType.AUTO)` 保持一致 |

---

## 五、关于 `SERVER__PORT` 的更正

上一版报告把它列为"主因"——**这个判断需要修正**。

排查结果：它**不在** HKCU / HKLM 注册表里，也**不在** `~/.bashrc`、`~/.profile`、`/etc/profile` 里，只存在于当前这个会话的进程环境中，属于外层工具注入。
你自己在 VSCode / 系统终端里启动，大概率不会带上它，端口本来就是正常的 8080。

真正的元凶是第一节的包扫描。（不过 `.code-workspace` 里那个 null 兜底留着也无害，还能防同类问题。）

---

## 六、顺手该知道的几件事

1. **MySQL 一切正常**：9.5 已启动，`nameless_db` 和 `user` 表都在，账号 `root/123123` 可用。
2. **Git Bash 里的 `mvn` 是坏的**（报 `classworlds.Launcher` 找不到，因为没设 `M2_HOME`，而 `MAVEN_HOME` 是 Windows 路径）。修一行就好：
   ```bash
   echo 'export M2_HOME=/d/APACHE_MAVEN/apache-maven-3.9.6-bin/apache-maven-3.9.6' >> ~/.bashrc
   ```
   或者干脆用 `mvn.cmd` / 项目自带的 `./mvnw`。
3. **pom 的 artifactId 还是 `demo`**，打出来是 `demo-0.0.1-SNAPSHOT.jar`，和应用名 nameless 不一致。不影响运行，想改的时候说一声。

---

## 七、怎么用

双击打开 `nameless.code-workspace`（会自动定位到 Maven 工程根），然后：

```bash
mvn clean spring-boot:run
```

起来后：
- 接口根路径 `http://localhost:8080/user`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI 文档：`http://localhost:8080/v3/api-docs`

冒烟测试数据已从数据库清理干净（user 表当前 0 行）。
