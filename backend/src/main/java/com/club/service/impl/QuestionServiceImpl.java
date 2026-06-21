package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.entity.Club;
import com.club.entity.Question;
import com.club.entity.User;
import com.club.mapper.ClubMapper;
import com.club.mapper.QuestionMapper;
import com.club.mapper.UserMapper;
import com.club.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubMapper clubMapper;

    @Override
    public Result<?> publishQuestion(Question question) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Result.error("未认证");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) return Result.error("用户不存在");

        question.setAuthorId(user.getId());
        this.save(question);
        return Result.success(null);
    }

    @Override
    public Result<?> getQuestions(String keyword, Integer targetClubId) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(Question::getTitle, keyword).or().like(Question::getContent, keyword));
        }
        if (targetClubId != null) {
            wrapper.eq(Question::getTargetClubId, targetClubId);
        }
        
        wrapper.orderByDesc(Question::getCreateTime);
        
        List<Question> list = this.list(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Question q : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", q.getId());
            map.put("title", q.getTitle());
            map.put("content", q.getContent());
            map.put("createTime", q.getCreateTime());
            map.put("targetRole", q.getTargetRole());
            map.put("targetClubId", q.getTargetClubId());

            User author = userMapper.selectById(q.getAuthorId());
            map.put("authorName", author != null ? author.getRealName() : "未知");
            map.put("authorAvatar", author != null ? author.getAvatar() : null);

            if (q.getTargetClubId() != null) {
                Club c = clubMapper.selectById(q.getTargetClubId());
                map.put("targetClubName", c != null ? c.getName() : "未知社团");
            }
            result.add(map);
        }

        return Result.success(result);
    }
}
