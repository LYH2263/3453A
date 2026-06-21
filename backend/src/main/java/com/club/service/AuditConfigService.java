package com.club.service;

import com.club.entity.AuditConfig;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AuditConfigService extends IService<AuditConfig> {
    AuditConfig getConfigByType(String type);
}
