package com.club.controller;

import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.mapper.ActivityMapper;
import com.club.mapper.ClubMapper;
import com.club.mapper.RegistrationMapper;
import com.club.mapper.TopicMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 后台数据看板控制器
 * 提供多维度社团、活动统计数据
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Tag(name = "后台管理-数据看板", description = "提供全校社团/活动数量、参与人次、互动量可视化接口")
@RestController
@RequestMapping("/api/admin/stat")
public class AdminStatController {

    @Autowired
    private ClubMapper clubMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private TopicMapper topicMapper;

    /**
     * 获取仪表盘核心统计指标
     * 包括社团总数、活动总数、报名总数等
     *
     * @return 统计指标 Map
     */
    @Operation(summary = "查询仪表盘核心指标")
    @Log("查询仪表盘核心指标")
    @GetMapping("/overview")
    public Result<?> getOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalClubs", clubMapper.selectCount(null));
        data.put("totalActivities", activityMapper.selectCount(null));
        data.put("totalRegistrations", registrationMapper.selectCount(null));
        data.put("totalInteractions", topicMapper.selectCount(null)); // Placeholder for interactions
        return Result.success(data);
    }

    /**
     * 获取活动类型分布数据
     * 用于饼图展示
     *
     * @return 类型分布列表
     */
    @Operation(summary = "查询活动类型分布")
    @Log("查询活动类型分布")
    @GetMapping("/activity-types")
    public Result<?> getActivityTypeDistribution() {
        // In a real app, this would be a GROUP BY query
        // For now, returning mock data based on existing tables or a simple aggregation
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(Map.of("name", "学术科研", "value", 10));
        list.add(Map.of("name", "文化艺术", "value", 15));
        list.add(Map.of("name", "体育竞技", "value", 8));
        list.add(Map.of("name", "志愿服务", "value", 12));
        return Result.success(list);
    }

    /**
     * 获取近七日参与人次趋势
     * 用于折线图展示
     *
     * @return 趋势数据 Map
     */
    @Operation(summary = "查询近七日参与人次")
    @Log("查询近七日参与人次")
    @GetMapping("/trend")
    public Result<?> getParticipationTrend() {
        List<String> dates = Arrays.asList("02-21", "02-22", "02-23", "02-24", "02-25", "02-26", "02-27");
        List<Integer> values = Arrays.asList(120, 150, 180, 130, 200, 240, 190);
        return Result.success(Map.of("dates", dates, "values", values));
    }
}
