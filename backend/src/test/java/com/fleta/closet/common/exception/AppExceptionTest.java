package com.fleta.closet.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppExceptionTest {

    @Test
    void invalidCredentials_returns401WithCorrectCode() {
        AppException ex = AppException.invalidCredentials();
        assertThat(ex.getStatus()).isEqualTo(401);
        assertThat(ex.getCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(ex.getMessage()).isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void expiredToken_returns401WithCorrectCode() {
        AppException ex = AppException.expiredToken();
        assertThat(ex.getStatus()).isEqualTo(401);
        assertThat(ex.getCode()).isEqualTo("EXPIRED_TOKEN");
    }

    @Test
    void duplicateEmail_returns409WithCorrectCode() {
        AppException ex = AppException.duplicateEmail();
        assertThat(ex.getStatus()).isEqualTo(409);
        assertThat(ex.getCode()).isEqualTo("DUPLICATE_EMAIL");
    }

    @Test
    void invalidToken_returns401WithCorrectCode() {
        AppException ex = AppException.invalidToken();
        assertThat(ex.getStatus()).isEqualTo(401);
        assertThat(ex.getCode()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void forbidden_returns403WithCorrectCode() {
        AppException ex = AppException.forbidden();
        assertThat(ex.getStatus()).isEqualTo(403);
        assertThat(ex.getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void clothingNotFound_returns404WithCorrectCode() {
        AppException ex = AppException.clothingNotFound();
        assertThat(ex.getStatus()).isEqualTo(404);
        assertThat(ex.getCode()).isEqualTo("CLOTHING_NOT_FOUND");
    }

    @Test
    void userNotFound_returns404WithCorrectCode() {
        AppException ex = AppException.userNotFound();
        assertThat(ex.getStatus()).isEqualTo(404);
        assertThat(ex.getCode()).isEqualTo("USER_NOT_FOUND");
    }
}
