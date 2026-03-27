package com.fleta.closet.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final int status;
    private final String code;

    public AppException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    // 팩토리 메서드 - 에러 코드를 한 곳에서 처리
    public static AppException invalidCredentials() {
        return new AppException(401, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    public static AppException expiredToken() {
        return new AppException(401, "EXPIRED_TOKEN", "만료된 토큰입니다.");
    }

    public static AppException duplicateEmail() {
        return new AppException(409, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다.");
    }

    public static AppException invalidToken() {
        return new AppException(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다");
    }

    public static AppException forbidden() {
        return new AppException(403, "FORBIDDEN", "접근 권한이 없습니다");
    }

    public static AppException clothingNotFound() {
        return new AppException(404, "CLOTHING_NOT_FOUND", "의류를 찾을 수 없습니다");
    }

    public static AppException userNotFound() {
        return new AppException(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다");
    }
}
