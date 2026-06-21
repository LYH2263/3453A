-- ===========================================
-- 活动反馈情绪分析功能数据库迁移
-- F-11: 活动反馈情绪标签
-- ===========================================

USE club_db;

-- 1. 为 activity_registrations 表添加 sentiment 字段（情绪标签）
ALTER TABLE activity_registrations 
ADD COLUMN sentiment ENUM('POSITIVE', 'NEUTRAL', 'NEGATIVE') 
DEFAULT NULL 
COMMENT '反馈情绪标签: POSITIVE-正面, NEUTRAL-中性, NEGATIVE-负面'
AFTER feedback;

-- 2. 为 activity_registrations 表添加 feedback_tags 字段（命中关键词数组，JSON格式）
ALTER TABLE activity_registrations 
ADD COLUMN feedback_tags VARCHAR(500) 
DEFAULT NULL 
COMMENT '命中的情绪关键词标签（JSON数组格式）'
AFTER sentiment;

-- 3. 创建情绪关键词词典表
CREATE TABLE IF NOT EXISTS sentiment_dictionary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(50) NOT NULL COMMENT '关键词',
    sentiment ENUM('POSITIVE', 'NEGATIVE') NOT NULL COMMENT '情绪类型: POSITIVE-正面, NEGATIVE-负面',
    weight INT NOT NULL DEFAULT 1 COMMENT '权重（1-5，默认为1）',
    category VARCHAR(50) DEFAULT NULL COMMENT '关键词分类（如：服务、内容、组织等）',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_keyword (keyword)
) COMMENT='情绪分析关键词词典表';

-- 4. 初始化正面关键词
INSERT INTO sentiment_dictionary (keyword, sentiment, weight, category) VALUES
('棒', 'POSITIVE', 2, '通用'),
('好', 'POSITIVE', 1, '通用'),
('赞', 'POSITIVE', 2, '通用'),
('优秀', 'POSITIVE', 2, '通用'),
('满意', 'POSITIVE', 2, '通用'),
('喜欢', 'POSITIVE', 2, '通用'),
('精彩', 'POSITIVE', 2, '内容'),
('完美', 'POSITIVE', 3, '通用'),
('超棒', 'POSITIVE', 3, '通用'),
('太好了', 'POSITIVE', 2, '通用'),
('有意思', 'POSITIVE', 1, '内容'),
('有趣', 'POSITIVE', 2, '内容'),
('收获满满', 'POSITIVE', 3, '内容'),
('学到很多', 'POSITIVE', 3, '内容'),
('受益匪浅', 'POSITIVE', 3, '内容'),
('组织有序', 'POSITIVE', 2, '组织'),
('安排合理', 'POSITIVE', 2, '组织'),
('服务好', 'POSITIVE', 2, '服务'),
('热情', 'POSITIVE', 2, '服务'),
('贴心', 'POSITIVE', 2, '服务'),
('专业', 'POSITIVE', 2, '通用'),
('流畅', 'POSITIVE', 2, '组织'),
('顺利', 'POSITIVE', 2, '组织'),
('成功', 'POSITIVE', 2, '通用'),
('漂亮', 'POSITIVE', 2, '通用'),
('舒适', 'POSITIVE', 2, '环境'),
('干净', 'POSITIVE', 2, '环境'),
('温馨', 'POSITIVE', 2, '环境'),
('期待下次', 'POSITIVE', 3, '通用'),
('下次还来', 'POSITIVE', 3, '通用'),
('推荐', 'POSITIVE', 3, '通用'),
('值得', 'POSITIVE', 2, '通用'),
('给力', 'POSITIVE', 2, '通用'),
('666', 'POSITIVE', 2, '通用'),
('点赞', 'POSITIVE', 2, '通用'),
('比心', 'POSITIVE', 2, '通用'),
('爱了', 'POSITIVE', 2, '通用'),
('yyds', 'POSITIVE', 3, '通用'),
('绝绝子', 'POSITIVE', 2, '通用'),
('很有意义', 'POSITIVE', 2, '内容'),
('有价值', 'POSITIVE', 2, '内容'),
('高质量', 'POSITIVE', 2, '内容'),
('印象深刻', 'POSITIVE', 2, '内容'),
('难忘', 'POSITIVE', 2, '内容'),
('开心', 'POSITIVE', 2, '通用'),
('愉快', 'POSITIVE', 2, '通用'),
('快乐', 'POSITIVE', 2, '通用'),
('满足', 'POSITIVE', 2, '通用'),
('感动', 'POSITIVE', 2, '通用'),
('震撼', 'POSITIVE', 2, '内容'),
('惊艳', 'POSITIVE', 2, '内容'),
('创新', 'POSITIVE', 2, '内容'),
('实用', 'POSITIVE', 2, '内容'),
('干货', 'POSITIVE', 2, '内容'),
('充实', 'POSITIVE', 2, '内容');

-- 5. 初始化负面关键词
INSERT INTO sentiment_dictionary (keyword, sentiment, weight, category) VALUES
('差', 'NEGATIVE', 2, '通用'),
('烂', 'NEGATIVE', 3, '通用'),
('糟糕', 'NEGATIVE', 2, '通用'),
('失望', 'NEGATIVE', 2, '通用'),
('不好', 'NEGATIVE', 2, '通用'),
('不满意', 'NEGATIVE', 2, '通用'),
('浪费时间', 'NEGATIVE', 3, '内容'),
('无聊', 'NEGATIVE', 2, '内容'),
('没意思', 'NEGATIVE', 2, '内容'),
('混乱', 'NEGATIVE', 2, '组织'),
('乱', 'NEGATIVE', 2, '组织'),
('迟到', 'NEGATIVE', 2, '组织'),
('拖延', 'NEGATIVE', 2, '组织'),
('敷衍', 'NEGATIVE', 2, '服务'),
('态度差', 'NEGATIVE', 2, '服务'),
('不专业', 'NEGATIVE', 2, '服务'),
('冷漠', 'NEGATIVE', 2, '服务'),
('挤', 'NEGATIVE', 2, '环境'),
('热', 'NEGATIVE', 2, '环境'),
('冷', 'NEGATIVE', 2, '环境'),
('吵', 'NEGATIVE', 2, '环境'),
('脏', 'NEGATIVE', 2, '环境'),
('环境差', 'NEGATIVE', 2, '环境'),
('坑', 'NEGATIVE', 3, '通用'),
('骗', 'NEGATIVE', 3, '通用'),
('后悔', 'NEGATIVE', 2, '通用'),
('不值', 'NEGATIVE', 2, '通用'),
('垃圾', 'NEGATIVE', 3, '通用'),
('差评', 'NEGATIVE', 3, '通用'),
('投诉', 'NEGATIVE', 3, '通用'),
('不会再来', 'NEGATIVE', 3, '通用'),
('不推荐', 'NEGATIVE', 3, '通用'),
('取消', 'NEGATIVE', 2, '组织'),
('没学到东西', 'NEGATIVE', 3, '内容'),
('内容水', 'NEGATIVE', 2, '内容'),
('水', 'NEGATIVE', 2, '内容'),
('形式主义', 'NEGATIVE', 3, '内容'),
('走过场', 'NEGATIVE', 2, '内容'),
('敷衍了事', 'NEGATIVE', 3, '内容'),
('准备不足', 'NEGATIVE', 2, '组织'),
('设备差', 'NEGATIVE', 2, '环境'),
('音响差', 'NEGATIVE', 2, '环境'),
('话筒', 'NEGATIVE', 1, '环境'),
('座位', 'NEGATIVE', 1, '环境'),
('太远', 'NEGATIVE', 2, '环境'),
('太小', 'NEGATIVE', 2, '环境'),
('太热', 'NEGATIVE', 2, '环境'),
('太冷', 'NEGATIVE', 2, '环境'),
('太吵', 'NEGATIVE', 2, '环境'),
('太挤', 'NEGATIVE', 2, '环境'),
('太慢', 'NEGATIVE', 2, '组织'),
('太快', 'NEGATIVE', 2, '内容'),
('听不懂', 'NEGATIVE', 2, '内容'),
('太难', 'NEGATIVE', 2, '内容'),
('太简单', 'NEGATIVE', 2, '内容');

-- 6. 为已有反馈数据初始化情绪标签（可选，用于历史数据）
-- 先创建一个存储过程来批量更新历史数据
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS update_existing_feedback_sentiment()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE reg_id INT;
    DECLARE feedback_text TEXT;
    DECLARE cur CURSOR FOR SELECT id, feedback FROM activity_registrations WHERE feedback IS NOT NULL AND feedback != '';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO reg_id, feedback_text;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 简单规则：rating >= 4 正面，rating <= 2 负面，否则中性
        -- 对于没有 rating 但有 feedback 的，先标记为 NEUTRAL
        UPDATE activity_registrations 
        SET sentiment = CASE 
            WHEN rating >= 4 THEN 'POSITIVE'
            WHEN rating <= 2 THEN 'NEGATIVE'
            ELSE 'NEUTRAL'
        END,
        feedback_tags = NULL
        WHERE id = reg_id;
    END LOOP;

    CLOSE cur;
END //

DELIMITER ;

-- 执行存储过程更新历史数据
CALL update_existing_feedback_sentiment();

-- 删除存储过程
DROP PROCEDURE IF EXISTS update_existing_feedback_sentiment;
