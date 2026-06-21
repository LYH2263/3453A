package com.club.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.club.entity.Club;
import com.club.common.Result;

public interface ClubService extends IService<Club> {
    Result<?> createClub(Club club);
    Result<?> getDashboardData();
}
