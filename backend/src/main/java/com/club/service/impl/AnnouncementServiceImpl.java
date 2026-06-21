package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.entity.Announcement;
import com.club.entity.Club;
import com.club.entity.User;
import com.club.mapper.AnnouncementMapper;
import com.club.mapper.ClubMapper;
import com.club.mapper.UserMapper;
import com.club.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Override
    public Result<?> publishAnnouncement(Announcement announcement) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Result.error("未认证");
        }
        
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) return Result.error("用户不存在");

        announcement.setPublisherId(user.getId());

        if (RoleConstants.CLUB_LEADER.equals(user.getRole())) {
            if (user.getClubId() == null) {
                return Result.error("未绑定社团，无法发布局域公告");
            }
            announcement.setClubId(user.getClubId());
        } else if (RoleConstants.ADMIN.equals(user.getRole()) || RoleConstants.UNION_ADMIN.equals(user.getRole())) {
            announcement.setClubId(null); // 全局公告
        } else {
            return Result.error("无权限发布公告");
        }

        this.save(announcement);
        return Result.success(null);
    }

    @Override
    public Result<?> getAnnouncements() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Result.error("未认证");

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) return Result.error("用户不存在");

        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getCreateTime);

        if (RoleConstants.MEMBER.equals(user.getRole()) || RoleConstants.CLUB_LEADER.equals(user.getRole())) {
            if (user.getClubId() != null) {
                wrapper.and(w -> w.isNull(Announcement::getClubId).or().eq(Announcement::getClubId, user.getClubId()));
            } else {
                wrapper.isNull(Announcement::getClubId);
            }
        }

        List<Announcement> announcements = this.list(wrapper);
        
        // Enrich data
        List<Map<String, Object>> result = new ArrayList<>();
        for (Announcement a : announcements) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("title", a.getTitle());
            map.put("content", a.getContent());
            map.put("createTime", a.getCreateTime());
            map.put("publisherId", a.getPublisherId());
            map.put("clubId", a.getClubId());
            
            User pub = userMapper.selectById(a.getPublisherId());
            map.put("publisherName", pub != null ? pub.getRealName() : "未知");
            
            if (a.getClubId() != null) {
                Club c = clubMapper.selectById(a.getClubId());
                map.put("clubName", c != null ? c.getName() : "未知");
            } else {
                map.put("clubName", "全局公告");
            }
            result.add(map);
        }

        return Result.success(result);
    }

    @Override
    public Result<?> deleteAnnouncement(Integer id) {
        this.removeById(id);
        return Result.success(null);
    }
}
