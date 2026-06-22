package com.club.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.club.entity.AuditConfig;
import com.club.entity.Club;
import com.club.mapper.AuditConfigMapper;
import com.club.mapper.ClubMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class BudgetLimitService {

    @Autowired
    private ClubMapper clubMapper;

    @Autowired
    private AuditConfigMapper auditConfigMapper;

    @Value("${budget.token.secret:club-budget-limit-secret-key-for-signing-tokens-2026}")
    private String tokenSecret;

    @Value("${budget.token.ttl-seconds:300}")
    private long tokenTtlSeconds;

    public static class BudgetLimitResult {
        public final BigDecimal limit;
        public final String enforceMode;
        public final boolean fromClub;

        public BudgetLimitResult(BigDecimal limit, String enforceMode, boolean fromClub) {
            this.limit = limit;
            this.enforceMode = enforceMode;
            this.fromClub = fromClub;
        }
    }

    public BudgetLimitResult resolveBudgetLimit(Integer clubId) {
        BigDecimal limit = null;
        String enforceMode = null;
        boolean fromClub = false;

        Club club = clubMapper.selectById(clubId);
        if (club != null) {
            if (club.getMonthlyBudgetLimit() != null && club.getMonthlyBudgetLimit().compareTo(BigDecimal.ZERO) > 0) {
                limit = club.getMonthlyBudgetLimit();
                fromClub = true;
            }
            if (club.getBudgetEnforceMode() != null && !club.getBudgetEnforceMode().trim().isEmpty()) {
                enforceMode = club.getBudgetEnforceMode();
                fromClub = true;
            }
        }

        if (limit == null || enforceMode == null) {
            AuditConfig defaultConfig = auditConfigMapper.selectOne(
                new LambdaQueryWrapper<AuditConfig>()
                    .eq(AuditConfig::getType, "MONTHLY_BUDGET_LIMIT")
                    .eq(AuditConfig::getIsActive, 1));
            if (defaultConfig != null && defaultConfig.getNodes() != null) {
                try {
                    String nodes = defaultConfig.getNodes();
                    if (limit == null && nodes.contains("defaultLimit")) {
                        String limitStr = nodes.replaceAll(".*\"defaultLimit\"\\s*:\\s*", "").replaceAll("[^0-9.].*", "").trim();
                        if (!limitStr.isEmpty()) limit = new BigDecimal(limitStr);
                    }
                    if (enforceMode == null && nodes.contains("defaultEnforceMode")) {
                        String mode = nodes.replaceAll(".*\"defaultEnforceMode\"\\s*:\\s*\"", "").replaceAll("\".*", "").trim();
                        if (!mode.isEmpty()) enforceMode = mode;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (limit == null) limit = new BigDecimal("5000");
        if (enforceMode == null) enforceMode = "SOFT";

        return new BudgetLimitResult(limit, enforceMode, fromClub);
    }

    public String generateBudgetToken(Integer clubId, BigDecimal projectedTotal, BigDecimal limit) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiration = new Date(now + tokenTtlSeconds * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("clubId", clubId);
        claims.put("projectedTotal", projectedTotal.toPlainString());
        claims.put("limit", limit.toPlainString());

        SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(issuedAt)
            .setExpiration(expiration)
            .signWith(key)
            .compact();
    }

    public Map<String, Object> verifyBudgetToken(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        try {
            SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            Object clubIdObj = claims.get("clubId");
            Object projectedTotal = claims.get("projectedTotal");
            Object limit = claims.get("limit");
            if (clubIdObj == null || projectedTotal == null || limit == null) return null;
            Integer clubId;
            if (clubIdObj instanceof Number) {
                clubId = ((Number) clubIdObj).intValue();
            } else {
                clubId = Integer.parseInt(clubIdObj.toString());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("clubId", clubId);
            result.put("projectedTotal", new BigDecimal(projectedTotal.toString()));
            result.put("limit", new BigDecimal(limit.toString()));
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
