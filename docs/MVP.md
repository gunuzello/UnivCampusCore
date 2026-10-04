# UCC 1차 MVP

학생회 운영의 전체 흐름을 실제 PostgreSQL과 REST API로 처리한다. 완료 기준은 로컬 실행이며 배포는 포함하지 않는다.

## 프로토타입 매핑

최종 기준: 사용자가 제공한 `docs/prototype/ucc_final_v8.html`. 이전 HTML 대신 이 파일의 레이아웃, 정보 구조, 스타일과 상호작용을 따른다.

- 실제 구현: 첫 로그인(학교 인증 제외), 소속, 행사 생성/신청/관리, 모집/지원/명단, 회의, 캘린더, 지난 활동, 조직 권한, 학생회 생성, 프로필.
- UI 유지: AI FIND, 프로그램 추천, 팀 구하기, 동아리, 학교 인증. 준비 중으로 명시한다.
- 대여, 건의는 이번 기능 범위 밖이다.
- 디자인: 코랄 #F0503F, 회색 배경, 둥근 반투명 카드, 홈/찾기/캘린더/소속 내비게이션을 유지한다. PC 레이아웃에서는 사이드바로 확장한다.

## 모델과 권한

User → OrganizationMembership ← Organization. 조직 역할은 MEMBER/STAFF/LEADER.
Organization → Event/Recruitment/Meeting/Schedule. Event/Recruitment → Questions → Application/Answers.
완료/종료 데이터는 삭제하지 않고 기록으로 조회하며 외부 자료 링크와 인수인계 메모를 조직 권한으로 보호한다.
정원/중복/기간/권한은 서버가 판단한다. 마지막 자리 신청은 행사 행 잠금으로 직렬화한다.

## 아키텍처와 순서

React + TypeScript + Vite + Router + Query / Spring Boot + Security + JPA / PostgreSQL + Flyway.
하나의 백엔드에서 auth, user, organization, event, recruitment, meeting, schedule, archive, notification 패키지를 분리한다.
기본 환경 → 인증 → 조직 → 행사 → 모집 → 회의/일정 → 기록/재사용 → 알림.
웹 인증은 서버 세션 + HttpOnly 쿠키 + CSRF를 사용한다. 모바일은 향후 별도 인증 어댑터를 추가할 수 있다.
