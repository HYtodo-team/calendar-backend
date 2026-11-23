package com.hytodo.backend.domain.dailynote.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hytodo.backend.domain.dailynote.dto.DailyNoteRequest;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.entity.DailyNote;
import com.hytodo.backend.domain.dailynote.repository.DailyNoteRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyNoteService {

    private final DailyNoteRepository dailyNoteRepository;
    private final UserRepository userRepository;

    // API-NOTE-001: 날짜 메모 조회 (없으면 content:null)
    public DailyNoteResponse getNote(Long userId, LocalDate date) {
        return dailyNoteRepository.findByUserIdAndNoteDate(userId, date)
                .map(DailyNoteResponse::from)
                .orElseGet(() -> DailyNoteResponse.empty(date));
    }

    // API-NOTE-002: 날짜 메모 작성·수정 ((userId, date) 기준 upsert)
    @Transactional
    public DailyNoteResponse upsertNote(Long userId, LocalDate date, DailyNoteRequest request) {
        DailyNote existing = dailyNoteRepository.findByUserIdAndNoteDate(userId, date)
                .orElse(null);

        if (existing != null) {
            existing.changeContent(request.content());
            return DailyNoteResponse.from(existing);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        DailyNote created = DailyNote.builder()
                .user(user)
                .noteDate(date)
                .content(request.content())
                .build();

        DailyNote saved = dailyNoteRepository.save(created);
        return DailyNoteResponse.from(saved);
    }

    // API-NOTE-003: 날짜 메모 삭제 (없어도 멱등하게 204)
    @Transactional
    public void deleteNote(Long userId, LocalDate date) {
        dailyNoteRepository.findByUserIdAndNoteDate(userId, date)
                .ifPresent(dailyNoteRepository::delete);
    }
}
