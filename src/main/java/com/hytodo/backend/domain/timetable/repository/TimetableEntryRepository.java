package com.hytodo.backend.domain.timetable.repository;

import java.util.List;
import java.util.Optional;

import com.hytodo.backend.domain.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

	List<TimetableEntry> findAllByTimetableIdOrderByDayOfWeekAscStartTimeAsc(Long timetableId);

	Optional<TimetableEntry> findByIdAndTimetableId(Long id, Long timetableId);
}
