package com.club.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrantBadgeDTO {
    @NotNull(message = "徽章ID不能为空")
    private Integer badgeId;

    @NotNull(message = "用户ID不能为空")
    private Integer userId;
}
