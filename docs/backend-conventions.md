# Backend Conventions

## Package Structure

HYtodo 백엔드는 도메인별 패키지 구조를 사용합니다. Controller, Service, Repository를 계층별 최상위 폴더에 모으지 않고 기능 도메인 안에 함께 둡니다.

```text
src/main/java/com/hytodo/backend
├── domain
│   ├── user
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   ├── event
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   ├── todo
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   ├── dailynote
│   │   ├── controller
│   │   ├── dto
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   └── timetable
│       ├── controller
│       ├── dto
│       ├── entity
│       ├── repository
│       └── service
└── global
    ├── config
    ├── entity
    ├── exception
    ├── health
    ├── response
    └── security
```

## Domain And Table Mapping

| Domain | Tables |
| --- | --- |
| `user` | `users` |
| `event` | `events` |
| `todo` | `todos` |
| `dailynote` | `daily_notes` |
| `timetable` | `timetables`, `timetable_entries` |

`TimetableEntry`는 `Timetable`에 종속되므로 `timetable/entity` 안에 함께 둡니다.

## Entity Rules

- Entity에 `@Setter`를 사용하지 않습니다.
- Entity에 `@Data`를 사용하지 않습니다.
- 기본 생성자는 `protected`로 제한합니다.
- Lombok 사용 시 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용합니다.
- 연관관계 fetch 전략은 기본적으로 `LAZY`를 사용합니다.
- Controller에서 Entity를 직접 반환하지 않습니다.
- Request/Response DTO를 별도로 작성합니다.
- 상태 변경은 setter가 아니라 의미 있는 메서드로 수행합니다.
- 테이블명과 컬럼명을 명시적으로 작성합니다.
- 날짜/시간은 용도에 맞게 `LocalDate`, `LocalTime`, `LocalDateTime`을 구분합니다.
- 생성/수정 시간이 필요한 Entity는 `BaseTimeEntity`를 상속합니다.
- DB 제약 조건과 Entity 필드 길이를 맞춥니다.
- 도메인 핵심 필드는 생성자 또는 상태 변경 메서드에서 최소한의 유효성을 검증합니다.

## Entity Example

아래는 핵심 구조 예시입니다. 검증 메서드까지 포함한 전체 구현은 [User.java](../src/main/java/com/hytodo/backend/domain/user/entity/User.java)를 참고합니다.

```java
@Entity
@Table(name = "users")
@Getter
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
}
```

## Repository Rules

- Repository는 각 도메인의 `repository` 패키지에 둡니다.
- 기본 CRUD는 `JpaRepository<Entity, Long>`를 사용합니다.
- 이메일, 외부 식별자처럼 중복 확인이 필요한 필드는 `existsBy...` 메서드를 함께 둡니다.
- 조회 실패 처리가 필요한 단건 조회는 `Optional<Entity>`를 반환합니다.

## Common Entity

공통 생성/수정 시각은 `global/entity/BaseTimeEntity.java`에서 관리합니다. 메인 클래스에는 `@EnableJpaAuditing`을 적용합니다.
