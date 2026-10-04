package com.hytodo.backend.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Hibernate가 시간 값을 DB에 보낼 때 시간대 변환 없이 그대로 보내는지 검증한다 (#20).
 * JPA로 쓰고 JPA로 읽으면 변환이 되돌려져 버그가 가려지므로, DB가 받은 값을 문자열로 직접 확인한다.
 */
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@Transactional
class TimeZoneBindingTest {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	@Container
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
			.withDatabaseName("hytodo")
			.withUsername("hytodo")
			.withPassword("1234");

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
	}

	@Test
	void jvmTimeZoneIsSeoul() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
	}

	@Test
	void localTimeParameterReachesDatabaseUnchanged() {
		Object received = entityManager.createNativeQuery("SELECT CAST(:time AS CHAR)")
				.setParameter("time", LocalTime.of(8, 0))
				.getSingleResult();

		assertThat(received).isEqualTo("08:00:00");
	}

	@Test
	void createdAtIsStoredInJvmLocalTime() {
		User user = userRepository.saveAndFlush(User.builder()
				.email("timezone@example.com")
				.passwordHash("encoded-password")
				.nickname("tester")
				.build());

		String rawCreatedAt = (String) entityManager
				.createNativeQuery("SELECT CAST(created_at AS CHAR) FROM users WHERE id = :id")
				.setParameter("id", user.getId())
				.getSingleResult();

		assertThat(LocalDateTime.parse(rawCreatedAt.replace(' ', 'T')))
				.isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
	}
}
