package com.club.controller;

import com.club.common.Result;
import com.club.entity.Answer;
import com.club.entity.Question;
import com.club.service.AnswerService;
import com.club.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qa")
public class QAController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private AnswerService answerService;

    @GetMapping("/questions")
    public Result<?> getQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer targetClubId) {
        return questionService.getQuestions(keyword, targetClubId);
    }

    @PostMapping("/questions")
    public Result<?> publishQuestion(@RequestBody Question question) {
        return questionService.publishQuestion(question);
    }

    @GetMapping("/questions/{id}/answers")
    public Result<?> getAnswers(@PathVariable Integer id) {
        return answerService.getAnswers(id);
    }

    @PostMapping("/answers")
    public Result<?> publishAnswer(@RequestBody Answer answer) {
        return answerService.publishAnswer(answer);
    }

    @PostMapping("/answers/{id}/best")
    public Result<?> markBestAnswer(@PathVariable Integer id) {
        return answerService.markBestAnswer(id);
    }
}
