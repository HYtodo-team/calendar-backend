package com.hytodo.backend.domain.timetable.repository;

import java.util.List;
import java.util.Optional;

import com.hytodo.backend.domain.timetable.entity.Timetable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

	List<Timetable> findAllByUserId(Long userId, Sort sort);

	Optional<Timetable> findByIdAndUserId(Long id, Long userId);

	List<Timetable> findAllByUserIdAndActiveTrue(Long userId);
}
