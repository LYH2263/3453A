-- 幂等 schema 补丁：兼容已有 volume，补齐缺失表/字段
USE club_db;

SET @db := DATABASE();

-- clubs 预算字段
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'clubs' AND COLUMN_NAME = 'monthly_budget_limit') = 0,
    'ALTER TABLE clubs ADD COLUMN monthly_budget_limit DECIMAL(10,2) DEFAULT 5000.00 COMMENT ''月度预算上限''',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'clubs' AND COLUMN_NAME = 'budget_enforce_mode') = 0,
    'ALTER TABLE clubs ADD COLUMN budget_enforce_mode ENUM(''SOFT'',''HARD'') DEFAULT ''SOFT'' COMMENT ''预算超限模式''',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- activity_registrations 情绪分析字段
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'activity_registrations' AND COLUMN_NAME = 'sentiment') = 0,
    'ALTER TABLE activity_registrations ADD COLUMN sentiment ENUM(''POSITIVE'', ''NEUTRAL'', ''NEGATIVE'') DEFAULT NULL COMMENT ''反馈情绪标签'' AFTER feedback',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'activity_registrations' AND COLUMN_NAME = 'feedback_tags') = 0,
    'ALTER TABLE activity_registrations ADD COLUMN feedback_tags VARCHAR(500) DEFAULT NULL COMMENT ''命中关键词JSON'' AFTER sentiment',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- answers 乐观锁
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'answers' AND COLUMN_NAME = 'version') = 0,
    'ALTER TABLE answers ADD COLUMN version INT DEFAULT 0 COMMENT ''乐观锁版本号''',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 管理模块表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '操作人',
    operation VARCHAR(100) NOT NULL COMMENT '操作描述',
    method VARCHAR(255) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    time BIGINT COMMENT '执行时长(毫秒)',
    ip VARCHAR(64) COMMENT 'IP地址',
    status TINYINT(1) DEFAULT 1 COMMENT '状态(0:失败 1:成功)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS login_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '登录账号',
    ip VARCHAR(64) COMMENT '登录IP',
    location VARCHAR(255) COMMENT '登录地点',
    browser VARCHAR(100) COMMENT '浏览器',
    os VARCHAR(100) COMMENT '操作系统',
    status TINYINT(1) DEFAULT 1 COMMENT '状态(0:失败 1:成功)',
    msg VARCHAR(255) COMMENT '提示消息',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间'
) COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS exception_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) COMMENT '操作人',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(255) COMMENT '方法名',
    params TEXT COMMENT '参数',
    exception_name VARCHAR(255) COMMENT '异常名称',
    exception_message TEXT COMMENT '异常信息',
    stack_trace TEXT COMMENT '堆栈详情',
    ip VARCHAR(64) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间'
) COMMENT='异常日志表';

CREATE TABLE IF NOT EXISTS audit_configs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE COMMENT '业务类型',
    nodes TEXT NOT NULL COMMENT '审核节点JSON',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否激活',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='审核流程配置表';

-- 徽章体系
CREATE TABLE IF NOT EXISTS badges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL COMMENT '所属社团ID',
    name VARCHAR(100) NOT NULL COMMENT '徽章名称',
    icon_url VARCHAR(255) DEFAULT NULL COMMENT '徽章图标URL',
    description TEXT COMMENT '徽章描述',
    is_public TINYINT(1) DEFAULT 1 COMMENT '是否公开',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    INDEX idx_club_id (club_id),
    INDEX idx_public (is_public)
) COMMENT='徽章定义表';

CREATE TABLE IF NOT EXISTS user_badges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    badge_id INT NOT NULL COMMENT '徽章ID',
    granted_by INT NOT NULL COMMENT '授予人ID',
    granted_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授予时间',
    revoked TINYINT(1) DEFAULT 0 COMMENT '是否已撤销',
    revoked_by INT DEFAULT NULL COMMENT '撤销人ID',
    revoked_time DATETIME DEFAULT NULL COMMENT '撤销时间',
    revoke_reason VARCHAR(255) DEFAULT NULL COMMENT '撤销原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (badge_id) REFERENCES badges(id),
    FOREIGN KEY (granted_by) REFERENCES users(id),
    FOREIGN KEY (revoked_by) REFERENCES users(id),
    INDEX idx_user_id (user_id),
    INDEX idx_badge_id (badge_id),
    INDEX idx_user_badge (user_id, badge_id),
    INDEX idx_revoked (revoked)
) COMMENT='用户徽章关联表';

CREATE TABLE IF NOT EXISTS activity_co_hosts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL COMMENT '活动ID',
    club_id INT NOT NULL COMMENT '合作社团ID',
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '确认状态',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    confirm_time DATETIME DEFAULT NULL COMMENT '确认/拒绝时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (activity_id) REFERENCES activities(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    UNIQUE KEY uk_activity_club (activity_id, club_id)
) COMMENT='活动合作社团关联表';

CREATE TABLE IF NOT EXISTS answer_votes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '投票用户ID',
    answer_id INT NOT NULL COMMENT '被投票的答案ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    UNIQUE KEY uk_user_answer (user_id, answer_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (answer_id) REFERENCES answers(id)
) COMMENT='回答投票表';

CREATE TABLE IF NOT EXISTS sentiment_dictionary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(50) NOT NULL COMMENT '关键词',
    sentiment ENUM('POSITIVE', 'NEGATIVE') NOT NULL COMMENT '情绪类型',
    weight INT NOT NULL DEFAULT 1 COMMENT '权重',
    category VARCHAR(50) DEFAULT NULL COMMENT '关键词分类',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_keyword (keyword)
) COMMENT='情绪分析关键词词典表';

CREATE TABLE IF NOT EXISTS comment_mentions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    comment_id INT NOT NULL COMMENT '评论ID',
    topic_id INT NOT NULL COMMENT '话题ID',
    mentioned_user_id INT NOT NULL COMMENT '被@用户ID',
    mention_type VARCHAR(20) NOT NULL DEFAULT 'USERNAME' COMMENT '提及方式',
    mention_text VARCHAR(100) NOT NULL COMMENT '提及的原始文本',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (comment_id) REFERENCES comments(id),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (mentioned_user_id) REFERENCES users(id),
    INDEX idx_mentioned_user (mentioned_user_id),
    INDEX idx_topic (topic_id)
) COMMENT='评论提及表';

CREATE TABLE IF NOT EXISTS user_notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '接收通知的用户ID',
    type VARCHAR(30) NOT NULL COMMENT '通知类型',
    topic_id INT DEFAULT NULL COMMENT '关联话题ID',
    comment_id INT DEFAULT NULL COMMENT '关联评论ID',
    content TEXT COMMENT '通知内容/预览',
    trigger_user_id INT DEFAULT NULL COMMENT '触发用户ID',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_time DATETIME DEFAULT NULL COMMENT '阅读时间',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    FOREIGN KEY (comment_id) REFERENCES comments(id),
    FOREIGN KEY (trigger_user_id) REFERENCES users(id),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_user_create (user_id, create_time DESC)
) COMMENT='用户通知表';

INSERT IGNORE INTO audit_configs (type, nodes, is_active) VALUES
('MONTHLY_BUDGET_LIMIT', '{"defaultLimit": 5000, "defaultEnforceMode": "SOFT"}', 1);

-- 徽章种子数据（仅表为空时插入）
INSERT INTO badges (id, club_id, name, icon_url, description, is_public)
SELECT 1, 1, '技术先锋', 'https://api.dicebear.com/7.x/shapes/svg?seed=tech1&backgroundColor=409eff', '在技术领域有突出贡献的社员', 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM badges LIMIT 1);

INSERT INTO badges (id, club_id, name, icon_url, description, is_public)
SELECT 2, 1, '代码大师', 'https://api.dicebear.com/7.x/shapes/svg?seed=tech2&backgroundColor=67c23a', '编程能力卓越，解决重大技术难题', 1
FROM DUAL WHERE (SELECT COUNT(*) FROM badges) < 2;

INSERT INTO badges (id, club_id, name, icon_url, description, is_public)
SELECT 3, 1, '创新之星', 'https://api.dicebear.com/7.x/shapes/svg?seed=tech3&backgroundColor=e6a23c', '提出创新想法并成功落地', 0
FROM DUAL WHERE (SELECT COUNT(*) FROM badges) < 3;

INSERT INTO badges (id, club_id, name, icon_url, description, is_public)
SELECT 4, 2, '艺术达人', 'https://api.dicebear.com/7.x/shapes/svg?seed=art1&backgroundColor=f56c6c', '在艺术表演中表现出色', 1
FROM DUAL WHERE (SELECT COUNT(*) FROM badges) < 4;

INSERT INTO badges (id, club_id, name, icon_url, description, is_public)
SELECT 5, 2, '最佳歌手', 'https://api.dicebear.com/7.x/shapes/svg?seed=art2&backgroundColor=909399', '歌唱比赛获奖选手', 1
FROM DUAL WHERE (SELECT COUNT(*) FROM badges) < 5;

INSERT INTO badges (id, club_id, name, icon_url, description, is_public)
SELECT 6, 2, '幕后英雄', 'https://api.dicebear.com/7.x/shapes/svg?seed=art3&backgroundColor=8e44ad', '默默奉献的后勤工作人员', 0
FROM DUAL WHERE (SELECT COUNT(*) FROM badges) < 6;
