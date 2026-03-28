package com.fleta.closet.common.security;

import com.fleta.closet.common.exception.AppException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 15 * 60 * 1000L;               // 15min
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;     // 7days

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        return buildToken(userId, "access", ACCESS_TOKEN_EXPIRE_TIME);
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, "refresh", REFRESH_TOKEN_EXPIRE_TIME);
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw AppException.expiredToken();
        } catch (JwtException | IllegalArgumentException e) {
            throw AppException.invalidToken();
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseToken(token).get("type", String.class));
    }

    private String buildToken(Long userId, String type, long expireTime) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireTime))
                .signWith(key)
                .compact();
    }
}
