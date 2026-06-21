package com.club.controller;

import com.club.common.Result;
import com.club.entity.Recruitment;
import com.club.service.RecruitmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/recruitments")
public class RecruitmentController {

    @Autowired
    private RecruitmentService recruitmentService;

    @GetMapping
    public Result<?> getRecruitments() {
        return recruitmentService.getRecruitments();
    }

    @PostMapping
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public Result<?> publishRecruitment(@RequestBody Recruitment recruitment) {
        return recruitmentService.publishRecruitment(recruitment);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public Result<?> closeRecruitment(@PathVariable Integer id) {
        return recruitmentService.closeRecruitment(id);
    }

    @PostMapping("/{id}/apply")
    public Result<?> apply(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return recruitmentService.apply(id, body.get("resumeText"));
    }

    @GetMapping("/{id}/applications")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public Result<?> getApplications(@PathVariable Integer id) {
        return recruitmentService.getApplications(id);
    }

    @PostMapping("/applications/{id}/audit")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public Result<?> auditApplication(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return recruitmentService.auditApplication(id, body.get("status"));
    }
}
