package com.fleta.closet.auth.domain;

public record TokenResponse (String accessToken, String refreshToken) { }
