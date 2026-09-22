package com.hytodo.backend.domain.todo.entity;

import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.global.entity.BaseTimeEntity;

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
    @Column(name = "id") // todo_id -> id 로 변경
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // TODO: Event 엔티티 구현 후 주석 해제 예정
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "event_id")
    // private Event event;

    @Column(name = "title", nullable = false, length = 255) // content를 title 컬럼과 매핑
    private String content;

    @Column(name = "description") // Flyway 스키마의 description 컬럼 추가
    private String description;

    @Column(name = "is_completed", nullable = false)
    private boolean isCompleted;

    @Builder
    public Todo(User user, String content, Boolean isCompleted) {
        this.user = user;
        this.content = content;
        this.isCompleted = isCompleted != null && isCompleted;
    }

    public void updateContent(String content) {
        if (content != null) {
            this.content = content;
        }
    }

    public void updateCompletion(boolean isCompleted) {
        this.isCompleted = isCompleted;
    }
}