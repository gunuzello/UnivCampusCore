# UnivCampusCore · UCC

학생회 모집, 행사, 회의, 일정, 활동 기록과 인수인계를 연결하는 로컬 MVP.

## 빠른 실행

필수: Java 17 이상, Node.js 22.12 이상, Docker Desktop, Python 3.

```sh
./scripts/dev.sh
```

PostgreSQL, Spring Boot, React 개발 서버를 실행한다. 프론트엔드 의존성을 확인하고 백엔드가 준비된 뒤 화면 서버를 실행한다. 종료는 터미널에서 Ctrl+C. PostgreSQL 데이터는 Docker 볼륨에 보존한다.

- 웹: http://127.0.0.1:5173
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5433 (기존 로컬 DB의 5432 포트와 충돌 방지)

이미 개발 서버가 실행 중이면 추가로 실행하지 않는다.

## 로컬 예시 계정

`local` 프로필에서만 예시 계정과 학생회·행사·모집·회의·일정·인수인계 메모를 생성한다. 첫 생성 이후 다시 실행해도 기존 데이터는 덮어쓰지 않는다. 아래는 실제 개인정보가 아닌 가명이다.

| 역할 | 이메일 | 비밀번호 |
|---|---|---|
| 대표 | leader@ucc.local | ucc-local-2026! |
| 운영진 | staff@ucc.local | ucc-local-2026! |
| 일반 학생 | student@ucc.local | ucc-local-2026! |

대표는 소속에서 구성원을 추가하고 역할을 지정할 수 있다. 학생은 공개된 행사와 모집에 참여하고, 오른쪽 상단 프로필에서 본인의 신청·지원 상태를 확인한다.

직접 확인할 업무 순서와 검증 범위: [로컬 MVP 확인 안내](docs/MVP_ACCEPTANCE.md).

## 개별 실행 / 빈 데이터로 시작

```sh
docker compose up -d --wait
cd backend
./mvnw spring-boot:run
```

다른 터미널:

```sh
cd frontend
npm ci
npm run dev
```

예시 데이터를 원하면 백엔드 실행에 `-Dspring-boot.run.profiles=local`을 추가한다. 예시 프로필 없이 실행해도 이미 DB에 생성된 데이터는 유지된다.

전체 기능 확장 순서와 현재 진행 상태: [기능 구현 로드맵](docs/FEATURE_ROADMAP.md).

## 구현 범위

- 회원가입, 로그인, 로그아웃, 프로필 조회·수정.
- 조직 생성·수정, 구성원 추가·제거, MEMBER/STAFF/LEADER 역할 변경, 마지막 대표 보호.
- 행사 초안·수정·공개·마감·취소·완료, 정원 관리, 추가 질문, 신청·취소·참가 상태 관리, 지난 행사 복제.
- 모집 공고·질문 구성·공개·마감·취소, 지원서 제출·취소, 운영진 검토·합격·불합격 처리.
- 행사·모집 신청자 검색, 상태 필터, 질문 답변을 포함한 명단 CSV 내보내기.
- 회의 참석 대상·안건·내용·결정사항, 월·주 통합 캘린더, 내부 일정 생성·수정·삭제.
- 완료 행사·종료 모집·지난 회의·내부 일정 기록, 연도 필터, 외부 자료 링크 추가·수정·삭제, 기수별 인수인계 메모.
- 웹 내부 알림, 본인 신청·지원 목록.
- 대여 물품·재고·신청 기간(수령 후 일수), 학생 신청·취소, 운영진 수령·반려·반납, 연체·대여 이력.

AI FIND, 프로그램 추천, 팀 구하기, 동아리는 프로토타입의 화면 구조를 유지하는 준비 중 UI다. TODO, 건의, 학교 인증, 결제, 채팅, 외부 서비스 직접 연동은 구현하지 않는다.

## 검증

```sh
./scripts/check.sh
```

테스트는 개발 데이터와 분리된 `ucc_test` PostgreSQL DB에서 실행한다. 회원 인증/CSRF/조직 권한, 정원·중복·기간 규칙, 마지막 한 자리 동시 신청, 모집 결과, 내부 기록 접근 권한, 링크 주소 검증, 행사 기록·복제와 개인 신청 내역을 검증한다.

GitHub Actions에도 PostgreSQL 기반 백엔드 테스트와 프론트엔드 타입 검사·빌드를 설정했다. 원격 실행 결과는 아직 확인하지 않았다.

## 구성

- Frontend: TypeScript, React, Vite, React Router, TanStack Query.
- Backend: Java 17 호환, Spring Boot 3.5.5, JPA, Spring Security, OpenAPI.
- Database: PostgreSQL 16, Flyway V1–V6 마이그레이션.
- Architecture: 도메인별 패키지를 둔 단일 Spring Boot 애플리케이션.
- 인증: HttpOnly 세션 쿠키 + CSRF 토큰. 웹은 Vite 프록시로 같은 출처에서 API를 호출한다.

정원·중복·기간·권한은 서버가 최종 판단한다. 행사 신청·취소·정원 변경은 행사 행에 PostgreSQL 비관적 잠금을 걸어 직렬화한다. 공개한 뒤에는 질문을 변경할 수 없다. 취소 후 신청 기간 내에 다시 신청할 수 있다. 행사 완료는 종료 시각 이후에만 가능하다.

프로토타입 기준은 `docs/prototype/ucc_final_v8.html`이며 원본은 보존한다. 코랄색, 유리 카드, 정보 구조, 네 가지 주요 내비게이션을 React에서 재사용하고 PC에서는 사이드바로 배치했다. 원본을 픽셀 단위로 완전히 동일하게 복제한 것은 아니며 PC와 모바일 화면의 추가 조정은 가능하다.

## 환경변수

기본 계정과 비밀번호는 로컬 개발 전용이다. `.env.example`은 설정 예시이며 `.env`는 Git에서 제외한다.

Compose는 `.env`를 읽지만 백엔드는 셸 환경변수를 읽는다. 계정·포트를 바꾸면 두 설정을 함께 변경한다.

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`: Compose DB 설정.
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: 백엔드 DB 연결.
- `TEST_DB_URL`: 테스트 DB 주소 (기본 localhost:5433/ucc_test).
- `SERVER_ADDRESS`: 로컬 기본값 127.0.0.1.
- `SERVER_PORT`: 백엔드 포트 (기본 8080). 변경 시 Vite 프록시 주소도 수정한다.
- `COOKIE_SECURE`: HTTPS 환경에서 true로 설정한다.

배포는 이번 완료 범위에 포함하지 않는다. 서버 세션은 현재 메모리에 있으므로 백엔드를 재시작하면 다시 로그인해야 한다. 사용자·조직·활동 데이터는 DB에 남는다. Android/iOS용 인증 어댑터, 학교 인증과 푸시는 후속 범위다.
