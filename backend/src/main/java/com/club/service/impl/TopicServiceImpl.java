package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.entity.Club;
import com.club.entity.Topic;
import com.club.entity.TopicInteraction;
import com.club.entity.User;
import com.club.mapper.ClubMapper;
import com.club.mapper.TopicInteractionMapper;
import com.club.mapper.TopicMapper;
import com.club.mapper.UserMapper;
import com.club.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Autowired
    private TopicInteractionMapper topicInteractionMapper;

    @Override
    public Result<?> publishTopic(Topic topic) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) return Result.error("用户不存在");

        topic.setAuthorId(user.getId());
        topic.setLikesCount(0);
        topic.setFavoritesCount(0);
        topic.setStatus("NORMAL");

        if ("CROSS_CLUB".equals(topic.getType())) {
            topic.setAuditStatus("PENDING");
            topic.setClubId(null);
        } else {
            topic.setType("IN_CLUB");
            topic.setAuditStatus("APPROVED"); // 默认不用审核
            if (topic.getClubId() == null && user.getClubId() != null) {
                topic.setClubId(user.getClubId());
            } else if (topic.getClubId() == null) {
                return Result.error("社内话题必须归属一个社团");
            }
        }

        this.save(topic);
        return Result.success(null);
    }

    @Override
    public Result<?> getTopics(String type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));

        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getAuditStatus, "APPROVED");
        
        if ("CROSS_CLUB".equals(type)) {
            wrapper.eq(Topic::getType, "CROSS_CLUB");
        } else {
            wrapper.eq(Topic::getType, "IN_CLUB");
            if (user != null && user.getClubId() != null) {
                wrapper.eq(Topic::getClubId, user.getClubId());
            } else if (user != null && !RoleConstants.ADMIN.equals(user.getRole()) && !RoleConstants.UNION_ADMIN.equals(user.getRole())) {
                // Regular member with no club sees nothing for IN_CLUB
                return Result.success(new ArrayList<>());
            }
            // Admins/Union Admins see all IN_CLUB topics if they have no specific clubId
        }
        
        wrapper.orderByDesc(Topic::getCreateTime);
        return enrichResult(this.list(wrapper), user);
    }

    @Override
    public Result<?> getPendingTopics() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");
        boolean isUnionAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_UNION_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
        if (!isUnionAdmin) return Result.error("无权限");

        List<Topic> topics = this.list(new LambdaQueryWrapper<Topic>().eq(Topic::getAuditStatus, "PENDING").orderByDesc(Topic::getCreateTime));
        return enrichResult(topics, null);
    }

    @Override
    public Result<?> auditTopic(Integer id, String status) {
        Topic topic = this.getById(id);
        if (topic == null) return Result.error("话题不存在");
        topic.setAuditStatus(status);
        this.updateById(topic);
        return Result.success(null);
    }

    @Override
    @Transactional
    public Result<?> interact(Integer id, String type) { // LIKE or FAVORITE
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));

        Topic topic = this.getById(id);
        if (topic == null) return Result.error("话题不存在");

        long existing = topicInteractionMapper.selectCount(new LambdaQueryWrapper<TopicInteraction>()
                .eq(TopicInteraction::getTopicId, id)
                .eq(TopicInteraction::getUserId, user.getId())
                .eq(TopicInteraction::getType, type));
                
        if (existing > 0) {
            // Already interacted, we can choose to toggle off, but let's toggle off
            topicInteractionMapper.delete(new LambdaQueryWrapper<TopicInteraction>()
                    .eq(TopicInteraction::getTopicId, id)
                    .eq(TopicInteraction::getUserId, user.getId())
                    .eq(TopicInteraction::getType, type));
            if ("LIKE".equals(type)) topic.setLikesCount(Math.max(0, topic.getLikesCount() - 1));
            if ("FAVORITE".equals(type)) topic.setFavoritesCount(Math.max(0, topic.getFavoritesCount() - 1));
        } else {
            TopicInteraction ti = new TopicInteraction();
            ti.setTopicId(id);
            ti.setUserId(user.getId());
            ti.setType(type);
            topicInteractionMapper.insert(ti);
            if ("LIKE".equals(type)) topic.setLikesCount(topic.getLikesCount() + 1);
            if ("FAVORITE".equals(type)) topic.setFavoritesCount(topic.getFavoritesCount() + 1);
        }

        this.updateById(topic);
        return Result.success(null);
    }

    private Result<?> enrichResult(List<Topic> topics, User currentUser) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Topic t : topics) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("title", t.getTitle());
            map.put("content", t.getContent());
            map.put("type", t.getType());
            map.put("auditStatus", t.getAuditStatus());
            map.put("likesCount", t.getLikesCount());
            map.put("favoritesCount", t.getFavoritesCount());
            map.put("createTime", t.getCreateTime());

            User author = userMapper.selectById(t.getAuthorId());
            map.put("authorName", author != null ? author.getRealName() : "未知");
            map.put("authorAvatar", author != null ? author.getAvatar() : null);

            if (t.getClubId() != null) {
                Club c = clubMapper.selectById(t.getClubId());
                map.put("clubName", c != null ? c.getName() : "未知社团");
            } else {
                map.put("clubName", "跨社团公共");
            }

            // check if currentUser liked or favorited
            if (currentUser != null) {
                map.put("hasLiked", topicInteractionMapper.selectCount(new LambdaQueryWrapper<TopicInteraction>()
                        .eq(TopicInteraction::getTopicId, t.getId())
                        .eq(TopicInteraction::getUserId, currentUser.getId())
                        .eq(TopicInteraction::getType, "LIKE")) > 0);
                map.put("hasFavorited", topicInteractionMapper.selectCount(new LambdaQueryWrapper<TopicInteraction>()
                        .eq(TopicInteraction::getTopicId, t.getId())
                        .eq(TopicInteraction::getUserId, currentUser.getId())
                        .eq(TopicInteraction::getType, "FAVORITE")) > 0);
            }
            result.add(map);
        }
        return Result.success(result);
    }
}
