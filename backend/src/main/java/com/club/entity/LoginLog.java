package com.club.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_logs")
public class LoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String ip;
    private String location;
    private String browser;
    private String os;
    private Integer status;
    private String msg;
    private LocalDateTime loginTime;
}
