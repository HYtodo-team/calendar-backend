package com.hytodo.backend.domain.dailynote.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.hytodo.backend.domain.dailynote.entity.DailyNote;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class DailyNoteRepositoryTest {

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
    private DailyNoteRepository dailyNoteRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        dailyNoteRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(
                User.builder()
                        .email("user@example.com")
                        .passwordHash("encoded-password")
                        .nickname("user")
                        .build()
        );

        otherUser = userRepository.save(
                User.builder()
                        .email("other@example.com")
                        .passwordHash("encoded-password")
                        .nickname("other")
                        .build()
        );
    }

    @Test
    void sameUserAndDateViolatesUniqueConstraint() {
        LocalDate date = LocalDate.of(2026, 9, 7);

        dailyNoteRepository.saveAndFlush(
                DailyNote.builder()
                        .user(user)
                        .noteDate(date)
                        .content("첫 번째 메모")
                        .build()
        );

        assertThatThrownBy(() ->
                dailyNoteRepository.saveAndFlush(
                        DailyNote.builder()
                                .user(user)
                                .noteDate(date)
                                .content("같은 날짜의 다른 메모")
                                .build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameDateDifferentUsersAreIndependent() {
        LocalDate date = LocalDate.of(2026, 9, 7);

        dailyNoteRepository.saveAndFlush(
                DailyNote.builder()
                        .user(user)
                        .noteDate(date)
                        .content("내 메모")
                        .build()
        );

        dailyNoteRepository.saveAndFlush(
                DailyNote.builder()
                        .user(otherUser)
                        .noteDate(date)
                        .content("다른 사용자의 메모")
                        .build()
        );

        Optional<DailyNote> mine = dailyNoteRepository.findByUserIdAndNoteDate(user.getId(), date);
        Optional<DailyNote> theirs = dailyNoteRepository.findByUserIdAndNoteDate(otherUser.getId(), date);

        assertThat(mine).isPresent();
        assertThat(mine.get().getContent()).isEqualTo("내 메모");
        assertThat(theirs).isPresent();
        assertThat(theirs.get().getContent()).isEqualTo("다른 사용자의 메모");
    }
}
