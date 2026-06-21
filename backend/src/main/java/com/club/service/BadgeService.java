package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.dto.BadgeCreateDTO;
import com.club.dto.GrantBadgeDTO;
import com.club.dto.RevokeBadgeDTO;
import com.club.entity.Badge;

public interface BadgeService extends IService<Badge> {
    Result<?> createBadge(String username, BadgeCreateDTO dto);
    Result<?> updateBadge(String username, Integer badgeId, BadgeCreateDTO dto);
    Result<?> deleteBadge(String username, Integer badgeId);
    Result<?> getBadgeList(Integer clubId, Boolean isPublic);
    Result<?> getBadgeDetail(Integer badgeId);
    Result<?> grantBadge(String username, GrantBadgeDTO dto);
    Result<?> revokeBadge(String username, Integer userBadgeId, RevokeBadgeDTO dto);
    Result<?> getUserBadges(Integer userId);
    Result<?> getMyBadges(String username);
    Result<?> getClubPublicBadges(Integer clubId);
    Result<?> getBadgeRecipients(Integer badgeId);
}
