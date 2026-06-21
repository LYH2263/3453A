package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.JwtUtils;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.dto.LoginDTO;
import com.club.dto.RegisterDTO;
import com.club.dto.ResetPasswordDTO;
import com.club.dto.UpdateProfileDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.club.entity.Activity;
import com.club.entity.ActivityRegistration;
import com.club.entity.Topic;
import com.club.entity.User;
import com.club.entity.UserNotification;
import com.club.mapper.ActivityMapper;
import com.club.mapper.RegistrationMapper;
import com.club.mapper.TopicMapper;
import com.club.mapper.UserNotificationMapper;
import com.club.mapper.UserMapper;
import com.club.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private UserNotificationMapper userNotificationMapper;
    @Autowired
    private TopicMapper topicMapper;

    @Override
    public Result<?> login(LoginDTO loginDTO) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        boolean passwordMatch = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())
                || loginDTO.getPassword().equals(user.getPassword());
        if (!passwordMatch) {
            return Result.error("用户名或密码错误");
        }
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("clubId", user.getClubId());
        data.put("avatar", user.getAvatar());
        data.put("token", token);
        return Result.success(data);
    }

    @Override
    public Result<?> register(RegisterDTO dto) {
        // 用户名唯一校验
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }
        // 学号唯一校验（如果提供）
        if (dto.getStudentId() != null && !dto.getStudentId().isBlank()) {
            long sidCount = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, dto.getStudentId()));
            if (sidCount > 0) {
                return Result.error("该学号已被注册");
            }
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setStudentId(dto.getStudentId());
        // 注册时只允许 MEMBER 或 GUEST，防止越权
        String role = RoleConstants.MEMBER.equals(dto.getRole()) ? RoleConstants.MEMBER : RoleConstants.GUEST;
        user.setRole(role);
        this.save(user);
        return Result.success(null);
    }

    @Override
    public Result<?> resetPassword(ResetPasswordDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .eq(User::getStudentId, dto.getStudentId()));
        if (user == null) {
            return Result.error("用户名或学号不匹配");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
        return Result.success(null);
    }

    @Override
    public Result<?> getProfile(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("studentId", user.getStudentId());
        profile.put("role", user.getRole());
        profile.put("clubId", user.getClubId());
        profile.put("avatar", user.getAvatar());
        profile.put("createTime", user.getCreateTime());
        return Result.success(profile);
    }

    @Override
    public Result<?> updateProfile(String username, UpdateProfileDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getAvatar() != null && !dto.getAvatar().isBlank()) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getStudentId() != null && !dto.getStudentId().isBlank()) {
            // 学号唯一校验（排除自身）
            long sidCount = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, dto.getStudentId())
                    .ne(User::getId, user.getId()));
            if (sidCount > 0) {
                return Result.error("该学号已被其他用户使用");
            }
            user.setStudentId(dto.getStudentId());
        }
        this.updateById(user);
        return Result.success(null);
    }

    @Override
    public Result<?> getMyActivities(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 查询该用户报名的活动
        List<ActivityRegistration> regs = registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, user.getId())
                        .orderByDesc(ActivityRegistration::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityRegistration reg : regs) {
            Activity activity = activityMapper.selectById(reg.getActivityId());
            if (activity == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("registrationId", reg.getId());
            item.put("activityId", activity.getId());
            item.put("title", activity.getTitle());
            item.put("location", activity.getLocation());
            item.put("startTime", activity.getStartTime());
            item.put("endTime", activity.getEndTime());
            item.put("status", activity.getStatus());
            item.put("regStatus", reg.getStatus());
            item.put("rating", reg.getRating());
            item.put("feedback", reg.getFeedback());
            item.put("reply", reg.getReply());
            item.put("joinTime", reg.getCreateTime());
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getMyInteractions(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 查询有评分/反馈的互动记录
        List<ActivityRegistration> regs = registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, user.getId())
                        .isNotNull(ActivityRegistration::getRating)
                        .orderByDesc(ActivityRegistration::getUpdateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ActivityRegistration reg : regs) {
            Activity activity = activityMapper.selectById(reg.getActivityId());
            if (activity == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("registrationId", reg.getId());
            item.put("activityId", activity.getId());
            item.put("activityTitle", activity.getTitle());
            item.put("rating", reg.getRating());
            item.put("feedback", reg.getFeedback());
            item.put("reply", reg.getReply());
            item.put("updateTime", reg.getUpdateTime());
            result.add(item);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> getNotifications(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 通知：有负责人回复的记录
        List<ActivityRegistration> regs = registrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getUserId, user.getId())
                        .isNotNull(ActivityRegistration::getReply)
                        .orderByDesc(ActivityRegistration::getUpdateTime));

        List<Map<String, Object>> notifications = new ArrayList<>();
        for (ActivityRegistration reg : regs) {
            Activity activity = activityMapper.selectById(reg.getActivityId());
            if (activity == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("type", "REPLY");
            item.put("activityId", activity.getId());
            item.put("activityTitle", activity.getTitle());
            item.put("content", reg.getReply());
            item.put("time", reg.getUpdateTime());
            notifications.add(item);
        }
        return Result.success(notifications);
    }

    @Override
    public Result<?> getNotificationList(String username, Integer pageNum, Integer pageSize) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }

        IPage<UserNotification> page = userNotificationMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, user.getId())
                        .orderByDesc(UserNotification::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserNotification n : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("type", n.getType());
            item.put("topicId", n.getTopicId());
            item.put("commentId", n.getCommentId());
            item.put("content", n.getContent());
            item.put("isRead", n.getIsRead());
            item.put("createTime", n.getCreateTime());
            item.put("triggerUserId", n.getTriggerUserId());

            if (n.getTriggerUserId() != null) {
                User triggerUser = this.getById(n.getTriggerUserId());
                if (triggerUser != null) {
                    item.put("triggerUserName", triggerUser.getRealName());
                    item.put("triggerUserAvatar", triggerUser.getAvatar());
                }
            }

            if (n.getTopicId() != null) {
                Topic topic = topicMapper.selectById(n.getTopicId());
                if (topic != null) {
                    item.put("topicTitle", topic.getTitle());
                }
            }

            result.add(item);
        }

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("list", result);
        pageData.put("total", page.getTotal());
        pageData.put("pageNum", page.getCurrent());
        pageData.put("pageSize", page.getSize());
        return Result.success(pageData);
    }

    @Override
    public Result<?> getUnreadNotificationCount(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }

        long count = userNotificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, user.getId())
                        .eq(UserNotification::getIsRead, 0));

        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return Result.success(result);
    }

    @Override
    public Result<?> markNotificationRead(String username, Integer notificationId) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }

        UserNotification notification = userNotificationMapper.selectById(notificationId);
        if (notification == null) {
            return Result.error("通知不存在");
        }
        if (!notification.getUserId().equals(user.getId())) {
            return Result.error("无权限操作");
        }

        notification.setIsRead(1);
        notification.setReadTime(LocalDateTime.now());
        userNotificationMapper.updateById(notification);
        return Result.success(null);
    }

    @Override
    public Result<?> markAllNotificationsRead(String username) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            return Result.error("用户不存在");
        }

        List<UserNotification> unreadList = userNotificationMapper.selectList(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, user.getId())
                        .eq(UserNotification::getIsRead, 0));

        LocalDateTime now = LocalDateTime.now();
        for (UserNotification n : unreadList) {
            n.setIsRead(1);
            n.setReadTime(now);
            userNotificationMapper.updateById(n);
        }

        return Result.success(null);
    }

    @Override
    public Result<?> updateRole(Integer userId, String role) {
        User user = this.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setRole(role);
        this.updateById(user);
        return Result.success(null);
    }

    @Override
    public List<User> listUsers() {
        return this.list();
    }
}
