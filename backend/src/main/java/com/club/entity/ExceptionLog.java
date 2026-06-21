package com.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exception_logs")
public class ExceptionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String operation;
    private String method;
    private String params;
    private String exceptionName;
    private String exceptionMessage;
    private String stackTrace;
    private String ip;
    private LocalDateTime createTime;
}
