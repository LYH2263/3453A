package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@TableName("recruitments")
public class Recruitment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotNull(message = "社团ID不能为空")
    private Integer clubId;
    @NotBlank(message = "招新标题不能为空")
    private String title;
    @NotBlank(message = "招新简介不能为空")
    private String description;
    @NotBlank(message = "状态不能为空")
    private String status; // 'OPEN', 'CLOSED'
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getClubId() { return clubId; }
    public void setClubId(Integer clubId) { this.clubId = clubId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
