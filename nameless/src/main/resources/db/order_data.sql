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
