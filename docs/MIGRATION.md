# Supabase → Spring Boot + MySQL 이전 대응표

원본: 학기예보 1차 서비스(React + Supabase). 기준: 요구사항 명세서 'API·DB 함수', 'DB 테이블', '기능 명세' 시트.
상태: ⬜ 시작 전 · 🟨 진행 중 · ✅ 완료. 작업 PR에서 이 문서를 같이 갱신한다.

## 1. 결정 사항

| 항목 | 원본 | 이전 후 | 비고 |
|---|---|---|---|
| DB | Supabase PostgreSQL | MySQL 8 | 스키마는 Flyway로 다시 작성 |
| 인증 | Supabase Auth (Google, PKCE) | Google OAuth2 직접 구현 + 자체 JWT | 3장 |
| 권한 | RLS (본인 행만) | Service에서 현재 사용자 id 조건 | 남의 데이터 → 404 |
| SQL 함수·트리거 | DB 안 | Service + `@Transactional` | 4장 |
| AI 추천 | Edge Function `estimate-hours` | Spring Service (Claude API) | 키는 서버 환경변수 |
| 부담 계산 | 프론트 `calc.ts` | **프론트 유지** | 서버는 데이터 API만. `GET /forecast`는 추후 토론 |
| 응답 형식 | supabase-js | 성공: DTO 그대로 / 실패: `ErrorResponse{code,message,fieldErrors}` | |

## 2. 도메인 담당과 API

명세서 'API·DB 함수' 시트의 색 묶음 기준.

### A. 인증 · 프로필

| 기능 | 원본 | API | 서버 규칙 | 명세 | 상태 |
|---|---|---|---|---|---|
| Google 로그인 | `supabase.auth.signInWithOAuth` | `POST /auth/google` | 인가 코드 교환, id_token 검증, 첫 로그인이면 사용자·프로필·첫 학기 생성 | FR-01-1, FR-01-6 | ⬜ |
| 토큰 재발급 (세션 유지) | supabase-js 자동 갱신 | `POST /auth/refresh` *(추가)* | Refresh 토큰(HttpOnly 쿠키) 교체 발급 | FR-01-3 | ⬜ |
| 로그아웃 | `supabase.auth.signOut` | `POST /auth/logout` | Refresh 토큰 폐기 | FR-01-5 | ⬜ |
| 가입 제한 | `hook_google_only_signup` | (로그인 API 안) | Google 계정만. 데모(@hakgi.test)는 아래 데모 로그인으로 | FR-01-2 | ⬜ |
| 데모 로그인 | `VITE_DEMO_LOGIN` + 비밀번호 로그인 | `POST /auth/demo` *(추가)* | `local`·`dev` 프로필에서만 | FR-01-7 | ⬜ |
| 내 정보 조회 | `from('profiles').select` | `GET /me` | | | ⬜ |
| 내 정보·학기 설정 수정 | `from('profiles').update` + 트리거 | `PATCH /me` | 학기 설정은 B(`SemesterService`)에 위임, 동의 시각은 처음 한 번만 서버 시각으로 기록 | FR-02-3~5, FR-09-1 | ⬜ |
| 회원 탈퇴 | `rpc('delete_my_account')` | `DELETE /me` | 데모 계정 차단, 모든 데이터 CASCADE 삭제 | FR-09-2 | ⬜ |

### B. 학기 · 내 유형 · 기록 · AI

| 기능 | 원본 | API | 서버 규칙 | 상태 |
|---|---|---|---|---|
| 학기 목록 | `from('semesters').select` | `GET /semesters` | | ⬜ |
| 학기 전환 | `rpc('switch_semester')` | `POST /semesters/{id}/switch` | 빈 학기는 떠날 때 삭제 | ⬜ |
| 다음 학기 시작 | `rpc('start_next_semester')` | `POST /semesters` | 설정 일부 이어 씀, 개강일만 비움 | ⬜ |
| 이번 학기 초기화 | `rpc('reset_current_semester')` | `POST /semesters/current/reset` | 데모 계정 차단 | ⬜ |
| 학기 파일 불러오기 | 화면에서 여러 번 insert | `POST /semesters/current/import` | **한 트랜잭션으로** | ⬜ |
| 내 유형 목록·추가·삭제 | `from('plan_categories')` | `GET/POST /categories`, `DELETE /categories/{id}` | 10개 제한, 이름 1~10자 | ⬜ |
| 기록 목록 | `from('study_logs').select` | `GET /logs?from=&to=` | 지금 학기만 | ⬜ |
| 기록 저장 | 있으면 update, 없으면 insert | `PUT /logs` | 같은 날 같은 일은 한 줄 (UPSERT) | ⬜ |
| 기록 삭제 | `from('study_logs').delete` | `DELETE /logs/{id}` | | ⬜ |
| AI 추천 | Edge Function `estimate-hours` | `POST /ai/estimate` | kind 4종, 저장 안 함, 프롬프트 인젝션 가드 | ⬜ |
| AI 횟수 차감 | `rpc('consume_ai_quota')` | (`/ai/estimate` 안) | 하루 20회, UPSERT +1 | ⬜ |

### C. 온보딩 · 계획 · 자격증

| 기능 | 원본 | API | 서버 규칙 | 상태 |
|---|---|---|---|---|
| 온보딩 계획·자격증 저장 | `rpc('save_onboarding_plans')` | `POST /onboarding/plans` | 한 트랜잭션 | ⬜ |
| 계획 목록 / 하나 | `from('plans').select` | `GET /plans`, `GET /plans/{id}` | 지금 학기만 | ⬜ |
| 계획 추가 | count 확인 후 insert | `POST /plans` | 학기당 30개, semester_id 자동 | ⬜ |
| 여러 계획 추가 (시간표) | `insert([...])` | `POST /plans/bulk` | 30개 제한 포함 | ⬜ |
| 계획 수정 / 그만두기 | `from('plans').update` | `PATCH /plans/{id}` | | ⬜ |
| 계획 삭제 | `from('plans').delete` | `DELETE /plans/{id}` | 기록도 CASCADE | ⬜ |
| 개강일 변경 시 주차 재계산 | 화면에서 계획마다 update | (`PATCH /me` 처리 중 서버가 일괄) | A·B와 계약 필요 | ⬜ |
| 자격증 목록 | `from('certificates').select` | `GET /certificates?q=` | 공용 + 내 것 | ⬜ |
| 내가 고른 회차 | `from('user_certificates')` | `GET/POST /me/certificates`, `PATCH/DELETE /me/certificates/{certificateId}` | 지금 학기만, 학습 1~40h, 준비 1~16주 | ⬜ |
| 직접 추가 | insert 두 번 | `POST /certificates/custom` | 20개 제한, **한 트랜잭션** | ⬜ |
| 직접 추가 삭제 | `from('certificates').delete` | `DELETE /certificates/{id}` | 내 것만 | ⬜ |

## 3. 인증 설계 (A)

Spring Security `oauth2Login()`을 쓰지 않고 OAuth 흐름을 직접 구현한다.

```
1. 프론트: code_verifier·state 생성 → Google 인가 화면으로 이동 (PKCE, S256)
2. Google → 프론트 /auth/callback?code=...&state=...  (프론트가 state 확인)
3. 프론트 → POST /auth/google { code, codeVerifier, redirectUri }
4. 서버 → https://oauth2.googleapis.com/token 에서 code 교환 (client_secret 은 서버에만)
5. 서버 → id_token 검증: Google JWKS 서명, iss, aud(=우리 client_id), exp, email_verified
6. 서버 → users(google_sub) 조회/생성. 처음이면 profiles + 첫 semesters 생성 (한 트랜잭션)
7. 서버 → Access JWT(본문, 약 30분) + Refresh 토큰(HttpOnly·Secure·SameSite 쿠키, 약 14일)
8. 이후 요청: Authorization: Bearer <access>. 만료 시 POST /auth/refresh (Refresh 토큰 교체)
```

- `redirectUri`는 서버 설정의 허용 목록에 있는 값만 받는다(NFR-SEC-4).
- Refresh 토큰은 DB에 해시만 저장하고, 재발급마다 새 토큰으로 교체한다. 이미 쓴 토큰이 다시 오면 그 사용자의 토큰을 모두 폐기한다.
- 필요한 설정: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET` (`.env.example`).
- Google Cloud Console에 새 OAuth 클라이언트(웹)를 만들고 리디렉션 URI `http://localhost:5173/auth/callback`(+ 운영 주소)을 등록한다.

## 4. 테이블 · DB 함수 이전

### 테이블

| 원본 (PostgreSQL) | 이전 후 (MySQL) | 담당 | 바꿀 점 |
|---|---|---|---|
| `auth.users` | `users`, `refresh_tokens` | A | 직접 생성. `google_sub` UNIQUE |
| `profiles` | `profiles` | A | 학기 설정 중복(`profiles` ↔ `semesters`) 정리 — B와 함께 결정 |
| `semesters` | `semesters` | B | |
| `plan_categories` | `plan_categories` | B | 삭제 시 `plans.category_id` SET NULL |
| `plans` | `plans` | C | CHECK 제약 다수 → DB 제약 + DTO 검증. `study_days[]` → 별도 표현 |
| `certificates` | `certificates` | C | 공용/개인(`owner_id`) 분리 여부 토론 |
| `user_certificates` | `user_certificates` | C | 복합 PK(user, certificate, semester) |
| `study_logs` | `study_logs` | B | plan_id·certificate_id 중 정확히 하나. 날짜 범위 인덱스 |
| `ai_usage` | `ai_usage` | B | `INSERT ... ON DUPLICATE KEY UPDATE` |

### PostgreSQL → MySQL 타입

| PostgreSQL | MySQL | 메모 |
|---|---|---|
| `uuid` PK | `BIGINT AUTO_INCREMENT` | 새로 시작하므로 정수 PK |
| `timestamptz` | `DATETIME(6)` (UTC) | JPA `Instant` |
| `smallint[]`, `text[]` (`daily_hours`, `study_days`) | 별도 테이블 또는 `JSON` | 담당자가 PR에서 결정·기록 |
| `text` + CHECK(enum 값) | `VARCHAR` + `@Enumerated(STRING)` | |
| `numeric` (0.5 단위 시간) | `DECIMAL(4,1)` | |
| RLS 정책 | 없음 | Service 소유권 확인 + 테스트 |

### SQL 함수 · 트리거

| 원본 | 하던 일 | 옮길 위치 | 담당 | 상태 |
|---|---|---|---|---|
| `handle_new_user`, `create_first_semester` | 가입 시 프로필·첫 학기 생성 | `AuthService` → `SemesterService` | A·B | ⬜ |
| `hook_google_only_signup` | Google·데모만 가입 | `AuthService` | A | ⬜ |
| `stamp_privacy_agreed_at` | 동의 시각 한 번만 기록 | `Profile` 엔티티 메서드 | A | ⬜ |
| `delete_my_account` | 탈퇴(데모 차단) | `UserService` | A | ⬜ |
| `reset_my_data` | 체험 계정 전체 초기화 | (필요 시) `UserService` | A | ⬜ |
| `sync_current_semester`, `current_semester_id`, `fill_semester_id` | 학기 설정 동기화·현재 학기 자동 채움 | `SemesterService` | B | ⬜ |
| `switch_semester`, `start_next_semester`, `reset_current_semester` | 학기 관리 | `SemesterService` | B | ⬜ |
| `enforce_category_limit` | 내 유형 10개 | `CategoryService` | B | ⬜ |
| `consume_ai_quota` | AI 하루 20회 | `AiQuotaService` | B | ⬜ |
| `enforce_plan_limit` | 계획 30개 (P0030) | `PlanService` | C | ⬜ |
| `save_onboarding_plans` | 온보딩 일괄 저장 | `OnboardingService` | C | ⬜ |
| `enforce_own_certificate_limit` | 직접 추가 자격증 20개 | `CertificateService` | C | ⬜ |
| `set_updated_at` | 수정 시각 | `BaseTimeEntity` (JPA Auditing) | 공통 | ✅ |
| `reset_demo_account` | (원본에서 삭제됨) | 옮기지 않음 | - | - |

### 공용 데이터

- 자격증 회차 시드(원본 `supabase/seed.sql`)는 C 담당이 Flyway 마이그레이션(또는 `R__` 반복 마이그레이션)으로 옮긴다.
