package com.hytodo.backend.domain.dailynote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import com.hytodo.backend.domain.dailynote.dto.DailyNoteRequest;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.entity.DailyNote;
import com.hytodo.backend.domain.dailynote.repository.DailyNoteRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

class DailyNoteServiceTest {

    private final DailyNoteRepository dailyNoteRepository = Mockito.mock(DailyNoteRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);

    private final DailyNoteService dailyNoteService =
            new DailyNoteService(dailyNoteRepository, userRepository);

    @Test
    @DisplayName("메모가 없는 날짜를 조회하면 content:null을 반환한다")
    void getNote_noExistingNote_returnsNullContent() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.empty());

        DailyNoteResponse response = dailyNoteService.getNote(userId, date);

        assertThat(response.id()).isNull();
        assertThat(response.noteDate()).isEqualTo(date);
        assertThat(response.content()).isNull();
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    @DisplayName("메모가 있는 날짜를 조회하면 저장된 내용을 반환한다")
    void getNote_existingNote_returnsContent() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        DailyNote existing = DailyNote.builder()
                .user(userOf(userId))
                .noteDate(date)
                .content("오늘의 메모")
                .build();

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.of(existing));

        DailyNoteResponse response = dailyNoteService.getNote(userId, date);

        assertThat(response.content()).isEqualTo("오늘의 메모");
        assertThat(response.noteDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("메모가 없는 날짜에 PUT하면 새로 생성한다")
    void upsertNote_createsNewNote_whenNoneExists() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(userOf(userId)));
        given(dailyNoteRepository.saveAndFlush(any(DailyNote.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        DailyNoteResponse response = dailyNoteService.upsertNote(userId, date, new DailyNoteRequest("새 메모"));

        assertThat(response.content()).isEqualTo("새 메모");
        assertThat(response.noteDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("동시 요청으로 유니크 제약을 위반하면 재조회 후 기존 메모를 수정한다")
    void upsertNote_concurrentCreate_fallsBackToUpdate() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        DailyNote concurrentlyCreated = DailyNote.builder()
                .user(userOf(userId))
                .noteDate(date)
                .content("동시 요청으로 먼저 생성된 메모")
                .build();

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(concurrentlyCreated));
        given(userRepository.findById(userId)).willReturn(Optional.of(userOf(userId)));
        given(dailyNoteRepository.saveAndFlush(any(DailyNote.class)))
                .willThrow(new DataIntegrityViolationException("unique constraint violated"));

        DailyNoteResponse response =
                dailyNoteService.upsertNote(userId, date, new DailyNoteRequest("내가 보낸 메모"));

        assertThat(response.content()).isEqualTo("내가 보낸 메모");
        Mockito.verify(dailyNoteRepository, Mockito.times(2)).findByUserIdAndNoteDate(userId, date);
        Mockito.verify(dailyNoteRepository, Mockito.never()).save(any());
    }

    @Test
    @DisplayName("메모가 있는 날짜에 PUT하면 기존 메모 내용을 수정하고 flush 이후의 updatedAt을 반환한다")
    void upsertNote_updatesExistingNote_whenPresent() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        DailyNote existing = DailyNote.builder()
                .user(userOf(userId))
                .noteDate(date)
                .content("기존 메모")
                .build();

        LocalDateTime beforeFlush = LocalDateTime.of(2026, 9, 7, 10, 0);
        LocalDateTime afterFlush = LocalDateTime.of(2026, 9, 7, 10, 5);
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "updatedAt", beforeFlush);

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.of(existing));
        // flush 시점에 감사 리스너가 updatedAt을 갱신하는 것을 시뮬레이션한다.
        Mockito.doAnswer(invocation -> {
            org.springframework.test.util.ReflectionTestUtils.setField(existing, "updatedAt", afterFlush);
            return null;
        }).when(dailyNoteRepository).flush();

        DailyNoteResponse response = dailyNoteService.upsertNote(userId, date, new DailyNoteRequest("수정된 메모"));

        assertThat(response.content()).isEqualTo("수정된 메모");
        assertThat(response.updatedAt()).isEqualTo(afterFlush);
        Mockito.verify(dailyNoteRepository, Mockito.never()).save(any());
        Mockito.verify(userRepository, Mockito.never()).findById(any());
        Mockito.verify(dailyNoteRepository).flush();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 메모를 생성하면 RESOURCE_NOT_FOUND(404)를 던진다")
    void upsertNote_userNotFound_throwsResourceNotFound() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> dailyNoteService.upsertNote(userId, date, new DailyNoteRequest("메모")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하는 메모를 삭제하면 repository.delete가 호출된다")
    void deleteNote_removesExisting() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        DailyNote existing = DailyNote.builder()
                .user(userOf(userId))
                .noteDate(date)
                .content("삭제될 메모")
                .build();

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.of(existing));

        dailyNoteService.deleteNote(userId, date);

        Mockito.verify(dailyNoteRepository).delete(existing);
    }

    @Test
    @DisplayName("존재하지 않는 메모를 삭제해도 예외 없이 멱등하게 처리된다")
    void deleteNote_noExisting_doesNothing() {
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 9, 7);

        given(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatCode(() -> dailyNoteService.deleteNote(userId, date))
                .doesNotThrowAnyException();

        Mockito.verify(dailyNoteRepository, Mockito.never()).delete(any());
    }

    private User userOf(Long id) {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("encoded-password")
                .nickname("tester")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}