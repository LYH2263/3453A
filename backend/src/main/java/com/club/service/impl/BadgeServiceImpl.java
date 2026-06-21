package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.dto.BadgeCreateDTO;
import com.club.dto.GrantBadgeDTO;
import com.club.dto.RevokeBadgeDTO;
import com.club.entity.Badge;
import com.club.entity.Club;
import com.club.entity.User;
import com.club.entity.UserBadge;
import com.club.mapper.BadgeMapper;
import com.club.mapper.ClubMapper;
import com.club.mapper.UserBadgeMapper;
import com.club.mapper.UserMapper;
import com.club.service.BadgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BadgeServiceImpl extends ServiceImpl<BadgeMapper, Badge> implements BadgeService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Autowired
    private UserBadgeMapper userBadgeMapper;

    @Override
    public Result<?> createBadge(String username, BadgeCreateDTO dto) {
        User operator = getCurrentUser(username);
        if (operator == null) {
            return Result.error("用户不存在");
        }

        if (!canManageBadge(operator, dto.getClubId())) {
            return Result.error("无权限创建该社团的徽章");
        }

        Badge badge = new Badge();
        badge.setClubId(dto.getClubId());
        badge.setName(dto.getName());
        badge.setIconUrl(dto.getIconUrl());
        badge.setDescription(dto.getDescription());
        badge.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : 1);
        this.save(badge);

        return Result.success(buildBadgeDetail(badge));
    }

    @Override
    public Result<?> updateBadge(String username, Integer badgeId, BadgeCreateDTO dto) {
        User operator = getCurrentUser(username);
        if (operator == null) {
            return Result.error("用户不存在");
        }

        Badge badge = this.getById(badgeId);
        if (badge == null) {
            return Result.error("徽章不存在");
        }

        if (!canManageBadge(operator, badge.getClubId())) {
            return Result.error("无权限修改该徽章");
        }

        if (dto.getName() != null) {
            badge.setName(dto.getName());
        }
        if (dto.getIconUrl() != null) {
            badge.setIconUrl(dto.getIconUrl());
        }
        if (dto.getDescription() != null) {
            badge.setDescription(dto.getDescription());
        }
        if (dto.getIsPublic() != null) {
            badge.setIsPublic(dto.getIsPublic());
        }
        this.updateById(badge);

        return Result.success(buildBadgeDetail(badge));
    }

    @Override
    public Result<?> deleteBadge(String username, Integer badgeId) {
        User operator = getCurrentUser(username);
        if (operator == null) {
            return Result.error("用户不存在");
        }

        Badge badge = this.getById(badgeId);
        if (badge == null) {
            return Result.error("徽章不存在");
        }

        if (!canManageBadge(operator, badge.getClubId())) {
            return Result.error("无权限删除该徽章");
        }

        this.removeById(badgeId);
        return Result.success(null);
    }

    @Override
    public Result<?> getPublicBadgeList(Integer clubId) {
        LambdaQueryWrapper<Badge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Badge::getIsPublic, 1);
        if (clubId != null) {
            wrapper.eq(Badge::getClubId, clubId);
        }
        wrapper.orderByDesc(Badge::getCreateTime);

        List<Badge> badges = this.list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Badge badge : badges) {
            result.add(buildBadgeDetail(badge));
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getManagedBadges(String username) {
        User operator = getCurrentUser(username);
        if (operator == null) {
            return Result.error("用户不存在");
        }

        LambdaQueryWrapper<Badge> wrapper = new LambdaQueryWrapper<>();
        if (RoleConstants.ADMIN.equals(operator.getRole()) || RoleConstants.UNION_ADMIN.equals(operator.getRole())) {
            // admin/union see all
        } else if (RoleConstants.CLUB_LEADER.equals(operator.getRole())) {
            wrapper.eq(Badge::getClubId, operator.getClubId());
        } else {
            return Result.error("无权限管理徽章");
        }
        wrapper.orderByDesc(Badge::getCreateTime);

        List<Badge> badges = this.list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Badge badge : badges) {
            Map<String, Object> item = buildBadgeDetail(badge);

            List<UserBadge> activeGrants = userBadgeMapper.selectList(
                    new LambdaQueryWrapper<UserBadge>()
                            .eq(UserBadge::getBadgeId, badge.getId())
                            .eq(UserBadge::getRevoked, 0)
                            .orderByDesc(UserBadge::getGrantedTime));

            List<Map<String, Object>> grantList = new ArrayList<>();
            for (UserBadge ub : activeGrants) {
                User u = userMapper.selectById(ub.getUserId());
                if (u == null) continue;
                Map<String, Object> g = new HashMap<>();
                g.put("userBadgeId", ub.getId());
                g.put("userId", u.getId());
                g.put("userName", u.getRealName());
                g.put("userAvatar", u.getAvatar());
                g.put("grantedTime", ub.getGrantedTime());
                User grantor = userMapper.selectById(ub.getGrantedBy());
                g.put("grantedByName", grantor != null ? grantor.getRealName() : "");
                grantList.add(g);
            }
            item.put("grants", grantList);
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getBadgeDetail(String username, Integer badgeId) {
        Badge badge = this.getById(badgeId);
        if (badge == null) {
            return Result.error("徽章不存在");
        }

        if (badge.getIsPublic() == 0) {
            if (username == null) {
                return Result.error("无权限查看该徽章");
            }
            User user = getCurrentUser(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            if (!canManageBadge(user, badge.getClubId()) && !Objects.equals(user.getClubId(), badge.getClubId())) {
                return Result.error("该徽章不公开");
            }
        }

        return Result.success(buildBadgeDetail(badge));
    }

    @Override
    public Result<?> grantBadge(String username, GrantBadgeDTO dto) {
        User operator = getCurrentUser(username);
        if (operator == null) {
            return Result.error("用户不存在");
        }

        Badge badge = this.getById(dto.getBadgeId());
        if (badge == null) {
            return Result.error("徽章不存在");
        }

        if (!canManageBadge(operator, badge.getClubId())) {
            return Result.error("无权限授予该徽章");
        }

        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!Objects.equals(user.getClubId(), badge.getClubId())) {
            return Result.error("该用户不属于该社团，无法授予徽章");
        }

        long activeCount = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, dto.getUserId())
                .eq(UserBadge::getBadgeId, dto.getBadgeId())
                .eq(UserBadge::getRevoked, 0));
        if (activeCount > 0) {
            return Result.error("该用户已拥有此徽章");
        }

        UserBadge userBadge = new UserBadge();
        userBadge.setUserId(dto.getUserId());
        userBadge.setBadgeId(dto.getBadgeId());
        userBadge.setGrantedBy(operator.getId());
        userBadge.setGrantedTime(LocalDateTime.now());
        userBadge.setRevoked(0);
        userBadgeMapper.insert(userBadge);

        return Result.success(null);
    }

    @Override
    public Result<?> revokeBadge(String username, Integer userBadgeId, RevokeBadgeDTO dto) {
        User operator = getCurrentUser(username);
        if (operator == null) {
            return Result.error("用户不存在");
        }

        UserBadge userBadge = userBadgeMapper.selectById(userBadgeId);
        if (userBadge == null) {
            return Result.error("授予记录不存在");
        }

        if (userBadge.getRevoked() == 1) {
            return Result.error("该徽章已被撤销");
        }

        Badge badge = this.getById(userBadge.getBadgeId());
        if (badge == null) {
            return Result.error("徽章不存在");
        }

        if (!canManageBadge(operator, badge.getClubId())) {
            return Result.error("无权限撤销该徽章");
        }

        userBadge.setRevoked(1);
        userBadge.setRevokedBy(operator.getId());
        userBadge.setRevokedTime(LocalDateTime.now());
        if (dto != null && dto.getRevokeReason() != null) {
            userBadge.setRevokeReason(dto.getRevokeReason());
        }
        userBadgeMapper.updateById(userBadge);

        return Result.success(null);
    }

    @Override
    public Result<?> getUserBadges(Integer userId) {
        List<UserBadge> userBadges = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, userId)
                        .eq(UserBadge::getRevoked, 0)
                        .orderByDesc(UserBadge::getGrantedTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBadge ub : userBadges) {
            Badge badge = this.getById(ub.getBadgeId());
            if (badge == null) continue;

            if (badge.getIsPublic() == 0) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("userBadgeId", ub.getId());
            item.put("badgeId", badge.getId());
            item.put("name", badge.getName());
            item.put("iconUrl", badge.getIconUrl());
            item.put("description", badge.getDescription());
            item.put("clubId", badge.getClubId());
            item.put("grantedTime", ub.getGrantedTime());

            Club club = clubMapper.selectById(badge.getClubId());
            if (club != null) {
                item.put("clubName", club.getName());
            }

            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getMyBadges(String username) {
        User user = getCurrentUser(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        List<UserBadge> userBadges = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getUserId, user.getId())
                        .eq(UserBadge::getRevoked, 0)
                        .orderByDesc(UserBadge::getGrantedTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBadge ub : userBadges) {
            Badge badge = this.getById(ub.getBadgeId());
            if (badge == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("userBadgeId", ub.getId());
            item.put("badgeId", badge.getId());
            item.put("name", badge.getName());
            item.put("iconUrl", badge.getIconUrl());
            item.put("description", badge.getDescription());
            item.put("clubId", badge.getClubId());
            item.put("isPublic", badge.getIsPublic());
            item.put("grantedTime", ub.getGrantedTime());

            User grantor = userMapper.selectById(ub.getGrantedBy());
            if (grantor != null) {
                item.put("grantedByName", grantor.getRealName());
            }

            Club club = clubMapper.selectById(badge.getClubId());
            if (club != null) {
                item.put("clubName", club.getName());
            }

            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getClubPublicBadges(Integer clubId) {
        List<Badge> badges = this.list(new LambdaQueryWrapper<Badge>()
                .eq(Badge::getClubId, clubId)
                .eq(Badge::getIsPublic, 1)
                .orderByDesc(Badge::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Badge badge : badges) {
            Map<String, Object> item = buildBadgeDetail(badge);

            long recipientCount = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                    .eq(UserBadge::getBadgeId, badge.getId())
                    .eq(UserBadge::getRevoked, 0));
            item.put("recipientCount", recipientCount);

            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getBadgeRecipients(String username, Integer badgeId) {
        Badge badge = this.getById(badgeId);
        if (badge == null) {
            return Result.error("徽章不存在");
        }

        if (badge.getIsPublic() == 0) {
            if (username == null) {
                return Result.error("无权限查看获得者");
            }
            User user = getCurrentUser(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            if (!canManageBadge(user, badge.getClubId()) && !Objects.equals(user.getClubId(), badge.getClubId())) {
                return Result.error("该徽章不公开");
            }
        }

        List<UserBadge> userBadges = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>()
                        .eq(UserBadge::getBadgeId, badgeId)
                        .eq(UserBadge::getRevoked, 0)
                        .orderByDesc(UserBadge::getGrantedTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBadge ub : userBadges) {
            User user = userMapper.selectById(ub.getUserId());
            if (user == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("userBadgeId", ub.getId());
            item.put("userId", user.getId());
            item.put("userName", user.getRealName());
            item.put("userAvatar", user.getAvatar());
            item.put("grantedTime", ub.getGrantedTime());
            result.add(item);
        }
        return Result.success(result);
    }

    private User getCurrentUser(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    private boolean canManageBadge(User user, Integer clubId) {
        if (RoleConstants.ADMIN.equals(user.getRole()) || RoleConstants.UNION_ADMIN.equals(user.getRole())) {
            return true;
        }
        if (RoleConstants.CLUB_LEADER.equals(user.getRole())) {
            return Objects.equals(user.getClubId(), clubId);
        }
        return false;
    }

    private Map<String, Object> buildBadgeDetail(Badge badge) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", badge.getId());
        item.put("clubId", badge.getClubId());
        item.put("name", badge.getName());
        item.put("iconUrl", badge.getIconUrl());
        item.put("description", badge.getDescription());
        item.put("isPublic", badge.getIsPublic());
        item.put("createTime", badge.getCreateTime());

        Club club = clubMapper.selectById(badge.getClubId());
        if (club != null) {
            item.put("clubName", club.getName());
        }

        long recipientCount = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getBadgeId, badge.getId())
                .eq(UserBadge::getRevoked, 0));
        item.put("recipientCount", recipientCount);

        return item;
    }
}
