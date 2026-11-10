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
