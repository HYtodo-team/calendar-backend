package com.hytodo.backend.domain.user.entity;

import com.hytodo.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	private static final int MAX_EMAIL_LENGTH = 255;
	private static final int MAX_PASSWORD_HASH_LENGTH = 255;
	private static final int MAX_NICKNAME_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "email", nullable = false, unique = true, length = MAX_EMAIL_LENGTH)
	private String email;

	@Column(name = "password_hash", nullable = false, length = MAX_PASSWORD_HASH_LENGTH)
	private String passwordHash;

	@Column(name = "nickname", nullable = false, length = MAX_NICKNAME_LENGTH)
	private String nickname;

	@Builder
	private User(String email, String passwordHash, String nickname) {
		validateNotBlank(email, "email");
		validateMaxLength(email, MAX_EMAIL_LENGTH, "email");
		validateNotBlank(passwordHash, "passwordHash");
		validateMaxLength(passwordHash, MAX_PASSWORD_HASH_LENGTH, "passwordHash");
		validateNotBlank(nickname, "nickname");
		validateMaxLength(nickname, MAX_NICKNAME_LENGTH, "nickname");
		this.email = email;
		this.passwordHash = passwordHash;
		this.nickname = nickname;
	}

	public void changePasswordHash(String passwordHash) {
		validateNotBlank(passwordHash, "passwordHash");
		validateMaxLength(passwordHash, MAX_PASSWORD_HASH_LENGTH, "passwordHash");
		this.passwordHash = passwordHash;
	}

	public void changeNickname(String nickname) {
		validateNotBlank(nickname, "nickname");
		validateMaxLength(nickname, MAX_NICKNAME_LENGTH, "nickname");
		this.nickname = nickname;
	}

	private static void validateNotBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

	private static void validateMaxLength(String value, int maxLength, String fieldName) {
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must be less than or equal to " + maxLength + " characters");
		}
	}
}