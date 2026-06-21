package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.Recruitment;

public interface RecruitmentService extends IService<Recruitment> {
    Result<?> publishRecruitment(Recruitment recruitment);
    Result<?> getRecruitments();
    Result<?> closeRecruitment(Integer id);
    Result<?> apply(Integer recruitmentId, String resumeText);
    Result<?> getApplications(Integer recruitmentId);
    Result<?> auditApplication(Integer applicationId, String status);
}
