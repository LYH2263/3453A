USE club_db;

ALTER TABLE clubs ADD COLUMN monthly_budget_limit DECIMAL(10,2) DEFAULT 5000.00 COMMENT '月度预算上限';
ALTER TABLE clubs ADD COLUMN budget_enforce_mode ENUM('SOFT','HARD') DEFAULT 'SOFT' COMMENT '预算超限模式: SOFT=二次确认, HARD=硬拒绝';

INSERT INTO audit_configs (type, nodes, is_active) VALUES ('MONTHLY_BUDGET_LIMIT', '{"defaultLimit": 5000, "defaultEnforceMode": "SOFT"}', 1);
