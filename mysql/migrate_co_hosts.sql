-- 活动合作社团关联表
CREATE TABLE IF NOT EXISTS activity_co_hosts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    activity_id INT NOT NULL COMMENT '活动ID',
    club_id INT NOT NULL COMMENT '合作社团ID',
    status ENUM('PENDING', 'CONFIRMED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '确认状态: PENDING-待确认, CONFIRMED-已确认, REJECTED-已拒绝',
    reject_reason VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    confirm_time DATETIME DEFAULT NULL COMMENT '确认/拒绝时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    FOREIGN KEY (activity_id) REFERENCES activities(id),
    FOREIGN KEY (club_id) REFERENCES clubs(id),
    UNIQUE KEY uk_activity_club (activity_id, club_id)
) COMMENT='活动合作社团关联表';

-- 扩展活动状态，增加 DRAFT_COCONFIRM（合作社团确认中）
-- 注意：MySQL 8.0+ 可以直接 ALTER TABLE 修改 ENUM
-- 为了兼容，我们使用以下方式
ALTER TABLE activities MODIFY COLUMN status ENUM(
    'DRAFT_COCONFIRM',
    'PENDING_UNION', 
    'PENDING_SCHOOL', 
    'APPROVED', 
    'REJECTED', 
    'FINISHED'
) NOT NULL DEFAULT 'PENDING_UNION' COMMENT '状态: DRAFT_COCONFIRM-合作社团确认中, PENDING_UNION-社联初审, PENDING_SCHOOL-学校终审, APPROVED-已通过, REJECTED-已驳回, FINISHED-已结束';
