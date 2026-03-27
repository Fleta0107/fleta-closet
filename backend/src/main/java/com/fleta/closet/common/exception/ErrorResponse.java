package com.fleta.closet.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String code,
        String message,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(AppException e) {
        return new ErrorResponse(e.getStatus(), e.getCode(), e.getMessage(), LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String code, String message) {
        return new ErrorResponse(status, code, message, LocalDateTime.now());
    }
}
