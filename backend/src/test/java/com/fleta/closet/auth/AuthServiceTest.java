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
}
