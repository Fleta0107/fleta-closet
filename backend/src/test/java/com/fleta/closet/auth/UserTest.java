package com.fleta.closet.auth;

import com.fleta.closet.auth.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("빌더로 User 생성 시 필드 값 정상 저장")
    void builder_createsUserWithFields() {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();

        assertThat(user.getEmail()).isEqualTo("test@test.com");
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        assertThat(user.getNickname()).isEqualTo("테스터");
        assertThat(user.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("updateRefreshToken 호출 시 refreshToken 업데이트")
    void updateRefreshToken_storesNewToken() {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .build();

        user.updateRefreshToken("new-refresh-token");

        assertThat(user.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("clearRefreshToken 호출 시 refreshToken null 처리")
    void clearRefreshToken_setsNull() {
        User user = User.builder()
                .email("test@test.com")
                .password("encodedPassword")
                .nickname("테스터")
                .refreshToken("existing-token")
                .build();

        user.clearRefreshToken();

        assertThat(user.getRefreshToken()).isNull();
    }
}
