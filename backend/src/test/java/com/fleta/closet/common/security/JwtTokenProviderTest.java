package com.fleta.closet.common.security;

import com.fleta.closet.common.exception.AppException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtTokenProviderTest {

    private static final String SECRET =
            "fleta-closet-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET);

    @Test
    @DisplayName("Access Token 생성 후 userId 추출 성공")
    void createAccessToken_and_getUserId() {
        String token = provider.createAccessToken(42L);

        assertThat(provider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("Access Token type 확인")
    void accessToken_isAccessToken_true() {
        String token = provider.createAccessToken(1L);

        assertThat(provider.isAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("Refresh Token type은 Access Token과 다르다")
    void refreshToken_isAccessToken_false() {
        String token = provider.createRefreshToken(1L);

        assertThat(provider.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰을 파싱했을 때 INVALID_TOKEN 예외 발생")
    void invalidToken_throws_AppException() {
        assertThatThrownBy(() -> provider.parseToken("invalid-token-value"))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("만료된 토큰을 파싱했을 때 EXPIRED_TOKEN 예외 발생")
    void expiredToken_throws_AppException() {
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("type", "access")
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .expiration(new Date(System.currentTimeMillis() - 1000))  // 이미 만료
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> provider.parseToken(expiredToken))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo("EXPIRED_TOKEN");
    }
}
