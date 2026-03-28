package com.fleta.closet.auth;

import com.fleta.closet.auth.domain.SignupRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SignupRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("유효한 입력이면 검증 통과")
    void validRequest_noViolations() {
        SignupRequest request = new SignupRequest("test@test.com", "password123", "닉네임");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 검증 실패")
    void invalidEmail_violation() {
        SignupRequest request = new SignupRequest("not-an-email", "password123", "닉네임");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("이메일이 빈 값이면 검증 실패")
    void blankEmail_violation() {
        SignupRequest request = new SignupRequest("", "password123", "닉네임");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 검증 실패")
    void shortPassword_violation() {
        SignupRequest request = new SignupRequest("test@test.com", "pass", "닉네임");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("비밀번호가 빈 값이면 검증 실패")
    void blankPassword_violation() {
        SignupRequest request = new SignupRequest("test@test.com", "", "닉네임");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("닉네임이 빈 값이면 검증 실패")
    void blankNickname_violation() {
        SignupRequest request = new SignupRequest("test@test.com", "password123", "");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @Test
    @DisplayName("닉네임이 최대 길이 초과하면 검증 실패")
    void longNickname_violation() {
        SignupRequest request = new SignupRequest("test@test.com", "password123", "열한글자닉네임테스트용");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }
}
