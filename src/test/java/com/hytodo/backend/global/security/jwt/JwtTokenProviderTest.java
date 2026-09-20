package com.hytodo.backend.global.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-secret-key-must-be-at-least-256-bits-long-for-hs256";
    private static final long EXPIRATION_SECONDS = 7200L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(new JwtProperties(SECRET, EXPIRATION_SECONDS));
    }

    @Test
    @DisplayName("토큰을 생성하고 검증하면 userId를 그대로 꺼낼 수 있다")
    void generateAndValidate_success() {
        String token = jwtTokenProvider.generateAccessToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("만료 시간을 설정값 그대로 반환한다")
    void expirationSeconds_matchesConfiguredValue() {
        assertThat(jwtTokenProvider.getAccessTokenExpirationSeconds()).isEqualTo(EXPIRATION_SECONDS);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 검증에 실패한다")
    void validateToken_malformedToken_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not-a-valid-jwt")).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void validateToken_expiredToken_returnsFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(past.minusSeconds(7200)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 검증에 실패한다")
    void validateToken_wrongSecret_returnsFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor("other-secret-key-must-be-at-least-256-bits-long-too".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String tokenSignedWithOtherKey = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        assertThat(jwtTokenProvider.validateToken(tokenSignedWithOtherKey)).isFalse();
    }
}
