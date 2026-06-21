package com.club.controller;

import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.dto.UpdateProfileDTO;
import com.club.service.UserService;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /** 获取当前用户个人资料 */
    @GetMapping("/profile")
    public Result<?> getProfile(Authentication auth) {
        return userService.getProfile(auth.getName());
    }

    /** 更新当前用户个人资料 */
    @PutMapping("/profile")
    public Result<?> updateProfile(@Valid @RequestBody UpdateProfileDTO dto, Authentication auth) {
        return userService.updateProfile(auth.getName(), dto);
    }

    /** 我的活动（报名记录） */
    @GetMapping("/my-activities")
    public Result<?> getMyActivities(Authentication auth) {
        return userService.getMyActivities(auth.getName());
    }

    /** 我的互动记录（评分/反馈） */
    @GetMapping("/my-interactions")
    public Result<?> getMyInteractions(Authentication auth) {
        return userService.getMyInteractions(auth.getName());
    }

    /** 消息通知（负责人回复） */
    @GetMapping("/notifications")
    public Result<?> getNotifications(Authentication auth) {
        return userService.getNotifications(auth.getName());
    }

    // ---- 管理员接口 ----


    /** 超级管理员：获取所有用户列表 */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "')")
    public Result<?> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(userService.page(new Page<>(pageNum, pageSize)));
    }

    /** 超级管理员：修改用户角色 */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('" + RoleConstants.ADMIN + "')")
    public Result<?> updateRole(@PathVariable Integer id, @RequestParam String role) {
        com.club.entity.User user = userService.getById(id);
        if (user == null) return Result.error("用户不存在");
        user.setRole(role);
        userService.updateById(user);
        return Result.success(null);
    }

    /** 超级管理员/社联：禁用用户 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "')")
    public Result<?> deleteUser(@PathVariable Integer id) {
        userService.removeById(id);
        return Result.success(null);
    }
}
