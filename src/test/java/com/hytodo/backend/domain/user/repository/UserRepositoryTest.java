package com.hytodo.backend.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hytodo.backend.domain.user.entity.User;
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

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class UserRepositoryTest {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	@Container
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
			.withDatabaseName("hytodo")
			.withUsername("hytodo")
			.withPassword("1234");

	@Autowired
	private UserRepository userRepository;

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
	}

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void saveAndFindByEmail() {
		User user = User.builder()
				.email("user@example.com")
				.passwordHash("encoded-password")
				.nickname("tester")
				.build();

		userRepository.save(user);

		assertThat(userRepository.findByEmail("user@example.com"))
				.isPresent()
				.get()
				.extracting(User::getNickname)
				.isEqualTo("tester");
	}

	@Test
	void existsByEmail() {
		User user = User.builder()
				.email("exists@example.com")
				.passwordHash("encoded-password")
				.nickname("tester")
				.build();

		userRepository.save(user);

		assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
		assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
	}

	@Test
	void emailMustBeUnique() {
		User firstUser = User.builder()
				.email("duplicate@example.com")
				.passwordHash("encoded-password")
				.nickname("first")
				.build();
		User secondUser = User.builder()
				.email("duplicate@example.com")
				.passwordHash("encoded-password")
				.nickname("second")
				.build();

		userRepository.saveAndFlush(firstUser);

		assertThatThrownBy(() -> userRepository.saveAndFlush(secondUser))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
