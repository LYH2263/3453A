-- 活动反馈情绪分析功能数据库迁移（幂等）
USE club_db;

SET @db := DATABASE();

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

-- 仅词典为空时初始化关键词
INSERT INTO sentiment_dictionary (keyword, sentiment, weight, category)
SELECT '棒', 'POSITIVE', 2, '通用' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sentiment_dictionary LIMIT 1);
