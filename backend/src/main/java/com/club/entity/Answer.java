package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("answers")
public class Answer {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer questionId;
    private Integer authorId;
    private String content;
    private Integer isBest;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getIsBest() { return isBest; }
    public void setIsBest(Integer isBest) { this.isBest = isBest; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String authorRole;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
}
