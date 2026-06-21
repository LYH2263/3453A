package com.club.controller;

import com.club.common.Result;
import com.club.dto.ActivityCreateDTO;
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
    public Result<?> create(@RequestBody ActivityCreateDTO dto,
                            @RequestParam(required = false, defaultValue = "false") Boolean forceBudget) {
        Activity activity = new Activity();
        activity.setTitle(dto.getTitle());
        activity.setDescription(dto.getDescription());
        activity.setProcess(dto.getProcess());
        activity.setLocation(dto.getLocation());
        if (dto.getStartTime() != null && !dto.getStartTime().isEmpty()) {
            activity.setStartTime(parseDateTime(dto.getStartTime()));
        }
        if (dto.getEndTime() != null && !dto.getEndTime().isEmpty()) {
            activity.setEndTime(parseDateTime(dto.getEndTime()));
        }
        activity.setMaxCount(dto.getMaxCount());
        activity.setBudget(dto.getBudget());
        activity.setPoster(dto.getPoster());

        return activityService.createActivity(activity, forceBudget, dto.getCoHostClubIds());
    }

    private java.time.LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            return java.time.LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            try {
                return java.time.LocalDateTime.parse(dateTimeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Integer id,
                            @RequestBody Activity activity,
                            @RequestParam(required = false, defaultValue = "false") Boolean forceBudget) {
        return activityService.updateActivity(id, activity, forceBudget);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id) {
        return activityService.deleteActivity(id);
    }

    @PostMapping("/{id}/audit")
    public Result<?> audit(@PathVariable Integer id, @RequestBody Map<String, String> params) {
        return activityService.auditActivity(id, params.get("status"), params.get("reason"));
    }

    @PostMapping("/{id}/register")
    public Result<?> register(@PathVariable Integer id, @RequestParam Integer userId) {
        return activityService.register(id, userId);
    }

    @PostMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable Integer id, @RequestParam Integer userId) {
        return activityService.cancelRegistration(id, userId);
    }

    @PostMapping("/{id}/leave-waitlist")
    public Result<?> leaveWaitlist(@PathVariable Integer id, @RequestParam Integer userId) {
        return activityService.leaveWaitlist(id, userId);
    }

    @GetMapping("/{id}/waitlist")
    public Result<?> getWaitlist(@PathVariable Integer id, @RequestParam(required = false) Integer userId) {
        return activityService.getWaitlistInfo(id, userId);
    }

    @GetMapping("/{id}/detail")
    public Result<?> getDetail(@PathVariable Integer id, @RequestParam(required = false) Integer userId) {
        return activityService.getActivityDetail(id, userId);
    }

    @PostMapping("/{id}/expand")
    public Result<?> expandCapacity(@PathVariable Integer id, @RequestBody Map<String, Object> params) {
        Integer newMaxCount = (Integer) params.get("newMaxCount");
        Integer operatorId = (Integer) params.get("operatorId");
        return activityService.expandCapacity(id, newMaxCount, operatorId);
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

    @PostMapping("/{id}/co-hosts/{coHostId}/confirm")
    public Result<?> confirmCoHost(@PathVariable Integer id,
                                   @PathVariable Integer coHostId,
                                   @RequestBody Map<String, String> params) {
        return activityService.confirmCoHost(id, coHostId, params.get("status"), params.get("reason"));
    }

    @GetMapping("/my-pending-co-hosts")
    public Result<?> getMyPendingCoHosts() {
        return activityService.getMyPendingCoHosts();
    }
}
