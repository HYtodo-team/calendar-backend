package com.hytodo.backend.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class MigrationSchemaTest {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	@Container
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
			.withDatabaseName("hytodo")
			.withUsername("hytodo")
			.withPassword("1234");

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
	}

	@Test
	void finalMvpColumnsAreApplied() {
		assertColumn("events", "location", "varchar", 255, true);
		assertColumn("events", "memo", "text", null, true);
		assertColumn("events", "is_important", "tinyint", null, false);
		assertMissingColumn("events", "description");
		assertMissingColumn("events", "color");

		assertColumn("todos", "content", "varchar", 255, false);
		assertMissingColumn("todos", "title");
		assertMissingColumn("todos", "description");
		assertMissingColumn("todos", "due_date");
		assertMissingColumn("todos", "display_order");

		assertColumn("timetables", "title", "varchar", 100, false);
		assertColumn("timetables", "semester", "varchar", 50, false);
		assertColumn("timetables", "is_active", "tinyint", null, false);
		assertMissingColumn("timetables", "name");
		assertMissingColumn("timetables", "is_default");

		assertColumn("timetable_entries", "location", "varchar", 255, true);
		assertColumn("timetable_entries", "memo", "text", null, true);
		assertColumn("timetable_entries", "color", "char", 7, true);
	}

	@Test
	void finalMvpIndexesAreApplied() {
		assertMissingIndex("todos", "idx_todos_user_due_date");
		assertIndexExists("todos", "idx_todos_user_completed_created_at");
		assertMissingIndex("timetables", "idx_timetables_user");
		assertIndexExists("timetables", "idx_timetables_user_active");
	}

	private void assertColumn(
			String tableName,
			String columnName,
			String expectedDataType,
			Integer expectedCharacterLength,
			boolean expectedNullable
	) {
		Map<String, Object> column = jdbcTemplate.queryForMap("""
				SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
				FROM information_schema.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND COLUMN_NAME = ?
				""", tableName, columnName);

		assertThat(column.get("DATA_TYPE")).isEqualTo(expectedDataType);
		assertThat(column.get("IS_NULLABLE")).isEqualTo(expectedNullable ? "YES" : "NO");
		if (expectedCharacterLength != null) {
			assertThat(((Number) column.get("CHARACTER_MAXIMUM_LENGTH")).intValue())
					.isEqualTo(expectedCharacterLength);
		}
	}

	private void assertMissingColumn(String tableName, String columnName) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.COLUMNS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND COLUMN_NAME = ?
				""", Integer.class, tableName, columnName);

		assertThat(count).isZero();
	}

	private void assertIndexExists(String tableName, String indexName) {
		assertIndexCount(tableName, indexName, 1);
	}

	private void assertMissingIndex(String tableName, String indexName) {
		assertIndexCount(tableName, indexName, 0);
	}

	private void assertIndexCount(String tableName, String indexName, int minimumCount) {
		Integer count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM information_schema.STATISTICS
				WHERE TABLE_SCHEMA = DATABASE()
				  AND TABLE_NAME = ?
				  AND INDEX_NAME = ?
				""", Integer.class, tableName, indexName);

		if (minimumCount == 0) {
			assertThat(count).isZero();
			return;
		}
		assertThat(count).isGreaterThanOrEqualTo(minimumCount);
	}
}
