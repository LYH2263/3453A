package com.club.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    private String avatar;
    
    @NotBlank(message = "学号不能为空")
    private String studentId;
}
