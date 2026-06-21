package com.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.club.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    
    @Select("SELECT * FROM activities WHERE id = #{id} FOR UPDATE")
    Activity selectByIdForUpdate(Integer id);

    @Select("SELECT COALESCE(SUM(budget), 0) FROM activities WHERE club_id = #{clubId} AND status NOT IN ('REJECTED') AND is_deleted = 0 AND YEAR(create_time) = YEAR(CURRENT_DATE()) AND MONTH(create_time) = MONTH(CURRENT_DATE())")
    BigDecimal sumMonthlyBudget(@Param("clubId") Integer clubId);
}
