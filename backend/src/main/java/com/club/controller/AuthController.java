package com.club.controller;

import com.club.common.Result;
import com.club.dto.LoginDTO;
import com.club.dto.RegisterDTO;
import com.club.dto.ResetPasswordDTO;
import com.club.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return userService.register(registerDTO);
    }

    @PostMapping("/reset-password")
    public Result<?> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        return userService.resetPassword(dto);
    }
}
