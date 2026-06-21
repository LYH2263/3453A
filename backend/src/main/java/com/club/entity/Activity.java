package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@TableName("activities")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "活动标题不能为空")
    private String title;
    @NotBlank(message = "活动详情不能为空")
    private String description;
    private Integer clubId;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    @NotBlank(message = "地点不能为空")
    private String location;
    private Integer maxCount;
    private BigDecimal budget;
    private String status; // PENDING_UNION, PENDING_SCHOOL, APPROVED, REJECTED, FINISHED
    private String rejectReason;
    private String process;
    private String poster;

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getClubId() { return clubId; }
    public void setClubId(Integer clubId) { this.clubId = clubId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getMaxCount() { return maxCount; }
    public void setMaxCount(Integer maxCount) { this.maxCount = maxCount; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public String getProcess() { return process; }
    public void setProcess(String process) { this.process = process; }

    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
