package com.club.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.club.common.Result;
import com.club.entity.*;
import com.club.mapper.*;
import com.club.service.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.*;

@Service
public class ClubServiceImpl extends ServiceImpl<ClubMapper, Club> implements ClubService {
    
    @Autowired
    private ActivityMapper activityMapper;
    
    @Autowired
    private RegistrationMapper registrationMapper;
    
    @Autowired
    private UserMapper userMapper;
    @Override
    public Result<?> createClub(Club club) {
        this.save(club);
        return Result.success(null);
    }

    @Override
    public Result<?> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalClubs", this.count());
        
        long activeCount = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, Arrays.asList("PENDING_UNION", "PENDING_SCHOOL", "APPROVED", "FINISHED")));
        data.put("activeActivities", activeCount);
        
        long memberCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, com.club.common.RoleConstants.MEMBER));
        data.put("totalMembers", memberCount);
        
        // Completion rate: FINISHED / all approved+finished
        long totalApprovedOrFinished = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .in(Activity::getStatus, Arrays.asList("APPROVED", "FINISHED")));
        long finishedCount = activityMapper.selectCount(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, "FINISHED"));
        double completionRate = totalApprovedOrFinished > 0 ? (double) finishedCount / totalApprovedOrFinished : 0;
        data.put("completionRate", Math.round(completionRate * 100)); // Percentage
        
        // Average satisfaction
        List<ActivityRegistration> ratedRegs = registrationMapper.selectList(new LambdaQueryWrapper<ActivityRegistration>()
                .isNotNull(ActivityRegistration::getRating));
        double avgSatisfaction = ratedRegs.isEmpty() ? 0 : ratedRegs.stream().mapToInt(ActivityRegistration::getRating).average().orElse(0);
        data.put("satisfaction", String.format("%.1f", avgSatisfaction));
        
        return Result.success(data);
    }
}
