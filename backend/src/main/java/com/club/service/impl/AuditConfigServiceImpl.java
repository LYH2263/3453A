package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.entity.AuditConfig;
import com.club.mapper.AuditConfigMapper;
import com.club.service.AuditConfigService;
import org.springframework.stereotype.Service;

@Service
public class AuditConfigServiceImpl extends ServiceImpl<AuditConfigMapper, AuditConfig> implements AuditConfigService {

    @Override
    public AuditConfig getConfigByType(String type) {
        return this.getOne(new LambdaQueryWrapper<AuditConfig>()
                .eq(AuditConfig::getType, type)
                .eq(AuditConfig::getIsActive, 1));
    }
}
