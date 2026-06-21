-- 问答社区：回答投票表
CREATE TABLE IF NOT EXISTS answer_votes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '投票用户ID',
    answer_id INT NOT NULL COMMENT '被投票的答案ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投票时间',
    UNIQUE KEY uk_user_answer (user_id, answer_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (answer_id) REFERENCES answers(id)
) COMMENT='回答投票表';

-- 为 answers 表增加乐观锁版本字段
ALTER TABLE answers ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
