-- ============================================================
-- nameless 数据库初始化（Docker 首次启动时自动执行）
-- 由 db/*.sql 拼接生成：rbac_schema → rbac_data → order_schema → order_data
-- 如果改了上面任何一个文件，请同步重新生成这个文件
-- ============================================================

-- ---------- 来自 rbac_schema.sql ----------
-- ============================================================
-- nameless 项目 RBAC 权限模型建表脚本
-- 数据库：nameless_db
-- 说明：与项目现有 user 表配合使用，user.id 为 BIGINT UNSIGNED
-- ============================================================

USE nameless_db;

-- ------------------------------------------------------------
-- 角色表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code   VARCHAR(50)     NOT NULL                COMMENT '角色标识，如 admin / user',
    role_name   VARCHAR(50)     NOT NULL                COMMENT '角色名称',
    description VARCHAR(200)             DEFAULT NULL   COMMENT '描述',
    sort        INT                      DEFAULT 0      COMMENT '排序',
    status      TINYINT                  DEFAULT 1      COMMENT '状态：1启用 0禁用',
    deleted     TINYINT                  DEFAULT 0      COMMENT '逻辑删除：0未删 1已删',
    create_time DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- ------------------------------------------------------------
-- 权限表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    perm_code   VARCHAR(100)    NOT NULL                COMMENT '权限标识，如 user:read',
    perm_name   VARCHAR(50)     NOT NULL                COMMENT '权限名称',
    perm_type   TINYINT                  DEFAULT 1      COMMENT '类型：1接口 2菜单 3按钮',
    parent_id   BIGINT UNSIGNED          DEFAULT 0      COMMENT '父权限ID，0为顶级',
    path        VARCHAR(200)             DEFAULT NULL   COMMENT '前端路由/接口路径',
    sort        INT                      DEFAULT 0      COMMENT '排序',
    status      TINYINT                  DEFAULT 1      COMMENT '状态：1启用 0禁用',
    deleted     TINYINT                  DEFAULT 0      COMMENT '逻辑删除',
    create_time DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

-- ------------------------------------------------------------
-- 用户-角色关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- ------------------------------------------------------------
-- 角色-权限关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    perm_id     BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, perm_id),
    KEY idx_perm_id (perm_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';

-- ---------- 来自 rbac_data.sql ----------
-- ============================================================
-- nameless 项目 RBAC 测试数据
-- 密码（BCrypt cost=12）：
--   admin@test.com  / Admin@123
--   alice@test.com  / Test@1234
--   bob@test.com    / Test@1234
-- 可重复执行（幂等）
-- ============================================================

USE nameless_db;

-- ------------------------- 角色 -------------------------
INSERT INTO sys_role (role_code, role_name, description, sort, status, deleted)
VALUES ('admin', '管理员', '拥有全部权限', 1, 1, 0),
       ('user',  '普通用户', '可查看用户与订单', 2, 1, 0),
       ('guest', '访客', '仅可查看订单', 3, 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- ------------------------- 权限 -------------------------
INSERT INTO sys_permission (perm_code, perm_name, perm_type, path, sort, status, deleted)
VALUES ('user:read',   '查看用户', 1, '/user/list',   1, 1, 0),
       ('user:create', '新增用户', 1, '/user/register', 2, 1, 0),
       ('user:update', '修改用户', 1, '/user/update', 3, 1, 0),
       ('user:delete', '删除用户', 1, '/user/delete', 4, 1, 0),
       ('role:read',   '查看角色', 1, '/role/list',   5, 1, 0),
       ('role:assign', '分配角色', 1, '/role/assign', 6, 1, 0),
       ('order:read',  '查看订单', 1, '/order/list',  7, 1, 0),
       ('order:manage','管理订单', 1, '/order/manage',8, 1, 0)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), path = VALUES(path);

-- ------------------------- 测试用户 -------------------------
-- admin@test.com / Admin@123
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
VALUES ('13900000001', '系统管理员', '$2a$12$3G2VU4HdUGtvfv1/RxQaIO5J9ryt.jRtlVu.2EmNlKTHpraRKjDe.', 'admin@test.com', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

-- alice@test.com / Test@1234
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
VALUES ('13900000002', 'Alice', '$2a$12$/FS7Ru47eJJxB2a9SmKIvOR3x16aA1GyRD2YjPsz2aucybXrWUWrS', 'alice@test.com', 2, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

-- bob@test.com / Test@1234
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
VALUES ('13900000003', 'Bob', '$2a$12$/FS7Ru47eJJxB2a9SmKIvOR3x16aA1GyRD2YjPsz2aucybXrWUWrS', 'bob@test.com', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

-- ------------------------- 用户绑定角色 -------------------------
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM user u, sys_role r
WHERE u.email = 'admin@test.com' AND r.role_code = 'admin';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM user u, sys_role r
WHERE u.email = 'alice@test.com' AND r.role_code = 'user';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM user u, sys_role r
WHERE u.email = 'bob@test.com' AND r.role_code = 'guest';

-- ------------------------- 角色绑定权限 -------------------------
-- admin：全部权限
INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.role_code = 'admin';

-- user：user:read / order:read
INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'user' AND p.perm_code IN ('user:read', 'order:read');

-- guest：order:read
INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'guest' AND p.perm_code = 'order:read';

-- ---------- 来自 order_schema.sql ----------
-- ============================================================
-- 抢单功能建表脚本
-- 依赖：nameless_db、user 表、RBAC 四张表
-- ============================================================

USE nameless_db;

-- ------------------------------------------------------------
-- 工单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS work_order;
CREATE TABLE work_order (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    order_no    VARCHAR(32)     NOT NULL                COMMENT '工单号',
    title       VARCHAR(100)    NOT NULL                COMMENT '标题',
    type        VARCHAR(30)              DEFAULT NULL   COMMENT '类型：空调/水电/网络',
    content     VARCHAR(500)             DEFAULT NULL   COMMENT '描述',
    status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待抢/PROCESSING处理中/COMPLETED已完成/CANCELLED已取消',
    assignee_id BIGINT UNSIGNED          DEFAULT NULL   COMMENT '处理人ID，NULL 表示未分配',
    grab_time   DATETIME                 DEFAULT NULL   COMMENT '抢单时间',
    deadline    DATETIME                 DEFAULT NULL   COMMENT '处理截止时间',
    version     INT             NOT NULL DEFAULT 0      COMMENT '版本号',
    create_time DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME                 DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT                  DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_status_create (status, create_time),
    KEY idx_assignee (assignee_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工单表';

-- ------------------------------------------------------------
-- 抢单流水表（幂等 + 审计）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS grab_record;
CREATE TABLE grab_record (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id    BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '抢单人ID',
    success     TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    cost_ms     INT                      DEFAULT NULL COMMENT '耗时(毫秒)，压测用',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_user (order_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='抢单流水表';

-- ---------- 来自 order_data.sql ----------
-- ============================================================
-- 抢单功能初始化数据：权限 + engineer 角色 + 待抢工单
-- 可重复执行（幂等）
-- ============================================================

USE nameless_db;

-- ------------------------- 抢单权限 -------------------------
INSERT INTO sys_permission (perm_code, perm_name, perm_type, path, sort, status, deleted)
VALUES ('order:grab', '抢单', 1, '/order', 9, 1, 0),
       ('order:complete', '完成工单', 1, '/order/complete', 10, 1, 0)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name), path = VALUES(path);

-- ------------------------- 工程师角色 -------------------------
INSERT INTO sys_role (role_code, role_name, description, sort, status, deleted)
VALUES ('engineer', '工程师', '可抢单并处理工单', 4, 1, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), description = VALUES(description);

-- engineer 角色绑定抢单、完成权限
INSERT IGNORE INTO sys_role_permission (role_id, perm_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'engineer' AND p.perm_code IN ('order:grab', 'order:complete');

-- ------------------------- 测试工程师账号 -------------------------
-- engineer1@test.com / Test@1234
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
VALUES ('13900000011', '工程师一号', '$2a$12$/FS7Ru47eJJxB2a9SmKIvOR3x16aA1GyRD2YjPsz2aucybXrWUWrS',
        'engineer1@test.com', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

-- engineer2@test.com / Test@1234
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
VALUES ('13900000012', '工程师二号', '$2a$12$/FS7Ru47eJJxB2a9SmKIvOR3x16aA1GyRD2YjPsz2aucybXrWUWrS',
        'engineer2@test.com', 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM user u, sys_role r
WHERE u.email = 'engineer1@test.com' AND r.role_code = 'engineer';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM user u, sys_role r
WHERE u.email = 'engineer2@test.com' AND r.role_code = 'engineer';

-- ------------------------- 待抢工单 -------------------------
INSERT INTO work_order (order_no, title, type, content, status, create_time)
VALUES ('WO20261019001', '3 号楼空调不制冷', '空调', '3 号楼 502 室空调开机不制冷，需要上门检查', 'PENDING', NOW()),
       ('WO20261019002', '2 号楼卫生间漏水', '水电', '2 号楼 301 卫生间水管渗水', 'PENDING', NOW()),
       ('WO20261019003', '5 号楼网络不通', '网络', '5 号楼 108 室网口无法上网', 'PENDING', NOW()),
       ('WO20261019004', '1 号楼门禁失灵', '门禁', '1 号楼东门门禁刷卡无反应', 'PENDING', NOW()),
       ('WO20261019005', '4 号楼照明故障', '水电', '4 号楼走廊 3 盏灯不亮', 'PENDING', NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);

