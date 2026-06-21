package com.club.controller;

import com.alibaba.excel.EasyExcel;
import com.club.common.annotation.Log;
import com.club.entity.Activity;
import com.club.entity.Club;
import com.club.entity.OperationLog;
import com.club.service.ActivityService;
import com.club.service.AdminLogService;
import com.club.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 报表导出控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Tag(name = "后台管理-数据导出", description = "社团、活动、日志的 Excel 导出")
@RestController
@RequestMapping("/api/admin/export")
public class ExportController {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private AdminLogService adminLogService;

    /**
     * 导出全校社团 Excel 报表
     */
    @Operation(summary = "导出社团列表")
    @Log("导出社团列表")
    @GetMapping("/clubs")
    public void exportClubs(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("社团列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        List<Club> list = clubService.list();
        EasyExcel.write(response.getOutputStream(), Club.class).sheet("社团").doWrite(list);
    }

    /**
     * 导出全校活动 Excel 报表
     */
    @Operation(summary = "导出活动列表")
    @Log("导出活动列表")
    @GetMapping("/activities")
    public void exportActivities(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("活动列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        List<Activity> list = activityService.list();
        EasyExcel.write(response.getOutputStream(), Activity.class).sheet("活动").doWrite(list);
    }

    /**
     * 导出系统操作日志 Excel 报表
     */
    @Operation(summary = "导出操作日志")
    @Log("导出操作日志")
    @GetMapping("/logs")
    public void exportLogs(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("操作日志", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        List<OperationLog> list = adminLogService.list();
        EasyExcel.write(response.getOutputStream(), OperationLog.class).sheet("日志").doWrite(list);
    }
}
