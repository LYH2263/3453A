package com.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_configs")
public class AuditConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String type;
    private String nodes;
    private Integer isActive;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
