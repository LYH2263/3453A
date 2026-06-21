SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS club_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE club_db;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    student_id VARCHAR(20) DEFAULT NULL COMMENT '学号',
    role ENUM('ADMIN', 'UNION_ADMIN', 'CLUB_LEADER', 'MEMBER', 'GUEST') NOT NULL DEFAULT 'GUEST' COMMENT '角色',
    club_id INT DEFAULT NULL COMMENT '所属社团ID',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除'
) COMMENT='用户表';

-- 社团表
CREATE TABLE IF NOT EXISTS clubs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '社团名称',
    description TEXT COMMENT '社团简介',
    leader_id INT COMMENT '负责人ID',
    status ENUM('NORMAL', 'RETIRED') NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
    logo VARCHAR(255) DEFAULT NULL COMMENT '社团Logo',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (leader_id) REFERENCES users(id)
) COMMENT='社团表';

-- 活动表
CREATE TABLE IF NOT EXISTS activities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '详情',
    description TEXT COMMENT '活动详情',
    club_id INT DEFAULT NULL COMMENT '所属社团ID',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    location VARCHAR(255) NOT NULL COMMENT '地点',
    max_count INT NOT NULL DEFAULT 50 COMMENT '人数限制',
    budget DECIMAL(10, 2) DEFAULT 0.00 COMMENT '预算',
    process TEXT COMMENT '活动流程',
    status ENUM('PENDING_UNION', 'PENDING_SCHOOL', 'APPROVED', 'REJECTED', 'FINISHED') NOT NULL DEFAULT 'PENDING_UNION' COMMENT '状态',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '审核驳回原因',
    poster VARCHAR(255) DEFAULT NULL COMMENT '活动海报',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (club_id) REFERENCES clubs(id)
) COMMENT='活动表';

-- 活动报名表
CREATE TABLE IF NOT EXISTS activity_registrations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL COMMENT '活动ID',
    user_id INT NOT NULL COMMENT '用户ID',
    status ENUM('REGISTERED', 'SIGNED_IN') NOT NULL DEFAULT 'REGISTERED' COMMENT '报名状态',
    rating INT DEFAULT NULL COMMENT '评分(1-5)',
    feedback TEXT DEFAULT NULL COMMENT '反馈内容',
    reply TEXT DEFAULT NULL COMMENT '负责人回复',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (activity_id) REFERENCES activities(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='活动报名表';

-- 公告表
CREATE TABLE IF NOT EXISTS announcements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    publisher_id INT NOT NULL COMMENT '发布者ID',
    club_id INT DEFAULT NULL COMMENT '定向社团ID(NULL为全校)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (publisher_id) REFERENCES users(id)
) COMMENT='公告表';

-- 话题讨论表
CREATE TABLE IF NOT EXISTS topics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    author_id INT NOT NULL COMMENT '作者ID',
    club_id INT DEFAULT NULL COMMENT '关联社团ID',
    type ENUM('IN_CLUB', 'CROSS_CLUB') NOT NULL DEFAULT 'IN_CLUB' COMMENT '类型',
    audit_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态(跨社团需审)',
    status ENUM('NORMAL', 'TOP', 'HIDDEN') NOT NULL DEFAULT 'NORMAL' COMMENT '状态',
    likes_count INT DEFAULT 0 COMMENT '点赞数',
    favorites_count INT DEFAULT 0 COMMENT '收藏数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (author_id) REFERENCES users(id)
) COMMENT='话题讨论表';

-- 评论表
CREATE TABLE IF NOT EXISTS comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    topic_id INT NOT NULL COMMENT '话题ID',
    author_id INT NOT NULL COMMENT '作者ID',
    content TEXT NOT NULL COMMENT '内容',
    reply_id INT DEFAULT NULL COMMENT '回复的评论ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
) COMMENT='评论表';

-- 话题互动表(点赞/收藏)
CREATE TABLE IF NOT EXISTS topic_interactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    topic_id INT NOT NULL COMMENT '话题ID',
    user_id INT NOT NULL COMMENT '用户ID',
    type ENUM('LIKE', 'FAVORITE') NOT NULL COMMENT '互动类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    UNIQUE KEY uk_topic_user_type (topic_id, user_id, type),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='话题互动表';

-- 招新信息表
CREATE TABLE IF NOT EXISTS recruitments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL COMMENT '社团ID',
    title VARCHAR(200) NOT NULL COMMENT '招新标题',
    description TEXT NOT NULL COMMENT '招新要求与介绍',
    status ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (club_id) REFERENCES clubs(id)
) COMMENT='招新信息表';

-- 招新报名表
CREATE TABLE IF NOT EXISTS recruitment_applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    recruitment_id INT NOT NULL COMMENT '招新ID',
    user_id INT NOT NULL COMMENT '用户ID',
    resume_text TEXT NOT NULL COMMENT '个人陈述/简历',
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (recruitment_id) REFERENCES recruitments(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='招新报名表';

-- 问答社区：问题表
CREATE TABLE IF NOT EXISTS questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '问题标题',
    content TEXT NOT NULL COMMENT '问题详情',
    author_id INT NOT NULL COMMENT '提问者ID',
    target_club_id INT DEFAULT NULL COMMENT '指定社团ID(可选)',
    target_role ENUM('ADMIN', 'UNION_ADMIN', 'CLUB_LEADER') DEFAULT NULL COMMENT '指定解答角色(可选)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间',
    FOREIGN KEY (author_id) REFERENCES users(id),
    FOREIGN KEY (target_club_id) REFERENCES clubs(id)
) COMMENT='问题表';

-- 问答社区：回答表
CREATE TABLE IF NOT EXISTS answers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    question_id INT NOT NULL COMMENT '问题ID',
    author_id INT NOT NULL COMMENT '回答者ID',
    content TEXT NOT NULL COMMENT '回答内容',
    is_best TINYINT(1) DEFAULT 0 COMMENT '是否最佳答案',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '回答时间',
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (author_id) REFERENCES users(id)
) COMMENT='回答表';

-- ----------------------------
-- Mock Data (测试数据)
-- ----------------------------

-- 用户数据 (密码均为 123456 的 BCrypt 加密或简单明文，取决于系统实现。这里假设后端处理)
-- 提示：Admin 默认密码通常由后端配置，此处仅供展示
INSERT INTO users (id, username, password, real_name, student_id, role, avatar) VALUES
(1, 'admin', '123456', '超级管理员', 'ADMIN001', 'ADMIN', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'),
(2, 'union_admin', '123456', '社联负责人-张老师', 'UNION001', 'UNION_ADMIN', 'https://api.dicebear.com/7.x/avataaars/svg?seed=union'),
(3, 'tech_leader', '123456', '极客社长-赵极客', '20210001', 'CLUB_LEADER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=tech'),
(4, 'art_leader', '123456', '艺术社长-刘艺术', '20210002', 'CLUB_LEADER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=art'),
(5, 'member_lisi', '123456', '李四', '20220005', 'MEMBER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=lisi'),
(6, 'member_wangwu', '123456', '王五', '20220006', 'MEMBER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=wangwu'),
(7, 'club_leader1', '123456', '测试社团负责人', '20260001', 'CLUB_LEADER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=cl1'),
(8, 'student1', '123456', '测试学生1', '20260002', 'MEMBER', 'https://api.dicebear.com/7.x/avataaars/svg?seed=st1');

-- 社团数据
INSERT INTO clubs (id, name, description, leader_id, status, logo) VALUES
(1, '极客电子社', '致力于探索前沿电子技术、机器人开发与编程挑战的学术技术型社团。', 3, 'NORMAL', '/mock_images/club_logo_tech_1772179162369.png'),
(2, '悦动艺术社', '聚集全校音乐、绘画与表演爱好者，旨在打造多元化的校园文化艺术氛围。', 4, 'NORMAL', '/mock_images/club_logo_music_art_1772179189054.png');

-- 活动数据
-- 状态: PENDING_UNION, PENDING_SCHOOL, APPROVED, REJECTED, FINISHED
INSERT INTO activities (id, title, description, club_id, start_time, end_time, location, max_count, budget, process, status, poster) VALUES
(1, '2024春季校园马拉松', '一年一度的校园长跑盛会，全校师生均可参与，传播健康生活理念。', 2, '2024-03-20 09:00:00', '2024-03-20 12:00:00', '学校大操场', 500, 2000.00, '1. 集合签到\n2. 开幕式\n3. 正式开跑\n4. 颁奖闭幕', 'APPROVED', '/mock_images/activity_poster_sports_1772179174561.png'),
(2, '极客创意编程大赛', '针对全校学生的编程挑战赛，不限语言，旨在解决校园生活中的实际问题。', 1, '2024-04-15 14:00:00', '2024-04-15 18:00:00', '科技楼102', 100, 500.00, '1. 选题\n2. 现场编码\n3. 作品展示', 'PENDING_UNION', '/mock_images/activity_poster_coding_1772179331295.png'),
(3, '校园歌手大赛', '寻找校园最美声音，专业的评委阵容，丰厚的优胜奖品。', 2, '2024-02-15 19:30:00', '2024-02-15 22:00:00', '大礼堂', 300, 1500.00, '1. 海选\n2. 决赛', 'FINISHED', '/mock_images/activity_poster_music_1772179342665.png');

-- 报名数据
INSERT INTO activity_registrations (activity_id, user_id, status, rating, feedback) VALUES
(1, 5, 'SIGNED_IN', 5, '组织得很棒，奖牌很漂亮！'),
(1, 6, 'REGISTERED', NULL, NULL),
(3, 5, 'SIGNED_IN', 4, '音响效果很好，就是座位有点挤。');

-- 公告数据
INSERT INTO announcements (title, content, publisher_id, club_id) VALUES
('关于加强社团活动安全的通知', '各社团在举办大型户外活动时需严格遵守校园安全规定...', 1, NULL),
('极客社本周三例会通知', '请所有干事下午4点准时到达社团办公室，讨论招新后续。', 3, 1);

-- 话题讨论
-- 类型: IN_CLUB, CROSS_CLUB
INSERT INTO topics (id, title, content, author_id, club_id, type, audit_status) VALUES
(1, '关于极客社新项目招人的想法', '我想在社团内发起一个智能平衡车项目，欢迎有兴趣的同学加入讨论！', 3, 1, 'IN_CLUB', 'APPROVED'),
(2, '校园活动如何平衡学业与爱好？', '大家是怎么分配时间的？感觉最近活动太多有点忙不过来了。', 5, NULL, 'CROSS_CLUB', 'APPROVED');

-- 评论
INSERT INTO comments (topic_id, author_id, content) VALUES
(1, 6, '支持社长！我有Arduino开发的经验。'),
(2, 4, '我觉得还是要制定清晰的优先级，建议使用时间管理APP。');

-- 招新信息
INSERT INTO recruitments (id, club_id, title, description, status) VALUES
(1, 1, '2024极客社春季招新', '寻找对技术有热忱、敢于挑战自我的你，不限年级不限专业！', 'OPEN'),
(2, 2, '艺术社舞蹈部招社员', '如果你热爱舞蹈，有一定基础，欢迎加入我们的大家庭。', 'CLOSED');

-- 招新报名
INSERT INTO recruitment_applications (recruitment_id, user_id, resume_text, status) VALUES
(1, 5, '我对嵌入式开发非常感兴趣，自学过C语言。', 'PENDING'),
(1, 6, '我有较强的团队协作能力，想在技术社团提升技能。', 'APPROVED');

-- 问答
INSERT INTO questions (id, title, content, author_id, target_club_id, target_role) VALUES
(1, '社团管理系统如何申请账号？', '我是大一新生，想加入社团，请问系统账号是统一分配还是自己注册？', 6, NULL, 'ADMIN'),
(2, '极客社的项目经费如何报销？', '作为项目负责人，想了解具体的报销流程和所需票据。', 3, 1, 'CLUB_LEADER');

-- 回答
INSERT INTO answers (question_id, author_id, content, is_best) VALUES
(1, 1, '系统账号需通过学生证号注册，后台审核后即可登录。', 1),
(2, 2, '报销流程需先在社管系统提交申请，附上电子发票，张老师审核后通过。', 0);
