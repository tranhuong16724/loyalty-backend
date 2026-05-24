package com.doan.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Tiện ích tạo và xác thực JWT cho Android App.
 *
 * Secret được đọc từ application.properties (jwt.secret).
 * Nếu chưa có, Spring sẽ báo lỗi khi khởi động — buộc cấu hình rõ ràng.
 */
@Component
public class JwtUtil {

    /** Thời hạn token: 30 ngày (ms). Có thể chỉnh trong application.properties. */
    private static final long EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000;

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        // HMAC-SHA256 key — secret phải >= 32 ký tự (256 bit)
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** Tạo JWT cho một customer (subject = customerId dạng String). */
    public String generateToken(Long customerId) {
        return Jwts.builder()
                .setSubject(String.valueOf(customerId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Lấy customerId từ token đã xác thực. */
    public Long extractCustomerId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /** Kiểm tra token hợp lệ (chữ ký đúng + chưa hết hạn). */
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