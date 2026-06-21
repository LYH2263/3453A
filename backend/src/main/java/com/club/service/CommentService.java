package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.Comment;

public interface CommentService extends IService<Comment> {
    Result<?> getCommentsByTopicId(Integer topicId);
    Result<?> publishComment(Comment comment);
}
