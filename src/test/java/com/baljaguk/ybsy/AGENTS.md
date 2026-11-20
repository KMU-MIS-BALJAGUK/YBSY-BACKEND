# 테스트 가이드

## 실행

```bash
./gradlew test                                   # 전체
./gradlew test --tests "*PlanServiceTest"        # 하나만
```

- 테스트는 `test` 프로필(`src/test/resources/application-test.yml`)로 실행하며 H2(MySQL 호환 모드)를 쓴다. 로컬 MySQL·`.env` 없이 돌아가야 한다.
- H2가 MySQL 전용 문법(예: `ON DUPLICATE KEY UPDATE`, JSON 함수)을 지원하지 않아 마이그레이션이 깨지면, H2에 맞추려고 운영 SQL을 바꾸지 말고 이슈로 올려 Testcontainers(MySQL) 도입을 논의한다.

## 어떤 테스트를 쓰나

| 대상 | 방식 | 예 |
|---|---|---|
| 도메인 규칙 (Entity·값 검증) | 순수 단위 테스트 (Spring 없음) | 주차 시작 ≤ 끝, 0.5시간 단위 |
| Service 규칙 | `@SpringBootTest` + `@Transactional` 또는 Mockito | 계획 30개 초과 시 `PLAN_LIMIT_EXCEEDED` |
| Repository 쿼리 | `@DataJpaTest` | 지금 학기·본인 데이터만 조회 |
| Controller·보안 | `@SpringBootTest` + `@AutoConfigureMockMvc` (또는 `@WebMvcTest`) | 401·404·검증 오류 응답 형식 |

## 반드시 남길 테스트

- **소유권**: 다른 사용자의 데이터를 id로 조회·수정·삭제하면 404가 나는지. 원본 RLS 점검(ARCHITECTURE.md 3장, 20개 항목)을 옮긴 것이다.
- **원본 규칙**: SQL 함수·트리거에서 옮긴 제한·자동 처리(개수 제한, 데모 계정 차단, 트랜잭션 롤백).
- **오류 응답**: 새 `ErrorCode`를 추가하면 그 코드가 응답 `code`로 나오는지.

## 작성 규칙

- 테스트 이름은 `@DisplayName`으로 한국어 문장(무엇을 하면 무엇이 된다).
- given / when / then 순서로 쓰고, 한 테스트는 한 가지 동작만 확인한다.
- 테스트끼리 데이터를 공유하지 않는다. 필요한 데이터는 각 테스트에서 만든다.
- 실제 Google·Claude API를 부르지 않는다. 외부 호출은 인터페이스 뒤에 두고 테스트에서 가짜로 바꾼다.
- 실패하는 테스트를 `@Disabled`로 끄고 머지하지 않는다.
