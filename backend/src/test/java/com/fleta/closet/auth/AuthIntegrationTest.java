package com.fleta.closet.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleta.closet.auth.domain.LoginRequest;
import com.fleta.closet.auth.domain.RefreshRequest;
import com.fleta.closet.auth.domain.SignupRequest;
import com.fleta.closet.auth.domain.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("fleta_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 → 로그인 → 내 정보 조회 전체 흐름")
    void signup_login_me_flow() throws Exception {
        // 1. 회원가입
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("test@fleta.com", "password123", "테스터"))))
                .andExpect(status().isCreated());

        // 2. 로그인
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("test@fleta.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokens = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class);
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        // 3. 내 정보 조회 — @AuthenticationPrincipal Long userId 실제 주입 검증
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@fleta.com"))
                .andExpect(jsonPath("$.nickname").value("테스터"));
    }

    @Test
    @DisplayName("Refresh Token으로 새 Access Token 발급 (Rotation)")
    void refresh_flow() throws Exception {
        // 1. 가입 + 로그인
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("refresh@fleta.com", "password123", "갱신유저"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("refresh@fleta.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse tokens = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class);

        // 2. Refresh Token으로 새 토큰 발급
        // Thread.sleep(1100): JWT iat는 초 단위 — 동일 초 내 생성 시 구/신 토큰이 동일 값이 되어 Rotation 검증 불가
        Thread.sleep(1100);
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();

        TokenResponse newTokens = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), TokenResponse.class);
        assertThat(newTokens.accessToken()).isNotBlank();
        assertThat(newTokens.refreshToken()).isNotBlank();
        assertThat(newTokens.refreshToken()).isNotEqualTo(tokens.refreshToken());  // Rotation: 새 refresh 토큰 발급 확인

        // 3. 새 access token으로 보호된 엔드포인트 접근 가능 확인
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + newTokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("refresh@fleta.com"));

        // 4. Rotation 무효화: 구 refresh token 재사용 → 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(tokens.refreshToken()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("인증 없이 보호된 경로 접근 → 401")
    void unauthenticated_returns_401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 → 409")
    void signup_duplicate_returns_409() throws Exception {
        String body = objectMapper.writeValueAsString(
                new SignupRequest("dup@fleta.com", "password123", "닉"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }
}
