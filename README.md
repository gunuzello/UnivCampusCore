# UnivCampusCore · UCC

학생회 모집, 행사, 회의, 일정, 활동 기록과 인수인계를 연결하는 로컬 MVP.

## 실행

필수: Java 17 이상, Node.js 22.12 이상, Docker.

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

웹: http://localhost:5173 / API: http://localhost:8080 / Swagger: http://localhost:8080/swagger-ui.html

PostgreSQL 호스트 포트는 기존 로컬 DB와 충돌하지 않도록 5433을 사용한다.

기본 DB 계정은 로컬 개발 전용이다. 변경할 경우 환경변수 DB_URL, DB_USERNAME, DB_PASSWORD와 Compose 설정을 함께 변경한다. `.env`는 Compose에 사용되고 백엔드 실행에는 셸 환경변수를 사용한다.

## 검증

```sh
cd backend && ./mvnw test
cd frontend && npm run build
```

개발 범위와 디자인 매핑은 `docs/MVP.md`를 참고한다. 원본 HTML은 `docs/prototype/`에 보존한다.
