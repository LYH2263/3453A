package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("comment_mentions")
public class CommentMention {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer commentId;
    private Integer topicId;
    private Integer mentionedUserId;
    private String mentionType;
    private String mentionText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCommentId() { return commentId; }
    public void setCommentId(Integer commentId) { this.commentId = commentId; }

    public Integer getTopicId() { return topicId; }
    public void setTopicId(Integer topicId) { this.topicId = topicId; }

    public Integer getMentionedUserId() { return mentionedUserId; }
    public void setMentionedUserId(Integer mentionedUserId) { this.mentionedUserId = mentionedUserId; }

    public String getMentionType() { return mentionType; }
    public void setMentionType(String mentionType) { this.mentionType = mentionType; }

    public String getMentionText() { return mentionText; }
    public void setMentionText(String mentionText) { this.mentionText = mentionText; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
