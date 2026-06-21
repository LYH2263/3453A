package com.club.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.club.dto.LoginDTO;
import com.club.dto.RegisterDTO;
import com.club.dto.ResetPasswordDTO;
import com.club.dto.UpdateProfileDTO;
import com.club.entity.User;
import com.club.common.Result;

public interface UserService extends IService<User> {
    Result<?> login(LoginDTO loginDTO);
    Result<?> register(RegisterDTO registerDTO);
    Result<?> resetPassword(ResetPasswordDTO dto);
    Result<?> getProfile(String username);
    Result<?> updateProfile(String username, UpdateProfileDTO dto);
    Result<?> getMyActivities(String username);
    Result<?> getMyInteractions(String username);
    Result<?> getNotifications(String username);
    Result<?> updateRole(Integer userId, String role);
    java.util.List<User> listUsers();
}
