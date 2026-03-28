package com.fleta.closet.auth.service;

import com.fleta.closet.auth.domain.*;
import com.fleta.closet.auth.repository.UserRepository;
import com.fleta.closet.common.exception.AppException;
import com.fleta.closet.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw AppException.duplicateEmail();
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();
        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(AppException::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw AppException.invalidCredentials();
        }

        String accessToken  = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        try {
            if (jwtTokenProvider.isAccessToken(refreshToken)) {
                throw AppException.invalidToken();
            }
        } catch (AppException e) {
            // 만료/잘못된 토큰은 모두 INVALID_TOKEN으로 통일
            throw AppException.invalidToken();
        }

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(AppException::invalidToken);

        String newAccess  = jwtTokenProvider.createAccessToken(user.getId());
        String newRefresh = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(newRefresh);
        userRepository.save(user);

        return new TokenResponse(newAccess, newRefresh);
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(AppException::userNotFound);
        user.clearRefreshToken();
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(AppException::userNotFound);
        return UserResponse.from(user);
    }
}
