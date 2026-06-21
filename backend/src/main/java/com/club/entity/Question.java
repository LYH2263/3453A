package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@TableName("questions")
public class Question {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "问题标题不能为空")
    private String title;
    @NotBlank(message = "问题详情不能为空")
    private String content;
    @NotNull(message = "提问者ID不能为空")
    private Integer authorId;
    private Integer targetClubId;
    private String targetRole; // 'ADMIN', 'UNION_ADMIN', 'CLUB_LEADER'
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }

    public Integer getTargetClubId() { return targetClubId; }
    public void setTargetClubId(Integer targetClubId) { this.targetClubId = targetClubId; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    // Transient fields for join queries
    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String targetClubName;
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getTargetClubName() { return targetClubName; }
    public void setTargetClubName(String targetClubName) { this.targetClubName = targetClubName; }
}
