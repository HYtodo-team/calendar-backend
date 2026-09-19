package com.hytodo.backend.global.exception;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class OwnershipValidator {

    private OwnershipValidator() {
    }

    public static <T, ID> T validate(Optional<T> resource, Function<T, ID> ownerIdExtractor,
                                     ID currentUserId, String resourceName) {
        return resource
                .filter(r -> Objects.equals(ownerIdExtractor.apply(r), currentUserId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, resourceName + "을(를) 찾을 수 없습니다."));
    }
}
