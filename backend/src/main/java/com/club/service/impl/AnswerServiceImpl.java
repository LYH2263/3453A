package com.club.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.entity.Answer;
import com.club.entity.AnswerVote;
import com.club.entity.Question;
import com.club.entity.User;
import com.club.mapper.AnswerMapper;
import com.club.mapper.AnswerVoteMapper;
import com.club.mapper.QuestionMapper;
import com.club.mapper.UserMapper;
import com.club.service.AnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private AnswerVoteMapper answerVoteMapper;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
    }

    @Override
    public Result<?> publishAnswer(Answer answer) {
        User user = getCurrentUser();
        if (user == null) return Result.error("未认证");

        answer.setAuthorId(user.getId());
        answer.setIsBest(0);
        this.save(answer);
        return Result.success(null);
    }

    @Override
    public Result<?> getAnswers(Integer questionId) {
        User currentUser = getCurrentUser();
        Integer currentUserId = currentUser != null ? currentUser.getId() : null;

        List<Answer> list = this.list(new LambdaQueryWrapper<Answer>()
                .eq(Answer::getQuestionId, questionId)
                .orderByDesc(Answer::getIsBest)
                .orderByAsc(Answer::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Answer a : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("questionId", a.getQuestionId());
            map.put("authorId", a.getAuthorId());
            map.put("content", a.getContent());
            map.put("isBest", a.getIsBest() == 1);
            map.put("createTime", a.getCreateTime());

            User author = userMapper.selectById(a.getAuthorId());
            map.put("authorName", author != null ? author.getRealName() : "未知");
            map.put("authorRole", author != null ? author.getRole() : "未知");
            map.put("authorAvatar", author != null ? author.getAvatar() : null);

            int votesCount = answerVoteMapper.selectCount(
                    new LambdaQueryWrapper<AnswerVote>().eq(AnswerVote::getAnswerId, a.getId())
            ).intValue();
            map.put("votesCount", votesCount);

            boolean hasVoted = false;
            if (currentUserId != null) {
                Long voted = answerVoteMapper.selectCount(
                        new LambdaQueryWrapper<AnswerVote>()
                                .eq(AnswerVote::getAnswerId, a.getId())
                                .eq(AnswerVote::getUserId, currentUserId)
                );
                hasVoted = voted != null && voted > 0;
            }
            map.put("hasVoted", hasVoted);

            result.add(map);
        }
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> markBestAnswer(Integer id) {
        User user = getCurrentUser();
        if (user == null) return Result.error("未认证");

        Answer answer = this.getById(id);
        if (answer == null) return Result.error("回答不存在");

        Question q = questionMapper.selectById(answer.getQuestionId());
        if (q == null || !q.getAuthorId().equals(user.getId())) {
            return Result.error("只有提问者可以采纳最佳答案");
        }

        if (answer.getIsBest() != null && answer.getIsBest() == 1) {
            return Result.error("该回答已是最佳答案");
        }

        List<Answer> sameQuestionAnswers = this.list(
                new LambdaQueryWrapper<Answer>()
                        .eq(Answer::getQuestionId, answer.getQuestionId())
                        .eq(Answer::getIsBest, 1)
        );
        if (!sameQuestionAnswers.isEmpty()) {
            return Result.error("该问题已有最佳答案，已被采纳");
        }

        this.update(
                new LambdaUpdateWrapper<Answer>()
                        .eq(Answer::getQuestionId, answer.getQuestionId())
                        .eq(Answer::getIsBest, 1)
                        .set(Answer::getIsBest, 0)
        );

        answer.setIsBest(1);
        try {
            boolean updated = this.updateById(answer);
            if (!updated) {
                return Result.error("已被采纳，采纳失败");
            }
        } catch (OptimisticLockingFailureException e) {
            return Result.error("已被采纳，并发冲突");
        }

        return Result.success(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> voteAnswer(Integer answerId) {
        User user = getCurrentUser();
        if (user == null) return Result.error("未认证");

        Answer answer = this.getById(answerId);
        if (answer == null) return Result.error("回答不存在");

        Question q = questionMapper.selectById(answer.getQuestionId());
        if (q != null && q.getAuthorId().equals(user.getId())) {
            return Result.error("提问者不能给自己问题的答案投票");
        }
        if (answer.getAuthorId().equals(user.getId())) {
            return Result.error("不能给自己的回答投票");
        }

        LambdaQueryWrapper<AnswerVote> voteWrapper = new LambdaQueryWrapper<AnswerVote>()
                .eq(AnswerVote::getAnswerId, answerId)
                .eq(AnswerVote::getUserId, user.getId());

        AnswerVote existingVote = answerVoteMapper.selectOne(voteWrapper);
        if (existingVote != null) {
            answerVoteMapper.deleteById(existingVote.getId());
        } else {
            AnswerVote newVote = new AnswerVote();
            newVote.setAnswerId(answerId);
            newVote.setUserId(user.getId());
            answerVoteMapper.insert(newVote);
        }

        int votesCount = answerVoteMapper.selectCount(
                new LambdaQueryWrapper<AnswerVote>().eq(AnswerVote::getAnswerId, answerId)
        ).intValue();

        Map<String, Object> result = new HashMap<>();
        result.put("votesCount", votesCount);
        result.put("hasVoted", existingVote == null);
        return Result.success(result);
    }
}
