package com.club.controller;

import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.entity.AuditConfig;
import com.club.service.AuditConfigService;
import com.club.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统基础配置控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Tag(name = "后台管理-基础配置", description = "审核流程、角色权限配置")
@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    @Autowired
    private AuditConfigService auditConfigService;

    @Autowired
    private UserService userService;

    /**
     * 获取所有审核流程配置
     */
    @Operation(summary = "查询审核配置")
    @Log("查询审核配置")
    @GetMapping("/audit")
    public Result<?> listAuditConfigs() {
        return Result.success(auditConfigService.list());
    }

    /**
     * 保存或更新审核配置
     */
    @Operation(summary = "更新审核配置")
    @Log("更新审核配置")
    @PostMapping("/audit")
    public Result<?> saveAuditConfig(@RequestBody AuditConfig config) {
        return Result.success(auditConfigService.saveOrUpdate(config));
    }

    /**
     * 获取系统定义的角色列表
     */
    @Operation(summary = "查询角色列表")
    @Log("查询角色列表")
    @GetMapping("/roles")
    public Result<?> listRoles() {
        // Enums or static list from RoleConstants
        return Result.success(java.util.Arrays.asList("ADMIN", "UNION_ADMIN", "CLUB_LEADER", "MEMBER", "GUEST"));
    }

    /**
     * 调整用户角色
     *
     * @param userId 用户ID
     * @param role   目标角色
     */
    @Operation(summary = "更新用户角色")
    @Log("更新用户角色")
    @PutMapping("/user-role")
    public Result<?> updateUserRole(@RequestParam Integer userId, @RequestParam String role) {
        // Implementation might need User entity update
        return userService.updateRole(userId, role);
    }
}
