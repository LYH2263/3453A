package com.club.controller;

import com.club.common.Result;
import com.club.common.annotation.Log;
import com.club.service.AdminLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Tag(name = "后台管理-日志管理", description = "操作日志查询接口")
@RestController
@RequestMapping("/api/admin/logs")
public class LogController {

    @Autowired
    private AdminLogService adminLogService;


    /**
     * 分页查询操作日志
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 日志列表
     */
    @Operation(summary = "查询操作日志")
    @Log("查询操作日志")
    @GetMapping("/operation")
    public Result<?> listOperationLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminLogService.page(new Page<>(pageNum, pageSize)));
    }

    // Additional endpoints for login/exception logs could be added here
}
