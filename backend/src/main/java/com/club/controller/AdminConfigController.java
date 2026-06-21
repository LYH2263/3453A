package com.club.controller;

import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.entity.AuditConfig;
import com.club.entity.Club;
import com.club.mapper.ClubMapper;
import com.club.service.AuditConfigService;
import com.club.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Tag(name = "后台管理-基础配置", description = "审核流程、角色权限、预算阈值配置")
@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    @Autowired
    private AuditConfigService auditConfigService;

    @Autowired
    private UserService userService;

    @Autowired
    private ClubMapper clubMapper;

    @Operation(summary = "查询审核配置")
    @Log("查询审核配置")
    @GetMapping("/audit")
    public Result<?> listAuditConfigs() {
        return Result.success(auditConfigService.list());
    }

    @Operation(summary = "更新审核配置")
    @Log("更新审核配置")
    @PostMapping("/audit")
    public Result<?> saveAuditConfig(@RequestBody AuditConfig config) {
        return Result.success(auditConfigService.saveOrUpdate(config));
    }

    @Operation(summary = "查询角色列表")
    @Log("查询角色列表")
    @GetMapping("/roles")
    public Result<?> listRoles() {
        return Result.success(java.util.Arrays.asList("ADMIN", "UNION_ADMIN", "CLUB_LEADER", "MEMBER", "GUEST"));
    }

    @Operation(summary = "更新用户角色")
    @Log("更新用户角色")
    @PutMapping("/user-role")
    public Result<?> updateUserRole(@RequestParam Integer userId, @RequestParam String role) {
        return userService.updateRole(userId, role);
    }

    @Operation(summary = "查询各社团预算阈值配置")
    @Log("查询各社团预算阈值配置")
    @GetMapping("/budget")
    public Result<?> listBudgetConfigs() {
        List<Club> clubs = clubMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Club club : clubs) {
            Map<String, Object> item = new HashMap<>();
            item.put("clubId", club.getId());
            item.put("clubName", club.getName());
            item.put("monthlyBudgetLimit", club.getMonthlyBudgetLimit());
            item.put("budgetEnforceMode", club.getBudgetEnforceMode());
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "更新社团预算阈值配置")
    @Log("更新社团预算阈值配置")
    @PutMapping("/budget/{clubId}")
    public Result<?> updateBudgetConfig(@PathVariable Integer clubId,
                                        @RequestBody Map<String, Object> params) {
        Club club = clubMapper.selectById(clubId);
        if (club == null) return Result.error("社团不存在");

        if (params.containsKey("monthlyBudgetLimit")) {
            Object limitObj = params.get("monthlyBudgetLimit");
            if (limitObj != null) {
                club.setMonthlyBudgetLimit(new BigDecimal(limitObj.toString()));
            } else {
                club.setMonthlyBudgetLimit(null);
            }
        }
        if (params.containsKey("budgetEnforceMode")) {
            club.setBudgetEnforceMode((String) params.get("budgetEnforceMode"));
        }
        clubMapper.updateById(club);
        return Result.success(null);
    }
}
