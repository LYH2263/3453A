-- =============================================================
-- 候补功能迁移脚本
-- 适用于已有数据库（旧 ENUM 仅 REGISTERED, SIGNED_IN）
-- 执行前请备份 activity_registrations 表
-- =============================================================

USE club_db;

-- 1. 扩展 activity_registrations.status 枚举，新增 WAITLIST 和 CANCELLED
ALTER TABLE activity_registrations
    MODIFY COLUMN status ENUM('REGISTERED', 'SIGNED_IN', 'WAITLIST', 'CANCELLED')
        NOT NULL DEFAULT 'REGISTERED'
        COMMENT '报名状态: REGISTERED-已报名, SIGNED_IN-已签到, WAITLIST-候补中, CANCELLED-已取消';

-- 2. 新增 waitlist_order 列
ALTER TABLE activity_registrations
    ADD COLUMN waitlist_order INT DEFAULT NULL
        COMMENT '候补序号' AFTER reply;

-- 3. 新增唯一索引（防重复报名），若已存在则跳过
-- 先检查是否已存在该唯一索引
SET @exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_registrations'
      AND INDEX_NAME = 'uk_activity_user');

SET @sql = IF(@exists = 0,
    'ALTER TABLE activity_registrations ADD UNIQUE KEY uk_activity_user (activity_id, user_id)',
    'SELECT ''uk_activity_user already exists, skipping''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 创建递补日志表（IF NOT EXISTS 保证幂等）
CREATE TABLE IF NOT EXISTS activity_promotion_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL COMMENT '活动ID',
    user_id INT NOT NULL COMMENT '递补用户ID',
    original_order INT NOT NULL COMMENT '原候补序号',
    source ENUM('CANCEL', 'EXPAND') NOT NULL COMMENT '递补来源: CANCEL-取消报名触发, EXPAND-扩容触发',
    trigger_user_id INT DEFAULT NULL COMMENT '触发用户ID(取消报名的用户或扩容负责人)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '递补时间',
    FOREIGN KEY (activity_id) REFERENCES activities(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='递补日志表';
