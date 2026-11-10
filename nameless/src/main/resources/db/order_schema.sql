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
