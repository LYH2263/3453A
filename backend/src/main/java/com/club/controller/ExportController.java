package com.club.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.club.common.Result;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * 导出活动反馈 Excel 报表（支持按情绪筛选）
     */
    @Operation(summary = "导出活动反馈")
    @Log("导出活动反馈")
    @GetMapping("/feedback")
    public void exportFeedback(
            HttpServletResponse response,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) Integer activityId,
            @RequestParam(required = false) Integer clubId) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        StringBuilder fileNameBuilder = new StringBuilder("活动反馈");
        if (sentiment != null && !sentiment.isEmpty()) {
            fileNameBuilder.append("_").append(sentiment);
        }
        String fileName = URLEncoder.encode(fileNameBuilder.toString(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        Result<?> result = activityService.getFeedbackList(sentiment, activityId, clubId);
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.getData();

        List<FeedbackExportVO> exportList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> item : data) {
            FeedbackExportVO vo = new FeedbackExportVO();
            vo.setActivityName((String) item.get("activity_title"));
            vo.setClubName((String) item.get("club_name"));
            vo.setUserName((String) item.get("real_name"));
            vo.setUserAccount((String) item.get("username"));

            Object rating = item.get("rating");
            if (rating != null) {
                vo.setRating(((Number) rating).intValue());
            }

            vo.setFeedback((String) item.get("feedback"));

            String sentimentValue = (String) item.get("sentiment");
            vo.setSentiment(sentimentValue != null ? getSentimentText(sentimentValue) : "未分析");

            List<String> tags = (List<String>) item.get("tags");
            if (tags != null && !tags.isEmpty()) {
                vo.setTags(String.join(", ", tags));
            } else {
                vo.setTags("无");
            }

            Object updateTime = item.get("update_time");
            if (updateTime != null) {
                vo.setUpdateTime(updateTime.toString());
            }

            exportList.add(vo);
        }

        EasyExcel.write(response.getOutputStream(), FeedbackExportVO.class).sheet("反馈列表").doWrite(exportList);
    }

    private String getSentimentText(String sentiment) {
        switch (sentiment) {
            case "POSITIVE":
                return "正面";
            case "NEGATIVE":
                return "负面";
            case "NEUTRAL":
                return "中性";
            default:
                return "未知";
        }
    }

    public static class FeedbackExportVO {
        @ExcelProperty("活动名称")
        private String activityName;

        @ExcelProperty("所属社团")
        private String clubName;

        @ExcelProperty("用户姓名")
        private String userName;

        @ExcelProperty("用户账号")
        private String userAccount;

        @ExcelProperty("评分")
        private Integer rating;

        @ExcelProperty("反馈内容")
        private String feedback;

        @ExcelProperty("情绪标签")
        private String sentiment;

        @ExcelProperty("关键词标签")
        private String tags;

        @ExcelProperty("更新时间")
        private String updateTime;

        public String getActivityName() { return activityName; }
        public void setActivityName(String activityName) { this.activityName = activityName; }

        public String getClubName() { return clubName; }
        public void setClubName(String clubName) { this.clubName = clubName; }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }

        public String getUserAccount() { return userAccount; }
        public void setUserAccount(String userAccount) { this.userAccount = userAccount; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }

        public String getSentiment() { return sentiment; }
        public void setSentiment(String sentiment) { this.sentiment = sentiment; }

        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }

        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
    }
}
