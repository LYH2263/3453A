package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.dto.ActivityFeedbackStats;
import com.club.dto.SentimentAnalysisResult;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.ActivityService;
import com.club.service.BudgetLimitService;
import com.club.service.SentimentAnalysisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityServiceImpl.class);

    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ActivityPromotionLogMapper promotionLogMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ClubMapper clubMapper;
    @Autowired
    private ActivityCoHostMapper coHostMapper;
    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BudgetLimitService budgetLimitService;

    private Result<?> checkBudgetLimit(Integer clubId, BigDecimal newBudget, BigDecimal oldBudget, String budgetToken) {
        if (newBudget == null || newBudget.compareTo(BigDecimal.ZERO) <= 0) return null;

        BudgetLimitService.BudgetLimitResult resolved = budgetLimitService.resolveBudgetLimit(clubId);
        BigDecimal limit = resolved.limit;
        String enforceMode = resolved.enforceMode;

        BigDecimal currentSum = activityMapper.sumMonthlyBudget(clubId);
        BigDecimal projectedTotal = currentSum;
        if (oldBudget != null) {
            projectedTotal = projectedTotal.subtract(oldBudget);
        }
        projectedTotal = projectedTotal.add(newBudget);

        if (projectedTotal.compareTo(limit) > 0) {
            if ("HARD".equals(enforceMode)) {
                return Result.error(4003, "月度预算已超限（当前合计：" + projectedTotal + "元，上限：" + limit + "元），不允许发起活动");
            }

            if (budgetToken != null && !budgetToken.trim().isEmpty()) {
                Map<String, Object> verified = budgetLimitService.verifyBudgetToken(budgetToken);
                if (verified != null) {
                    Integer tokenClubId = (Integer) verified.get("clubId");
                    BigDecimal tokenProjectedTotal = (BigDecimal) verified.get("projectedTotal");
                    BigDecimal tokenLimit = (BigDecimal) verified.get("limit");
                    if (tokenClubId != null && tokenClubId.equals(clubId)
                        && tokenProjectedTotal != null && tokenProjectedTotal.compareTo(projectedTotal) == 0
                        && tokenLimit != null && tokenLimit.compareTo(limit) == 0) {
                        return null;
                    }
                }
                return Result.error(4004, "预算确认凭证已失效或不匹配，请重新确认");
            }

            String token = budgetLimitService.generateBudgetToken(clubId, projectedTotal, limit);
            Map<String, Object> data = new HashMap<>();
            data.put("budgetWarning", true);
            data.put("currentTotal", currentSum);
            data.put("projectedTotal", projectedTotal);
            data.put("limit", limit);
            data.put("budgetToken", token);
            return Result.success(data);
        }
        return null;
    }

    @Override
    @Transactional
    public Result<?> createActivity(Activity activity, String budgetToken, List<Integer> coHostClubIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer currentUserId = null;
        Integer currentUserClubId = null;
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (currentUser != null && com.club.common.RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
                if (currentUser.getClubId() == null) {
                    return Result.error("您尚未绑定任何社团，无法发起活动");
                }
                activity.setClubId(currentUser.getClubId());
                currentUserId = currentUser.getId();
                currentUserClubId = currentUser.getClubId();
            }
        }

        if (activity.getClubId() != null) {
            Result<?> budgetCheck = checkBudgetLimit(activity.getClubId(), activity.getBudget(), null, budgetToken);
            if (budgetCheck != null) return budgetCheck;
        }

        boolean hasCoHosts = coHostClubIds != null && !coHostClubIds.isEmpty();
        if (hasCoHosts) {
            Integer hostClubId = activity.getClubId();
            if (hostClubId == null) {
                return Result.error("主办社团不能为空");
            }
            for (Integer coHostClubId : coHostClubIds) {
                if (coHostClubId.equals(hostClubId)) {
                    return Result.error("合作社团不能与主办社团相同");
                }
            }
            activity.setStatus("DRAFT_COCONFIRM");
        } else {
            activity.setStatus("PENDING_UNION");
        }

        boolean saved = this.save(activity);
        if (!saved) {
            return Result.error("活动保存失败");
        }

        if (hasCoHosts) {
            for (Integer coHostClubId : coHostClubIds) {
                ActivityCoHost coHost = new ActivityCoHost();
                coHost.setActivityId(activity.getId());
                coHost.setClubId(coHostClubId);
                coHost.setStatus("PENDING");
                coHostMapper.insert(coHost);
            }
        }

        return Result.success(null);
    }

    @Override
    public Result<?> updateActivity(Integer id, Activity activity, String budgetToken) {
        Activity existing = this.getById(id);
        if (existing == null) return Result.error("活动不存在");

        if ("APPROVED".equals(existing.getStatus()) || "FINISHED".equals(existing.getStatus())) {
            return Result.error("已通过或已结束的活动无法编辑");
        }

        if (activity.getClubId() != null || existing.getClubId() != null) {
            Integer clubId = existing.getClubId() != null ? existing.getClubId() : activity.getClubId();
            BigDecimal newBudget = activity.getBudget() != null ? activity.getBudget() : existing.getBudget();
            Result<?> budgetCheck = checkBudgetLimit(clubId, newBudget, existing.getBudget(), budgetToken);
            if (budgetCheck != null) return budgetCheck;
        }

        if (activity.getTitle() != null) existing.setTitle(activity.getTitle());
        if (activity.getDescription() != null) existing.setDescription(activity.getDescription());
        if (activity.getProcess() != null) existing.setProcess(activity.getProcess());
        if (activity.getLocation() != null) existing.setLocation(activity.getLocation());
        if (activity.getStartTime() != null) existing.setStartTime(activity.getStartTime());
        if (activity.getEndTime() != null) existing.setEndTime(activity.getEndTime());
        if (activity.getMaxCount() != null) existing.setMaxCount(activity.getMaxCount());
        if (activity.getBudget() != null) existing.setBudget(activity.getBudget());
        if (activity.getPoster() != null) existing.setPoster(activity.getPoster());

        this.updateById(existing);
        return Result.success(null);
    }

    @Override
    public Result<?> deleteActivity(Integer id) {
        Activity activity = this.getById(id);
        if (activity == null) return Result.error("活动不存在");
        this.removeById(id);
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

        List<ActivityCoHost> coHosts = coHostMapper.selectList(
            new LambdaQueryWrapper<ActivityCoHost>()
                .eq(ActivityCoHost::getActivityId, activityId)
                .orderByAsc(ActivityCoHost::getCreateTime)
        );
        for (ActivityCoHost coHost : coHosts) {
            Club club = clubMapper.selectById(coHost.getClubId());
            coHost.setClub(club);
        }
        detail.put("coHosts", coHosts);

        Club hostClub = clubMapper.selectById(activity.getClubId());
        detail.put("hostClub", hostClub);

        return Result.success(detail);
    }

    @Override
    @Transactional
    public Result<?> expandCapacity(Integer activityId, Integer newMaxCount) {
        Activity activity = activityMapper.selectByIdForUpdate(activityId);
        if (activity == null) return Result.error("活动不存在");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Result.error("尚未认证");

        User currentUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (currentUser == null) return Result.error("用户不存在");

        if (!com.club.common.RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
            return Result.error("只有社团负责人才能扩容");
        }

        if (!activity.getClubId().equals(currentUser.getClubId())) {
            return Result.error("只有主办社团负责人才能扩容此活动");
        }

        if (newMaxCount == null || newMaxCount <= 0) {
            return Result.error("人数上限必须大于0");
        }

        if (activity.getMaxCount() != null && newMaxCount <= activity.getMaxCount()) {
            return Result.error("新的人数上限必须大于当前上限");
        }

        int operatorId = currentUser.getId();

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

        SentimentAnalysisResult analysisResult = sentimentAnalysisService.analyze(feedback);
        reg.setSentiment(analysisResult.getSentiment());
        try {
            String tagsJson = objectMapper.writeValueAsString(analysisResult.getTags());
            reg.setFeedbackTags(tagsJson);
        } catch (JsonProcessingException e) {
            reg.setFeedbackTags(null);
        }

        registrationMapper.updateById(reg);

        Map<String, Object> result = new HashMap<>();
        result.put("sentiment", analysisResult.getSentiment());
        result.put("tags", analysisResult.getTags());
        return Result.success(result);
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

    @Override
    @Transactional
    public Result<?> confirmCoHost(Integer activityId, Integer coHostId, String status, String reason) {
        Activity activity = this.getById(activityId);
        if (activity == null) return Result.error("活动不存在");

        if (!"DRAFT_COCONFIRM".equals(activity.getStatus())) {
            return Result.error("活动当前状态不支持合作社团确认操作");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Result.error("尚未认证");
        String username = auth.getName();
        User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (currentUser == null) return Result.error("用户不存在");
        if (!com.club.common.RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
            return Result.error("只有社团负责人才能确认合作活动");
        }

        ActivityCoHost coHost = coHostMapper.selectById(coHostId);
        if (coHost == null) return Result.error("合作记录不存在");
        if (!coHost.getActivityId().equals(activityId)) return Result.error("合作记录与活动不匹配");
        if (!"PENDING".equals(coHost.getStatus())) {
            return Result.error("该合作社团已确认或拒绝，无法重复操作");
        }
        if (!coHost.getClubId().equals(currentUser.getClubId())) {
            return Result.error("您不是该合作社团的负责人，无权操作");
        }

        if ("CONFIRMED".equals(status)) {
            coHost.setStatus("CONFIRMED");
            coHost.setConfirmTime(LocalDateTime.now());
            coHost.setRejectReason(null);
            coHostMapper.updateById(coHost);

            long pendingCount = coHostMapper.selectCount(
                new LambdaQueryWrapper<ActivityCoHost>()
                    .eq(ActivityCoHost::getActivityId, activityId)
                    .eq(ActivityCoHost::getStatus, "PENDING")
            );

            if (pendingCount == 0) {
                activity.setStatus("PENDING_UNION");
                this.updateById(activity);
            }
        } else if ("REJECTED".equals(status)) {
            if (reason == null || reason.trim().isEmpty()) {
                return Result.error("拒绝原因不能为空");
            }
            coHost.setStatus("REJECTED");
            coHost.setConfirmTime(LocalDateTime.now());
            coHost.setRejectReason(reason);
            coHostMapper.updateById(coHost);

            coHostMapper.update(null,
                new LambdaUpdateWrapper<ActivityCoHost>()
                    .eq(ActivityCoHost::getActivityId, activityId)
                    .eq(ActivityCoHost::getStatus, "PENDING")
                    .set(ActivityCoHost::getStatus, "REJECTED")
                    .set(ActivityCoHost::getRejectReason, "活动已被其他合作社团拒绝")
                    .set(ActivityCoHost::getConfirmTime, LocalDateTime.now())
            );

            activity.setStatus("REJECTED");
            activity.setRejectReason("合作社团拒绝：" + reason);
            this.updateById(activity);
        } else {
            return Result.error("无效的状态值");
        }

        return Result.success(null);
    }

    @Override
    public Result<?> getMyPendingCoHosts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Result.error("尚未认证");
        String username = auth.getName();
        User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (currentUser == null) return Result.error("用户不存在");
        if (!com.club.common.RoleConstants.CLUB_LEADER.equals(currentUser.getRole())) {
            return Result.success(new ArrayList<>());
        }

        Integer clubId = currentUser.getClubId();
        List<ActivityCoHost> coHosts = coHostMapper.selectList(
            new LambdaQueryWrapper<ActivityCoHost>()
                .eq(ActivityCoHost::getClubId, clubId)
                .eq(ActivityCoHost::getStatus, "PENDING")
                .orderByDesc(ActivityCoHost::getCreateTime)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityCoHost coHost : coHosts) {
            Activity activity = this.getById(coHost.getActivityId());
            if (activity == null) continue;
            if (!"DRAFT_COCONFIRM".equals(activity.getStatus())) continue;

            Club hostClub = clubMapper.selectById(activity.getClubId());

            Map<String, Object> item = new HashMap<>();
            item.put("coHostId", coHost.getId());
            item.put("activityId", activity.getId());
            item.put("activityTitle", activity.getTitle());
            item.put("activityDescription", activity.getDescription());
            item.put("startTime", activity.getStartTime());
            item.put("endTime", activity.getEndTime());
            item.put("location", activity.getLocation());
            item.put("budget", activity.getBudget());
            item.put("hostClub", hostClub);
            item.put("createTime", coHost.getCreateTime());
            result.add(item);
        }

        return Result.success(result);
    }

    @Override
    public Result<ActivityFeedbackStats> getActivityFeedbackStats(Integer activityId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        String role = currentUser.getRole();
        boolean isAdmin = com.club.common.RoleConstants.ADMIN.equals(role)
                || com.club.common.RoleConstants.UNION_ADMIN.equals(role);
        boolean isLeader = com.club.common.RoleConstants.CLUB_LEADER.equals(role);
        if (!isAdmin && !isLeader) {
            return Result.error(403, "仅负责人可查看反馈统计");
        }
        Activity activity = this.getById(activityId);
        if (activity == null) {
            return Result.error("活动不存在");
        }
        if (isLeader && !activity.getClubId().equals(currentUser.getClubId())) {
            return Result.error(403, "仅可查看本社团的反馈统计");
        }
        ActivityFeedbackStats stats = new ActivityFeedbackStats();

        List<Map<String, Object>> sentimentCounts = registrationMapper.countBySentimentForActivity(activityId);

        long positiveCount = 0;
        long neutralCount = 0;
        long negativeCount = 0;

        for (Map<String, Object> row : sentimentCounts) {
            String sentiment = (String) row.get("sentiment");
            Long count = ((Number) row.get("count")).longValue();
            if ("POSITIVE".equals(sentiment)) {
                positiveCount = count;
            } else if ("NEUTRAL".equals(sentiment)) {
                neutralCount = count;
            } else if ("NEGATIVE".equals(sentiment)) {
                negativeCount = count;
            }
        }

        long totalCount = positiveCount + neutralCount + negativeCount;

        stats.setTotalCount(totalCount);
        stats.setPositiveCount(positiveCount);
        stats.setNeutralCount(neutralCount);
        stats.setNegativeCount(negativeCount);

        if (totalCount > 0) {
            stats.setPositivePercentage(Math.round(positiveCount * 100.0 / totalCount * 10.0) / 10.0);
            stats.setNeutralPercentage(Math.round(neutralCount * 100.0 / totalCount * 10.0) / 10.0);
            stats.setNegativePercentage(Math.round(negativeCount * 100.0 / totalCount * 10.0) / 10.0);
        }

        Double avgRating = registrationMapper.getAverageRatingForActivity(activityId);
        if (avgRating != null) {
            stats.setAverageRating(Math.round(avgRating * 10.0) / 10.0);
        }

        stats.setPositiveExamples(getFeedbackExamples(activityId, "POSITIVE", 10));
        stats.setNeutralExamples(getFeedbackExamples(activityId, "NEUTRAL", 10));
        stats.setNegativeExamples(getFeedbackExamples(activityId, "NEGATIVE", 10));

        stats.setTagFrequency(calculateTagFrequency(activityId));

        return Result.success(stats);
    }

    private List<Map<String, Object>> getFeedbackExamples(Integer activityId, String sentiment, int limit) {
        List<Map<String, Object>> examples = registrationMapper.getFeedbackExamplesBySentiment(
                activityId, sentiment, limit);
        for (Map<String, Object> example : examples) {
            String tagsJson = (String) example.get("feedback_tags");
            if (tagsJson != null && !tagsJson.isEmpty()) {
                try {
                    JsonNode node = objectMapper.readTree(tagsJson);
                    List<String> tags = new ArrayList<>();
                    for (JsonNode tagNode : node) {
                        tags.add(tagNode.asText());
                    }
                    example.put("tags", tags);
                } catch (JsonProcessingException e) {
                    example.put("tags", new ArrayList<>());
                }
            } else {
                example.put("tags", new ArrayList<>());
            }
        }
        return examples;
    }

    private List<Map<String, Object>> calculateTagFrequency(Integer activityId) {
        List<ActivityRegistration> registrations = registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .isNotNull(ActivityRegistration::getFeedbackTags)
                        .ne(ActivityRegistration::getFeedbackTags, ""));

        Map<String, Integer> tagCount = new HashMap<>();
        for (ActivityRegistration reg : registrations) {
            String tagsJson = reg.getFeedbackTags();
            if (tagsJson != null && !tagsJson.isEmpty()) {
                try {
                    JsonNode node = objectMapper.readTree(tagsJson);
                    for (JsonNode tagNode : node) {
                        String tag = tagNode.asText();
                        tagCount.put(tag, tagCount.getOrDefault(tag, 0) + 1);
                    }
                } catch (JsonProcessingException e) {
                    logger.warn("解析标签失败: {}", tagsJson, e);
                }
            }
        }

        return tagCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(15)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("tag", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result<?> getFeedbackList(String sentiment, Integer activityId, Integer clubId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        String role = currentUser.getRole();
        boolean isAdmin = com.club.common.RoleConstants.ADMIN.equals(role)
                || com.club.common.RoleConstants.UNION_ADMIN.equals(role);
        boolean isLeader = com.club.common.RoleConstants.CLUB_LEADER.equals(role);
        if (!isAdmin && !isLeader) {
            return Result.error(403, "仅负责人可查看反馈数据");
        }
        Integer filterClubId = clubId;
        if (isLeader) {
            if (filterClubId == null) {
                filterClubId = currentUser.getClubId();
            } else if (!filterClubId.equals(currentUser.getClubId())) {
                return Result.error(403, "仅可查看本社团的反馈数据");
            }
        }
        if (activityId != null) {
            Activity activity = this.getById(activityId);
            if (activity == null) {
                return Result.error("活动不存在");
            }
            if (isLeader && !activity.getClubId().equals(currentUser.getClubId())) {
                return Result.error(403, "仅可查看本社团的反馈数据");
            }
        }
        List<Map<String, Object>> list = registrationMapper.getFeedbackListWithFilters(
                sentiment, activityId, filterClubId);

        for (Map<String, Object> item : list) {
            String tagsJson = (String) item.get("feedback_tags");
            if (tagsJson != null && !tagsJson.isEmpty()) {
                try {
                    JsonNode node = objectMapper.readTree(tagsJson);
                    List<String> tags = new ArrayList<>();
                    for (JsonNode tagNode : node) {
                        tags.add(tagNode.asText());
                    }
                    item.put("tags", tags);
                } catch (JsonProcessingException e) {
                    item.put("tags", new ArrayList<>());
                }
            } else {
                item.put("tags", new ArrayList<>());
            }
        }

        return Result.success(list);
    }

    @Override
    public Result<?> getCalendarActivities(String start, String end, Integer clubId) {
        LocalDateTime startDateTime = parseDateTime(start);
        LocalDateTime endDateTime = parseDateTime(end);

        if (startDateTime == null || endDateTime == null) {
            return Result.error("日期参数格式错误，应为 YYYY-MM-DD HH:mm:ss");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer effectiveClubId = clubId;

        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (currentUser != null) {
                String role = currentUser.getRole();
                if (com.club.common.RoleConstants.CLUB_LEADER.equals(role)) {
                    effectiveClubId = currentUser.getClubId();
                } else if (com.club.common.RoleConstants.ADMIN.equals(role)
                        || com.club.common.RoleConstants.UNION_ADMIN.equals(role)) {
                } else {
                    effectiveClubId = null;
                }
            }
        }

        LambdaQueryWrapper<Activity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Activity::getStatus, "APPROVED", "FINISHED");
        queryWrapper.and(w -> w
            .and(w1 -> w1.le(Activity::getStartTime, endDateTime).ge(Activity::getEndTime, startDateTime))
            .or(w1 -> w1.between(Activity::getStartTime, startDateTime, endDateTime))
            .or(w1 -> w1.between(Activity::getEndTime, startDateTime, endDateTime))
        );

        if (effectiveClubId != null) {
            queryWrapper.eq(Activity::getClubId, effectiveClubId);
        }

        List<Activity> activities = this.list(queryWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Activity act : activities) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", act.getId());
            item.put("title", act.getTitle());
            item.put("startTime", act.getStartTime());
            item.put("endTime", act.getEndTime());
            item.put("location", act.getLocation());
            item.put("clubId", act.getClubId());
            item.put("status", act.getStatus());
            result.add(item);
        }

        return Result.success(result);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeStr,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
    }
}
