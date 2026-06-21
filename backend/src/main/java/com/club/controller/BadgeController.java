package com.club.controller;

import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.common.annotation.Log;
import com.club.dto.BadgeCreateDTO;
import com.club.dto.GrantBadgeDTO;
import com.club.dto.RevokeBadgeDTO;
import com.club.service.BadgeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    @Autowired
    private BadgeService badgeService;

    @Log("获取徽章列表")
    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) Integer clubId,
            @RequestParam(required = false) Boolean isPublic) {
        return badgeService.getBadgeList(clubId, isPublic);
    }

    @Log("获取徽章详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Integer id) {
        return badgeService.getBadgeDetail(id);
    }

    @Log("获取社团公开徽章")
    @GetMapping("/club/{clubId}/public")
    public Result<?> getClubPublicBadges(@PathVariable Integer clubId) {
        return badgeService.getClubPublicBadges(clubId);
    }

    @Log("获取用户徽章")
    @GetMapping("/user/{userId}")
    public Result<?> getUserBadges(@PathVariable Integer userId) {
        return badgeService.getUserBadges(userId);
    }

    @Log("获取我的徽章")
    @GetMapping("/mine")
    public Result<?> getMyBadges(Authentication auth) {
        return badgeService.getMyBadges(auth.getName());
    }

    @Log("获取徽章获得者列表")
    @GetMapping("/{id}/recipients")
    public Result<?> getBadgeRecipients(@PathVariable Integer id) {
        return badgeService.getBadgeRecipients(id);
    }

    @Log("创建徽章")
    @PostMapping
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "', '" + RoleConstants.CLUB_LEADER + "')")
    public Result<?> create(@Valid @RequestBody BadgeCreateDTO dto, Authentication auth) {
        return badgeService.createBadge(auth.getName(), dto);
    }

    @Log("更新徽章")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "', '" + RoleConstants.CLUB_LEADER + "')")
    public Result<?> update(@PathVariable Integer id, @RequestBody BadgeCreateDTO dto, Authentication auth) {
        return badgeService.updateBadge(auth.getName(), id, dto);
    }

    @Log("删除徽章")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "', '" + RoleConstants.CLUB_LEADER + "')")
    public Result<?> delete(@PathVariable Integer id, Authentication auth) {
        return badgeService.deleteBadge(auth.getName(), id);
    }

    @Log("授予徽章")
    @PostMapping("/grant")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "', '" + RoleConstants.CLUB_LEADER + "')")
    public Result<?> grant(@Valid @RequestBody GrantBadgeDTO dto, Authentication auth) {
        return badgeService.grantBadge(auth.getName(), dto);
    }

    @Log("撤销徽章")
    @PostMapping("/user-badges/{userBadgeId}/revoke")
    @PreAuthorize("hasAnyRole('" + RoleConstants.ADMIN + "', '" + RoleConstants.UNION_ADMIN + "', '" + RoleConstants.CLUB_LEADER + "')")
    public Result<?> revoke(@PathVariable Integer userBadgeId,
                            @RequestBody(required = false) RevokeBadgeDTO dto,
                            Authentication auth) {
        return badgeService.revokeBadge(auth.getName(), userBadgeId, dto);
    }
}
