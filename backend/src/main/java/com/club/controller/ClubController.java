package com.club.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.entity.Club;
import com.club.entity.User;
import com.club.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.club.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
    @Autowired
    private ClubService clubService;

    @Autowired
    private UserMapper userMapper;

    @Log("查询社团列表")
    @GetMapping
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(clubService.page(new Page<>(pageNum, pageSize)));
    }

    @Log("创建社团")
    @PostMapping
    public Result<?> create(@Valid @RequestBody Club club) {
        return clubService.createClub(club);
    }

    @Log("更新社团信息")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Integer id, @RequestBody Club club) {
        club.setId(id);
        return Result.success(clubService.updateById(club));
    }

    @Log("注销社团")
    @DeleteMapping("/{id}")
    public Result<?> retire(@PathVariable Integer id) {
        Club club = new Club();
        club.setId(id);
        club.setStatus("RETIRED");
        return Result.success(clubService.updateById(club));
    }

    @Log("查看社团看板")
    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        return clubService.getDashboardData();
    }

    @Log("获取社团成员列表")
    @GetMapping("/{clubId}/members")
    public Result<?> getMembers(@PathVariable Integer clubId) {
        List<User> members = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getClubId, clubId)
                        .orderByAsc(User::getId));
        List<Map<String, Object>> result = members.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName());
            m.put("username", u.getUsername());
            m.put("role", u.getRole());
            m.put("avatar", u.getAvatar());
            return m;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
