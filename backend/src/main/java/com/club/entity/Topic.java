package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@TableName("topics")
public class Topic {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    private Integer authorId;
    private Integer clubId;
    @NotBlank(message = "类型不能为空")
    private String type; // IN_CLUB, CROSS_CLUB
    private String auditStatus; // PENDING, APPROVED, REJECTED
    private String status; // NORMAL, TOP, HIDDEN
    private Integer likesCount;
    private Integer favoritesCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }

    public Integer getClubId() { return clubId; }
    public void setClubId(Integer clubId) { this.clubId = clubId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }

    public Integer getFavoritesCount() { return favoritesCount; }
    public void setFavoritesCount(Integer favoritesCount) { this.favoritesCount = favoritesCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String clubName;

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }
}
