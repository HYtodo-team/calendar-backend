# HYtodo Backend

하이엔드 미니프로젝트팀 HYtodo의 백엔드 저장소입니다. Spring Boot 기반 일정 관리 REST API 서버로 시작하며, 데이터베이스는 MySQL 8.4와 Flyway Migration을 기본으로 사용합니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.1
- MySQL 8.4
- Gradle Wrapper, Spring Data JPA, Flyway, Testcontainers

## 현재 구현 범위

공통 응답·예외 처리, Health API, 초기 DB 스키마, 회원 Entity/Repository와 통합 테스트가 준비되어 있습니다. 일정·투두·일일 메모·시간표는 패키지와 테이블만 준비되어 있으며, 각 담당자가 Entity부터 기능을 구현합니다.

현재 `SecurityConfig`는 개발 초기 설정으로 모든 요청을 허용합니다. 회원가입·로그인·JWT·사용자별 접근 제어와 CORS는 후속 이슈에서 구현해야 합니다. `JWT_SECRET`은 아직 사용하지 않는 예시 변수이며, 현재 프로젝트는 로컬 개발 시작용입니다.

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

### 실행 전 준비

Git, JDK 21, Docker Desktop(또는 Docker Engine과 Compose v2)이 필요합니다. Gradle과 MySQL은 별도로 설치하지 않습니다. Docker가 Linux 컨테이너를 실행할 수 있는 상태인지 확인합니다.

```bash
java --version
docker info
docker compose version
```

IntelliJ의 Project SDK와 Gradle JVM도 JDK 21로 맞춥니다. IDE에서는 `build.gradle`이 있는 프로젝트 루트를 열고, 실행 구성의 Working directory도 프로젝트 루트로 설정합니다. Spring Boot는 해당 위치의 `.env`를 자동으로 읽습니다.

### 처음 내려받기

초기 세팅 PR이 `develop`에 병합된 뒤 다음 명령으로 내려받습니다.

```bash
git clone --branch develop https://github.com/HYtodo-team/calendar-backend.git
cd calendar-backend
```

### 실행 순서

Spring Boot는 IDE 또는 로컬 Java에서, MySQL만 Docker에서 실행합니다. 각 팀원의 DB와 데이터는 본인 PC에 따로 저장됩니다.

1. 환경 변수 예시 파일을 복사합니다. 이미 `.env`가 있으면 기존 설정을 유지합니다.

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

2. MySQL을 실행하고 접속 준비가 끝날 때까지 기다립니다.

```bash
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

3. 아래 '테스트와 빌드' 명령이 성공하는지 확인한 뒤 애플리케이션을 실행합니다.

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

처음 검증할 때는 `./gradlew clean build` 또는 Windows에서 `.\gradlew.bat clean build`를 실행합니다.

테스트는 Testcontainers로 별도의 MySQL 8.4 컨테이너를 만들고 임의의 호스트 포트를 사용합니다. Docker가 실행 중이어야 하며, 사용할 수 없으면 테스트가 실패합니다. Compose의 개발 DB나 `.env`의 DB 접속 값은 테스트에 사용하지 않습니다. 테스트가 끝나면 테스트 컨테이너는 정리되고 개발 DB 데이터는 유지됩니다.

## 환경 변수

| 이름 | 설명 | 기본값 |
| --- | --- | --- |
| `MYSQL_DATABASE` | Docker MySQL 데이터베이스 | `hytodo` |
| `MYSQL_USER` | Docker MySQL 사용자 | `hytodo` |
| `MYSQL_PASSWORD` | Docker MySQL 일반 사용자 비밀번호 | `1234` |
| `MYSQL_ROOT_PASSWORD` | Docker MySQL root 비밀번호 | `change-root-password` |
| `MYSQL_PORT` | 로컬 PC에서 MySQL에 접속할 포트 | `3306` |
| `SERVER_PORT` | Spring Boot 서버 포트 | `8080` |
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/hytodo` |
| `DB_USERNAME` | MySQL 사용자 | `hytodo` |
| `DB_PASSWORD` | Spring Boot DB 접속 비밀번호 | `1234` |
| `JWT_SECRET` | JWT 구현 시 사용할 예시 키(현재 미사용) | `change-to-a-long-random-secret` |

실제 비밀번호, 토큰, API 키는 코드와 README에 작성하지 않고 로컬 `.env` 또는 GitHub Secrets에만 저장합니다.

`.env.example`의 비밀번호는 로컬 개발용 예시입니다. `.env`는 개인 설정이므로 Git에서 제외하고 `.env.example`만 공유합니다. `MYSQL_USER`와 `DB_USERNAME`, `MYSQL_PASSWORD`와 `DB_PASSWORD`는 서로 맞춰야 합니다. DB 이름을 변경하면 `MYSQL_DATABASE`와 `DB_URL`의 마지막 DB 이름도 함께 변경합니다.

8080 포트가 이미 사용 중이면 `.env`에서 `SERVER_PORT`를 다른 값으로 바꾼 뒤 다시 실행합니다.

```env
SERVER_PORT=8081
```

MySQL 3306 포트가 이미 사용 중이면 `.env`의 두 값을 함께 바꾼 뒤 `docker compose up -d --wait --wait-timeout 180`으로 반영합니다. 컨테이너 내부 포트는 3306을 유지합니다.

```env
MYSQL_PORT=3307
DB_URL=jdbc:mysql://localhost:3307/hytodo
```

MySQL은 로컬 PC의 `127.0.0.1`에서만 접속하도록 설정되어 있습니다. DB 관리 도구에서는 호스트 `127.0.0.1`, 포트 `MYSQL_PORT`, DB `MYSQL_DATABASE`, 계정 `MYSQL_USER`, 비밀번호 `MYSQL_PASSWORD`를 사용합니다.

### DB 확인과 종료

```bash
docker compose exec mysql mysql -u hytodo -p hytodo
```

비밀번호는 `.env`의 `MYSQL_PASSWORD` 값입니다. 서버를 한 번 실행한 뒤 다음 명령으로 테이블을 확인합니다.

```sql
SHOW TABLES;
SELECT version, description, success FROM flyway_schema_history;
```

Spring Boot는 터미널의 Ctrl+C 또는 IDE의 중지 버튼으로 종료하고, DB는 `docker compose stop`으로 중지합니다. 컨테이너를 정리하려면 `docker compose down`을 사용합니다. DB 데이터는 볼륨에 남지만, `docker compose down -v`는 DB 데이터까지 삭제하므로 일반 종료에 사용하지 않습니다.

`Access denied`가 발생하면 `.env`의 계정 값과 실제 DB 계정을 확인합니다. MySQL 이미지의 `MYSQL_*` 초기화 값은 빈 데이터 볼륨에서 처음 실행할 때만 적용되므로, 기존 DB의 비밀번호는 `.env` 수정이나 컨테이너 재시작만으로 바뀌지 않습니다. 기존 계정으로 접속해 비밀번호를 변경한 뒤 `.env`도 맞춥니다. 자세한 동작은 [MySQL 이미지 초기화 안내](https://hub.docker.com/_/mysql)를 참고합니다.

## 데이터베이스 마이그레이션

초기 스키마는 아래 파일에서 관리합니다.

```text
src/main/resources/db/migration/V1__create_initial_schema.sql
```

현재 포함된 테이블은 `users`, `events`, `todos`, `daily_notes`, `timetables`, `timetable_entries`입니다.

Spring Boot 시작 시 Flyway가 스키마를 적용하고 JPA의 `ddl-auto: validate`가 작성된 Entity 매핑을 검사합니다. 이 시점에 `flyway_schema_history`도 생성됩니다.

공유되었거나 적용된 `V1__create_initial_schema.sql`은 수정하지 않습니다. 스키마 변경은 `V2__설명.sql`처럼 다음 버전 파일로 추가하고 Entity도 함께 맞춥니다. 버전 번호가 다른 브랜치와 겹치지 않도록 PR 전에 확인합니다. DB 관리 도구에서만 스키마를 수정하거나 `ddl-auto`를 `update`로 바꾸지 않습니다.

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

기본 주소는 `http://localhost:8080/api/health`입니다. DB 연결을 포함한 상태는 `http://localhost:8080/actuator/health`의 `UP` 응답으로 확인합니다.

## 기능 개발 시작

초기 세팅 PR 병합 후 이슈를 배정하고 최신 `develop`에서 작업 브랜치를 만듭니다. 아래 이슈 번호와 기능명은 실제 배정받은 값으로 바꿉니다.

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/12-event-create
```

작업 후 테스트를 통과시키고 `develop` 대상으로 PR을 작성합니다. 저장소의 Backend CI는 `main`·`develop` 대상 PR과 해당 브랜치의 push에서 테스트와 빌드를 실행합니다. 브랜치 보호, 필수 CI 검사, 리뷰 승인 1명 이상 설정은 팀장이 GitHub 저장소 설정에서 관리합니다.
