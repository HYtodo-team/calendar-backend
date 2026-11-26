package com.hytodo.backend.domain.dailynote.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hytodo.backend.domain.dailynote.dto.DailyNoteRequest;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.entity.DailyNote;
import com.hytodo.backend.domain.dailynote.repository.DailyNoteRepository;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

@Service
@Transactional(readOnly = true)
public class DailyNoteService {

    private final DailyNoteRepository dailyNoteRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public DailyNoteService(DailyNoteRepository dailyNoteRepository, UserRepository userRepository) {
        this(dailyNoteRepository, userRepository, Clock.systemDefaultZone());
    }

    // 테스트에서 고정 Clock을 전달할 수 있는 생성자
    DailyNoteService(DailyNoteRepository dailyNoteRepository, UserRepository userRepository, Clock clock) {
        this.dailyNoteRepository = dailyNoteRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // API-NOTE-001: 날짜 메모 조회 (없으면 content:null)
    public DailyNoteResponse getNote(Long userId, LocalDate date) {
        return dailyNoteRepository.findByUserIdAndNoteDate(userId, date)
                .map(DailyNoteResponse::from)
                .orElseGet(() -> DailyNoteResponse.empty(date));
    }

    /**
     * API-NOTE-002: 날짜 메모 작성·수정.
     * 조회 후 분기하는 대신 DB native upsert(INSERT ... ON DUPLICATE KEY UPDATE)를 사용해
     * UNIQUE(user_id, note_date) 위반 없이 동시 PUT 요청도 원자적으로 처리한다.
     */
    @Transactional
    public DailyNoteResponse upsertNote(Long userId, LocalDate date, DailyNoteRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        dailyNoteRepository.upsert(userId, date, request.content(), LocalDateTime.now(clock));

        DailyNote saved = dailyNoteRepository.findByUserIdAndNoteDate(userId, date)
                .orElseThrow(() -> new IllegalStateException("upsert 직후 메모를 찾을 수 없습니다."));

        return DailyNoteResponse.from(saved);
    }

    // API-NOTE-003: 날짜 메모 삭제 (없어도 멱등하게 204)
    @Transactional
    public void deleteNote(Long userId, LocalDate date) {
        dailyNoteRepository.findByUserIdAndNoteDate(userId, date)
                .ifPresent(dailyNoteRepository::delete);
    }
}