-- ============================================================
-- 压测数据：50 张待抢工单 + 75 个工程师账号
-- 可重复执行（幂等，重复跑会把工单重置为待抢状态）
-- ============================================================

USE nameless_db;

-- ------------------------------------------------------------
-- 75 个工程师账号
--   手机号：13910000001 ~ 13910000075
--   邮箱：  perf01@test.com ~ perf75@test.com
--   密码：  Test@1234（BCrypt cost=12）
-- ------------------------------------------------------------
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 75
)
SELECT CONCAT('1391000', LPAD(n, 4, '0')),
       CONCAT('压测工程师', LPAD(n, 2, '0')),
       '$2a$12$/FS7Ru47eJJxB2a9SmKIvOR3x16aA1GyRD2YjPsz2aucybXrWUWrS',
       CONCAT('perf', LPAD(n, 2, '0'), '@test.com'),
       1, 1, 0, NOW(), NOW()
FROM seq
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

-- 全部绑定 engineer 角色（拥有 order:grab / order:complete）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u, sys_role r
WHERE u.email LIKE 'perf%@test.com'
  AND r.role_code = 'engineer';

-- ------------------------------------------------------------
-- 50 张待抢工单
--   工单号：WO2026LOAD0001 ~ WO2026LOAD0050
-- ------------------------------------------------------------
INSERT INTO work_order (order_no, title, type, content, status, create_time, update_time, deleted)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 50
)
SELECT CONCAT('WO2026LOAD', LPAD(n, 4, '0')),
       CONCAT(ELT(1 + (n % 5), '空调不制冷', '卫生间漏水', '网络不通', '门禁失灵', '照明故障'),
              ' - 压测单', n),
       ELT(1 + (n % 5), '空调', '水电', '网络', '门禁', '水电'),
       CONCAT('压力测试工单 #', n),
       'PENDING', NOW(), NOW(), 0
FROM seq
ON DUPLICATE KEY UPDATE
    status = 'PENDING',
    assignee_id = NULL,
    grab_time = NULL,
    deadline = NULL,
    version = 0,
    deleted = 0;
