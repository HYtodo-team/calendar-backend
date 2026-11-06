
package com.hytodo.backend.domain.todo.entity;

import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.global.entity.BaseTimeEntity;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Todo 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Todo에 연결된 일정 (연결하지 않아도 됨)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "content", nullable = false, length = 255)
    private String content;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Builder
    public Todo(User user, Event event, String content, Boolean isCompleted) {
        validateContent(content);

        this.user = user;
        this.event = event;
        this.content = content;
        this.isCompleted = isCompleted != null && isCompleted;
    }

    // Todo 내용 수정 (null이면 기존 내용 유지)
    public void updateContent(String content) {
        if (content != null) {
            validateContent(content);
            this.content = content;
        }
    }

    // Todo 완료 상태 변경
    public void updateCompletion(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    // Todo와 Event 연결 또는 연결 해제
    public void updateEvent(Event event) {
        this.event = event;
    }

    // 내용이 비어 있거나 255자를 초과하면 오류 발생
    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "투두 내용은 비어 있을 수 없습니다."
            );
        }

        if (content.length() > 255) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "투두 내용은 255자 이하이어야 합니다."
            );
        }
    }
}
