package com.fleta.closet.auth;

import com.fleta.closet.auth.domain.*;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.auth.service.AuthService;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("회원가입 성공 — 암호화된 비밀번호로 저장")
    void signup_success() {
        SignupRequest request = new SignupRequest("user@test.com", "pass1234!", "테스터");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed_pw");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("pass1234!");
    }

    @Test
    @DisplayName("중복 이메일 회원가입 → DUPLICATE_EMAIL 예외")
    void signup_duplicateEmail_throws() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("dup@test.com", "pass1234!", "닉")))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    @DisplayName("로그인 성공 → accessToken + refreshToken 반환")
    void login_success() {
        User user = User.builder().email("user@test.com").password("hashed_pw").nickname("닉").build();
        ReflectionTestUtils.setField(user, "id", 1L);  // JPA @GeneratedValue 대신 직접 설정
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass1234!", "hashed_pw")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(user);

        TokenResponse response = authService.login(new LoginRequest("user@test.com", "pass1234!"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 → INVALID_CREDENTIALS 예외")
    void login_wrongPassword_throws() {
        User user = User.builder().email("user@test.com").password("hashed_pw").nickname("닉").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "wrong")))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("존재하지 않는 이메일 로그인 → INVALID_CREDENTIALS 예외")
    void login_emailNotFound_throws() {
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("none@test.com", "pass")))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 새 토큰 발급 (Rotation)")
    void refresh_success() {
        User user = User.builder().email("u@test.com").password("pw").nickname("닉")
                .refreshToken("valid-refresh").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(jwtTokenProvider.isAccessToken("valid-refresh")).thenReturn(false);
        when(userRepository.findByRefreshToken("valid-refresh")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(user);

        TokenResponse response = authService.refresh("valid-refresh");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");  // Rotation 검증
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("DB에 없는 Refresh Token → INVALID_TOKEN 예외")
    void refresh_unknownToken_throws() {
        when(jwtTokenProvider.isAccessToken("unknown")).thenReturn(false);
        when(userRepository.findByRefreshToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown"))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Access Token으로 refresh 요청 시 → INVALID_TOKEN 예외")
    void refresh_withAccessToken_throws() {
        when(jwtTokenProvider.isAccessToken("access-token")).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh("access-token"))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("만료된 Refresh Token → INVALID_TOKEN 예외")
    void refresh_expiredToken_throws() {
        when(jwtTokenProvider.isAccessToken("expired-refresh"))
                .thenThrow(AppException.expiredToken());

        assertThatThrownBy(() -> authService.refresh("expired-refresh"))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("로그아웃 시 Refresh Token null 처리")
    void logout_success() {
        User user = User.builder().email("u@test.com").password("pw").nickname("닉")
                .refreshToken("some-token").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.logout(1L);

        assertThat(user.getRefreshToken()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그아웃 → USER_NOT_FOUND 예외")
    void logout_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(99L))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("USER_NOT_FOUND");
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_success() {
        User user = User.builder().email("me@test.com").password("pw").nickname("나").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = authService.getMe(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("me@test.com");
        assertThat(response.nickname()).isEqualTo("나");
    }

    @Test
    @DisplayName("존재하지 않는 사용자 내 정보 조회 → USER_NOT_FOUND 예외")
    void getMe_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(99L))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo("USER_NOT_FOUND");
    }
}
