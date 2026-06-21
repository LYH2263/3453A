package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("user_notifications")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String type;
    private Integer topicId;
    private Integer commentId;
    private String content;
    private Integer triggerUserId;
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime readTime;

    @TableField(exist = false)
    private String triggerUserName;
    @TableField(exist = false)
    private String triggerUserAvatar;
    @TableField(exist = false)
    private String topicTitle;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getTopicId() { return topicId; }
    public void setTopicId(Integer topicId) { this.topicId = topicId; }

    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getTriggerUserId() { return triggerUserId; }
    public void setTriggerUserId(Integer triggerUserId) { this.triggerUserId = triggerUserId; }

    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getReadTime() { return readTime; }
    public void setReadTime(LocalDateTime readTime) { this.readTime = readTime; }

    public String getTriggerUserName() { return triggerUserName; }
    public void setTriggerUserName(String triggerUserName) { this.triggerUserName = triggerUserName; }

    public String getTriggerUserAvatar() { return triggerUserAvatar; }
    public void setTriggerUserAvatar(String triggerUserAvatar) { this.triggerUserAvatar = triggerUserAvatar; }

    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }
}
