package com.fleta.closet.auth;

import com.fleta.closet.auth.domain.LoginRequest;
import com.fleta.closet.auth.domain.TokenResponse;
import com.fleta.closet.auth.domain.User;
import com.fleta.closet.auth.domain.UserResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // LoginRequest 검증

    @Test
    @DisplayName("LoginRequest: 유효한 입력이면 검증 통과")
    void loginRequest_valid_noViolations() {
        LoginRequest request = new LoginRequest("test@test.com", "password123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("LoginRequest: 이메일 형식 오류이면 검증 실패")
    void loginRequest_invalidEmail_violation() {
        LoginRequest request = new LoginRequest("not-an-email", "password123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("LoginRequest: 비밀번호가 빈 값이면 검증 실패")
    void loginRequest_blankPassword_violation() {
        LoginRequest request = new LoginRequest("test@test.com", "");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    // TokenResponse

    @Test
    @DisplayName("TokenResponse: 토큰 값 정상 저장")
    void tokenResponse_storesTokens() {
        TokenResponse response = new TokenResponse("access-token", "refresh-token");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    // UserResponse

    @Test
    @DisplayName("UserResponse.from(): User 엔티티에서 응답 DTO 생성")
    void userResponse_from_user() {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPw")
                .nickname("테스터")
                .build();

        UserResponse response = UserResponse.from(user);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.nickname()).isEqualTo("테스터");
        assertThat(response.id()).isNull();  // DB 저장 전이므로 id는 null
    }
}
