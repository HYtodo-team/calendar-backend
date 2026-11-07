package com.hytodo.backend.domain.user.service;

import java.util.Optional;

import com.hytodo.backend.domain.user.dto.UserResponse;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("인증된 사용자의 프로필을 조회하면 id/email/nickname/createdAt을 반환한다")
    void getMyProfile_success() {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("encoded-password")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.getMyProfile(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("tester");
        assertThat(response.createdAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 RESOURCE_NOT_FOUND(404)를 던진다")
    void getMyProfile_userNotFound_throwsResourceNotFound() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
