package com.club.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.dto.ActivityFeedbackStats;
import com.club.entity.Activity;

import java.util.List;

public interface ActivityService extends IService<Activity> {
    Result<?> createActivity(Activity activity, String budgetToken, List<Integer> coHostClubIds);
    Result<?> updateActivity(Integer id, Activity activity, String budgetToken);
    Result<?> deleteActivity(Integer id);
    Result<?> auditActivity(Integer id, String status, String reason);
    Result<?> register(Integer activityId, Integer userId);
    Result<?> signin(Integer activityId, Integer userId);
    Result<?> feedback(Integer activityId, Integer userId, Integer rating, String feedback);
    Result<?> finishActivity(Integer id);
    Result<?> replyFeedback(Integer activityId, Integer userId, String reply);
    Result<?> cancelRegistration(Integer activityId, Integer userId);
    Result<?> leaveWaitlist(Integer activityId, Integer userId);
    Result<?> getWaitlistInfo(Integer activityId, Integer userId);
    Result<?> expandCapacity(Integer activityId, Integer newMaxCount);
    Result<?> getActivityDetail(Integer activityId, Integer userId);
    Result<?> confirmCoHost(Integer activityId, Integer coHostId, String status, String reason);
    Result<?> getMyPendingCoHosts();
    Result<ActivityFeedbackStats> getActivityFeedbackStats(Integer activityId);
    Result<?> getFeedbackList(String sentiment, Integer activityId, Integer clubId);
    Result<?> getCalendarActivities(String start, String end, Integer clubId);
}
