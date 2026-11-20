# Flyway 마이그레이션

- 파일 이름: `V{번호}__{설명}.sql` (예: `V1__create_users.sql`). 밑줄 두 개.
- 한 번 `main` 에 머지된 파일은 **절대 수정하지 않는다.** 고칠 게 있으면 새 번호로 추가한다.
- 번호가 겹치지 않도록 PR 을 올리기 전에 `main` 을 받아 마지막 번호를 확인한다.
- MySQL 8 문법으로 작성한다. 원본(Supabase PostgreSQL)과의 차이는 `docs/MIGRATION.md` 참고.
