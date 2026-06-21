package com.club.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.security.Key;

@Component
public class JwtUtils {
    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRE = 604800000; // 7 days

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRE);
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaimsByToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
