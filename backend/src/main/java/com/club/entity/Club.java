package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@TableName("clubs")
public class Club {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "社团名称不能为空")
    private String name;
    @NotBlank(message = "社团简介不能为空")
    private String description;
    private Integer leaderId;
    private String status; // NORMAL, RETIRED
    private String logo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDeleted;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getLeaderId() { return leaderId; }
    public void setLeaderId(Integer leaderId) { this.leaderId = leaderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
