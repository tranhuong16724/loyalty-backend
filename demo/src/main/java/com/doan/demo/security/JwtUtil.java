package com.doan.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    //token hết hạn
    private static final long EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000;

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
// jwt được tạo
    public String generateToken(Long customerId) {
        return Jwts.builder()
                // tạo jwt chứa customerid
                .setSubject(String.valueOf(customerId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    //hàm lấy id
    public Long extractCustomerId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }
    // hàm kiểm tra thật
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}