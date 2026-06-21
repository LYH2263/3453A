package com.club.config;

import com.club.common.RoleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 启用 @PreAuthorize 细粒度权限控制
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口：登录、注册、密码重置
                .requestMatchers(
                    "/api/auth/**",
                    "/api/clubs/dashboard",
                    "/api/admin/export/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                // 用户管理列表和基础配置仅管理员可访问
                .requestMatchers("/api/user/list", "/api/admin/config/**").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN)
                .requestMatchers("/api/user/*/role").hasRole(RoleConstants.ADMIN)
                // 社团负责人需要有数据看板和社团管理的权限
                .requestMatchers("/api/admin/stat/**", "/api/admin/dashboard/stats", "/api/admin/clubs/**").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN, RoleConstants.CLUB_LEADER)
                .requestMatchers("/api/admin/**").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN)
                .requestMatchers("/api/user/*").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN, RoleConstants.CLUB_LEADER, RoleConstants.MEMBER)
                // 活动管理权限
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/activities").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN, RoleConstants.CLUB_LEADER)
                .requestMatchers("/api/activities/*/finish", "/api/activities/*/reply").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN, RoleConstants.CLUB_LEADER)
                // 活动审核仅社联/管理员
                .requestMatchers("/api/activities/*/audit").hasAnyRole(
                    RoleConstants.ADMIN, RoleConstants.UNION_ADMIN)
                // 其余接口需登录
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
