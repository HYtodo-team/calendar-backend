package com.hytodo.backend.domain.user.service;

import java.util.Optional;

import com.hytodo.backend.domain.user.dto.LoginRequest;
import com.hytodo.backend.domain.user.dto.SignupRequest;
import com.hytodo.backend.domain.user.dto.TokenResponse;
import com.hytodo.backend.domain.user.dto.UserResponse;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import com.hytodo.backend.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("이메일이 중복되지 않으면 회원가입에 성공한다")
    void signup_success() {
        SignupRequest request = new SignupRequest("user@example.com", "password1", "tester");
        given(userRepository.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("password1")).willReturn("encoded-password");

        User saved = User.builder()
                .email("user@example.com")
                .passwordHash("encoded-password")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(saved, "id", 1L);
        given(userRepository.save(any(User.class))).willReturn(saved);

        UserResponse response = authService.signup(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("이메일이 중복되면 EMAIL_ALREADY_EXISTS(409)를 던지고 저장하지 않는다")
    void signup_duplicateEmail_throwsEmailAlreadyExists() {
        SignupRequest request = new SignupRequest("dup@example.com", "password1", "tester");
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("올바른 이메일과 비밀번호면 로그인에 성공하고 토큰을 반환한다")
    void login_success() {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("encoded-password")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        LoginRequest request = new LoginRequest("user@example.com", "password1");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1", "encoded-password")).willReturn(true);
        given(jwtTokenProvider.generateAccessToken(1L)).willReturn("access-token");
        given(jwtTokenProvider.getAccessTokenExpirationSeconds()).willReturn(7200L);

        TokenResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200L);
    }

    @Test
    @DisplayName("가입되지 않은 이메일로 로그인하면 INVALID_CREDENTIALS(401)를 던진다")
    void login_emailNotFound_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("missing@example.com", "password1");
        given(userRepository.findByEmail("missing@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(jwtTokenProvider, never()).generateAccessToken(anyLong());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 INVALID_CREDENTIALS(401)를 던지고, 이메일 미가입과 동일한 예외다")
    void login_wrongPassword_throwsInvalidCredentials() {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("encoded-password")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));

        verify(jwtTokenProvider, never()).generateAccessToken(anyLong());
    }
}
