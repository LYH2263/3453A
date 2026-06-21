package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result<?> createActivity(Activity activity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (currentUser != null && com.club.common.RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
                if (currentUser.getClubId() == null) {
                    return Result.error("您尚未绑定任何社团，无法发起活动");
                }
                activity.setClubId(currentUser.getClubId());
            }
        }
        
        activity.setStatus("PENDING_UNION");
        boolean saved = this.save(activity);
        if (!saved) {
            return Result.error("活动保存失败");
        }
        return Result.success(null);
    }

    @Override
    public Result<?> auditActivity(Integer id, String status, String reason) {
        Activity activity = this.getById(id);
        if (activity == null) return Result.error("活动不存在");
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("尚未认证");
        boolean isUnionAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_UNION_ADMIN"));
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if ("REJECTED".equals(status)) {
            activity.setStatus("REJECTED");
            activity.setRejectReason(reason);
        } else if ("APPROVED".equals(status)) {
            if (isUnionAdmin) {
                activity.setStatus("PENDING_SCHOOL");
            } else if (isAdmin) {
                activity.setStatus("APPROVED");
            } else {
                return Result.error("无权限审批此步骤");
            }
        }
        
        this.updateById(activity);
        return Result.success(null);
    }

    @Override
    public Result<?> register(Integer activityId, Integer userId) {
        Activity activity = this.getById(activityId);
        if (activity == null) return Result.error("活动不存在");

        long count = registrationMapper.selectCount(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (count > 0) return Result.error("已报过名");

        long currentEnrollment = registrationMapper.selectCount(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId));
        if (activity.getMaxCount() != null && currentEnrollment >= activity.getMaxCount()) {
            return Result.error("活动人数已报满");
        }

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityId(activityId);
        reg.setUserId(userId);
        reg.setStatus("REGISTERED");
        registrationMapper.insert(reg);
        return Result.success(null);
    }

    @Override
    public Result<?> signin(Integer activityId, Integer userId) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (reg == null) return Result.error("未报名该活动");
        reg.setStatus("SIGNED_IN");
        registrationMapper.updateById(reg);
        return Result.success(null);
    }

    @Override
    public Result<?> feedback(Integer activityId, Integer userId, Integer rating, String feedback) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (reg == null) return Result.error("未参与该活动");
        reg.setRating(rating);
        reg.setFeedback(feedback);
        registrationMapper.updateById(reg);
        return Result.success(null);
    }
    @Override
    public Result<?> finishActivity(Integer id) {
        Activity activity = this.getById(id);
        if (activity == null) return Result.error("活动不存在");
        if (!"APPROVED".equals(activity.getStatus())) return Result.error("只有已通过并举办的活动才能结束");
        activity.setStatus("FINISHED");
        this.updateById(activity);
        return Result.success(null);
    }

    @Override
    public Result<?> replyFeedback(Integer activityId, Integer userId, String reply) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (reg == null) return Result.error("记录不存在");
        reg.setReply(reply);
        registrationMapper.updateById(reg);
        return Result.success(null);
    }
}
