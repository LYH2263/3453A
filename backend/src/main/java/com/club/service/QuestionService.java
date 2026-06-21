package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.Question;

public interface QuestionService extends IService<Question> {
    Result<?> publishQuestion(Question question);
    Result<?> getQuestions(String keyword, Integer targetClubId);
}
