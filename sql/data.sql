-- ============================================================
-- 个人博客 初始化数据脚本
-- 在 schema.sql 之后执行
-- ============================================================
USE `blog`;

-- ============================================================
-- 1. 角色
-- ============================================================
INSERT INTO `role` (`id`, `role_name`, `role_code`, `description`) VALUES
(1, '超级管理员', 'super_admin', '拥有全部权限'),
(2, '编辑',       'editor',        '文章增改、图片上传；仅删自己评论'),
(3, '评论管理员', 'comment_admin', '查看全部评论，可删除任意评论'),
(4, '普通用户',   'user',          '浏览、评论、删除自己评论');

-- ============================================================
-- 2. 权限（接口权限 type=1，菜单权限 type=2）
-- ============================================================

-- ---- 接口权限 type=1 ----
INSERT INTO `permission` (`id`, `perm_code`, `perm_name`, `type`, `parent_id`, `path`, `icon`, `sort`) VALUES
-- 文章模块
(101, 'article:list',   '文章列表', 1, 0, NULL, NULL, 1),
(102, 'article:view',   '文章详情', 1, 0, NULL, NULL, 2),
(103, 'article:create', '发布文章', 1, 0, NULL, NULL, 3),
(104, 'article:update', '编辑文章', 1, 0, NULL, NULL, 4),
(105, 'article:delete', '删除文章', 1, 0, NULL, NULL, 5),
-- 图片素材模块
(111, 'image:list',   '素材列表', 1, 0, NULL, NULL, 11),
(112, 'image:upload', '上传图片', 1, 0, NULL, NULL, 12),
(113, 'image:delete', '删除图片', 1, 0, NULL, NULL, 13),
-- 评论模块
(121, 'comment:view',       '查看评论',   1, 0, NULL, NULL, 21),
(122, 'comment:create',     '发表评论',   1, 0, NULL, NULL, 22),
(123, 'comment:delete',     '删除自己评论', 1, 0, NULL, NULL, 23),
(124, 'comment:delete:any', '删除任意评论', 1, 0, NULL, NULL, 24),
-- 用户管理模块
(131, 'user:list',         '用户列表',   1, 0, NULL, NULL, 31),
(132, 'user:add',          '管理员建号', 1, 0, NULL, NULL, 32),
(133, 'user:update',       '编辑用户',   1, 0, NULL, NULL, 33),
(134, 'user:role:assign',  '分配角色',   1, 0, NULL, NULL, 34),
-- 角色权限模块
(141, 'role:list',        '角色列表',   1, 0, NULL, NULL, 41),
(142, 'role:add',         '新增角色',   1, 0, NULL, NULL, 42),
(143, 'role:update',      '编辑角色',   1, 0, NULL, NULL, 43),
(144, 'role:delete',      '删除角色',   1, 0, NULL, NULL, 44),
(145, 'permission:list',  '权限列表',   1, 0, NULL, NULL, 45),
(146, 'role:assignPerm',  '角色分配权限', 1, 0, NULL, NULL, 46);

-- ---- 菜单权限 type=2 ----
INSERT INTO `permission` (`id`, `perm_code`, `perm_name`, `type`, `parent_id`, `path`, `icon`, `sort`) VALUES
(201, 'menu:article',       '文章管理', 2, 0,   '/article',       'document-outline',       1),
(202, 'menu:image',         '素材相册', 2, 0,   '/image',         'images-outline',         2),
(203, 'menu:comment',       '评论管理', 2, 0,   '/comment',       'chatbubbles-outline',    3),
(204, 'menu:rbac:user',     '用户管理', 2, 200, '/rbac/user',     'people-outline',         4),
(205, 'menu:rbac:role',     '角色管理', 2, 200, '/rbac/role',     'shield-outline',         5),
(206, 'menu:rbac:permission','权限管理',2, 200, '/rbac/permission','key-outline',           6),
-- RBAC父菜单
(200, 'menu:rbac',          '权限管理', 2, 0,   '/rbac',          'lock-closed-outline',    4);

-- ============================================================
-- 3. 角色-权限映射
-- ============================================================

-- super_admin: 拥有全部权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission`;

-- editor: 文章增改查、图片上传列表删除、评论查看/创建/删自己
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(2, 101), (2, 102), (2, 103), (2, 104),
(2, 111), (2, 112), (2, 113),
(2, 121), (2, 122), (2, 123),
(2, 201), (2, 202), (2, 203);

-- comment_admin: 评论查看、删任意评论、删自己评论、创建评论
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(3, 121), (3, 122), (3, 123), (3, 124),
(3, 203);

-- user: 浏览文章、评论查看/创建/删自己
INSERT INTO `role_permission` (`role_id`, `permission_id`) VALUES
(4, 101), (4, 102),
(4, 121), (4, 122), (4, 123);

-- ============================================================
-- 4. 超级管理员账号
--    用户名: admin  密码: admin123 (BCrypt加密)
-- ============================================================
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `avatar`) VALUES
(1, 'admin', '$2a$10$N.ZMy9bE6XhJZpDxQK0kQe.5JqLp3oM5jYQ9ZQm2jJZ9ZQm2jJZ9', '管理员', NULL);

INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);
