# 도메인 코드 가이드 (Controller · Service · Repository · Entity · DTO)

## 도메인 담당

명세서 'API·DB 함수' 시트의 색 구분을 따른다. 자세한 API 목록은 `docs/MIGRATION.md`.

| 묶음 | 도메인 패키지 | 담당 |
|---|---|---|
| A | `auth`, `user`(프로필·회원 탈퇴) | (GitHub ID 기입) |
| B | `semester`, `category`, `studylog`, `ai` | (GitHub ID 기입) |
| C | `plan`(온보딩 저장 포함), `certificate` | (GitHub ID 기입) |

- 다른 묶음의 도메인 코드를 고쳐야 하면 담당자에게 리뷰를 요청한다.
- 도메인 사이 호출이 필요하면 상대 Service에 메서드를 요청·추가하고, 계약(입력·출력·예외)을 PR에 적는다. 예: 가입 시 첫 학기 생성(`user` → `SemesterService`).

## 도메인 내부 구조

```
domain/plan
├── controller/   PlanController
├── service/      PlanService
├── repository/   PlanRepository
├── entity/       Plan, PlanType(enum)
└── dto/          PlanCreateRequest, PlanUpdateRequest, PlanResponse
```

## 계층별 규칙

- **Controller**: 요청 검증(`@Valid`)·현재 사용자 받기·Service 호출·HTTP 상태 결정만 한다. 비즈니스 규칙, Repository 호출을 두지 않는다.
- **Service**: 규칙과 트랜잭션의 위치. 소유권 확인(현재 사용자 id)을 여기서 한다. 원본 SQL 함수·트리거가 하던 일(개수 제한, 자동 채움, 동기화)도 여기로 옮긴다.
- **Repository**: Spring Data JPA 인터페이스. 사용자 데이터 조회 메서드는 `userId`를 조건에 포함한다(`findAllByUserIdAndSemesterId`). 복잡한 조회는 `@Query`(JPQL)로 쓰고 이유를 주석으로 남긴다.
- **Entity**: `BaseTimeEntity` 상속, 기본 생성자는 `protected`, `@Setter` 금지, 생성은 정적 팩토리나 `@Builder`. 원본 CHECK 제약에 해당하는 불변식은 생성·변경 메서드 안에서도 지킨다.
- **DTO**: record로 작성. 이름은 `{대상}{동작}Request` / `{대상}Response`. Entity → DTO 변환은 `PlanResponse.from(plan)` 같은 정적 메서드로.

## 원본에서 옮길 때 확인할 것

- 원본 SQL 함수·트리거를 옮길 때는 `docs/MIGRATION.md` 대응표의 "옮길 위치"를 채우고, 원본이 지키던 규칙(제한 개수, 오류 코드, 데모 계정 차단 등)을 테스트로 남긴다.
- 원본 오류 코드(예: 계획 30개 초과 `P0030`)는 `ErrorCode` 이름으로 바꾸고 대응표에 적는다.
- "지금 학기만" 규칙이 있는 데이터(plans, user_certificates, study_logs)는 현재 학기 id를 항상 조건에 넣는다.
