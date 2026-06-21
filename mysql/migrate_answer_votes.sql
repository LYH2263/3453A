-- 问答社区：回答投票表（幂等）
USE club_db;

CREATE TABLE IF NOT EXISTS answer_votes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '投票用户ID',
    answer_id INT NOT NULL COMMENT '被投票的答案ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    UNIQUE KEY uk_user_answer (user_id, answer_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (answer_id) REFERENCES answers(id)
) COMMENT='回答投票表';

SET @db := DATABASE();
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'answers' AND COLUMN_NAME = 'version') = 0,
    'ALTER TABLE answers ADD COLUMN version INT DEFAULT 0 COMMENT ''乐观锁版本号''',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
