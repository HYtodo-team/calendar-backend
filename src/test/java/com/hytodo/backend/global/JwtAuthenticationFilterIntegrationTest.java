package com.hytodo.backend.global.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtAuthenticationFilterIntegrationTest {

    private static final String TEST_JWT_SECRET = "test-only-secret-key-must-be-at-least-256-bits-long-for-hs256";

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

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

    @LocalServerPort
    private int port;

    @Autowired
    private com.hytodo.backend.global.security.jwt.JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("토큰 없이 보호된 API를 호출하면 401을 반환한다")
    void protectedApi_withoutToken_returns401() throws Exception {
        assertThat(callWithToken(null)).isEqualTo(401);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰으로 보호된 API를 호출하면 401을 반환한다")
    void protectedApi_withMalformedToken_returns401() throws Exception {
        assertThat(callWithToken("not-a-valid-jwt")).isEqualTo(401);
    }

    @Test
    @DisplayName("만료된 토큰으로 보호된 API를 호출하면 401을 반환한다")
    void protectedApi_withExpiredToken_returns401() throws Exception {
        assertThat(callWithToken(generateExpiredToken())).isEqualTo(401);
    }

    @Test
    @DisplayName("유효한 토큰이면 인증 필터를 통과한다")
    void validToken_passesAuthenticationFilter() throws Exception {
        String validToken = jwtTokenProvider.generateAccessToken(1L);
        assertThat(callWithToken(validToken)).isNotEqualTo(401);
    }

    @Test
    @DisplayName("signup/login/health는 토큰 없이 호출 가능하다")
    void publicEndpoints_accessibleWithoutToken() throws Exception {
        assertThat(postWithoutToken("/api/v1/auth/signup", "{}")).isNotEqualTo(401);
        assertThat(postWithoutToken("/api/v1/auth/login", "{}")).isNotEqualTo(401);
        assertThat(callWithToken(null, "/api/health")).isNotEqualTo(401);
    }

    private int callWithToken(String token) throws Exception {
        return callWithToken(token, "/api/v1/timetables");
    }

    private int callWithToken(String token, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        }
    }

    private int postWithoutToken(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        }
    }

    private String generateExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        return Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(past.minusSeconds(7200)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();
    }
}
