-- ============================================================
-- 150 并发压测：在原有 75 个账号基础上追加 perf76 ~ perf150
-- 前提：先执行过 perf_data.sql
-- 可重复执行
-- ============================================================

USE nameless_db;

-- ------------------------------------------------------------
-- 追加 75 个工程师账号（perf76 ~ perf150），密码统一 Test@1234
-- ------------------------------------------------------------
INSERT INTO user (phone, nickname, password, email, gender, status, deleted, create_time, update_time)
WITH RECURSIVE seq AS (
    SELECT 76 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 150
)
SELECT CONCAT('1391000', LPAD(n, 4, '0')),
       CONCAT('压测工程师', n),
       '$2a$12$/FS7Ru47eJJxB2a9SmKIvOR3x16aA1GyRD2YjPsz2aucybXrWUWrS',
       CONCAT('perf', n, '@test.com'),
       1, 1, 0, NOW(), NOW()
FROM seq
ON DUPLICATE KEY UPDATE password = VALUES(password), status = 1, deleted = 0;

-- 绑定 engineer 角色（幂等，已绑定的会忽略）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u, sys_role r
WHERE u.email LIKE 'perf%@test.com'
  AND r.role_code = 'engineer';

-- ------------------------------------------------------------
-- 清空抢单流水并把工单重置为待抢
-- 不清流水的话，同一用户重复抢同一单会直接返回"你已抢到"，测不出真实并发
-- ------------------------------------------------------------
DELETE FROM grab_record;

UPDATE work_order
SET status = 'PENDING',
    assignee_id = NULL,
    grab_time = NULL,
    deadline = NULL,
    version = 0
WHERE deleted = 0;

SELECT COUNT(*) AS 压测账号数 FROM user WHERE email LIKE 'perf%@test.com';
SELECT COUNT(*) AS 待抢工单数 FROM work_order WHERE status = 'PENDING' AND deleted = 0;
