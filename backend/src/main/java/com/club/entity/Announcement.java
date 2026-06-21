package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@TableName("announcements")
public class Announcement {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "公告标题不能为空")
    private String title;
    @NotBlank(message = "公告内容不能为空")
    private String content;
    private Integer publisherId;
    private Integer clubId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableLogic
    private Integer isDeleted;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPublisherId() { return publisherId; }
    public void setPublisherId(Integer publisherId) { this.publisherId = publisherId; }

    public Integer getClubId() { return clubId; }
    public void setClubId(Integer clubId) { this.clubId = clubId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
