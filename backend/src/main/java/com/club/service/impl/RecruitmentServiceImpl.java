package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.entity.Club;
import com.club.entity.Recruitment;
import com.club.entity.RecruitmentApplication;
import com.club.entity.User;
import com.club.mapper.ClubMapper;
import com.club.mapper.RecruitmentApplicationMapper;
import com.club.mapper.RecruitmentMapper;
import com.club.mapper.UserMapper;
import com.club.service.RecruitmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecruitmentServiceImpl extends ServiceImpl<RecruitmentMapper, Recruitment> implements RecruitmentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Autowired
    private RecruitmentApplicationMapper applicationMapper;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
    }

    @Override
    public Result<?> publishRecruitment(Recruitment recruitment) {
        User user = getCurrentUser();
        if (user == null) return Result.error("未认证");
        if (!RoleConstants.CLUB_LEADER.equals(user.getRole())) return Result.error("仅社团负责人可发布招新");
        if (user.getClubId() == null) return Result.error("未绑定社团");

        recruitment.setClubId(user.getClubId());
        recruitment.setStatus("OPEN");
        this.save(recruitment);
        return Result.success(null);
    }

    @Override
    public Result<?> getRecruitments() {
        List<Recruitment> list = this.list(new LambdaQueryWrapper<Recruitment>().orderByDesc(Recruitment::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        User user = getCurrentUser();

        for (Recruitment r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("title", r.getTitle());
            map.put("description", r.getDescription());
            map.put("status", r.getStatus());
            map.put("createTime", r.getCreateTime());

            Club c = clubMapper.selectById(r.getClubId());
            map.put("clubName", c != null ? c.getName() : "未知社团");

            if (user != null) {
                long applied = applicationMapper.selectCount(new LambdaQueryWrapper<RecruitmentApplication>()
                        .eq(RecruitmentApplication::getRecruitmentId, r.getId())
                        .eq(RecruitmentApplication::getUserId, user.getId()));
                map.put("hasApplied", applied > 0);
            }
            result.add(map);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> closeRecruitment(Integer id) {
        Recruitment r = this.getById(id);
        if (r == null) return Result.error("招新记录不存在");
        r.setStatus("CLOSED");
        this.updateById(r);
        return Result.success(null);
    }

    @Override
    public Result<?> apply(Integer recruitmentId, String resumeText) {
        User user = getCurrentUser();
        if (user == null) return Result.error("未认证");

        Recruitment r = this.getById(recruitmentId);
        if (r == null || "CLOSED".equals(r.getStatus())) return Result.error("招新通道未开放");

        long count = applicationMapper.selectCount(new LambdaQueryWrapper<RecruitmentApplication>()
                .eq(RecruitmentApplication::getRecruitmentId, recruitmentId)
                .eq(RecruitmentApplication::getUserId, user.getId()));
        if (count > 0) return Result.error("您已投递过简历");

        RecruitmentApplication app = new RecruitmentApplication();
        app.setRecruitmentId(recruitmentId);
        app.setUserId(user.getId());
        app.setResumeText(resumeText);
        app.setStatus("PENDING");
        applicationMapper.insert(app);

        return Result.success(null);
    }

    @Override
    public Result<?> getApplications(Integer recruitmentId) {
        User user = getCurrentUser();
        if (user == null || !RoleConstants.CLUB_LEADER.equals(user.getRole())) return Result.error("无权限");

        List<RecruitmentApplication> apps = applicationMapper.selectList(new LambdaQueryWrapper<RecruitmentApplication>()
                .eq(RecruitmentApplication::getRecruitmentId, recruitmentId)
                .orderByDesc(RecruitmentApplication::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (RecruitmentApplication app : apps) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", app.getId());
            map.put("resumeText", app.getResumeText());
            map.put("status", app.getStatus());
            map.put("createTime", app.getCreateTime());

            User student = userMapper.selectById(app.getUserId());
            map.put("realName", student != null ? student.getRealName() : "未知");
            map.put("studentId", student != null ? student.getStudentId() : "未知");
            
            result.add(map);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> auditApplication(Integer applicationId, String status) {
        RecruitmentApplication app = applicationMapper.selectById(applicationId);
        if (app == null) return Result.error("申请不存在");
        app.setStatus(status);
        applicationMapper.updateById(app);
        
        // Option: if approved, automatically change user role to MEMBER and update club_id?
        if ("APPROVED".equals(status)) {
            Recruitment r = this.getById(app.getRecruitmentId());
            User student = userMapper.selectById(app.getUserId());
            if (student != null && "GUEST".equals(student.getRole())) { // Assuming non-members are guests or basic roles
                // Assign to club
                student.setClubId(r.getClubId());
                student.setRole(RoleConstants.MEMBER);
                userMapper.updateById(student);
            }
        }
        
        return Result.success(null);
    }
}
