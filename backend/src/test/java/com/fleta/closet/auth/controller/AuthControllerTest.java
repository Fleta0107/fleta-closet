package com.fleta.closet.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleta.closet.auth.domain.TokenResponse;
import com.fleta.closet.auth.domain.UserResponse;
import com.fleta.closet.auth.service.AuthService;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.exception.GlobalExceptionHandler;
import com.fleta.closet.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean JwtTokenProvider jwtTokenProvider;  // JwtAuthFilter 의존성 해소

    @Test
    @DisplayName("POST /api/auth/signup → 201 Created")
    void signup_returns201() throws Exception {
        doNothing().when(authService).signup(any());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"pass1234!\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isCreated());

        verify(authService).signup(any());
    }

    @Test
    @DisplayName("POST /api/auth/signup 이메일 형식 오류 → 400")
    void signup_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"pass1234!\",\"nickname\":\"테스터\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /api/auth/signup 중복 이메일 → 409")
    void signup_duplicateEmail_returns409() throws Exception {
        doThrow(AppException.duplicateEmail()).when(authService).signup(any());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@test.com\",\"password\":\"pass1234!\",\"nickname\":\"닉\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 with tokens")
    void login_returns200WithTokens() throws Exception {
        when(authService.login(any())).thenReturn(new TokenResponse("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"pass1234!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 200 with new tokens")
    void refresh_returns200WithNewTokens() throws Exception {
        when(authService.refresh(any())).thenReturn(new TokenResponse("new-access", "new-refresh"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh 만료된 토큰 → 401")
    void refresh_expiredToken_returns401() throws Exception {
        when(authService.refresh(any())).thenThrow(AppException.invalidToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("POST /api/auth/login 잘못된 자격증명 → 401")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(AppException.invalidCredentials());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/auth/logout → 204 No Content")
    void logout_returns204() throws Exception {
        doNothing().when(authService).logout(nullable(Long.class));

        mockMvc.perform(post("/api/auth/logout")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()))))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/auth/me → 200 with user info")
    void me_returns200WithUserInfo() throws Exception {
        // nullable: @AuthenticationPrincipal은 슬라이스 테스트에서 null → Task 11 통합 테스트에서 실제 주입 검증
        when(authService.getMe(nullable(Long.class))).thenReturn(new UserResponse(1L, "me@test.com", "나"));

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(
                                new UsernamePasswordAuthenticationToken(1L, null, Collections.emptyList()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.com"))
                .andExpect(jsonPath("$.nickname").value("나"));
    }
}
