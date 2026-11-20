# YBSY(학기예보) Server 작업 가이드

학기예보는 Supabase(PostgreSQL·Auth·Edge Function)로 만든 서비스를 **Spring Boot + MySQL 서버로 옮기며 백엔드를 공부하는** 발자국 3기 프로젝트다. 백엔드 3명, 프론트 1명.

- 스택: Java 21, Spring Boot 4.1, Gradle, Spring Data JPA, Flyway, MySQL 8, Spring Security(Google OAuth2 직접 구현 + JWT), springdoc(Swagger)
- 기준 문서: 요구사항 명세서(팀 공유 xlsx), [`docs/MIGRATION.md`](docs/MIGRATION.md) — 원본 기능·테이블·함수와 이 서버의 대응표, 도메인 담당

## 공통 원칙

- 이슈 범위를 벗어난 리팩터링·설계 변경은 별도 이슈로 분리하고, 변경은 검증 가능한 범위에서 최소화한다.
- 작업 대상 경로에 더 가까운 `AGENTS.md`가 이 문서보다 우선한다. 문서와 코드가 충돌하거나 규칙 변경이 필요하면 변경 범위를 PR에서 함께 검토한다.
- 이 문서의 규칙과 코드의 기존 패턴은 안전한 기본값이자 최소 기준이며, 설계의 상한선은 아니다. 더 나은 대안이 있으면 문제·대안·영향 범위·검증 방법을 이슈나 PR에 명확히 제시하고 합의 후 적용할 수 있다.
- API 키, 비밀번호, 토큰, 서버 주소 등 민감정보를 코드·문서·로그에 하드코딩하지 않는다. 값은 환경변수(로컬은 `.env`, 커밋 금지)로만 받고, 새 변수를 추가하면 `.env.example`에 이름과 설명을 함께 추가한다.
- 원본 동작을 옮길 때는 원본(Supabase SQL 함수·트리거·RLS 정책)이 지키던 규칙을 빠뜨리지 않는다. 옮긴 위치와 상태는 `docs/MIGRATION.md` 대응표에 갱신한다.
- 공부가 목적인 프로젝트다. AI 에이전트는 코드를 대신 쓰기 전에 왜 그렇게 하는지 설명하고, 사용자가 직접 구현하겠다고 하면 설명·리뷰 위주로 돕는다.

## 작업별 안내

| 작업 유형 | 확인할 문서 |
|---|---|
| 운영 Java 코드, API, 설정, 보안, 영속성 | `src/main/java/com/baljaguk/ybsy/AGENTS.md` |
| 도메인 Controller·Service·Repository·Entity·DTO | `src/main/java/com/baljaguk/ybsy/domain/AGENTS.md` 및 더 가까운 특수 문서 |
| DB 스키마 변경 (Flyway) | `src/main/resources/db/migration/README.md` + 운영 코드 문서의 영속성 절 |
| 테스트 작성·수정·실행 | `src/test/java/com/baljaguk/ybsy/AGENTS.md` |
| 이슈·PR·브랜치·CI/CD·GitHub 워크플로우 | `.github/AGENTS.md` |

루트 문서는 모든 세부 점검을 강제하지 않는다. 해당 작업 유형의 문서만 읽고 적용한다.

## 자주 쓰는 명령

```bash
./gradlew build          # 컴파일 + 전체 테스트 (PR 전 필수)
./gradlew test           # 테스트만
./gradlew bootRun        # 로컬 실행 (local 프로필, .env 필요)
docker compose up -d     # 로컬 MySQL 실행
```
