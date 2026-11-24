
package com.hytodo.backend.domain.todo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TodoUpdateRequest {

    private Long eventId;

    private boolean eventIdProvided = false;

    @Size(max = 255, message = "내용은 255자 이하이어야 합니다.")
    @Pattern(regexp = "(?s).*\\S.*", message = "내용은 공백일 수 없습니다.")
    private String content;

    // JSON 요청은 기본 생성자를 사용하도록 지정
    @JsonCreator
    public TodoUpdateRequest() {
    }

    // Java 테스트 코드에서 직접 객체 생성할 때 사용
    public TodoUpdateRequest(Long eventId, String content) {
        this.eventId = eventId;
        this.content = content;
        this.eventIdProvided = true;
    }

    @JsonSetter("eventId")
    public void setEventId(Long eventId) {
        this.eventId = eventId;
        this.eventIdProvided = true;
    }

    @JsonSetter("content")
    public void setContent(String content) {
        this.content = content;
    }

    public Long eventId() {
        return eventId;
    }

    public String content() {
        return content;
    }

    public boolean eventIdProvided() {
        return eventIdProvided;
    }
}
