USE club_db;

SET @db := DATABASE();

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

INSERT IGNORE INTO audit_configs (type, nodes, is_active) VALUES ('MONTHLY_BUDGET_LIMIT', '{"defaultLimit": 5000, "defaultEnforceMode": "SOFT"}', 1);
