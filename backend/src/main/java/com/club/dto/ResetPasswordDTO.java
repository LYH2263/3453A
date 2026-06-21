package com.club.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "学号不能为空")
    private String studentId;   // 通过学号验证身份
    
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
