package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ActivityPromotionLogMapper promotionLogMapper;
    @Autowired
    private ActivityMapper activityMapper;

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
    @Transactional
    public Result<?> register(Integer activityId, Integer userId) {
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) return Result.error("活动不存在");
        if (!"APPROVED".equals(activity.getStatus())) return Result.error("活动未审核通过，无法报名");

        ActivityRegistration existing = registrationMapper.selectOne(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));

        if (existing != null) {
            if ("CANCELLED".equals(existing.getStatus())) {
                return handleReRegister(activity, existing);
            }
            if ("WAITLIST".equals(existing.getStatus())) {
                return Result.error("您已在候补队列中");
            }
            return Result.error("已报过名");
        }

        long registeredCount = registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .in(ActivityRegistration::getStatus, "REGISTERED", "SIGNED_IN"));

        if (activity.getMaxCount() != null && registeredCount < activity.getMaxCount()) {
            return registerDirectly(activityId, userId);
        } else {
            return joinWaitlist(activityId, userId);
        }
    }

    private Result<?> handleReRegister(Activity activity, ActivityRegistration existing) {
        long registeredCount = registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, existing.getActivityId())
                .in(ActivityRegistration::getStatus, "REGISTERED", "SIGNED_IN"));

        if (activity.getMaxCount() != null && registeredCount < activity.getMaxCount()) {
            existing.setStatus("REGISTERED");
            existing.setWaitlistOrder(null);
            registrationMapper.updateById(existing);
            return Result.success("报名成功");
        } else {
            int waitlistCount = Math.toIntExact(registrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, existing.getActivityId())
                    .eq(ActivityRegistration::getStatus, "WAITLIST")));
            existing.setStatus("WAITLIST");
            existing.setWaitlistOrder(waitlistCount + 1);
            registrationMapper.updateById(existing);
            return Result.success("加入候补成功，当前排位：" + (waitlistCount + 1));
        }
    }

    private Result<?> registerDirectly(Integer activityId, Integer userId) {
        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityId(activityId);
        reg.setUserId(userId);
        reg.setStatus("REGISTERED");
        registrationMapper.insert(reg);
        return Result.success("报名成功");
    }

    private Result<?> joinWaitlist(Integer activityId, Integer userId) {
        int waitlistCount = Math.toIntExact(registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, "WAITLIST")));

        ActivityRegistration reg = new ActivityRegistration();
        reg.setActivityId(activityId);
        reg.setUserId(userId);
        reg.setStatus("WAITLIST");
        reg.setWaitlistOrder(waitlistCount + 1);
        registrationMapper.insert(reg);

        return Result.success("加入候补成功，当前排位：" + (waitlistCount + 1));
    }

    @Override
    @Transactional
    public Result<?> cancelRegistration(Integer activityId, Integer userId) {
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) return Result.error("活动不存在");

        ActivityRegistration reg = registrationMapper.selectOne(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));

        if (reg == null) return Result.error("未报名该活动");
        if ("CANCELLED".equals(reg.getStatus())) return Result.error("您已取消报名");
        if ("SIGNED_IN".equals(reg.getStatus())) return Result.error("已签到活动无法取消");
        if ("WAITLIST".equals(reg.getStatus())) {
            return leaveWaitlistInternal(reg);
        }

        reg.setStatus("CANCELLED");
        reg.setWaitlistOrder(null);
        registrationMapper.updateById(reg);

        promoteNextWaitlist(activityId, userId, "CANCEL");

        return Result.success("取消报名成功");
    }

    @Override
    @Transactional
    public Result<?> leaveWaitlist(Integer activityId, Integer userId) {
        ActivityRegistration reg = registrationMapper.selectOne(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));

        if (reg == null || !"WAITLIST".equals(reg.getStatus())) {
            return Result.error("您不在候补队列中");
        }

        return leaveWaitlistInternal(reg);
    }

    private Result<?> leaveWaitlistInternal(ActivityRegistration reg) {
        int removedOrder = reg.getWaitlistOrder();
        reg.setStatus("CANCELLED");
        reg.setWaitlistOrder(null);
        registrationMapper.updateById(reg);

        registrationMapper.update(null,
            new LambdaUpdateWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, reg.getActivityId())
                .eq(ActivityRegistration::getStatus, "WAITLIST")
                .gt(ActivityRegistration::getWaitlistOrder, removedOrder)
                .setSql("waitlist_order = waitlist_order - 1"));

        return Result.success("已退出候补队列");
    }

    private void promoteNextWaitlist(Integer activityId, Integer triggerUserId, String source) {
        ActivityRegistration next = registrationMapper.selectOne(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, "WAITLIST")
                .orderByAsc(ActivityRegistration::getWaitlistOrder)
                .last("LIMIT 1"));

        if (next != null) {
            int originalOrder = next.getWaitlistOrder();
            next.setStatus("REGISTERED");
            next.setWaitlistOrder(null);
            registrationMapper.updateById(next);

            ActivityPromotionLog log = new ActivityPromotionLog();
            log.setActivityId(activityId);
            log.setUserId(next.getUserId());
            log.setOriginalOrder(originalOrder);
            log.setSource(source);
            log.setTriggerUserId(triggerUserId);
            promotionLogMapper.insert(log);

            registrationMapper.update(null,
                new LambdaUpdateWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .eq(ActivityRegistration::getStatus, "WAITLIST")
                    .gt(ActivityRegistration::getWaitlistOrder, originalOrder)
                    .setSql("waitlist_order = waitlist_order - 1"));
        }
    }

    @Override
    public Result<?> getWaitlistInfo(Integer activityId, Integer userId) {
        Map<String, Object> info = new HashMap<>();

        ActivityRegistration reg = registrationMapper.selectOne(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));

        boolean inWaitlist = reg != null && "WAITLIST".equals(reg.getStatus());
        info.put("inWaitlist", inWaitlist);
        info.put("myPosition", inWaitlist ? reg.getWaitlistOrder() : null);

        long waitlistTotal = registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, "WAITLIST"));
        info.put("waitlistTotal", waitlistTotal);

        return Result.success(info);
    }

    @Override
    public Result<?> getActivityDetail(Integer activityId, Integer userId) {
        Activity activity = this.getById(activityId);
        if (activity == null) return Result.error("活动不存在");

        Map<String, Object> detail = new HashMap<>();
        detail.put("activity", activity);

        long registeredCount = registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .in(ActivityRegistration::getStatus, "REGISTERED", "SIGNED_IN"));
        detail.put("registeredCount", registeredCount);

        long waitlistCount = registrationMapper.selectCount(
            new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, "WAITLIST"));
        detail.put("waitlistCount", waitlistCount);

        boolean isFull = activity.getMaxCount() != null && registeredCount >= activity.getMaxCount();
        detail.put("isFull", isFull);

        if (userId != null) {
            ActivityRegistration reg = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .eq(ActivityRegistration::getUserId, userId));

            detail.put("myStatus", reg != null ? reg.getStatus() : null);
            detail.put("myPosition", reg != null && "WAITLIST".equals(reg.getStatus()) ? reg.getWaitlistOrder() : null);
        }

        return Result.success(detail);
    }

    @Override
    @Transactional
    public Result<?> expandCapacity(Integer activityId, Integer newMaxCount, Integer operatorId) {
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) return Result.error("活动不存在");

        if (newMaxCount == null || newMaxCount <= 0) {
            return Result.error("人数上限必须大于0");
        }

        if (activity.getMaxCount() != null && newMaxCount <= activity.getMaxCount()) {
            return Result.error("新的人数上限必须大于当前上限");
        }

        int oldMax = activity.getMaxCount() != null ? activity.getMaxCount() : 0;
        int availableSlots = newMaxCount - oldMax;

        activity.setMaxCount(newMaxCount);
        this.updateById(activity);

        int promotedCount = 0;
        for (int i = 0; i < availableSlots; i++) {
            long currentRegistered = registrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .in(ActivityRegistration::getStatus, "REGISTERED", "SIGNED_IN"));

            if (currentRegistered >= newMaxCount) break;

            ActivityRegistration next = registrationMapper.selectOne(
                new LambdaQueryWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .eq(ActivityRegistration::getStatus, "WAITLIST")
                    .orderByAsc(ActivityRegistration::getWaitlistOrder)
                    .last("LIMIT 1"));

            if (next == null) break;

            int originalOrder = next.getWaitlistOrder();
            next.setStatus("REGISTERED");
            next.setWaitlistOrder(null);
            registrationMapper.updateById(next);

            ActivityPromotionLog log = new ActivityPromotionLog();
            log.setActivityId(activityId);
            log.setUserId(next.getUserId());
            log.setOriginalOrder(originalOrder);
            log.setSource("EXPAND");
            log.setTriggerUserId(operatorId);
            promotionLogMapper.insert(log);

            registrationMapper.update(null,
                new LambdaUpdateWrapper<ActivityRegistration>()
                    .eq(ActivityRegistration::getActivityId, activityId)
                    .eq(ActivityRegistration::getStatus, "WAITLIST")
                    .gt(ActivityRegistration::getWaitlistOrder, originalOrder)
                    .setSql("waitlist_order = waitlist_order - 1"));

            promotedCount++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newMaxCount", newMaxCount);
        result.put("promotedCount", promotedCount);
        return Result.success(result);
    }

    @Override
    public Result<?> signin(Integer activityId, Integer userId) {
        ActivityRegistration reg = registrationMapper.selectOne(new LambdaQueryWrapper<ActivityRegistration>()
                .eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId));
        if (reg == null) return Result.error("未报名该活动");
        if (!"REGISTERED".equals(reg.getStatus())) return Result.error("只有已报名状态才能签到");
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
