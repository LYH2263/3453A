package com.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.club.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    
    @Select("SELECT * FROM activities WHERE id = #{id} FOR UPDATE")
    Activity selectByIdForUpdate(Integer id);
}
