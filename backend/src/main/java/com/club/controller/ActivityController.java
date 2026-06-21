package com.club.controller;

import com.club.common.Result;
import com.club.entity.Activity;
import com.club.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    @Autowired
    private ActivityService activityService;

    @GetMapping
    public Result<?> list() {
        return Result.success(activityService.list());
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody Activity activity) {
        return activityService.createActivity(activity);
    }

    @PostMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        return activityService.auditActivity(id, params.get("status"), params.get("reason"));
    }

    @PostMapping("/{id}/register")
    public Result<?> register(@PathVariable Integer id, @RequestParam Integer userId) {
        return activityService.register(id, userId);
    }

    @PostMapping("/{id}/signin")
    public Result<?> signin(@PathVariable Integer id, @RequestParam Integer userId) {
        return activityService.signin(id, userId);
    }

    @PostMapping("/{id}/finish")
    public Result<?> finish(@PathVariable Integer id) {
        return activityService.finishActivity(id);
    }

    @PostMapping("/{id}/feedback")
    public Result<?> feedback(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        return activityService.feedback(id, (Integer) params.get("userId"), (Integer) params.get("rating"), (String) params.get("feedback"));
    }

    @PostMapping("/{id}/reply")
    public Result<?> reply(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        return activityService.replyFeedback(id, (Integer) params.get("userId"), (String) params.get("reply"));
    }
}
