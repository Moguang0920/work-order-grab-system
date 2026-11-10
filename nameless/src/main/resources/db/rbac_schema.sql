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
