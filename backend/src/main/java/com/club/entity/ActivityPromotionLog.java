package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("activity_promotion_logs")
public class ActivityPromotionLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer activityId;
    private Integer userId;
    private Integer originalOrder;
    private String source;
    private Integer triggerUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getActivityId() { return activityId; }
    public void setActivityId(Integer activityId) { this.activityId = activityId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getOriginalOrder() { return originalOrder; }
    public void setOriginalOrder(Integer originalOrder) { this.originalOrder = originalOrder; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getTriggerUserId() { return triggerUserId; }
    public void setTriggerUserId(Integer triggerUserId) { this.triggerUserId = triggerUserId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
