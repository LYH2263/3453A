package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.Answer;

public interface AnswerService extends IService<Answer> {
    Result<?> publishAnswer(Answer answer);
    Result<?> getAnswers(Integer questionId);
    Result<?> markBestAnswer(Integer id);
}
