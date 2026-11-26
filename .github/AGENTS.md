# 이슈 · PR · 브랜치 · CI 가이드

## 이슈

- 작업은 이슈부터 만든다. 템플릿: 기능(`feature.yml`), 버그(`bug.yml`).
- 제목: `[도메인] 할 일` (예: `[auth] Google 로그인 API`). 명세서 기능 ID(FR-01-1 등)를 본문에 적는다.
- 하나의 이슈는 하나의 PR로 끝낼 수 있는 크기로 나눈다.

## 브랜치

- `main`: 항상 빌드·테스트가 통과하는 상태. 직접 push 금지, PR로만 머지.
- 작업 브랜치: `{종류}/#{이슈번호}-{짧은-설명}` (예: `feat/#3-google-login`, `fix/#12-plan-limit`)
- 종류: `feat` `fix` `refactor` `docs` `test` `chore`

## 커밋

- 형식: `{종류}: {무엇을} (#{이슈번호})` (예: `feat: Google id_token 검증 추가 (#3)`)
- 커밋 하나에는 한 가지 변경만 담는다. 빌드가 깨지는 중간 커밋은 PR 전에 정리한다.
- `.env`, 키 파일, `build/`, IDE 설정은 커밋하지 않는다(`.gitignore`).

## PR

- 템플릿(`pull_request_template.md`)을 채운다: 관련 이슈, 변경 내용, 테스트 방법, `docs/MIGRATION.md` 갱신 여부.
- 올리기 전 `./gradlew build` 통과를 확인한다. CI가 같은 명령을 실행한다.
- 리뷰어: 백엔드 팀원 최소 1명 승인 후 머지. 다른 묶음 도메인을 건드리면 그 담당자를 리뷰어로 넣는다.
- API를 추가·변경하면 프론트 담당에게 PR 링크와 Swagger 경로를 공유한다.
- 머지 방식: Squash merge. 머지 후 작업 브랜치는 삭제한다.
- 마이그레이션 파일은 날짜·시간 버전(`V202611261530__...`)이라 번호가 겹치지 않는다. 대신 같은 테이블을 건드리는 PR끼리는 머지 순서를 미리 정한다.

## CI / CD

- `.github/workflows/ci.yml`: PR과 `main` push마다 JDK 21로 `./gradlew build` 실행.
- 비밀 값이 필요한 워크플로는 GitHub Secrets만 사용하고, 로그에 출력하지 않는다.
- 배포(CD)는 배포 환경이 정해지면 별도 이슈로 추가한다.
