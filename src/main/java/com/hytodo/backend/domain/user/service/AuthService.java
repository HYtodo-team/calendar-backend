package com.hytodo.backend.domain.user.service;

import com.hytodo.backend.domain.user.dto.LoginRequest;
import com.hytodo.backend.domain.user.dto.SignupRequest;
import com.hytodo.backend.domain.user.dto.TokenResponse;
import com.hytodo.backend.domain.user.dto.UserResponse;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import com.hytodo.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    // 존재하지 않는 이메일로 로그인 시도 시 비교 대상이 없어 매칭 연산이 스킵되면서
    // 존재하는 이메일과 응답 시간이 달라지는 것을 막기 위한 더미 해시(실제 비밀번호와 무관).
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$ESF6HAqVyfps0BMbGQU2nuJzs0vMsvirsfTB8A6hMbRnit1mfdmUa";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())){
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        String passwordHash = (user != null) ? user.getPasswordHash() : DUMMY_PASSWORD_HASH;

        if (!passwordEncoder.matches(request.password(), passwordHash) || user == null) {
            throw BusinessException.invalidCredentials();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        return TokenResponse.of(accessToken, jwtTokenProvider.getAccessTokenExpirationSeconds());
    }
}
