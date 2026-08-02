# 보안 및 세션

## 인증 방식

CalTalk는 브라우저 기반 개인 일정 서비스의 현재 범위에 맞춰 서버 세션 인증을 사용한다. 인증 정보를 JavaScript 저장소에 보관하지 않고, 서버가 세션의 만료와 무효화를 통제할 수 있다.

로그인 성공 시 Spring Security context를 세션에 저장하고 `ChangeSessionIdAuthenticationStrategy`로 기존 session ID를 교체한다. 브라우저에는 `CALTALK_SESSION` 쿠키가 전달된다.

## Spring Session JDBC

- 인증 상태는 PostgreSQL의 `SPRING_SESSION`과 `SPRING_SESSION_ATTRIBUTES`에 저장한다.
- 유휴 만료는 12시간이다.
- Spring Session 자동 schema 초기화는 사용하지 않고 Flyway V4가 테이블을 관리한다.
- attributes FK는 세션 삭제 시 `ON DELETE CASCADE`로 정리된다.
- 테스트는 로그인 이후 DB 행과 Security context 저장, 다음 요청의 인증 복원을 직접 확인한다.

로그아웃은 현재 HTTP 세션을 무효화하고 Security context와 `CALTALK_SESSION` 쿠키를 정리한다. 회원 탈퇴는 사용자 DB 트랜잭션이 성공한 뒤 같은 정리를 수행한다. 다른 사용자 세션이나 다른 브라우저의 세션을 일괄 삭제한다고 보장하지 않는다. 삭제된 사용자의 남은 세션은 다음 보호 요청에서 사용자 조회 실패로 정리된다.

## 쿠키 정책

| 쿠키 | 용도 | HttpOnly | SameSite | Path | Secure |
|---|---|---:|---|---|---|
| `CALTALK_SESSION` | 서버 세션 식별 | true | Lax | `/` | 환경 설정 연동 |
| `XSRF-TOKEN` | 브라우저의 CSRF 헤더 구성 | false | Lax | `/` | 환경 설정 연동 |

운영 HTTPS 환경에서는 `SESSION_COOKIE_SECURE=true`로 두 쿠키의 Secure 속성을 활성화해야 한다. Domain은 현재 지정하지 않는다.

## CSRF 계약

`CookieCsrfTokenRepository`가 CSRF token을 쿠키에 저장한다.

1. 클라이언트가 credentials를 포함해 `GET /api/v1/csrf`를 호출한다.
2. 서버가 `XSRF-TOKEN` 쿠키를 발급하고 `204`를 반환한다.
3. 클라이언트가 쿠키 값을 읽어 상태 변경 요청의 `X-XSRF-TOKEN` 헤더로 보낸다.
4. `CALTALK_SESSION` 전송을 위해 계속 credentials를 포함한다.

회원가입과 로그인은 현재 CSRF 예외다. 로그아웃은 공개 접근이 가능하지만 CSRF 예외는 아니므로 유효한 token이 필요하다. 누락 또는 불일치는 redirect나 HTML 대신 `403 FORBIDDEN` JSON으로 응답한다.

## CORS

개발 origin은 `http://localhost:5173` 하나만 허용한다.

- credentials: true
- methods: GET, POST, PATCH, DELETE, OPTIONS
- headers: Content-Type, X-XSRF-TOKEN, Accept
- wildcard origin과 wildcard header 없음

preflight 허용은 인증이나 CSRF 우회를 의미하지 않는다. 실제 보호 API 요청은 별도로 세션과 CSRF 검사를 통과해야 한다. 운영 배포에서는 실제 frontend origin 또는 동일 출처 reverse proxy 정책으로 교체해야 한다.

## 소유권과 정보 은닉

일정 API는 URL이나 JSON으로 user ID와 이메일을 받지 않는다. session principal의 정규화 이메일로 사용자를 조회하고, 해당 사용자가 소유한 일정만 접근한다. 다른 사용자의 일정과 존재하지 않는 일정은 같은 `404 SCHEDULE_NOT_FOUND`로 처리해 존재 여부를 숨긴다.

confirmation도 현재 사용자 소유권을 검사한다. 다른 사용자의 confirmation 승인은 `403`이며 후보 내부에는 다른 사용자의 민감정보를 포함하지 않는다.

## 입력과 출력 보호

- 비밀번호는 8~64자 정책과 BCrypt encoding을 적용한다.
- 로그인 실패 원인은 `INVALID_CREDENTIALS`로 일반화한다.
- Jackson은 알 수 없는 DTO 필드를 거부하고 `422 VALIDATION_ERROR`의 `UNKNOWN_FIELD`를 반환한다.
- 응답에는 password, password hash, token, session ID, role, authority를 포함하지 않는다.
- 검증 오류는 필드 정보만 제공하며 입력 비밀번호와 내부 stack trace를 반사하지 않는다.
- 인증 응답과 사용자·일정 응답은 필요한 곳에서 `Cache-Control: no-store`를 사용한다.

## 오류 응답

공통 오류는 timestamp, HTTP status, code, message와 field errors를 JSON으로 제공한다. 인증 실패는 `401`, 권한 또는 CSRF 실패는 `403`, 소유권 은닉은 `404`, 입력 오류는 `422`, 충돌은 `409`로 구분한다.

## 운영 배포 체크리스트

- HTTPS 강제와 `SESSION_COOKIE_SECURE=true`
- 운영 frontend origin 또는 동일 출처 reverse proxy 적용
- DB credential을 외부 비밀 저장소에서 주입
- PostgreSQL 외부 공개 차단과 최소 권한 계정 사용
- session timeout과 cookie 정책 재검토
- rate limiting과 로그인 시도 제한 구현
- 보안 header, proxy header, TLS 종료 지점 검증
- 로그에서 요청 본문·cookie·token·개인정보 제외
- 세션 정리와 계정 삭제 동작을 배포 환경에서 재검증
