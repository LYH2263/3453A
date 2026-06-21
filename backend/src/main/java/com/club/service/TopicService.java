package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.common.Result;
import com.club.entity.Topic;

public interface TopicService extends IService<Topic> {
    Result<?> publishTopic(Topic topic);
    Result<?> getTopics(String type);
    Result<?> getPendingTopics();
    Result<?> auditTopic(Integer id, String status);
    Result<?> interact(Integer id, String type); // type: LIKE, FAVORITE
}
