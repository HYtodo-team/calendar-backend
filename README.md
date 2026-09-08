# HYtodo Backend

하이엔드 미니프로젝트팀 HYtodo의 백엔드 저장소입니다. Spring Boot 기반 일정 관리 REST API 서버로 시작하며, 데이터베이스는 MySQL 8.4와 Flyway Migration을 기본으로 사용합니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.1
- MySQL 8.4

## 프로젝트 구조

백엔드는 도메인별 패키지 구조를 사용합니다. 자세한 패키지 구조와 Entity 작성 원칙은 `docs/backend-conventions.md`에서 관리합니다.

```text
src/main/java/com/hytodo/backend
├── domain
│   ├── user
│   ├── event
│   ├── todo
│   ├── dailynote
│   └── timetable
└── global
    ├── config
    ├── entity
    ├── exception
    ├── health
    ├── response
    └── security
```

## 브랜치

- `main`: 검증이 끝난 안정 버전
- `develop`: 기능 PR이 합쳐지는 개발 통합 브랜치
- `feature/{이슈번호}-{기능명}`: 기능 개발
- `fix/{이슈번호}-{버그명}`: 버그 수정
- `refactor/{이슈번호}-{기능명}`: 리팩터링

모든 기능 브랜치는 `develop`에서 만들고, PR과 1명 이상의 리뷰 승인을 거쳐 `develop`에 병합합니다.

## 커밋 컨벤션

```text
feat: 일정 생성 API 구현 (#12)
fix: 로그인 예외 처리 수정 (#18)
docs: README 실행 방법 추가 (#3)
chore: CI 설정 추가 (#1)
```

## 로컬 실행

1. 환경 변수 예시 파일을 복사합니다.

```bash
cp .env.example .env
```

2. MySQL을 실행합니다.

```bash
docker compose up -d
```

3. 애플리케이션을 실행합니다.

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat bootRun
```

## 테스트와 빌드

```bash
./gradlew test
./gradlew build
```

Windows PowerShell에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

테스트는 Testcontainers로 MySQL 8.4 컨테이너를 사용하므로 Docker Desktop이 실행 중이어야 합니다.

## 환경 변수

| 이름 | 설명 | 기본값 |
| --- | --- | --- |
| `MYSQL_DATABASE` | Docker MySQL 데이터베이스 | `hytodo` |
| `MYSQL_USER` | Docker MySQL 사용자 | `hytodo` |
| `MYSQL_PASSWORD` | Docker MySQL 일반 사용자 비밀번호 | `1234` |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 비밀번호 | `change-root-password` |
| `SERVER_PORT` | Spring Boot 서버 포트 | `8080` |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/hytodo` |
| `DB_USERNAME` | MySQL 사용자 | `hytodo` |
| `DB_PASSWORD` | Spring Boot DB 접속 비밀번호 | `1234` |
| `JWT_SECRET` | JWT 서명 키 | `change-to-a-long-random-secret` |

실제 비밀번호, 토큰, API 키는 코드와 README에 작성하지 않고 로컬 `.env` 또는 GitHub Secrets에만 저장합니다.

8080 포트가 이미 사용 중이면 `.env`에서 `SERVER_PORT`를 다른 값으로 바꾼 뒤 다시 실행합니다.

```env
SERVER_PORT=8081
```

## 데이터베이스 마이그레이션

초기 스키마는 아래 파일에서 관리합니다.

```text
src/main/resources/db/migration/V1__create_initial_schema.sql
```

현재 포함된 테이블은 `users`, `events`, `todos`, `daily_notes`, `timetables`, `timetable_entries`입니다.

## 상태 확인

서버 실행 후 아래 주소로 기본 상태를 확인할 수 있습니다.

```text
GET /api/health
```

응답 예시:

```json
{
  "status": "ok"
}
```