-- 活动合作社团关联表（幂等）
USE club_db;

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

-- 扩展活动状态 ENUM（已含 DRAFT_COCONFIRM 时跳过）
SET @has_draft := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activities'
      AND COLUMN_NAME = 'status'
      AND COLUMN_TYPE LIKE '%DRAFT_COCONFIRM%'
);
SET @sql := IF(
    @has_draft = 0,
    'ALTER TABLE activities MODIFY COLUMN status ENUM(''DRAFT_COCONFIRM'', ''PENDING_UNION'', ''PENDING_SCHOOL'', ''APPROVED'', ''REJECTED'', ''FINISHED'') NOT NULL DEFAULT ''PENDING_UNION''',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
