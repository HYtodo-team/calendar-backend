package com.hytodo.backend.domain.dailynote.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.hytodo.backend.domain.dailynote.dto.DailyNoteRequest;
import com.hytodo.backend.domain.dailynote.repository.DailyNoteRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

/**
 * 같은 (userId, date)에 동시에 PUT이 들어와도 UNIQUE(user_id, note_date) 위반 없이
 * 메모가 정확히 1개만 저장되는지 실제 MySQL(Testcontainers)로 검증한다.
 */
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class DailyNotePutConcurrencyIntegrationTest {

    private static final DockerImageName MYSQL_IMAGE =
            DockerImageName.parse("mysql:8.4");

    @Container
    static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("hytodo")
            .withUsername("hytodo")
            .withPassword("1234");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private DailyNoteService dailyNoteService;

    @Autowired
    private DailyNoteRepository dailyNoteRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        dailyNoteRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(
                User.builder()
                        .email("concurrency@example.com")
                        .passwordHash("encoded-password")
                        .nickname("tester")
                        .build()
        );
    }

    @Test
    void concurrentPutsOnSameDateCreateExactlyOneNote() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);
        Long userId = user.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        try {
            Future<?> first = executor.submit(() -> {
                awaitBarrier(barrier);
                dailyNoteService.upsertNote(userId, date, new DailyNoteRequest("요청 A"));
            });
            Future<?> second = executor.submit(() -> {
                awaitBarrier(barrier);
                dailyNoteService.upsertNote(userId, date, new DailyNoteRequest("요청 B"));
            });

            first.get();
            second.get();
        } finally {
            executor.shutdown();
        }

        List<com.hytodo.backend.domain.dailynote.entity.DailyNote> all = dailyNoteRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getContent()).isIn("요청 A", "요청 B");
        assertThat(dailyNoteRepository.findByUserIdAndNoteDate(userId, date)).isPresent();
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
