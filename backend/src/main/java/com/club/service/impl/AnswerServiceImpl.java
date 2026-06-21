package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.entity.Answer;
import com.club.entity.Question;
import com.club.entity.User;
import com.club.mapper.AnswerMapper;
import com.club.mapper.QuestionMapper;
import com.club.mapper.UserMapper;
import com.club.service.AnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnswerServiceImpl extends ServiceImpl<AnswerMapper, Answer> implements AnswerService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Override
    public Result<?> publishAnswer(Answer answer) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Result.error("未认证");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) return Result.error("用户不存在");

        answer.setAuthorId(user.getId());
        answer.setIsBest(0);
        this.save(answer);
        return Result.success(null);
    }

    @Override
    public Result<?> getAnswers(Integer questionId) {
        List<Answer> list = this.list(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .orderByDesc(Answer::getIsBest) // Best answers first
                .orderByAsc(Answer::getCreateTime));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Answer a : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("content", a.getContent());
            map.put("isBest", a.getIsBest() == 1);
            map.put("createTime", a.getCreateTime());

            User author = userMapper.selectById(a.getAuthorId());
            map.put("authorName", author != null ? author.getRealName() : "未知");
            map.put("authorRole", author != null ? author.getRole() : "未知");
            map.put("authorAvatar", author != null ? author.getAvatar() : null);

            result.add(map);
        }
        return Result.success(result);
    }

    @Override
    public Result<?> markBestAnswer(Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Result.error("未认证");
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));

        Answer answer = this.getById(id);
        if (answer == null) return Result.error("回答不存在");

        Question q = questionMapper.selectById(answer.getQuestionId());
        if (q == null || !q.getAuthorId().equals(user.getId())) {
            return Result.error("只有提问者可以采纳最佳答案");
        }

        answer.setIsBest(1);
        this.updateById(answer);
        return Result.success(null);
    }
}
