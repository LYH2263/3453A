package com.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BadgeCreateDTO {
    @NotNull(message = "社团ID不能为空")
    private Integer clubId;

    @NotBlank(message = "徽章名称不能为空")
    @Size(max = 100, message = "徽章名称不能超过100个字符")
    private String name;

    private String iconUrl;

    private String description;

    private Integer isPublic;
}
