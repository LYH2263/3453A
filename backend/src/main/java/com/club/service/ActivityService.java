package com.club.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.club.entity.Activity;
import com.club.common.Result;

public interface ActivityService extends IService<Activity> {
    Result<?> createActivity(Activity activity);
    Result<?> auditActivity(Integer id, String status, String reason);
    Result<?> register(Integer activityId, Integer userId);
    Result<?> signin(Integer activityId, Integer userId);
    Result<?> feedback(Integer activityId, Integer userId, Integer rating, String feedback);
    Result<?> finishActivity(Integer id);
    Result<?> replyFeedback(Integer activityId, Integer userId, String reply);
}
