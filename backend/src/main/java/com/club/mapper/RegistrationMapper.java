package com.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.club.entity.ActivityRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RegistrationMapper extends BaseMapper<ActivityRegistration> {

    @Select("SELECT sentiment, COUNT(*) as count FROM activity_registrations " +
            "WHERE activity_id = #{activityId} AND feedback IS NOT NULL AND feedback != '' " +
            "GROUP BY sentiment")
    List<Map<String, Object>> countBySentimentForActivity(@Param("activityId") Integer activityId);

    @Select("SELECT AVG(rating) as avg_rating FROM activity_registrations " +
            "WHERE activity_id = #{activityId} AND rating IS NOT NULL")
    Double getAverageRatingForActivity(@Param("activityId") Integer activityId);

    @Select("SELECT ar.*, u.real_name, u.username FROM activity_registrations ar " +
            "LEFT JOIN users u ON ar.user_id = u.id " +
            "WHERE ar.activity_id = #{activityId} AND ar.sentiment = #{sentiment} " +
            "AND ar.feedback IS NOT NULL AND ar.feedback != '' " +
            "ORDER BY ar.update_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getFeedbackExamplesBySentiment(
            @Param("activityId") Integer activityId,
            @Param("sentiment") String sentiment,
            @Param("limit") Integer limit);

    @Select("SELECT ar.*, u.real_name, u.username, a.title as activity_title, c.name as club_name " +
            "FROM activity_registrations ar " +
            "LEFT JOIN users u ON ar.user_id = u.id " +
            "LEFT JOIN activities a ON ar.activity_id = a.id " +
            "LEFT JOIN clubs c ON a.club_id = c.id " +
            "WHERE ar.feedback IS NOT NULL AND ar.feedback != '' " +
            "AND (#{sentiment} IS NULL OR ar.sentiment = #{sentiment}) " +
            "AND (#{activityId} IS NULL OR ar.activity_id = #{activityId}) " +
            "AND (#{clubId} IS NULL OR a.club_id = #{clubId}) " +
            "ORDER BY ar.update_time DESC")
    List<Map<String, Object>> getFeedbackListWithFilters(
            @Param("sentiment") String sentiment,
            @Param("activityId") Integer activityId,
            @Param("clubId") Integer clubId);
}
