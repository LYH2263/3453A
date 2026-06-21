package com.club.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.entity.OperationLog;
import com.club.entity.LoginLog;
import com.club.entity.ExceptionLog;
import com.club.mapper.OperationLogMapper;
import com.club.mapper.LoginLogMapper;
import com.club.mapper.ExceptionLogMapper;
import com.club.service.AdminLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements AdminLogService {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private ExceptionLogMapper exceptionLogMapper;

    @Override
    @Transactional
    public void saveOperationLog(OperationLog log) {
        this.save(log);
    }

    @Override
    @Transactional
    public void saveLoginLog(LoginLog log) {
        loginLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void saveExceptionLog(ExceptionLog log) {
        exceptionLogMapper.insert(log);
    }
}
