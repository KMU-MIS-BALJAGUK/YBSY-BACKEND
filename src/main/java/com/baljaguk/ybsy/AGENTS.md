# 운영 코드 가이드 (API · 설정 · 보안 · 영속성)

## 패키지 구조

```
com.baljaguk.ybsy
├── global/                 # 여러 도메인이 같이 쓰는 것만
│   ├── config/             # JPA Auditing, OpenAPI, CORS 설정값
│   ├── common/             # BaseTimeEntity 등 공통 부모·유틸
│   ├── error/              # ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
│   └── security/           # SecurityConfig, JWT 필터, 현재 사용자 조회
└── domain/                 # 기능 단위 (auth, user, semester, plan, category, certificate, studylog, ai)
```

- `global`에는 도메인 하나만 쓰는 코드를 두지 않는다. 처음엔 도메인 안에 두고, 두 번째 도메인이 필요해질 때 옮긴다.
- 도메인끼리는 상대 도메인의 **Service**를 호출한다. 다른 도메인의 Repository를 직접 주입하지 않는다.

## API 규칙

- 경로는 명세서 'API·DB 함수' 시트의 "확정 API" 열을 따른다. 확정 전이면 "이전 후 API 후보"를 쓰고 PR에 적는다. 접두사(`/api`)는 붙이지 않는다.
- 로그인한 사용자 자신의 자원은 `/me/...`로 표현한다. 다른 사용자 id를 경로·쿼리로 받지 않는다.
- 요청·응답은 DTO(record 권장)로만 주고받는다. Entity를 응답으로 직접 내보내지 않는다.
- 성공 응답은 DTO를 그대로 돌려준다(감싸지 않음). 생성은 201, 본문 없는 성공은 204.
- 실패 응답은 항상 `ErrorResponse { code, message, fieldErrors? }`. 서비스 규칙 위반은 `BusinessException(ErrorCode)`를 던지고, 새 오류는 `ErrorCode`에 도메인 접두사를 붙여 추가한다(`PLAN_LIMIT_EXCEEDED`). 배포된 code 이름은 바꾸지 않는다(프론트가 code로 분기).
- 입력 검증은 DTO에 Bean Validation(`@NotBlank`, `@Size`, `@Min` …)으로 하고 Controller에서 `@Valid`. 원본 DB의 CHECK 제약(예: 제목 1~30자, 주당 0.5~60시간)은 **DTO 검증 + DB 제약 둘 다** 둔다.
- 새 API에는 Swagger 설명(`@Operation`, `@Tag`)을 붙인다. 프론트는 `/swagger-ui.html`을 보고 개발한다.

## 보안

- 세션을 쓰지 않는다(STATELESS). 로그인 상태는 `Authorization: Bearer <access JWT>`로만 판단한다.
- 공개 경로는 `SecurityConfig.PUBLIC_PATHS`에만 추가한다. 그 외는 모두 인증 필요.
- **원본 RLS("본인 행만")를 대신하는 것은 서비스 코드다.** 사용자 소유 데이터를 조회·수정·삭제할 때는 항상 현재 사용자 id 조건으로 찾는다(`findByIdAndUserId`). 남의 데이터면 403이 아니라 404(`NOT_FOUND`)로 응답해 존재 여부를 드러내지 않는다.
- 공용 자격증 회차(`owner_id` 없음)는 조회만 허용한다.
- 토큰·비밀번호·Google 응답 원문·개인정보를 로그에 남기지 않는다. Refresh 토큰은 DB에 해시로만 저장한다.
- 데모 전용 기능(데모 로그인 등)은 `local`·`dev` 프로필에서만 빈이 생성되게 한다(`@Profile`).

## 영속성 (MySQL · JPA · Flyway)

- 스키마는 **Flyway 마이그레이션으로만** 바꾼다(`src/main/resources/db/migration`). `ddl-auto`는 `validate`이며 바꾸지 않는다.
- 머지된 마이그레이션 파일은 수정하지 않는다. 새 버전을 추가한다.
- PK는 `BIGINT AUTO_INCREMENT`(`@GeneratedValue(strategy = IDENTITY)`). 문자셋 `utf8mb4`, 엔진 InnoDB.
- 시각은 UTC `Instant` ↔ `DATETIME(6)`, 날짜만이면 `LocalDate` ↔ `DATE`. 시간대 변환은 프론트에서 한다.
- 사용자 데이터 테이블은 `user_id`에 FK + `ON DELETE CASCADE`를 둔다(회원 탈퇴 시 함께 삭제, 명세 FR-09-2).
- 원본의 트리거·SQL 함수(개수 제한, 자동 채움, 여러 테이블 저장)는 Service 메서드 + `@Transactional`로 옮긴다. 여러 테이블을 바꾸는 작업은 반드시 한 트랜잭션.
- 조회 전용 서비스 메서드는 `@Transactional(readOnly = true)`. 연관관계는 기본 `LAZY`, N+1이 생기면 fetch join이나 `@EntityGraph`로 해결하고 PR에 적는다.
- Entity에 `@Setter`를 열지 않는다. 상태 변경은 의미 있는 메서드(`plan.stop()`, `profile.updateName(...)`)로 한다.

## 설정

- 프로필: `local`(기본), `prod`, 테스트는 `test`. 공통값은 `application.yml`, 환경별 값은 `application-{profile}.yml`.
- 설정값 묶음은 `@ConfigurationProperties` record로 받는다(`CorsProperties` 참고).
