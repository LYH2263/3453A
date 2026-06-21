package com.club.controller;

import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.entity.Club;
import com.club.mapper.ActivityMapper;
import com.club.mapper.ClubMapper;
import com.club.mapper.RegistrationMapper;
import com.club.mapper.TopicMapper;
import com.club.service.BudgetLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

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
    @Autowired
    private BudgetLimitService budgetLimitService;

    @Operation(summary = "查询仪表盘核心指标")
    @Log("查询仪表盘核心指标")
    @GetMapping("/overview")
    public Result<?> getOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalClubs", clubMapper.selectCount(null));
        data.put("totalActivities", activityMapper.selectCount(null));
        data.put("totalRegistrations", registrationMapper.selectCount(null));
        data.put("totalInteractions", topicMapper.selectCount(null));
        return Result.success(data);
    }

    @Operation(summary = "查询活动类型分布")
    @Log("查询活动类型分布")
    @GetMapping("/activity-types")
    public Result<?> getActivityTypeDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(Map.of("name", "学术科研", "value", 10));
        list.add(Map.of("name", "文化艺术", "value", 15));
        list.add(Map.of("name", "体育竞技", "value", 8));
        list.add(Map.of("name", "志愿服务", "value", 12));
        return Result.success(list);
    }

    @Operation(summary = "查询近七日参与人次")
    @Log("查询近七日参与人次")
    @GetMapping("/trend")
    public Result<?> getParticipationTrend() {
        List<String> dates = Arrays.asList("02-21", "02-22", "02-23", "02-24", "02-25", "02-26", "02-27");
        List<Integer> values = Arrays.asList(120, 150, 180, 130, 200, 240, 190);
        return Result.success(Map.of("dates", dates, "values", values));
    }

    @Operation(summary = "查询当月各社团预算占用率")
    @Log("查询当月各社团预算占用率")
    @GetMapping("/budget-utilization")
    public Result<?> getBudgetUtilization() {
        List<Club> clubs = clubMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Club club : clubs) {
            BigDecimal currentBudget = activityMapper.sumMonthlyBudget(club.getId());
            BudgetLimitService.BudgetLimitResult resolved = budgetLimitService.resolveBudgetLimit(club.getId());
            BigDecimal limit = resolved.limit;
            BigDecimal rate = currentBudget.multiply(new BigDecimal("100"))
                    .divide(limit, 1, RoundingMode.HALF_UP);

            Map<String, Object> item = new HashMap<>();
            item.put("clubId", club.getId());
            item.put("clubName", club.getName());
            item.put("currentBudget", currentBudget);
            item.put("budgetLimit", limit);
            item.put("utilizationRate", rate);
            item.put("limitFromClub", resolved.fromClub);
            result.add(item);
        }
        return Result.success(result);
    }
}
