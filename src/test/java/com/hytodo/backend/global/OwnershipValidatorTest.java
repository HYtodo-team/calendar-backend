package com.hytodo.backend.global;

import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import com.hytodo.backend.global.exception.OwnershipValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OwnershipValidatorTest {
    private record FakeEvent(Long id, Long userId) {}

    @Test
    void ownerMatches_returnsResource() {
        FakeEvent event = new FakeEvent(1L, 100L);
        FakeEvent result = OwnershipValidator.validate(Optional.of(event), FakeEvent::userId, 100L, "일정");
        assertThat(result).isEqualTo(event);
    }

    @Test
    void resourceMissing_throwsResourceNotFound() {
        assertThatThrownBy(() ->
                OwnershipValidator.validate(Optional.<FakeEvent>empty(), FakeEvent::userId, 100L, "일정"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void ownerMismatch_throwsSameExceptionAsMissing() {
        FakeEvent event = new FakeEvent(1L, 999L); // 다른 사용자 소유
        assertThatThrownBy(() ->
                OwnershipValidator.validate(Optional.of(event), FakeEvent::userId, 100L, "일정"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("소유자 ID가 null인 리소스는 NPE 없이 ResourceNotFoundException(404)을 던진다")
    void nullOwnerId_throwsResourceNotFound_notNpe() {
        FakeEvent eventWithNullOwner = new FakeEvent(1L, null);

        assertThatThrownBy(() ->
                OwnershipValidator.validate(Optional.of(eventWithNullOwner), FakeEvent::userId, 100L, "일정"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
    }