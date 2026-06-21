package com.club.common;

/**
 * 角色常量
 * ADMIN        - 超级管理员（学校）
 * UNION_ADMIN  - 社联管理员
 * CLUB_LEADER  - 社团负责人
 * MEMBER       - 普通社员
 * GUEST        - 游客（未注册/未认证学生）
 */
public class RoleConstants {
    public static final String ADMIN        = "ADMIN";
    public static final String UNION_ADMIN  = "UNION_ADMIN";
    public static final String CLUB_LEADER  = "CLUB_LEADER";
    public static final String MEMBER       = "MEMBER";
    public static final String GUEST        = "GUEST";

    // Spring Security 前缀形式
    public static final String ROLE_ADMIN        = "ROLE_ADMIN";
    public static final String ROLE_UNION_ADMIN  = "ROLE_UNION_ADMIN";
    public static final String ROLE_CLUB_LEADER  = "ROLE_CLUB_LEADER";
    public static final String ROLE_MEMBER       = "ROLE_MEMBER";
    public static final String ROLE_GUEST        = "ROLE_GUEST";
}
