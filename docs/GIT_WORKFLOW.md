# Git 작업 방식

기능 작업마다 `feat/기능명` 브랜치를 만들고, 실행·검증한 단위로 한국어 커밋을 작성한다.

현재 작업 이력:

- `feat/local-mvp-foundation`: 기본 환경과 최종 HTML 보존.
- `feat/auth-organization`: 회원 인증, 조직과 멤버십 권한.
- `feat/event-recruitment`: 행사 신청과 모집 지원 흐름.
- `feat/operations-archive`: 회의, 일정, 기록, 자료 링크, 알림, 개인 신청 내역 및 전체 UX 연결.

브랜치는 순서대로 앞 작업을 이어받는다. 현재 완성된 코드는 `feat/operations-archive`에 있다. 원격 브랜치 게시와 기본 브랜치 정책은 팀이 정할 수 있다.

2026-10-05 GitHub Desktop의 `bad object refs/heads/feat/auth-organization 2` 오류를 복구했다. 공백이 들어간 중복 참조 파일을 저장소 외부에 백업한 뒤 격리했고 `git fsck --full`과 `git fetch origin`이 성공했다. 정상 브랜치와 커밋은 보존했다. 이 중복 파일이 생성된 원인은 확인되지 않았다.
