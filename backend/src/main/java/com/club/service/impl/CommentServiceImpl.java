package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.common.RoleConstants;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Autowired
    private CommentMentionMapper commentMentionMapper;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([^@\\s:：,，。！!？?；;]+)");

    @Override
    public Result<?> getCommentsByTopicId(Integer topicId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");
        User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));

        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) return Result.error("话题不存在");

        if (!canViewTopic(currentUser, topic)) {
            return Result.error("无权限查看该话题评论");
        }

        List<Comment> comments = this.list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTopicId, topicId)
                .orderByAsc(Comment::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Comment c : comments) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("topicId", c.getTopicId());
            map.put("content", c.getContent());
            map.put("replyId", c.getReplyId());
            map.put("createTime", c.getCreateTime());

            User author = userMapper.selectById(c.getAuthorId());
            if (author != null) {
                map.put("authorId", author.getId());
                map.put("authorName", author.getRealName());
                map.put("authorUsername", author.getUsername());
                map.put("authorAvatar", author.getAvatar());
            } else {
                map.put("authorId", null);
                map.put("authorName", "未知用户");
                map.put("authorUsername", "");
                map.put("authorAvatar", null);
            }

            result.add(map);
        }
        return Result.success(result);
    }

    @Override
    @Transactional
    public Result<?> publishComment(Comment comment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");
        User currentUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (currentUser == null) return Result.error("用户不存在");

        if (comment.getTopicId() == null) return Result.error("话题ID不能为空");
        Topic topic = topicMapper.selectById(comment.getTopicId());
        if (topic == null) return Result.error("话题不存在");

        if (!canViewTopic(currentUser, topic)) {
            return Result.error("无权限在该话题下评论");
        }

        comment.setAuthorId(currentUser.getId());
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            return Result.error("评论内容不能为空");
        }
        this.save(comment);

        processMentions(comment, topic, currentUser);

        return Result.success(comment.getId());
    }

    private void processMentions(Comment comment, Topic topic, User currentUser) {
        String content = comment.getContent();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        Set<String> mentionTexts = new LinkedHashSet<>();
        while (matcher.find()) {
            mentionTexts.add(matcher.group(1).trim());
        }

        if (mentionTexts.isEmpty()) return;

        Set<Integer> mentionedUserIds = new HashSet<>();
        List<CommentMention> mentions = new ArrayList<>();
        List<UserNotification> notifications = new ArrayList<>();

        for (String text : mentionTexts) {
            User mentionedUser = findUserByMention(text, topic);
            if (mentionedUser == null) continue;
            if (mentionedUser.getId().equals(currentUser.getId())) continue;
            if (mentionedUserIds.contains(mentionedUser.getId())) continue;
            if (!canViewTopic(mentionedUser, topic)) continue;

            mentionedUserIds.add(mentionedUser.getId());

            CommentMention mention = new CommentMention();
            mention.setCommentId(comment.getId());
            mention.setTopicId(topic.getId());
            mention.setMentionedUserId(mentionedUser.getId());
            mention.setMentionType(determineMentionType(text, mentionedUser));
            mention.setMentionText(text);
            mentions.add(mention);

            UserNotification notification = new UserNotification();
            notification.setUserId(mentionedUser.getId());
            notification.setType("MENTION");
            notification.setTopicId(topic.getId());
            notification.setCommentId(comment.getId());
            notification.setContent(buildMentionNotificationContent(currentUser, topic, comment));
            notification.setTriggerUserId(currentUser.getId());
            notification.setIsRead(0);
            notifications.add(notification);
        }

        if (!mentions.isEmpty()) {
            for (CommentMention m : mentions) {
                commentMentionMapper.insert(m);
            }
        }
        if (!notifications.isEmpty()) {
            for (UserNotification n : notifications) {
                userNotificationMapper.insert(n);
            }
        }
    }

    private User findUserByMention(String text, Topic topic) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, text)
                .last("LIMIT 1"));
        if (user != null) return user;

        List<User> candidates = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRealName, text));

        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        if (topic != null && topic.getClubId() != null) {
            for (User u : candidates) {
                if (topic.getClubId().equals(u.getClubId())) {
                    return u;
                }
            }
        }

        return null;
    }

    private String determineMentionType(String text, User user) {
        if (text.equals(user.getUsername())) return "USERNAME";
        if (text.equals(user.getRealName())) return "REALNAME";
        return "USERNAME";
    }

    private String buildMentionNotificationContent(User triggerUser, Topic topic, Comment comment) {
        String preview = comment.getContent();
        if (preview.length() > 100) {
            preview = preview.substring(0, 100) + "...";
        }
        return triggerUser.getRealName() + " 在话题《" + topic.getTitle() + "》中@了你：" + preview;
    }

    private boolean canViewTopic(User user, Topic topic) {
        if (user == null || topic == null) return false;

        if (RoleConstants.ADMIN.equals(user.getRole()) || RoleConstants.UNION_ADMIN.equals(user.getRole())) {
            return true;
        }

        if ("CROSS_CLUB".equals(topic.getType())) {
            return true;
        }

        if ("IN_CLUB".equals(topic.getType())) {
            if (topic.getClubId() == null) return false;
            if (user.getClubId() == null) return false;
            return topic.getClubId().equals(user.getClubId());
        }

        return false;
    }
}
