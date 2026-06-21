package com.club.service;

import com.club.entity.OperationLog;
import com.club.entity.LoginLog;
import com.club.entity.ExceptionLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AdminLogService extends IService<OperationLog> {
    void saveOperationLog(OperationLog log);
    void saveLoginLog(LoginLog log);
    void saveExceptionLog(ExceptionLog log);
}
