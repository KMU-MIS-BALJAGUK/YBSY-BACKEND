# YBSY-BACKEND · 학기예보 API 서버

한 학기 동안 수업·활동·자격증 공부가 몰리는 주를 미리 알려 주는 **학기예보**의 백엔드입니다.
1차 서비스(React + Supabase)를 Spring Boot + MySQL 서버로 옮기며 백엔드를 공부하는 발자국 3기 프로젝트입니다.

| | |
|---|---|
| 언어·프레임워크 | Java 21, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation) |
| DB | MySQL 8, Flyway |
| 인증 | Google OAuth2(직접 구현) + JWT |
| 문서 | springdoc OpenAPI (Swagger UI) |
| 빌드·CI | Gradle, GitHub Actions |

## 시작하기

### 1. 준비물

- JDK 21 (Temurin 권장)
- IntelliJ IDEA — `Settings > Build > Gradle`에서 Gradle JVM을 21로, `Settings > Build > Compiler > Annotation Processors`에서 **Enable annotation processing** 체크 (Lombok)
- MySQL 8: [Docker Desktop](https://www.docker.com/products/docker-desktop/)으로 띄우거나 직접 설치

### 2. 환경변수

```powershell
Copy-Item .env.example .env
```

`.env`에 DB 비밀번호 등을 채웁니다. 실제 값은 팀 채널에서 공유받고, **`.env`는 커밋하지 않습니다.**

### 3. DB 실행

```powershell
docker compose up -d
```

직접 설치한 MySQL을 쓰면 `ybsy` 데이터베이스와 사용자를 만들고 `.env`의 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`를 맞춥니다.

### 4. 서버 실행

```powershell
./gradlew bootRun
```

IntelliJ에서는 `YbsyApplication`을 실행합니다(기본 프로필 `local`, 작업 디렉터리는 프로젝트 루트).

- API 문서: http://localhost:8080/swagger-ui.html
- 상태 확인: http://localhost:8080/actuator/health

### 5. 테스트

```powershell
./gradlew build
```

테스트는 H2 메모리 DB로 돌아가서 MySQL 없이 실행됩니다.

## 문서

- [`AGENTS.md`](AGENTS.md) — 작업 규칙 (사람·AI 공용). 하위 폴더의 `AGENTS.md`가 영역별 규칙
- [`docs/MIGRATION.md`](docs/MIGRATION.md) — 원본 기능 ↔ 이 서버 대응표, 도메인 담당, 인증 설계
- [`.github/AGENTS.md`](.github/AGENTS.md) — 이슈·브랜치·커밋·PR 규칙

## 구조

```
src/main/java/com/baljaguk/ybsy
├── global/   config · common · error · security
└── domain/   auth · user · semester · category · plan · certificate · studylog · ai
src/main/resources/db/migration   Flyway SQL (V1__...)
```
