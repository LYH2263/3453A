package com.club.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.club.entity.ClubAsset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ClubAssetMapper extends BaseMapper<ClubAsset> {

    @Select("SELECT * FROM club_assets WHERE id = #{id} FOR UPDATE")
    ClubAsset selectByIdForUpdate(Integer id);
}
