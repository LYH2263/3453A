package com.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.club.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    @Select("SELECT * FROM questions WHERE id = #{id} FOR UPDATE")
    Question selectByIdForUpdate(Integer id);
}
