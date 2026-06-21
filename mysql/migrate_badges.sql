-- ===========================================
-- 徽章体系迁移脚本
-- ===========================================

USE club_db;

-- 徽章定义表
CREATE TABLE IF NOT EXISTS badges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    club_id INT NOT NULL COMMENT '所属社团ID',
    name VARCHAR(100) NOT NULL COMMENT '徽章名称',
    icon_url VARCHAR(255) DEFAULT NULL COMMENT '徽章图标URL',
    description TEXT COMMENT '徽章描述',
    is_public TINYINT(1) DEFAULT 1 COMMENT '是否公开: 1-公开, 0-不公开',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    INDEX idx_club_id (club_id),
    INDEX idx_public (is_public)
) COMMENT='徽章定义表';

-- 用户徽章关联表（授予记录）
CREATE TABLE IF NOT EXISTS user_badges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    badge_id INT NOT NULL COMMENT '徽章ID',
    granted_by INT NOT NULL COMMENT '授予人ID',
    granted_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '授予时间',
    revoked TINYINT(1) DEFAULT 0 COMMENT '是否已撤销: 0-未撤销, 1-已撤销',
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

-- ===========================================
-- Mock Data (测试数据)
-- ===========================================

-- 徽章数据
INSERT INTO badges (id, club_id, name, icon_url, description, is_public) VALUES
(1, 1, '技术先锋', 'https://api.dicebear.com/7.x/shapes/svg?seed=tech1&backgroundColor=409eff', '在技术领域有突出贡献的社员', 1),
(2, 1, '代码大师', 'https://api.dicebear.com/7.x/shapes/svg?seed=tech2&backgroundColor=67c23a', '编程能力卓越，解决重大技术难题', 1),
(3, 1, '创新之星', 'https://api.dicebear.com/7.x/shapes/svg?seed=tech3&backgroundColor=e6a23c', '提出创新想法并成功落地', 0),
(4, 2, '艺术达人', 'https://api.dicebear.com/7.x/shapes/svg?seed=art1&backgroundColor=f56c6c', '在艺术表演中表现出色', 1),
(5, 2, '最佳歌手', 'https://api.dicebear.com/7.x/shapes/svg?seed=art2&backgroundColor=909399', '歌唱比赛获奖选手', 1),
(6, 2, '幕后英雄', 'https://api.dicebear.com/7.x/shapes/svg?seed=art3&backgroundColor=8e44ad', '默默奉献的后勤工作人员', 0);

-- 用户徽章授予记录
INSERT INTO user_badges (id, user_id, badge_id, granted_by, granted_time, revoked) VALUES
(1, 5, 1, 3, '2024-03-15 10:00:00', 0),
(2, 5, 4, 4, '2024-04-01 14:00:00', 0),
(3, 6, 2, 3, '2024-03-20 09:30:00', 0),
(4, 6, 5, 4, '2024-02-20 16:00:00', 1),
(5, 3, 1, 1, '2024-01-10 08:00:00', 0),
(6, 4, 4, 1, '2024-01-10 08:00:00', 0);
