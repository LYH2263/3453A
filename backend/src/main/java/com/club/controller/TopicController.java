package com.club.controller;

import com.club.common.Result;
import com.club.entity.Topic;
import com.club.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    @Autowired
    private TopicService topicService;

    @GetMapping
    public Result<?> getTopics(@RequestParam(required = false, defaultValue = "IN_CLUB") String type) {
        return topicService.getTopics(type);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNION_ADMIN')")
    public Result<?> getPendingTopics() {
        return topicService.getPendingTopics();
    }

    @PostMapping
    public Result<?> publishTopic(@Valid @RequestBody Topic topic) {
        return topicService.publishTopic(topic);
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNION_ADMIN')")
    public Result<?> auditTopic(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return topicService.auditTopic(id, body.get("status"));
    }

    @PostMapping("/{id}/interact")
    public Result<?> interact(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return topicService.interact(id, body.get("type")); // LIKE or FAVORITE
    }
}
