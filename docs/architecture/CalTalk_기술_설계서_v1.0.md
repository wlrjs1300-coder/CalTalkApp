CalTalk 기술 설계서 v1.0 Final
상태 표기: 확정(기획서/시작 프롬프트에서 이미 확정) · 기술 설계 확정안(본 문서에서 결정) · PoC 검증 필요 · 보류

본 기술 설계서 본문은 2.1부터 2.29까지 총 29개 절로 구성된다. 2.28의 완료 기준은 절 개수를 세어 맞추는 목록이 아니라, 문서 정합성과 승인 조건을 검증하기 위한 별도의 체크리스트다.

2.1 문서 목적, 범위, 비범위
목적: PWA·웹 자연어 대화·카카오톡 챗봇이 하나의 일정 도메인을 안전하게 공유하기 위한 시스템 구조, 데이터 모델, 동시성·보안 규칙을 정의한다.

범위: 기획서 7장(MVP-1~4)과 21장(MVP 제외 기능)의 범위 내 아키텍처, 데이터 모델, API 원칙, 보안 원칙, 배포 방식.

비범위: 화면 단위 UI 명세(다음 공식 단계), 반복·종일·공유·팀 일정·외부 캘린더 연동·선제 알림(MVP 제외 확정), 실제 카카오 스킬 생성·콜백 신청 결과(PoC 대상), 실 코드·설정 파일·저장소 구조.

2.2 전체 시스템 구성과 컴포넌트 책임

[PWA(React)]      [웹 자연어 대화 UI]      [카카오톡 채널 챗봇]
       \                  |                       /
        \-------- 채널 어댑터 계층 -------------/
                          |
                 일정 도메인 서비스
    (인증/권한/검증/충돌판정/확인·소비/이력)
                          |
   -----------------------------------------------
   |                      |                       |
PostgreSQL          OpenAI API 어댑터       카카오 스킬 응답 어댑터
(도메인 데이터 +
 Spring Session +
 보안 상태)
컴포넌트	책임	하지 않는 일
PWA 프런트엔드	로그인, 캘린더 UI, 직접 CRUD, 채팅 UI	서버 검증 로직 재구현
채널 어댑터	채널별 요청·응답 포맷 변환, 채널 제약 대응	도메인 규칙 판단, DB 접근
일정 도메인 서비스	인증/인가, 검증, 충돌 판정, 동시성 제어, 이력	채널별 포맷 처리
OpenAI API 어댑터	자연어 → 구조화 명령 변환, 스키마 검증	DB 접근, 최종 확정 판단, 의미적 정확성 보장
PostgreSQL	도메인 데이터, Spring Session 세션, 지속 필요 보안 상태	비즈니스 규칙 실행
상태: 확정

2.3 채널 어댑터 구조

채널 입력 → [채널별 어댑터] → 표준 내부 커맨드 → 도메인 서비스 → 표준 결과
→ [채널별 어댑터] → 채널 응답
PWA 직접 조작은 폼 입력을 표준 커맨드로 그대로 매핑하고, 웹 자연어 대화는 문장을 OpenAI 어댑터가 구조화해 표준 커맨드로 바꾸며, 카카오 챗봇은 스킬 요청을 연결 확인 후 동일하게 처리하고 응답을 카카오 스킬 규격으로 재포맷한다.

상태: 확정

2.4 공통 일정 도메인 서비스 구조
일정 조회(소유자 필터 강제)
일정 생성/수정: 후보 산출 → 최종 확인 → 원자적 소비·커밋
일정 삭제(PWA 직접 조작만, MVP, 즉시 하드 삭제 — 11절)
충돌 판정(경고, 저장 차단 아님)
변경 이력 기록
PWA 직접 등록·수정과 자연어 등록·수정은 하나의 확인(confirmation) 모델을 공유한다(8절). 자연어 경로는 항상 후보 생성 → 최종 확인을 거치고, PWA 직접 조작은 충돌이 없으면 저장 클릭 자체를 최종 확인으로 간주해 즉시 커밋하되, 충돌이 있으면 자연어 경로와 동일한 확인 절차로 전환된다.

상태: 확정

2.5 프론트엔드 아키텍처
항목	결정	이유	대안	상태
라우팅	React Router	선언적 클라이언트 라우팅	TanStack Router	기술 설계 확정안
서버 상태 관리	TanStack Query	서버 데이터 캐시·재조회 표준화	SWR	기술 설계 확정안
전역 클라이언트 상태	별도 라이브러리 없음(컴포넌트 상태 + URL 검색 파라미터 + TanStack Query 캐시)	공유 필요 상태가 적음	Zustand, Redux	기술 설계 확정안
API 호출 계층	공통 fetch 래퍼	쿠키 세션 인증에는 axios 이점이 적음	axios	기술 설계 확정안
PWA 구성	vite-plugin-pwa(Workbox 기반)	앱 셸 캐싱·설치 가능 상태 제공	수동 서비스워커	기술 설계 확정안
서비스워커 캐시 범위	정적 앱 셸만 캐시, /api/**·인증 응답은 NetworkOnly	오프라인 동기화 제외 원칙, 세션 간 데이터 오염 방지	stale-while-revalidate	확정
인증 상태 처리	서버 세션 쿠키, 로그인 여부는 GET /api/v1/users/me 응답으로 판단하고 클라이언트 메모리에만 반영	7절과 일치	로컬스토리지 JWT	기술 설계 확정안
캘린더 UI	월간 그리드 + 날짜 선택 + 선택 날짜 목록 + 이전/다음 달 + 오늘 이동만(일간 그리드 없음)	요구 범위 초과 방지	FullCalendar	기술 설계 확정안
2.6 백엔드 아키텍처

com.example.caltalk
 ├─ auth          (인증, 세션, CSRF, 로그인 보안 상태)
 ├─ user          (사용자 설정, 시간대, 회원 탈퇴)
 ├─ schedule      (일정 CRUD, 충돌 판정, 변경 이력)
 ├─ conversation  (대기 명령, 확인 후보, 최종 확인, 멱등성 — PWA 충돌 확인 포함)
 ├─ ai            (OpenAI API 어댑터, 구조화 출력 검증)
 ├─ kakao         (스킬 요청/응답, 사용자 연결)
 └─ common        (예외, 공통 응답 타입, 시간 도구 등 최소 공통 요소)
계층: Controller(요청·응답 매핑) / Application·Service(유스케이스, 트랜잭션 경계) / Domain(순수 규칙) / Infrastructure(Repository, 외부 API 어댑터).

common 제한 원칙: (1) 두 개 이상 모듈에서 실제 재사용이 확인된 것만 둔다. (2) 특정 도메인 개념을 담은 타입은 두지 않는다. (3) 새 유틸은 먼저 해당 도메인 모듈에 두고 두 번째 사용처가 생기면 승격한다.

상태: 기술 설계 확정안

2.7 인증·인가·배포 경계 설계
2.7.1 배포 경계와 출처 구조
프런트엔드·백엔드 물리적 분리 배포(확정). 운영 기본안은 /api/**를 백엔드로 전달하는 리버스 프록시로 브라우저 관점의 동일 출처를 만든다(실제 구현 수단은 22절, 보류). 개발 환경은 http://localhost:5173만 CORS로 정확히 허용한다.

2.7.2 인증 방식과 세션 저장소
이메일 기반 회원가입/로그인 + 서버 세션, 세션 저장소는 Spring Session JDBC로 PostgreSQL에 저장(확정 유지). 인프라 테이블 관리는 2.7.10 참조.
회원가입은 POST /api/v1/auth/signup에 email, password, passwordConfirmation만 전달한다. 이름·닉네임·표시 이름·시간대는 가입 요청에서 받지 않는다. 가입 성공 시 사용자만 생성하고 자동 로그인이나 세션 생성을 하지 않는다.
로그인은 POST /api/v1/auth/login에 email과 password만 전달한다. 이메일은 필수이며 앞뒤 공백 제거와 소문자 정규화 후 형식과 최대 254자를 검증한다. 비밀번호는 필수이며 8자 이상 64자 이하로 검증한다. 인증 성공 시 Spring Security 서버 세션을 생성하고 기존 세션이 있으면 세션 고정 공격 방지를 위해 세션 ID를 교체한다. 이미 로그인한 사용자도 제출한 새 인증 정보로 다시 인증하며 성공은 HTTP 200으로 처리하고 중복 로그인 오류를 반환하지 않는다.
로그아웃은 POST /api/v1/auth/logout을 사용한다. 인증된 사용자용 API이지만 유효한 CSRF 토큰을 제시한 미인증 요청도 멱등적으로 처리하여 인증 여부와 관계없이 HTTP 204 No Content를 반환하고 로그인 상태를 노출하지 않는다. 현재 세션만 종료하며 다른 사용자나 다른 세션에는 영향을 주지 않는다.
현재 사용자 조회는 GET /api/v1/users/me를 사용한다. CALTALK_SESSION으로 인증된 Spring Security Authentication의 principal에서 정규화 이메일을 얻고 users를 다시 조회하며, 클라이언트가 ID·이메일을 요청 파라미터나 본문으로 지정할 수 없다. 성공 응답은 email·timezone·createdAt만 포함하고 브라우저 새로고침과 앱 초기 진입의 인증 상태 복원 기준으로 사용한다.
현재 사용자 시간대 변경은 PATCH /api/v1/users/me를 사용한다. 공개 경로가 아닌 세션 인증·CSRF 보호 대상이며 요청 본문에는 timezone 하나만 받는다. 인증 principal로 현재 사용자를 다시 조회한 뒤 users.timezone만 변경하고 GET /api/v1/users/me와 동일한 응답 구조를 반환한다.
회원 탈퇴는 DELETE /api/v1/users/me를 사용한다. 공개 경로가 아닌 세션 인증·CSRF 보호 대상이며 currentPassword로 현재 비밀번호를 재검증한 뒤 2.7.9의 현재 V1~V3 삭제 범위를 처리한다.

2.7.3 세션 만료와 로그인 유지 정책
비활동 기준 세션 유효시간은 12시간이며 요청이 발생하면 만료 시각이 갱신되는 기본 유휴 시간 방식을 사용한다. 별도 자동 로그인·로그인 유지 기능과 로그인 상태 유지 체크박스는 제공하지 않고, 동시 로그인 제한도 이번 MVP에서 적용하지 않는다(확정).

2.7.4 세션 쿠키 속성
세션 쿠키 이름은 CALTALK_SESSION이다. HttpOnly=true, SameSite=Lax, Path=/를 사용하고 Domain과 Max-Age는 지정하지 않는 세션 쿠키로 설정한다. Secure는 환경별 설정으로 관리하여 로컬 HTTP 개발환경에서는 false, 운영 HTTPS 환경에서는 true로 강제하며 운영에서 false를 허용하지 않는다(확정).
로그아웃 응답에서는 CALTALK_SESSION을 빈 값, Path=/, Max-Age=0, HttpOnly=true, SameSite=Lax로 설정해 삭제한다. Domain은 지정하지 않고 Secure는 현재 환경 설정과 동일하게 적용한다.

2.7.5 비밀번호 해시
비밀번호는 필수이며 8자 이상 64자 이하로 제한한다. 문자·숫자·특수문자 조합은 강제하지 않지만 공백만으로 구성된 값은 허용하지 않는다. passwordConfirmation은 password와 정확히 일치해야 하며 저장하지 않는다. 저장 시 Spring Security의 기본 강도를 사용하는 BCrypt로 해시하고 password_hash에 해시만 저장한다(기술 설계 확정안).

2.7.6 CSRF 처리(SPA 기준)
쿠키 기반 CSRF 저장소(XSRF-TOKEN) + X-XSRF-TOKEN 헤더를 사용한다. 회원가입과 로그인의 기존 CSRF 예외 계약은 유지하되 POST 로그아웃과 PATCH·DELETE /api/v1/users/me는 상태 변경 요청이므로 CSRF 보호 대상이며 예외에 추가하지 않는다. 토큰 누락 또는 불일치는 HTML 리다이렉트 없이 HTTP 403 + FORBIDDEN 공통 오류 JSON으로 응답한다. 로그인 성공 후 세션 교체 시 CSRF 쿠키를 갱신하므로 프런트엔드는 최신 값을 다시 읽는다.

2.7.7 CORS
운영: 불필요(동일 출처). 개발: http://localhost:5173만 허용.

2.7.7.1 Spring Security 웹 오류 정책
POST /api/v1/auth/signup, POST /api/v1/auth/login, GET /api/v1/health, /actuator/health, /actuator/health/**는 공개 경로로 유지한다. POST /api/v1/auth/logout은 인증된 사용자용 경로이되 유효한 CSRF 토큰이 있는 미인증 요청을 멱등 성공으로 처리한다. GET·PATCH·DELETE /api/v1/users/me는 공개 경로에 추가하지 않고 세션 인증을 요구하며 PATCH와 DELETE에는 CSRF 토큰도 요구한다. 인증되지 않은 보호 API 요청과 로그아웃 뒤 기존 세션 쿠키로 보낸 보호 API 요청은 HTML 로그인 화면으로 리다이렉트하지 않고 HTTP 401 + UNAUTHORIZED 공통 오류 JSON을 반환하며, 권한 부족과 CSRF 실패는 HTTP 403 + FORBIDDEN 공통 오류 JSON을 반환한다. formLogin 화면은 사용하지 않는다.

2.7.8 사용자 소유권 검증
모든 일정 조회·수정·삭제 쿼리에 인증 사용자 ID를 강제하고 Application 계층에서 소유자를 재비교한다(확정).

2.7.9 회원 탈퇴와 데이터 삭제
확정 API는 `DELETE /api/v1/users/me`다. 세션으로 인증된 현재 사용자만 호출할 수 있고 CSRF 보호 대상이며, 사용자 ID나 이메일을 요청에서 받지 않는다. 요청 JSON은 `{ "currentPassword": "현재 비밀번호" }`만 허용한다. currentPassword는 필수 문자열이고 공백 전용과 64자 초과를 거부한다. 인증 principal의 정규화 이메일로 사용자를 다시 조회하고 `PasswordEncoder.matches(currentPassword, password_hash)`로 현재 비밀번호를 검증한다. 원문·해시·요청 본문을 로그나 오류 응답에 노출하지 않는다. 누락·공백 전용·형식·길이 오류는 HTTP 422 + VALIDATION_ERROR와 currentPassword fieldError, 불일치는 계정 존재 여부나 내부 상태 차이를 드러내지 않는 HTTP 401 + INVALID_CREDENTIALS와 빈 fieldErrors로 응답한다.

현재 V1~V3에서 실제 존재하는 사용자 관련 도메인 테이블만 DB 삭제 트랜잭션의 대상으로 확정한다. 순서는 (1) 현재 사용자 조회와 비밀번호 검증, (2) 해당 사용자의 confirmation_requests 전체 삭제 — PENDING·CONSUMED·SUPERSEDED·EXPIRED·CANCELLED를 구분하지 않고 self reference와 target_schedule_id 참조를 먼저 정리, (3) 해당 사용자의 schedules 전체 하드 삭제, (4) schedule_change_history가 `schedule_id ON DELETE CASCADE`로 함께 삭제됐는지 확인, (5) users 삭제, (6) 커밋이다. 다른 사용자의 일정·이력·confirmation은 삭제하지 않는다. 별도 DELETE 이력·감사 로그·soft delete를 추가하지 않으며 users에는 deleted_at을 두지 않는다.

pending_commands, idempotency_records, connection_codes, kakao_user_links, login_security_state는 현재 V1~V3에 존재하지 않으므로 현 구현의 삭제 대상으로 표현하지 않는다. 이 테이블들이 후속 migration으로 추가되면 해당 사용자 소유 행을 회원 탈퇴 DB 트랜잭션에 포함해야 한다는 후속 계약만 유지한다. 외부 카카오 API 호출이나 별도 네트워크 연결 해제는 현재 탈퇴 성공 조건에 포함하지 않는다.

DB 삭제와 HTTP 세션 정리는 하나의 원자적 트랜잭션이 아니다. DB 트랜잭션이 성공적으로 커밋된 뒤 현재 HTTP 세션을 무효화하고 SecurityContext를 제거하며, 응답에서 CALTALK_SESSION을 빈 값·Path=/·Max-Age=0·HttpOnly·SameSite=Lax·환경별 Secure·Domain 미지정으로 삭제한다. 현재 Spring Session 구조에는 사용자별 모든 세션을 안전하게 찾는 별도 인덱스 계약이 없으므로 MVP는 현재 요청 세션만 종료하며, 근거 없이 모든 기기·브라우저 세션 종료를 보장하지 않는다. 다른 세션이 남아 있어도 users 재조회 실패 시 기존 무효 사용자 세션 처리 경로로 세션·SecurityContext·쿠키를 정리하고 401을 반환한다. DB 삭제가 실패하면 커밋하지 않고 세션을 유지한 채 일반화된 HTTP 500 + INTERNAL_SERVER_ERROR를 반환한다. 성공은 HTTP 204 No Content, 빈 본문, `Cache-Control: no-store`이며 사용자 정보나 삭제 건수를 반환하지 않는다. 성공 뒤 같은 세션 쿠키로 보호 API를 호출하면 401 UNAUTHORIZED이고 탈퇴 요청 재시도 역시 204가 아닌 401이다.

관리형 DB 백업 보관 기간은 호스팅 제공업체 선택 후 개인정보 안내에 반영(보류). 일반 일정 삭제 정책은 11절 참조.

상태: 기술 설계 확정안(백업 보관 기간만 보류)

2.7.10 Spring Session 스키마 관리
로컬·테스트 환경은 Spring Session JDBC가 제공하는 DB별 기본 스키마 스크립트를 사용할 수 있다. 운영 환경은 자동 스키마 생성에 의존하지 않고, SPRING_SESSION·SPRING_SESSION_ATTRIBUTES 테이블 생성·변경을 도메인 테이블과 동일한 DB 마이그레이션 관리 체계의 범위에 포함시킨다(구체 도구는 저장소 초기화 단계 결정, 보류). 이 두 테이블은 도메인 테이블과 명확히 구분되는 인프라 테이블이며 애플리케이션 코드는 Spring Session API로만 접근한다. Spring Boot가 관리하는 Spring Session 의존성 버전은 별도로 직접 고정하지 않는다(24절).

상태: 기술 설계 확정안

2.8 확인 요청 소비, 자동 재계산, 동시성
2.8.1 confirmation_requests 스키마와 candidate_fingerprint 계산 규칙

confirmation_requests
 - id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY   -- API에는 숫자 confirmationId로 노출
 - user_id BIGINT NOT NULL                                  -- FK -> users.id, 삭제 정책 NO ACTION
 - origin_channel VARCHAR(20) NOT NULL                      -- PWA | WEB_CHAT | KAKAO
 - command_type VARCHAR(30) NOT NULL                        -- CREATE_EVENT | UPDATE_EVENT
 - target_schedule_id BIGINT NULL                           -- FK -> schedules.id, UPDATE_EVENT만 사용, 삭제 정책 NO ACTION
 - target_schedule_version BIGINT NULL                      -- 후보 생성(또는 재계산) 시점 schedules.version 스냅샷
 - title VARCHAR(200) NULL                                  -- CREATE_EVENT는 항상 값 있음, UPDATE_EVENT는 "변경 없음"이면 NULL
 - start_at TIMESTAMP WITH TIME ZONE NULL                   -- 위와 동일한 의미
 - end_at TIMESTAMP WITH TIME ZONE NULL
 - location_action VARCHAR(10) NOT NULL                     -- KEEP | REMOVE | SET
 - location_value VARCHAR(200) NULL
 - candidate_fingerprint CHAR(64) NOT NULL                  -- 아래 계산 규칙의 SHA-256 16진수
 - conflict_snapshot_hash CHAR(64) NOT NULL                 -- 계산 시점의 충돌 일정 목록(id+version) SHA-256, 충돌 없으면 빈 목록의 해시
 - conflict_acknowledged BOOLEAN NOT NULL DEFAULT FALSE
 - status VARCHAR(20) NOT NULL DEFAULT 'PENDING'            -- PENDING | CONSUMED | SUPERSEDED | EXPIRED | CANCELLED
 - superseded_by_confirmation_id BIGINT NULL                -- FK -> confirmation_requests.id, 삭제 정책 NO ACTION
   -- SUPERSEDED로 바뀔 때 이를 대체한 새 confirmation을 가리켜 감사 추적을 가능하게 함
 - created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
 - expires_at TIMESTAMP WITH TIME ZONE NOT NULL             -- 애플리케이션이 created_at 기준 5분으로 설정
 - consumed_at TIMESTAMP WITH TIME ZONE NULL

 CHECK (origin_channel IN ('PWA', 'WEB_CHAT', 'KAKAO'))
 CHECK (command_type IN ('CREATE_EVENT', 'UPDATE_EVENT'))
 CHECK (location_action IN ('KEEP', 'REMOVE', 'SET'))
 CHECK (status IN ('PENDING', 'CONSUMED', 'SUPERSEDED', 'EXPIRED', 'CANCELLED'))
 UNIQUE(user_id, candidate_fingerprint) WHERE status = 'PENDING'

 INDEX idx_confirmation_user_status ON confirmation_requests (user_id, status)
 INDEX idx_confirmation_expires ON confirmation_requests (expires_at)
 INDEX idx_confirmation_target_schedule ON confirmation_requests (target_schedule_id)

기존 컬럼만 물리화하며 별도 candidate_payload·cancelled_at 컬럼을 추가하지 않는다. 사용자 입력 전체 원문, 세션 ID, 쿠키, 비밀번호를 저장하지 않고 candidate_fingerprint와 conflict_snapshot_hash 전체값을 로그에 남기지 않는다. 만료된 PENDING 행은 승인·조회 시 EXPIRED로 전환한다. 별도 정리 스케줄러는 이번 MVP에 추가하지 않고 만료 행 정리는 후속 운영 작업으로 둔다.
candidate_fingerprint 계산 규칙

candidate_fingerprint는 단순히 "무엇이 바뀌었는가"만이 아니라, 다음 7개 값을 정규화한 canonical JSON을 기준으로 SHA-256 해시를 계산한 값이다.


command_type, target_schedule_id, title, start_at, end_at, location_action, location_value
정규화 규칙은 다음과 같다.

필드 순서 고정: canonical JSON은 항상 위에 나열한 순서(command_type → target_schedule_id → title → start_at → end_at → location_action → location_value)로 키를 나열한다. 직렬화 라이브러리의 기본 필드 순서(선언 순서, 알파벳 순서 등)에 의존하지 않고 이 순서를 명시적으로 고정한다. 같은 내용이라도 키 순서가 다르면 문자열이 달라지고 해시도 달라지므로, 순서 고정은 해시의 일관성을 위한 전제 조건이다.
시간 형식: start_at, end_at이 값을 가지면 UTC 기준 ISO-8601 형식(예: "2026-07-26T05:00:00Z")으로 표현한다. 사용자 시간대(예: Asia/Seoul) 오프셋이 섞인 표현을 쓰지 않는다 — 같은 절대 시각이 표현 방식 차이로 다른 해시가 되는 것을 막기 위함이다.
문자열 트리밍: title, location_value는 앞뒤 공백을 제거한 뒤 해시 계산에 사용한다.
null·빈 문자열·필드 미변경 구분:
title/start_at/end_at이 "이번 제안에서 변경하지 않음"을 의미하면 canonical JSON에서 해당 값은 JSON null로 고정한다.
실제 값이 있으면 트리밍된 문자열(또는 UTC ISO-8601 문자열)을 그대로 사용한다.
JSON의 null과 빈 문자열 ""은 서로 다른 토큰이므로 이 둘은 자동으로 구분된다. "미변경"은 항상 null로만 표현하며, 제목은 필수 입력값이므로 유효한 빈 문자열 값 자체가 발생하지 않는다.
location 3상태 유지: location_action은 KEEP/REMOVE/SET 세 값 중 하나를 그대로 문자열로 포함한다. location_action이 KEEP 또는 REMOVE이면 location_value는 항상 null로 정규화한다(저장소에 남아 있는 이전 값이 우연히 해시에 영향을 주지 않도록 한다). SET일 때만 location_value에 트리밍된 실제 문자열이 들어간다.
CREATE_EVENT의 target_schedule_id: 대상이 존재하지 않으므로 항상 null로 고정한다.
origin_channel 제외: origin_channel은 후보의 의미(무엇을 저장하려는지) 자체를 바꾸지 않는 부가 정보이므로 지문 계산에서 제외한다. 따라서 같은 제안 내용이 다른 채널에서 들어와도 동일한 지문이 계산되며, 2.8.2의 재사용·재검증 로직이 채널에 무관하게 적용된다. (다만 이 경우 재사용되는 확인의 origin_channel 컬럼 값은 그 후보를 처음 만든 채널로 남으며, 이는 감사용 부가 정보일 뿐 승인 로직에는 영향을 주지 않는다.)

{
  "command_type": "UPDATE_EVENT",
  "target_schedule_id": 482,
  "title": null,
  "start_at": "2026-07-26T07:00:00Z",
  "end_at": null,
  "location_action": "KEEP",
  "location_value": null
}
candidate_fingerprint = 위 canonical JSON 문자열을 UTF-8로 인코딩한 바이트열의 SHA-256 해시(16진수 또는 base64 문자열로 저장).

상태: 기술 설계 확정안

2.8.2 후보 생성, 동일 후보 재사용 시 재검증, 동시 최초 생성 경쟁 상태 처리
자연어 경로든 PWA 직접 조작(충돌 발생 시)이든, 제안 내용이 확정되면 다음 절차로 후보를 만든다.


[트랜잭션 시작]
1. 제안 델타 확정, candidate_fingerprint 계산(2.8.1 규칙)
2. SELECT * FROM confirmation_requests
     WHERE user_id = :userId AND candidate_fingerprint = :fp AND status = 'PENDING'
     FOR UPDATE
   -- 동일 사용자의 동일 후보가 이미 대기 중인지 조회하며, 존재하면 동시에 잠근다
3. 조회된 행이 없으면 새 후보를 만든다:
   a. target_schedule_version(대상 있는 경우 현재 버전), conflict_snapshot_hash(현재 충돌 목록 해시)를
      계산해 새 confirmation_requests 행을 INSERT 시도

   b. **이 INSERT가 부분 유니크 인덱스(`UNIQUE(user_id, candidate_fingerprint) WHERE status='PENDING'`)
      위반으로 실패할 수 있다.** `SELECT ... FOR UPDATE`는 이미 존재하는 행만 잠글 수 있으므로, 동일한
      PENDING 후보가 아직 하나도 없는 상태에서 완전히 동일한 최초 생성 요청 두 개가 거의 동시에 들어오면
      **둘 다 2단계에서 "없음"으로 조회할 수 있다.** 이 순간에는 잠글 대상 행 자체가 없으므로 애플리케이션
      레벨의 행 잠금만으로는 두 번째 INSERT를 막을 수 없다. **이 마지막 경쟁 구간에서는 부분 유니크
      인덱스가 최종 방어선 역할을 한다** — 두 트랜잭션 중 하나만 INSERT에 성공하고, 나머지 하나는 DB의
      고유 제약 위반 오류를 받는다.

   c. **이 고유 제약 위반을 HTTP 500 오류로 클라이언트에 노출하지 않는다.** 대신 다음을 수행한다.
      - (user_id, candidate_fingerprint, status='PENDING') 조건으로 방금 다른 트랜잭션이 먼저 만든
        행을 다시 조회한다(`SELECT ... FOR UPDATE`).
      - 4단계와 동일한 절차로 대상 일정의 현재 version과 최신 충돌 목록을 재검증한다.
      - 최신 상태가 그 행이 저장하고 있는 값과 동일하면 **그 기존 confirmationId를 그대로 반환**한다.
      - 최신 상태가 달라졌다면(이 짧은 경쟁 구간 사이에도 실제로 대상 버전이나 충돌이 바뀔 수 있음)
        **그 기존 행을 SUPERSEDED로 바꾸고, 최신 상태 기준의 새 후보를 발급**한다(2.8.3의 재계산 절차와
        동일 로직).

4. 조회된 행이 있으면(2단계 시점에 이미 존재했던 경우, 또는 3-c에서 새로 조회된 경우) —
   무조건 재사용하지 않는다:
   a. (UPDATE_EVENT인 경우) 대상 일정의 현재 version을 다시 조회
   b. 최신 활성 일정 기준으로 충돌 목록을 다시 계산해 new_conflict_snapshot_hash 산출
   c. 현재 version == 저장된 target_schedule_version
      AND new_conflict_snapshot_hash == 저장된 conflict_snapshot_hash
      → **변경이 없으므로 기존 confirmationId를 그대로 재사용**해 응답(내용 갱신 없음)
   d. 둘 중 하나라도 다르면
      → 기존 행 status='SUPERSEDED', superseded_by_confirmation_id는 곧 만들 새 행의 id로 채움
      → 최신 상태 기준으로 새 confirmation_requests 행을 INSERT(2.8.3의 "자동 재계산" 절차와 동일 로직)
      → 새 confirmationId·최신 제안 내용·최신 충돌 목록을 응답으로 반환
[트랜잭션 커밋]
부분 유니크 인덱스와의 정합성: UNIQUE(user_id, candidate_fingerprint) WHERE status = 'PENDING'은 "동일 사용자·동일 후보 지문 조합에 대해 PENDING 상태인 행은 항상 하나만 존재할 수 있다"는 것만 강제한다. 위 절차는 (1) 이미 존재하는 PENDING 행에 대해서는 SELECT ... FOR UPDATE로 경쟁을 막고, (2) 아직 존재하지 않는 PENDING 행을 향한 동시 최초 생성 경쟁은 DB의 고유 제약 자체가 막으며, 애플리케이션은 그 제약 위반을 정상적인 "이미 만들어졌음" 신호로 해석해 재조회·재검증으로 이어간다. 새 PENDING 행을 만들기 전에는 항상 기존 PENDING 행을 먼저 SUPERSEDED로 바꾸므로(4-d), 어떤 시점에도 동일 조합의 PENDING 행이 두 개 동시에 존재하지 않는다.

상태: 기술 설계 확정안

2.8.3 승인(소비) 절차와 SUPERSEDED 자동 재계산

[트랜잭션 시작]
1. SELECT * FROM confirmation_requests WHERE id = :confirmationId FOR UPDATE
2. 없거나 status != 'PENDING'이거나 expires_at <= now()
   → 실패 응답(만료/이미 처리됨, 404 CONFIRMATION_NOT_FOUND), 커밋(변경 없음), 종료
3. user_id 소유권 재검증(웹: 세션 사용자, 카카오: 연결된 사용자)
4. (UPDATE_EVENT인 경우) 대상 일정 재조회
   4-a. 대상이 더 이상 존재하지 않음
        → status='SUPERSEDED'(superseded_by_confirmation_id는 NULL로 둠 — 재구성 불가이므로 대체 후보가 없음)
        → 커밋, 404 CONFIRMATION_TARGET_GONE 응답("대상 일정을 찾을 수 없습니다. 다시 시도해주세요")
        → **이 경우에만 사용자 재입력을 요청한다.** 종료
   4-b. version != target_schedule_version → 5단계로 진행
5. 최신 활성 일정 기준 충돌 목록 재계산 → new_conflict_snapshot_hash
6. (4-b였거나) new_conflict_snapshot_hash != 저장된 conflict_snapshot_hash
   → **서버가 최신 일정·충돌 정보를 기준으로 후보를 자동으로 다시 계산한다**:
     a. 기존 행: status='SUPERSEDED'
     b. 저장된 델타(NULL이 아닌 title/start_at/end_at/location)를 최신 대상 일정 위에 덮어써
        최종 제안 내용을 재구성(CREATE_EVENT는 원래 제안값을 그대로 사용)
     c. 새 target_schedule_version(현재 값), 새 conflict_snapshot_hash, 새 candidate_fingerprint 계산
     d. 새 confirmation_requests 행 INSERT(status=PENDING, conflict_acknowledged=false, expires_at=now()+5분)
     e. 기존 행의 superseded_by_confirmation_id = 새 행 id
   → 커밋, **409 CONFIRMATION_SUPERSEDED 응답에 새 confirmationId + 최신 제안 내용 + 최신 충돌 목록을 함께 반환**
     (기존 confirmationId는 폐기, 다시 사용할 수 없음). 종료
7. 충돌이 존재하는데 이번 요청의 conflictAcknowledged != true
   → 소비하지 않음(status는 PENDING 유지), 409 CONFLICT_ACKNOWLEDGEMENT_REQUIRED 응답,
     트랜잭션 커밋(아무 것도 변경하지 않음), 종료
8. (충돌 없음, 또는 충돌 있고 6번에서 목록이 그대로이며 conflictAcknowledged=true)
   consumed_at=now(), status='CONSUMED'
   → 같은 트랜잭션 안에서 schedules INSERT/UPDATE(JPA @Version 2차 안전장치) 실행
   → schedule_change_history 기록
[트랜잭션 커밋]
"기존 정보만으로 새 후보 구성이 불가능한 경우"는 오직 4-a(대상 일정이 완전히 사라진 경우)뿐이다.
1회 실행 보장: 1단계의 SELECT ... FOR UPDATE가 동시 승인 요청 중 하나만 진행되도록 막는다.
같은 트랜잭션: 8단계의 소비와 일정 변경은 하나의 트랜잭션이다.
재승인 무한 반복 방지: 7단계는 "충돌 목록은 그대로인데 사용자 동의만 아직 실려 오지 않은" 경우에만 발생하며, 같은 confirmationId로 conflictAcknowledged: true를 한 번 더 보내면 즉시 종결된다. 충돌 목록 자체가 바뀌는 경우는 6단계에서 새 후보로 자동 전환되므로 같은 조건으로 반복되지 않는다.
2.8.4 PWA 직접 등록·수정의 충돌 승인(통합 확인 모델)

1. POST /api/v1/schedules 또는 PATCH /api/v1/schedules/{scheduleId}  (최초 요청, 승인 플래그 없음)
2. 서버가 입력 검증 + 충돌 계산
   - 충돌 없음 → 즉시 커밋
   - 충돌 있음 → DB 미변경, 2.8.2 절차로 confirmation_requests 생성(origin_channel=PWA),
     409 SCHEDULE_CONFLICT 응답 + confirmationId + 충돌 목록 반환
3. 사용자가 "그대로 저장" 클릭
4. 클라이언트가 POST /api/v1/confirmations/{confirmationId}/approve
   { "conflictAcknowledged": true } 호출
5. 서버는 2.8.3의 동일한 절차(잠금 → 재검증 → 소비/재계산)를 그대로 수행
최초 저장 요청은 승인 플래그를 받지 않으므로, 클라이언트가 임의로 플래그를 조작해 우회할 수 없다. 스키마·승인 절차가 자연어 경로와 완전히 동일하다.

2.8.5 멱등성(idempotency_records)
컬럼	설명
id	PK
scope_type	USER | KAKAO_BOT_USER
scope_id	범위 식별자(내부 ID)
request_key_hash	idempotency 키의 해시
request_fingerprint	정규화된 요청 본문 해시
status	PENDING | COMPLETED | FAILED
response_snapshot	완료 시 캐시된 응답의 최소 요약
created_at / expires_at	생성 시각 / created_at + 24시간
고유 제약: (scope_type, scope_id, request_key_hash). 만료 행은 하루 1회 배치 삭제. 카카오 원 요청 식별자를 안정적으로 얻을 수 있는지는 PoC 검증 필요.

상태: 2.8 전체 — 기술 설계 확정안(카카오 원 요청 식별자만 PoC 검증 필요)

2.9 PostgreSQL 데이터 모델
논리 모델이며 실제 DDL은 생성하지 않는다.

2.9.1 users

users
 - id (PK)
 - email (not null, unique)         -- trim + lower 정규화, 최대 254자
 - password_hash (not null)
 - timezone (not null, default 'Asia/Seoul')   -- IANA ZoneId, 계정 설정에서 변경 가능
 - created_at                       -- UTC 기준

회원가입 단계의 users 컬럼은 위 다섯 개뿐이다. name, display_name, nickname, updated_at은 두지 않는다. 이메일 고유 제약조건은 앞뒤 공백 제거와 소문자 변환을 마친 정규화 값에 적용한다. 가입 요청에서 시간대를 받지 않고 기본값 Asia/Seoul을 저장하며 이후 계정 설정에서 변경한다.
2.9.2 schedules

schedules
 - id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY
 - owner_user_id BIGINT NOT NULL               -- FK -> users.id, 삭제 정책 NO ACTION
 - title VARCHAR(200) NOT NULL
 - location VARCHAR(200) NULL
 - start_at TIMESTAMP WITH TIME ZONE NOT NULL
 - end_at TIMESTAMP WITH TIME ZONE NOT NULL
 - version BIGINT NOT NULL DEFAULT 0            -- JPA @Version
 - created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
 - updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP

 CHECK (end_at > start_at)

 INDEX idx_schedules_owner_start ON schedules (owner_user_id, start_at)
owner_user_id는 users.id 외래 키이며 PostgreSQL 기본 NO ACTION을 사용한다. 계정 탈퇴 시 2.7.9의 단일 트랜잭션에서 사용자 소유 일정을 먼저 하드 삭제하므로 ON DELETE CASCADE를 추가하지 않는다.
충돌 조건(자기 제외):


SELECT 1 FROM schedules existing
WHERE existing.owner_user_id = :ownerId
  AND existing.id <> :excludeScheduleId
  AND existing.start_at < :candidateEndAt
  AND existing.end_at   > :candidateStartAt;
삭제: DELETE FROM schedules WHERE id = :id AND owner_user_id = :ownerId — 즉시 하드 삭제.

2.9.3 schedule_change_history

schedule_change_history
 - id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY
 - schedule_id BIGINT NOT NULL                 -- FK -> schedules.id, ON DELETE CASCADE
 - changed_by_user_id BIGINT NOT NULL          -- FK -> users.id, 삭제 정책 NO ACTION
 - source_channel VARCHAR(20) NOT NULL         -- PWA | WEB_CHAT | KAKAO
 - change_type VARCHAR(20) NOT NULL            -- CREATE | UPDATE | DELETE
 - changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
 - title_before VARCHAR(200) NULL
 - title_after VARCHAR(200) NULL
 - start_at_before TIMESTAMP WITH TIME ZONE NULL
 - start_at_after TIMESTAMP WITH TIME ZONE NULL
 - end_at_before TIMESTAMP WITH TIME ZONE NULL
 - end_at_after TIMESTAMP WITH TIME ZONE NULL
 - location_before VARCHAR(200) NULL
 - location_after VARCHAR(200) NULL

 CHECK (source_channel IN ('PWA', 'WEB_CHAT', 'KAKAO'))
 CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE'))
 INDEX idx_history_schedule ON schedule_change_history (schedule_id, changed_at)
 INDEX idx_history_changed_by ON schedule_change_history (changed_by_user_id, changed_at)
일정이 하드 삭제되면 이력도 함께 삭제된다(ON DELETE CASCADE). 삭제된 일정을 조회하는 기능이 MVP에 없으므로, 근거 없는 장기 보관을 피하기 위해 별도의 삭제 감사 로그는 두지 않는다.
CREATE 이력은 before 컬럼을 NULL로 두고 생성된 일정 값을 after 컬럼에 기록한다. UPDATE는 before·after를 모두 기록한다. 일정 삭제 시 새 DELETE 이력은 생성하지 않고 기존 CREATE·UPDATE 이력만 같은 트랜잭션의 schedules 하드 삭제와 함께 CASCADE 삭제한다. 물리 CHECK에 남아 있는 change_type DELETE는 현재 MVP에서 사용하지 않는다.

2.9.4 kakao_user_links

kakao_user_links
 - id (PK)
 - user_id (FK -> users.id, unique)
 - logical_bot_key (text, not null)
 - external_user_hmac (text, not null)   -- 15절
 - linked_at (not null)

 UNIQUE (logical_bot_key, external_user_hmac)
연결 해제 시 하드 삭제(기존 결정 유지).
동일한 `(logical_bot_key, external_user_hmac)`가 이미 연결되어 있으면 새 연결 코드로 기존 연결을 자동 변경하지 않는다. 동일 CalTalk 계정으로 재연결을 시도해도 기존 연결 상태를 안내하고, 다른 CalTalk 계정의 코드를 입력한 경우에는 기존 연결을 먼저 명시적으로 해제하도록 안내한다. 두 경우 모두 거부된 연결 코드는 소비하지 않는다.

2.9.5 connection_codes

connection_codes
 - id (PK)
 - user_id (FK -> users.id)
 - code_hmac (text, not null)
 - issued_at (not null)
 - expires_at (not null)     -- issued_at + 5분
 - used_at (nullable)
 - invalidated_at (nullable)
 - fail_count (integer, not null, default 0)   -- 19절: 코드당 실패 5회 제한에 사용, PostgreSQL 저장 유지

 INDEX idx_connection_codes_user ON connection_codes (user_id, issued_at)
2.9.6 pending_commands

pending_commands
 - id (PK)
 - user_id (FK -> users.id, NOT NULL)
 - source_channel (WEB_CHAT | KAKAO)
 - command_type (CREATE_EVENT | UPDATE_EVENT | SEARCH_EVENTS)
 - partial_payload
 - missing_fields
 - created_at
 - expires_at    -- created_at + 10분
2.9.7 confirmation_requests
2.8.1 참조.

2.9.8 idempotency_records
2.8.5 참조.

2.9.9 login_security_state

login_security_state
 - user_id (PK, FK -> users.id)
 - failed_login_count (integer, not null, default 0)
 - locked_until (timestamptz, nullable)
 - last_failed_at (timestamptz, nullable)
 - updated_at (not null)
로그인 시도마다 last_failed_at이 현재 시각으로부터 15분보다 오래됐으면 failed_login_count를 0으로 리셋한 뒤 처리해, 별도 배치 없이 "15분 롤링 윈도" 효과를 낸다.
실패 시 failed_login_count를 증가시키고 last_failed_at을 갱신, 5회에 도달하면 locked_until = now() + 15분을 설정한다.
성공 시 failed_login_count = 0, locked_until = NULL로 초기화한다.
login_security_state가 후속 migration으로 추가되면 회원 탈퇴 DB 트랜잭션에서 해당 사용자 행을 하드 삭제한다(2.7.9). 현재 V1~V3의 실제 삭제 대상에는 포함되지 않는다.
2.9.10 Spring Session JDBC 인프라 테이블
SPRING_SESSION, SPRING_SESSION_ATTRIBUTES — 도메인 테이블과 구분되는 인프라 테이블(2.7.10).

상태: 기술 설계 확정안

2.10 날짜·시간과 과거 일정 규칙
항목	결정
DB 저장 형식	timestamptz(UTC)
Java 타입	Instant + ZoneId
사용자 시간대	users.timezone, IANA ZoneId만 허용, 기본값 Asia/Seoul, 변경 가능
상대 날짜 해석 위치	항상 백엔드, 요청 시점 사용자 저장 시간대 기준
자정 경계 일정	end_at > start_at만 강제
웹·카카오 자연어를 통한 과거 일정 생성	거부
자연어를 통한 과거 시각으로의 수정	거부
모호한 표현	표현 자체가 모호할 때만 재질문
PWA 직접 입력	과거 일정 등록·수정·삭제 허용
DST	Asia/Seoul은 서머타임 미적용(IANA tz database). 시간 계산은 항상 Instant/ZoneId 기반
상태: 확정

2.11 일정 도메인 규칙과 삭제 정책
생성: 필수(제목·날짜·시작시간), 종료 누락 시 시작 + 1시간 자동 설정, 저장은 확인 후 1회
조회: 소유자 필터 강제
부분 수정(지속시간 유지): 시작 시각만 바뀌면 기존 지속시간을 유지해 종료 시각 재계산
삭제: PWA에서 삭제하면 즉시 하드 삭제, status 컬럼 없음, 종속 이력은 ON DELETE CASCADE로 함께 삭제, 삭제된 일정은 조회·충돌·복구 대상으로 남지 않음, 휴지통·복구 기능 없음
회원 탈퇴 시 현재 V1~V3 범위와 후속 테이블 범위를 구분해 2.7.9의 순서로 하드 삭제
충돌 판정과 최종 재검증은 2.8의 절차를 따른다
상태: 확정

2.12 자연어 명령 처리
2.12.1 허용 명령과 구조화 출력
SEARCH_EVENTS, CREATE_EVENT, UPDATE_EVENT, UNKNOWN(확정). command 판별자 기반 유니온 스키마, missingFields/ambiguities 필드명 통일, UPDATE_EVENT의 target/changes 분리, 장소 3상태(action: KEEP|REMOVE|SET)를 유지한다.

2.12.2 OpenAI 연동 원칙과 데이터 처리 표현
Structured Outputs와 엄격한 JSON Schema를 기본안으로 사용한다.
store: false는 다음으로만 설명한다: OpenAI Responses API 호출 시 이 파라미터를 false로 설정하면 해당 호출의 응답 객체를 OpenAI 서버 쪽에서 이후 조회 가능한 상태로 저장하지 않는다는 뜻이다. 데이터가 전혀 처리·보관되지 않는다는 의미는 아니다.
Zero Data Retention(ZDR)은 별도 개념이다. 별도의 조직 단위 자격 심사와 계정/조직 설정이 필요한 정책이며, store: false 사용만으로 자동 적용되지 않는다. CalTalk가 ZDR을 사용한다고 이 문서에서 확정하지 않는다.
실제 API 데이터 보관·처리 정책은 구현 직전 OpenAI 공식 문서와 실제 사용할 계정/조직 설정에서 재확인한다.
개인정보 처리 안내에는 다음 사실만 반영한다: 자연어로 입력한 일정 관련 텍스트가 명령 해석을 위해 외부 API(OpenAI)로 전달될 수 있다.
Structured Outputs가 형식을 보장해도 의미적 정확성(날짜, 과거 일정, 권한, 대상, 충돌)은 보장하지 않으므로 서버에서 다시 검증한다.
모델명은 고정하지 않는다. 구현 직전 OpenAI 공식 지원 모델 목록에서 재확인한다.
이메일, 내부 사용자 ID, 인증 토큰은 전달하지 않는다.
상태: 기술 설계 확정안(store:false≠ZDR 구분, 실제 보관 정책은 PoC/구현 직전 재확인 필요)

2.13 대화 상태와 확인 절차
상태	유효시간
재질문 대기(pending_commands)	10분
최종 확인 대기(confirmation_requests)	5분
1회 실행·동시성 보장·자동 재계산·무한 반복 방지는 8절 참조. 동일 요청 중복 처리는 idempotency_records(2.8.5) 참조.

상태: 확정

2.14 카카오 연동 설계
스킬 요청 DTO, 봇별 사용자 식별값 매핑, 일회용 연결 코드, 5초 대응, 콜백 비필수 원칙(기존 결정 유지). AI 서비스 장애 시 카카오 채널의 응답 방식은 18.2절 참조.

누적 PoC 실측 결과:

| 검증 ID | 실제 측정값 | 증적 ID | 판정 | 판정 이유 |
|---|---|---|---|---|
| KPF-001 | 고정 응답 10회 연속 호출 성공, 서버 처리시간 0~1ms, 타임아웃·오류 0건, 사용자 식별값 10회 동일 | REQ-KPF-001-01, RES-KPF-001-01, TIM-KPF-001-01, SEC-KPF-001-01 | 통과 | 10회 모두 안정적으로 동일 사용자의 정상 응답을 확인 |
| KPF-002 | 실제 카카오톡 개발 채널에서 동일 사용자의 미연결 상태 조회 10회가 모두 서버에 도달하고 말풍선이 정상 표시됨. 10회 모두 “아직 CalTalk 계정과 연결되지 않았습니다.”로 동일했으며 서버 내부 elapsedMs는 0ms 8회·1ms 2회, 최대 1ms였다. 사용자 유형·마스킹된 사용자 식별값·시간대·언어가 일관됐고 테스트 헤더도 10회 모두 정상 수신됨 | REQ-KPF-002-01, RES-KPF-002-01, TIM-KPF-002-01, UI-KPF-002-01, SEC-KPF-002-01 | 통과 | 10회 모두 연결 상태 시나리오 목표 800ms 이하이고 실제 화면에서도 5초 이내 응답했으며 오류·타임아웃·응답 누락이 없었음. elapsedMs는 전체 네트워크 왕복시간이 아닌 서버 내부 측정값이므로 실제 말풍선 표시와 함께 판정했으며, 이번 결과는 미연결 상태 조회 경로에 한정됨 |
| KPF-003 | 단순 모의 메모리 조회의 응답시간 안정성을 10회 검증하는 항목으로 해석되며 목표 서버 시간 1초 이하, 내부 타임아웃 1.5초, 일반 스킬 5초 제한과 목표 4초 기준을 적용함. 그러나 비변경 메모리 조회 대상, 사용자 상태, 정확한 발화·응답과 KPF-003 전용 통과·제한적 통과·실패 기준이 정의되지 않았고 server.js에도 전용 발화·분기·응답이 없음. getConnectionStatus()는 KPF-002 범위이며 GET /health도 KPF-003 증적으로 대체할 근거가 없음 | 실행 전 | 미실행 | HOLD — 비변경 메모리 조회 대상, 정확한 발화, 응답 및 판정 기준이 정의되지 않았고 전용 구현 경로도 없어 현재 환경에서 검증할 수 없음. 대상·발화·응답·판정 기준을 먼저 확정하고 필요 시 최소 전용 분기를 구현한 뒤 10회 반복 검증 |
| KPF-007 | 3초 지연 응답 실제 말풍선 표시 성공, 약 3014ms | TIM-KPF-007-01, UI-KPF-007-01 | 통과 | 의도한 지연 후 실제 채널 표시 성공 |
| KPF-008 | 4초 지연 응답 실제 말풍선 표시 성공, 약 4007ms | TIM-KPF-008-01, UI-KPF-008-01 | 통과 | 의도한 지연 후 실제 채널 표시 성공 |
| KUI-017 | quickReplies 10개 표시, 10번째 항목 접근·클릭·발화 재전송 성공 | RES-KUI-017-01, UI-KUI-017-01, REQ-KUI-017-01 | 통과 | 최대 10개 표시와 마지막 항목의 후속 호출 확인 |
| KUI-008 | BasicCard 제목·설명·줄바꿈 정상 표시 | RES-KUI-008-01, UI-KUI-008-01 | 통과 | 실제 채널에서 카드 구성과 줄바꿈 확인 |
| KUI-013 | BasicCard 세로 버튼 3개 표시·클릭 성공 | RES-KUI-013-01, UI-KUI-013-01 | 통과 | 세로 버튼 3개의 표시와 동작 확인 |
| KUI-012 | webLink 버튼 표시 및 외부 웹페이지 이동 성공, 사용자 발화·서버 재호출 없음 | RES-KUI-012-01, UI-KUI-012-01, REQ-KUI-012-01 | 통과 | 외부 이동 동작과 스킬 재호출 비발생 확인 |
| KSE-005 | GET 스킬 요청 HTTP 404, 서버와 health 정상 유지 | REQ-KSE-005-01, RES-KSE-005-01 | 통과 | 허용하지 않은 메서드를 거부하고 서버 건전성 유지 |
| KSE-006 | text/plain HTTP 415, application/json HTTP 200 | REQ-KSE-006-01, RES-KSE-006-01 | 통과 | Content-Type 정책의 거부·허용 경계 확인 |
| KSE-002 | 헤더 누락·오류 HTTP 401, 올바른 헤더와 실제 개발 채널 요청 정상 | REQ-KSE-002-01, RES-KSE-002-01, SEC-KSE-002-01 | 통과 | 헤더 검증의 실패·성공 경로 확인 |
| KSE-008 | 사용자 ID 누락 시 예외 없이 안전 응답 | REQ-KSE-008-01, RES-KSE-008-01 | 통과 | 필수 식별정보 누락을 안전하게 처리 |
| KSE-007 | 1MiB 초과 본문 감지 후 안전 응답, 서버 정상 유지 | REQ-KSE-007-01, RES-KSE-007-01 | 통과 | 본문 제한 적용과 서버 건전성 유지 확인 |
| KSE-009 | 실제 카카오톡 개발 채널에서 동일 사용자가 일반 비변경 발화를 1초 미만 간격으로 6회 전송함. 단일 Node.js 인스턴스에서 HMAC 사용자 키와 메모리 Map 기반 10초 고정 시간창·사용자별 5회 허용 정책을 적용한 결과 1~5회차는 일반 정상 응답, 6회차는 HTTP 200 정상 Kakao SkillResponse의 “요청이 너무 많습니다. 잠시 후 다시 시도해주세요.” 안전 안내가 표시됐고 10초 이상 경과 후 다음 요청은 제한 로그 없이 정상 복구됨. 서버 프로세스와 `GET /health`가 정상이고 activeCodes 0·linkedUsers 0이며 오류·타임아웃·응답 누락 없음 | REQ-KSE-009-01~07, RES-KSE-009-01~07, TIM-KSE-009-01, SEC-KSE-009-01, UI-KSE-009-01 | 제한적 통과 | 제한 로그는 rateLimitExceeded true, rateLimitCategory KAKAO_SKILL_USER, requestCount 6, limit 5, windowSeconds 10, retryAfterSeconds 5였으며 발화·사용자 식별값·HMAC 키·헤더/토큰 원문 또는 일부와 전체 req.body·req.headers·process.env는 출력되지 않음. 동일 사용자의 허용·차단·시간창 후 복구와 서버 생존·로그 비노출은 확인했으나 두 번째 실제 사용자가 없어 사용자별 독립 제한을 실제 검증하지 못했고 단일 인스턴스 메모리 방식에 한정된다. 다중 서버·Redis·분산 제한은 미검증이며 서버 재시작 시 제한 상태가 초기화됨 |
| KSE-011 | 수정 전 요청 수신 로그에 userRequest.utterance 원문이 출력되는 문제를 확인하고 원문 로그를 제거함. 수정 후 실제 카카오톡 개발 채널 요청이 정상 처리됐으며 로그에는 utterancePresent true, utteranceLength 17, utteranceUtf8Bytes 25와 elapsedMs 1ms만 기록되고 발화 원문·일부·마스킹 문자열·해시는 기록되지 않음. 사용자 식별값·헤더값·요청 ID 후보는 기존 마스킹 상태를 유지하고 오류·타임아웃 없이 서버가 정상 유지됨 | REQ-KSE-011-01, RES-KSE-011-01, SEC-KSE-011-01 | 통과 | 발화 원문을 저장하지 않으면서 운영에 필요한 존재 여부·문자 수·UTF-8 바이트 수만 기록하고 기존 응답 기능과 스킬 동작을 유지함 |
| KSE-013 | 수정 전 testHeaderMasked가 테스트 헤더 원문의 앞·뒤 일부를 로그에 남기는 문제를 확인하고 해당 필드를 제거함. testHeaderReceived·testHeaderLength와 사용자 식별값·요청 ID 후보의 기존 마스킹은 유지함. 스킬 헤더는 정상 요청 성공, 잘못된 값과 누락 요청 HTTP 401, 복구 후 실제 카카오톡 요청 정상을 확인함. 관리자 토큰은 누락·잘못된 값 HTTP 401, 정상 값 HTTP 201을 확인했으며 원문·부분 원문·해시와 전체 req.headers는 로그에 남지 않음. 정상 관리자 호출의 연결 코드 원문은 응답으로만 반환되고 로그에는 남지 않았으며 서버 재시작 후 activeCodes 0, linkedUsers 0으로 복구됨 | REQ-KSE-013-01, RES-KSE-013-01, SEC-KSE-013-01 | 통과 | 스킬 헤더와 관리자 토큰의 정상·오류·누락 분기가 정상 동작하고 코드와 서버 로그에 원문·부분 원문·전체 헤더를 저장하지 않음을 확인함. request body 전체와 process.env 전체를 console.log 또는 console.error로 출력하는 코드가 없고 인증 실패·예외 처리에서도 request body 전체나 환경변수 전체를 로그에 남기지 않음. 파일·DB 저장 로직은 없고 로그는 콘솔만 사용하며 외부 콘솔 보존 정책은 PoC 코드 감사 범위 밖임 |
| KUI-015 | HTTP 200의 잘못된 JSON에서 정상 말풍선·깨진 원문 미표시, 관리자센터 오류 내역 없음 | RES-KUI-015-01, UI-KUI-015-01, ERR-KUI-015-01 | 제한적 통과 | 사용자 화면의 안전성은 확인했으나 관리자센터 오류 증적 없음 |
| KPF-012 | HTTP 400에서 정상 말풍선·내부 오류 원문 미표시, 관리자센터 오류 내역 없음 | RES-KPF-012-01, UI-KPF-012-01, ERR-KPF-012-01 | 제한적 통과 | 안전한 사용자 화면은 확인했으나 관리자센터 오류 증적 없음 |
| KPF-013 | HTTP 500에서 정상 말풍선·내부 오류 원문 미표시, 관리자센터 오류 내역 없음 | RES-KPF-013-01, UI-KPF-013-01, ERR-KPF-013-01 | 제한적 통과 | 안전한 사용자 화면은 확인했으나 관리자센터 오류 증적 없음 |
| KPF-014 | 터널 단절 시 말풍선 미표시·내부 URL 및 오류 원문 미노출·관리자센터 오류 내역 없음, 복구 후 정상 응답 | UI-KPF-014-01, ERR-KPF-014-01, RES-KPF-014-01 | 제한적 통과 | 장애·복구 동작은 확인했으나 관리자센터 오류 증적 없음 |
| KUI-018 | ASCII simpleText 30000·31000·40000·60000자 모두 표시 및 전체보기 제공, 관리자센터 오류 내역 없음; 공식 30720바이트의 플랫폼 계산값과 서버 text UTF-8 바이트가 동일하지 않음 | RES-KUI-018-01, UI-KUI-018-01, ERR-KUI-018-01 | 제한적 통과 | 표시 동작은 확인했으나 정확한 차단 경계 미확인 |
| KSK-012 | 관리자센터 테스트 도구와 실제 개발 채널 모두 동일 스킬 서버를 정상 호출하고 서버 처리시간 0ms 확인. 양쪽 모두 simpleText와 quickReplies가 정상 동작했으며 실제 채널에서는 말풍선과 quickReplies 3개가 정상 표시됨. 테스트 도구는 userType accountId, timezone Asia/Seoul, lang null, propertyKeys 없음, isFriend null이었고 실제 채널은 userType botUserKey, timezone Asia/Seoul, lang ko, 사용자 속성 키 존재, isFriend true였음 | REQ-KSK-012-01, RES-KSK-012-01, UI-KSK-012-01 | 통과 | 핵심 응답 표시 동작은 동일하며 사용자 식별 타입과 속성 차이가 PoC 응답에 문제를 일으키지 않음을 확인. 관리자센터 accountId를 실제 채널 botUserKey와 동일 사용자 식별자로 취급하지 않도록 주의 |
| KID-001 | 관리자센터 스킬 테스트 도구에서 동일 JSON과 발화를 5회 연속 전송해 모두 서버 도달·정상 응답, 처리시간 0ms, 오류·타임아웃 없음 확인. userType·사용자 식별값·발화는 동일했고 headerXRequestId는 매회 달랐으며 intentId·actionId·blockId·cloudflareRay는 동일했음 | REQ-KID-001-01~05, RES-KID-001-01~05, TIM-KID-001-01~05 | 제한적 통과 | 일반 반복 호출에서 x-request-id가 요청별 구분 후보임을 확인했으나 플랫폼 자동 재시도·동일 요청 재전송 시 유지 여부는 미확인. intent.id·action.id·block.id와 cloudflareRay는 단독 멱등성 키로 사용할 수 없으며 KID-001만으로 완전한 멱등성·중복 처리 정책을 확정하지 않음 |
| KID-002 | 실제 카카오톡 개발 채널에서 동일한 미연결 사용자가 기존 동일 퀵리플라이 버튼을 5회 반복 클릭함. 준비 발화 5회와 버튼 발화 5회 등 총 10회 요청이 모두 서버에 도달했고 버튼 클릭 5회 모두 사용자 발화 전송·정상 말풍선 표시가 이루어졌으며 오류·타임아웃·응답 누락이 없었음. 버튼 요청은 utterancePresent true, utteranceLength 6, utteranceUtf8Bytes 16, userType botUserKey, 동일한 마스킹 사용자 식별값, testHeaderReceived true, testHeaderLength 44로 일관됨. headerXRequestId는 5회 모두 달랐고 cloudflareRay·intentId·botId·actionId·blockId는 동일했음 | REQ-KID-002-01~05, RES-KID-002-01~05, UI-KID-002-01~05, SEC-KID-002-01 | 제한적 통과 | 동일 버튼 반복 클릭이 각각 별도 요청으로 처리되고 x-request-id가 일반 버튼 반복 요청의 구분 후보임을 확인함. intent.id·action.id·block.id·bot.id는 요청별 고유 ID가 아니며 cloudflareRay도 단독 멱등성 키로 사용할 수 없음. KID-002는 사용자가 동일 버튼을 반복 클릭해 생성한 신규 요청의 안정성만 검증하며 플랫폼 자동 재시도·동일 요청 재전송 시 식별자 유지 여부는 미확인이다. 따라서 KID-002 단독 결과만으로 서비스의 완전한 멱등성·중복 처리 정책을 확정하지 않으며 다른 KID 계열 테스트 결과와 함께 판단한다 |
| KID-003 | 관리자센터 스킬 테스트 화면에서 `클립보드로 복사`, `스킬서버로 전송`, `응답 초기화` 버튼을 확인했으나 기존 요청을 대상으로 하는 명시적 재전송·다시 요청·요청 다시 보내기 기능은 확인하지 못함. `스킬서버로 전송` 재클릭과 동일 JSON의 PowerShell·curl 전송은 모두 새 요청을 생성하므로 KID-003 재전송 증적으로 사용하지 않음 | CFG-KID-003-01 | 미실행 | HOLD — 원 요청 재전송 기능을 확인할 수 없어 원 요청과 재전송 요청 사이의 요청 ID·페이로드 유지 여부를 검증할 수 없음. `스킬서버로 전송` 재클릭은 KID-001의 동일 발화 반복 호출 범위로 구분 |
| KID-004 | 실제 카카오톡 개발 채널에서 동일한 미연결 사용자가 상태를 변경하지 않는 일반 텍스트 발화를 약 1~2초 간격으로 5회 전송함. 5회 모두 서버 도달·정상 말풍선 표시, 오류·타임아웃·응답 누락 없음. 서버 처리시간은 0ms 4회·1ms 1회, 최대 1ms였음. utterancePresent true, utteranceLength 13, utteranceUtf8Bytes 21, timezone Asia/Seoul, lang ko, userType botUserKey, 동일한 마스킹 사용자 식별값·propertyKeys, isFriend true, testHeaderReceived true, testHeaderLength 44가 5회 일관됨. headerXRequestId는 모두 달랐고 cloudflareRay·intentId·botId·actionId·blockId는 동일했음 | REQ-KID-004-01~05, RES-KID-004-01~05, TIM-KID-004-01~05, UI-KID-004-01~05, SEC-KID-004-01 | 제한적 통과 | 동일 일반 텍스트 발화 5회는 서버에 5개의 개별 요청으로 도달했고 각 요청은 서로 다른 headerXRequestId로 구분되어, 동일 요청 재사용이 아니라 각각 새로 생성된 별도 요청으로 처리됨. botUserKey 기반 실제 채널 일반 텍스트 반복 요청의 안정성과 요청별 x-request-id 차이를 확인함. intent.id·action.id·block.id·bot.id는 요청별 고유 ID가 아니며 cloudflareRay도 단독 멱등성 키로 사용할 수 없음. KID-001의 관리자센터 accountId 반복 결과와 다른 실제 채널 동작을 확인했으나 이 결과는 플랫폼 자동 재시도·원 요청 재전송 시 식별자 유지 여부를 증명하지 않으므로 KID-004 단독으로 완전한 멱등성·중복 처리 정책을 확정하지 않음 |
| KID-008 | 실제 카카오톡 개발 채널에서 동일 사용자가 단일 Node.js 인스턴스의 메모리 Map 기반 HMAC 보조 지문과 3초 시간창을 사용해 상태 변경 전용 요청을 3회 수행함. 최초 요청은 HTTP 200 정상 SkillResponse의 “요청 처리가 완료되었습니다.” 말풍선과 duplicateDetected false·stateMutationApplied true·mockMutationCount 1·elapsedMs 3, 3초 이내 두 번째 요청은 “이미 처리된 요청입니다.” 말풍선과 duplicateDetected true·stateMutationApplied false·mockMutationCount 1·elapsedMs 1, 3초 이후 세 번째 요청은 다시 처리 완료 말풍선과 duplicateDetected false·stateMutationApplied true·mockMutationCount 2·elapsedMs 1을 확인함. 세 회 모두 duplicateCategory KAKAO_SKILL_FINGERPRINT·dedupeWindowSeconds 3이 일관되고 headerXRequestId는 서로 달랐으며 사용자·시간대·언어·친구 상태·블록 관련 정보는 일관됨 | REQ-KID-008-01~03, RES-KID-008-01~03, TIM-KID-008-01~03, UI-KID-008-01~03, SEC-KID-008-01 | 제한적 통과 | x-request-id가 달라도 보조 지문으로 3초 이내 중복 상태 변경을 막고 3초 이후 새 요청으로 재처리해 mockMutationCount 1→1→2를 확인함. 모든 응답과 말풍선이 정상이고 서버·GET /health 정상, activeCodes 0·linkedUsers 0이며 발화·정규화 발화·지문 전체/일부·HMAC 키·사용자 식별값·clientExtra·전체 req.body/req.headers/process.env·인증값·연결 코드 원문은 로그에 출력되지 않음. 다만 실제 플랫폼 자동 재시도가 아니라 사용자가 새로 보낸 동일 요청과 모의 상태 변경만 검증했으며 실제 일정 CRUD·DB 트랜잭션, 두 실제 사용자 독립성, 다중 서버·Redis·분산 중복 방지는 미검증이다. 서버 재시작 시 중복 Map과 카운터가 초기화되므로 이 결과는 단일 인스턴스 PoC 방어 수단에 한정됨 |
| KPF-011 | 실제 카카오톡 개발 채널의 전용 테스트 요청에서 기존 요청 처리 try/catch 범위 안에 비민감 고정 내부 예외를 동기 방식으로 1회 발생시킴. 프로세스 종료 없이 5초 이내에 “요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.”라는 일반 안전 안내가 표시됐고 내부 오류 메시지·Error·스택·파일 경로·내부 함수명과 인증값·식별값·터널 정보는 사용자에게 노출되지 않음. 로그에는 errorOccurred true, errorCategory KPF011_TEST, 오류 처리 elapsedMs 2ms만 기록됐으며 전체 Error·스택·발화 원문·request body·req.headers·환경변수 및 민감정보 원문은 출력되지 않고 기존 사용자·요청 ID 마스킹이 유지됨 | REQ-KPF-011-01, RES-KPF-011-01, TIM-KPF-011-01, UI-KPF-011-01, SEC-KPF-011-01 | 통과 | 의도적 예외가 기존 예외 처리 경로에서 안전하게 처리됐고 일반 안전 안내 표시 후 서버 프로세스가 유지됨. 직후 일반 요청의 말풍선과 quickReplies 3개가 정상 표시되고 GET /health도 정상이었으며 activeCodes 0·linkedUsers 0, 오류·타임아웃·응답 누락 없음으로 서버 생존·후속 처리·로그 비노출 정책을 확인함 |

KSK-011은 두 번째 실제 카카오 계정이 없어 HOLD이며 미실행 상태를 유지한다. 누적 집계는 통과 50, 제한적 통과 14, 미실행 33, 전체 97이다. 실제 사용자 식별값·연결 코드·헤더값·토큰·터널 URL·이메일은 기록하지 않았다.

상태: 구조 원칙 확정, 콜백·정확한 스킬 스펙은 PoC 검증 필요

2.15 카카오 사용자 식별값 보호

external_user_hmac = HMAC-SHA-256(server_secret, logical_bot_key + ":" + kakao_user_id)
원문 미저장, 선제 메시지 미사용으로 복호화 불필요(기존 결정 유지).

상태: 기술 설계 확정안

2.16 카카오 스킬 요청 진위 검증
userRequest.user.id만으로 정상 요청을 확정하지 않는다. 서명/고정 헤더/IP 대역/사용자 정의 인증 헤더 제공 여부는 PoC 필수 검증 항목이며, 수단이 없을 경우 비밀 경로 + 빈도 제한 + 해시 매칭 + 로그 마스킹으로 방어한다(기존 결정 유지).

상태: PoC 검증 필요(카카오 제공 수단), 대안 원칙은 기술 설계 확정안

2.17 연결 코드 규칙
8자리(혼동 문자 제외), 5분, 1회, 코드당 실패 5회, 사용자당 활성 코드 1개, 10분 3회 발급 제한, 원문 미저장(HMAC), 연결 완료·해제 시 기존 대기 명령·확인 요청 폐기(기존 결정 유지).
이미 CalTalk 계정과 연결된 카카오 사용자는 새 연결 코드로 기존 연결을 자동 덮어쓸 수 없다. 동일 계정 재연결과 다른 계정 연결 시도를 모두 거부하고 코드를 소비하지 않으며, 다른 계정으로 변경하려면 기존 연결을 먼저 명시적으로 해제해야 한다. 다른 계정 연결 시 안내 문구는 “이미 다른 CalTalk 계정과 연결되어 있습니다. 기존 연결을 해제한 뒤 다시 시도해 주세요.”로 한다.
PoC에서 기연결 카카오 사용자가 동일 계정의 새 유효 코드를 입력했을 때도 `ALREADY_CONNECTED`로 거부되고 기존 연결이 유지되며, 거부된 코드는 소비·무효화되지 않았다. 연결 해제 후 동일 코드를 다시 입력해 연결에 성공했고 최종 연결 계정도 동일하게 유지됨을 확인했다. 다른 계정의 유효 코드를 입력한 경우에도 `ALREADY_CONNECTED`로 거부되고 기존 연결과 새 코드의 미사용 상태가 유지되며, 기존 연결을 명시적으로 해제한 뒤 동일 코드를 다시 입력하면 새 계정으로 정상 연결되는 것을 확인했다. 실제 사용자·코드·식별값 원문은 기록하지 않았다.

상태: 기술 설계 확정안

2.18 API 설계 원칙과 오류 모델
2.18.1 리소스와 엔드포인트
리소스	엔드포인트
인증·현재 사용자	POST /api/v1/auth/signup, POST /api/v1/auth/login, POST /api/v1/auth/logout, GET /api/v1/users/me, PATCH /api/v1/users/me, DELETE /api/v1/users/me
일정	GET /api/v1/schedules, GET /api/v1/schedules/{scheduleId}, POST /api/v1/schedules, PATCH /api/v1/schedules/{scheduleId}, DELETE /api/v1/schedules/{scheduleId}
웹 자연어 대화	POST /api/v1/chat/messages
확인(자연어+PWA 충돌 공통)	POST /api/v1/confirmations/{confirmationId}/approve, POST /api/v1/confirmations/{confirmationId}/cancel
카카오 연결 코드	POST /api/v1/kakao/link-codes, POST /api/v1/kakao/links/revoke
카카오 스킬 웹훅	POST /api/v1/kakao/skill(비밀 경로 원칙, 16절)
2.18.2 AI 서비스 장애의 HTTP 처리
웹 REST API에서 OpenAI 장애·타임아웃은 항상 HTTP 503 + AI_SERVICE_UNAVAILABLE로 응답한다.
LLM이 정상 응답했지만 의미를 해석하지 못한 경우만 HTTP 200 + state: REPHRASE_REQUIRED(대화형 응답)로 처리한다.
카카오 채널에서는 항상 카카오 규격의 정상 응답 JSON 안에 안전한 실패 안내 발화를 담아 반환한다(내부적으로는 원인을 구분해 로깅하되, 카카오로 나가는 HTTP 응답은 항상 카카오가 기대하는 정상 스킬 응답 형태).
2.18.3 확인 승인 엔드포인트의 상태 코드
상황	HTTP	코드	응답 본문
승인 성공	200	—	반영된 일정
confirmationId 없음/만료/이미 처리됨	404	CONFIRMATION_NOT_FOUND	—
소유권 불일치	403	FORBIDDEN	—
대상 일정 소멸(재구성 불가)	404	CONFIRMATION_TARGET_GONE	재입력 안내
대상 버전 또는 충돌 목록 변경(자동 재구성됨)	409	CONFIRMATION_SUPERSEDED	새 confirmationId + 최신 제안 내용 + 최신 충돌 목록
충돌 있고 미승인	409	CONFLICT_ACKNOWLEDGEMENT_REQUIRED	현재 충돌 목록(변경 없음)
최초 저장 요청에서 충돌 발견 시: 409 SCHEDULE_CONFLICT + confirmationId + 충돌 목록(2.8.4).
상태별 오류는 기존 계약을 유지한다. PENDING이 아니거나 만료·소비·취소된 confirmation은 404 CONFIRMATION_NOT_FOUND, 최신 일정 버전 또는 충돌 목록 변화로 대체되면 409 CONFIRMATION_SUPERSEDED, 충돌 목록이 같지만 명시적 동의가 없으면 409 CONFLICT_ACKNOWLEDGEMENT_REQUIRED를 사용한다. 소유권 불일치는 403 FORBIDDEN이다. `STALE_CONFIRMATION`은 기존 오류 사전에 정의되어 있지 않으므로 이번 작업에서 새로 추가하지 않으며, 별도 코드가 필요한지는 구현 전 후속 결정으로 보류한다.

2.18.4 422의 범위
클라이언트가 보낸 요청 DTO 자체의 형식 오류에만 사용한다(기존 결정 유지).

2.18.5 회원가입 요청·응답과 오류 계약
요청은 POST /api/v1/auth/signup과 다음 JSON을 사용한다.

```json
{
  "email": "user@example.com",
  "password": "example-password",
  "passwordConfirmation": "example-password"
}
```

email은 필수이며 앞뒤 공백 제거와 소문자 변환 후 올바른 이메일 형식과 최대 254자를 검증한다. password는 2.7.5의 정책을 따르고 passwordConfirmation은 password와 정확히 일치해야 한다.

성공 시 HTTP 201 Created와 다음 JSON을 반환한다. Location 헤더는 요구하지 않으며 자동 로그인과 세션 생성은 하지 않는다.

```json
{
  "email": "user@example.com",
  "timezone": "Asia/Seoul",
  "createdAt": "2026-07-30T00:00:00Z"
}
```

회원가입 오류는 입력 검증 HTTP 422 + VALIDATION_ERROR, 중복 이메일 HTTP 409 + DUPLICATE_EMAIL, 요청 제한 HTTP 429 + RATE_LIMITED, 서버 오류 HTTP 500 + SERVER_ERROR를 사용한다. 오류 응답은 다음 공통 구조를 사용한다.

```json
{
  "timestamp": "2026-07-30T00:00:00Z",
  "status": 422,
  "code": "VALIDATION_ERROR",
  "message": "입력한 내용을 다시 확인해주세요.",
  "fieldErrors": [
    {
      "field": "email",
      "code": "INVALID_EMAIL",
      "message": "올바른 이메일 형식이 아닙니다."
    }
  ]
}
```

timestamp와 createdAt은 UTC ISO-8601 문자열이다. fieldErrors가 없는 오류는 빈 배열을 사용한다. 응답에는 password, passwordConfirmation, passwordHash, token, secret을 포함하지 않으며 스택 트레이스, SQL 메시지, 내부 클래스명과 입력 비밀번호를 반환하지 않는다. 사전 중복 조회와 DB 고유 제약조건 위반을 모두 DUPLICATE_EMAIL로 안전하게 변환한다.

2.18.6 로그인 요청·응답과 인증 오류 계약
요청은 POST /api/v1/auth/login과 다음 JSON을 사용한다.

```json
{
  "email": "user@example.com",
  "password": "example-password"
}
```

성공 시 HTTP 200 OK와 다음 JSON을 반환하고 서버 세션을 생성한다. 비밀번호를 재해시하지 않으며 마지막 로그인 시각 컬럼을 추가하지 않는다.

```json
{
  "email": "user@example.com",
  "timezone": "Asia/Seoul"
}
```

응답에는 password, passwordHash, token, refreshToken, sessionId, secret을 포함하지 않는다. 이메일 불일치, 비밀번호 불일치, 계정 잠금은 모두 HTTP 401 + INVALID_CREDENTIALS와 다음 공통 오류 JSON으로 동일하게 처리한다.

```json
{
  "timestamp": "2026-07-30T00:00:00Z",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호를 확인해주세요.",
  "fieldErrors": []
}
```

이메일 존재 여부, 비밀번호 불일치 여부, 계정 잠금 여부, 남은 시도 횟수, 세션 ID와 내부 예외 정보를 노출하지 않는다. 입력 형식 오류는 기존 공통 구조의 HTTP 422 + VALIDATION_ERROR를 사용한다.

상태: 기술 설계 확정안

2.18.7 로그아웃·세션 종료 계약
POST /api/v1/auth/logout 성공은 HTTP 204 No Content이며 응답 본문과 리다이렉트 응답이 없다. 서버는 현재 Spring Session을 무효화하고 Spring Security 인증 정보를 제거해 SecurityContext를 초기화한다. 이미 세션이 없더라도 유효한 CSRF 토큰이 있으면 같은 204를 반환하며 로그인 상태를 응답으로 구분하지 않는다.

성공 응답은 CALTALK_SESSION 쿠키를 빈 값, Path=/, Max-Age=0, HttpOnly=true, SameSite=Lax, 환경별 Secure로 삭제하고 Domain은 지정하지 않는다. 세션 ID, 쿠키 값, CSRF 토큰 등 인증 정보를 본문이나 헤더의 애플리케이션 데이터로 노출하지 않는다. 로그아웃 후 기존 세션 쿠키로 보호 API를 호출하면 HTTP 401 + UNAUTHORIZED 공통 오류 JSON을 반환한다.

CSRF 토큰 누락 또는 불일치는 HTTP 403 + FORBIDDEN과 다음 공통 오류 JSON을 반환하며 HTML 리다이렉트를 하지 않는다.

```json
{
  "timestamp": "2026-07-30T00:00:00Z",
  "status": 403,
  "code": "FORBIDDEN",
  "message": "요청을 처리할 권한이 없습니다.",
  "fieldErrors": []
}
```

상태: 기술 설계 확정안

2.18.8 현재 사용자 조회·인증 상태 복원 계약
GET /api/v1/users/me는 현재 서버 세션의 인증 상태와 브라우저 새로고침 후 사용자 상태를 복원하는 보호 API다. Authentication principal의 정규화 이메일로 users를 다시 조회하며 요청 본문, 쿼리 파라미터 또는 경로로 사용자 ID나 이메일을 받지 않는다.

성공은 HTTP 200 OK와 `Cache-Control: no-store`를 반환한다. ETag와 Last-Modified는 사용하지 않는다.

```json
{
  "email": "user@example.com",
  "timezone": "Asia/Seoul",
  "createdAt": "2026-07-30T00:00:00Z"
}
```

createdAt은 UTC ISO-8601 문자열이다. 응답에는 id, password, passwordHash, token, refreshToken, sessionId, secret, roles, authorities를 포함하지 않으며 세션 principal 자체를 직렬화하지 않는다.

세션이 없거나 인증되지 않은 요청은 HTTP 401 + UNAUTHORIZED, message “인증이 필요합니다.”, 빈 fieldErrors의 공통 JSON으로 응답하고 HTML 리다이렉트와 Location 헤더를 사용하지 않는다. Authentication은 있으나 정규화 이메일에 해당하는 users 레코드가 없으면 삭제되거나 무효해진 사용자 세션으로 판단하여 현재 세션을 무효화하고 SecurityContext를 제거하며 CALTALK_SESSION을 삭제한 뒤 동일한 401 JSON을 반환한다. 사용자 존재 여부를 404나 별도 코드로 노출하지 않는다. 예상하지 못한 서버 오류는 500 SERVER_ERROR 공통 JSON을 사용하고 이메일·세션·쿠키·내부 오류 정보를 노출하지 않는다.

상태: 기술 설계 확정안

2.18.9 현재 사용자 시간대 변경 계약
PATCH /api/v1/users/me는 현재 로그인 사용자의 표시 시간대를 변경하는 보호 API다. CALTALK_SESSION으로 인증된 Spring Security Authentication의 principal에서 정규화 이메일을 얻어 users를 다시 조회하며, 요청 본문·쿼리·경로에서 사용자 ID나 이메일을 받지 않는다. 공개 경로 또는 CSRF 예외 목록에 추가하지 않는다.

요청 JSON은 다음과 같고 허용 필드는 timezone 하나뿐이다. email, password, userId, createdAt, roles, authorities 등 다른 필드는 받지 않는다.

```json
{
  "timezone": "Asia/Seoul"
}
```

timezone은 필수 문자열이며 앞뒤 공백을 제거한 뒤 Java `ZoneId`로 해석 가능한 IANA Time Zone ID인지 검증한다. `Asia/Seoul`, `Asia/Tokyo`, `America/New_York`, `Europe/London` 같은 지역 기반 ID를 허용한다. 빈 문자열, 공백 전용 값, 존재하지 않는 ID와 `KST`, `GMT+9`, `UTC+09:00`, `Seoul` 같은 약어·고정 오프셋·비지역 ID는 거부한다.

검증 실패는 HTTP 422 + VALIDATION_ERROR와 다음 공통 오류 JSON을 반환한다.

```json
{
  "timestamp": "2026-07-31T00:00:00Z",
  "status": 422,
  "code": "VALIDATION_ERROR",
  "message": "입력한 내용을 다시 확인해주세요.",
  "fieldErrors": [
    {
      "field": "timezone",
      "code": "INVALID_TIMEZONE",
      "message": "올바른 시간대를 선택해주세요."
    }
  ]
}
```

하나의 트랜잭션에서 현재 사용자 한 명의 users.timezone만 갱신한다. email, password_hash, created_at, 사용자 ID와 기존 일정의 start_at·end_at UTC 절대값은 변경하지 않으며 DB 마이그레이션, updated_at 컬럼, 별도 버전·낙관적 잠금을 추가하지 않는다. 현재 저장값과 동일한 유효 시간대 요청도 멱등적으로 HTTP 200으로 처리한다. 동시 요청은 트랜잭션별로 정상 처리하고 마지막으로 커밋된 요청 값을 최종 상태로 사용한다.

성공은 HTTP 200 OK, `Cache-Control: no-store`와 GET /api/v1/users/me와 동일한 다음 JSON을 반환한다.

```json
{
  "email": "user@example.com",
  "timezone": "Asia/Tokyo",
  "createdAt": "2026-07-30T00:00:00Z"
}
```

응답에는 id, password, passwordHash, token, refreshToken, sessionId, secret, roles, authorities를 포함하지 않는다. 시간대 변경은 저장된 일정의 절대 시각을 바꾸지 않고 표시 기준만 바꾼다. 클라이언트는 성공 후 현재 사용자 정보, 홈의 오늘 일정, 월간 캘린더, 선택 날짜 일정 목록, 현재 열린 일정 상세를 무효화하거나 다시 조회하고 새 시간대 기준으로 날짜·요일·시작·종료 시각·오늘 여부·선택 날짜 포함 여부를 다시 계산한다. 별도 캐시 API와 일정 일괄 변환 작업은 만들지 않는다.

미인증 요청과 Authentication principal에 대응하는 users 레코드가 없는 경우는 2.18.8과 동일하게 HTTP 401 + UNAUTHORIZED JSON으로 처리한다. 후자의 경우 세션을 무효화하고 SecurityContext를 제거하며 CALTALK_SESSION을 삭제하고 사용자 존재 여부를 노출하지 않는다. CSRF 토큰 누락·불일치는 HTTP 403 + FORBIDDEN 공통 JSON으로 처리한다.

상태: 기술 설계 확정안

2.18.10 일정 생성 요청·응답 계약
POST /api/v1/schedules는 CALTALK_SESSION으로 인증된 현재 사용자의 일정을 직접 생성하는 보호 API다. 공개 경로와 CSRF 예외 목록에 추가하지 않는다. 세션 principal의 정규화 이메일로 users를 조회하고 해당 사용자의 id를 schedules.owner_user_id로 사용하며, 요청에서 사용자 ID·이메일·시간대를 받지 않는다.

요청 JSON은 다음 네 필드만 사용한다.

```json
{
  "title": "팀 회의",
  "startAt": "2026-08-01T10:00:00+09:00",
  "endAt": "2026-08-01T11:00:00+09:00",
  "location": "회의실 A"
}
```

title은 필수 문자열이며 앞뒤 공백 제거 후 1자 이상 200자 이하다. 누락·null·빈 문자열·공백 전용 값은 title의 REQUIRED, 200자 초과는 title의 MAX_LENGTH fieldError로 HTTP 422 + VALIDATION_ERROR를 반환한다. location은 선택 문자열이며 앞뒤 공백을 제거하고 최대 200자로 제한한다. null과 누락을 허용하고 trim 후 빈 문자열은 null로 저장하며, 200자 초과는 location의 MAX_LENGTH fieldError로 같은 422 오류를 반환한다.

startAt과 endAt은 필수 ISO-8601 offset date-time 문자열이며 UTC 오프셋을 반드시 포함한다. 서버는 요청 오프셋을 반영해 UTC Instant로 변환하고 schedules.start_at·end_at에 절대값만 저장한다. 입력 문자열과 사용자 timezone을 일정 레코드에 중복 저장하지 않는다. endAt은 startAt보다 반드시 늦어야 하며 동일하거나 빠르면 endAt의 INVALID_TIME_RANGE fieldError와 “종료 시각은 시작 시각보다 늦어야 합니다.” 메시지로 HTTP 422 + VALIDATION_ERROR를 반환한다. 과거 절대 시각의 PWA 직접 생성은 허용한다.

description, allDay, userId, ownerUserId, email, timezone, recurrence, reminder, participants, color, category, createdAt, updatedAt, version 등 계약 외 필드는 요청 DTO에 포함하지 않는다. 계약 외 필드가 전달되면 무시해 저장하는 대신 HTTP 422 + VALIDATION_ERROR로 요청을 거부하며, 입력 전체 원문과 소유자 관련 값을 오류 응답에 반사하지 않는다. 반복·종일·알림·참석자·카테고리·색상은 MVP 범위에 추가하지 않는다.

입력과 소유권 검증 뒤 같은 owner_user_id의 기존 일정 중 `existing.start_at < candidateEndAt AND existing.end_at > candidateStartAt`을 만족하는 일정을 조회한다. 한 일정의 종료와 다른 일정의 시작이 정확히 맞닿는 경우는 충돌이 아니다. 충돌이 없으면 현재 사용자 조회, 일정과 CREATE 변경 이력 저장, 응답 생성까지 하나의 트랜잭션에서 처리한다. 저장 실패 시 일정과 이력을 모두 롤백하며 외부 API와 Redis를 호출하지 않는다.

충돌이 있으면 최초 POST에서 일정과 이력을 저장하지 않고 HTTP 409 + SCHEDULE_CONFLICT와 다음 확장 오류 JSON을 반환한다. confirmationId는 confirmation_requests.id를 JSON 숫자로 노출한다. conflicts는 현재 사용자의 충돌 일정만 startAt 오름차순, 같은 startAt이면 id 오름차순으로 정렬한다.

```json
{
  "timestamp": "2026-07-31T00:00:00Z",
  "status": 409,
  "code": "SCHEDULE_CONFLICT",
  "message": "같은 시간대에 다른 일정이 있습니다.",
  "fieldErrors": [],
  "confirmationId": 123,
  "conflicts": [
    {
      "id": 10,
      "title": "기존 일정",
      "startAt": "2026-08-01T00:30:00Z",
      "endAt": "2026-08-01T01:30:00Z",
      "location": "회의실 B"
    }
  ]
}
```

conflicts 항목은 id·title·startAt·endAt·location만 포함하고 시각은 UTC Z 표기를 사용한다. owner_user_id, user_id, 이메일, version, createdAt, updatedAt, candidate_fingerprint, conflict_snapshot_hash, 내부 자동 재계산 정보와 다른 사용자의 일정은 반환하지 않는다.

클라이언트 승인 boolean을 최초 요청이나 일정 생성 API 재요청에 추가하지 않으며, 사용자는 POST /api/v1/confirmations/{confirmationId}/approve에 `{ "conflictAcknowledged": true }`를 보내는 기존 공통 흐름으로 계속 생성을 승인한다. 승인 엔드포인트는 2.18.3의 HTTP 200 계약을 유지하고 confirmation의 user_id 소유권, PENDING 상태, 5분 만료, candidate_fingerprint, 필요한 target_schedule_version과 최신 conflict_snapshot_hash를 재검증한다. 충돌 목록이 바뀌면 기존 행을 SUPERSEDED로 바꾸고 최신 후보를 자동 발급하며, 승인 가능하면 일정·CREATE 이력 저장과 CONSUMED 전환을 같은 트랜잭션에서 처리한다. 다른 사용자의 일정은 충돌 조회 대상이 아니다. 일반 POST를 같은 값으로 반복하면 별개의 생성 요청으로 처리하되, 각 요청 시점에 충돌이 있으면 동일한 confirmation 절차를 거친다.

충돌 없는 생성 성공은 HTTP 201 Created, `Cache-Control: no-store`, `Location: /api/v1/schedules/{id}`와 다음 JSON을 반환한다.

```json
{
  "id": 1,
  "title": "팀 회의",
  "startAt": "2026-08-01T01:00:00Z",
  "endAt": "2026-08-01T02:00:00Z",
  "location": "회의실 A",
  "createdAt": "2026-07-31T01:30:00Z",
  "updatedAt": "2026-07-31T01:30:00Z",
  "version": 0
}
```

startAt, endAt, createdAt, updatedAt은 UTC ISO-8601 Z 표기다. location이 없으면 null을 반환한다. 응답에는 owner_user_id, userId, email, password, sessionId, token, secret과 confirmation 내부 정보를 포함하지 않는다.

미인증 요청은 HTML 리다이렉트 없이 HTTP 401 + UNAUTHORIZED JSON, CSRF 누락·불일치는 HTTP 403 + FORBIDDEN JSON을 반환한다. Authentication은 있으나 users 레코드가 없으면 2.18.8의 경로를 재사용해 세션을 무효화하고 SecurityContext를 제거하며 CALTALK_SESSION을 삭제한 뒤 사용자 존재 여부 노출 없이 401로 처리한다. 예상하지 못한 서버 오류는 HTTP 500 + SERVER_ERROR 공통 JSON으로 일반화하고 SQL 메시지·내부 클래스명·이메일·세션·쿠키·입력 전체 원문을 노출하지 않는다.

상태: 기술 설계 확정안

2.18.11 일정 조회 요청·응답 계약
기간별 일정 조회는 `GET /api/v1/schedules?from={from}&to={to}`를 사용한다. 오늘 일정, 월간 캘린더, 선택 날짜 일정 목록을 위한 별도 API는 만들지 않고 모두 이 API를 재사용한다. from과 to는 필수 ISO-8601 offset date-time이며 서버에서 UTC Instant로 변환한다. 오프셋 없는 시각은 거부한다. 조회 기간은 시작 포함·종료 미포함인 반개구간 `[from, to)`이고 from은 to보다 반드시 빨라야 한다.

조회는 현재 인증 사용자의 일정 중 다음 조건을 만족하는 모든 일정을 반환한다.

```sql
WHERE owner_user_id = :currentUserId
  AND start_at < :requestedTo
  AND end_at > :requestedFrom
ORDER BY start_at ASC, end_at ASC, id ASC
```

요청 시작 경계에 정확히 끝나는 일정은 제외하고 요청 시작 경계에 정확히 시작하는 일정은 포함한다. 요청 종료 경계에 정확히 시작하는 일정은 제외하고 요청 종료 경계에 정확히 끝나는 일정은 포함한다. 여러 날짜에 걸친 일정도 겹침 조건을 만족하면 포함한다. userId, ownerUserId, email 쿼리 파라미터는 허용하지 않는다. 페이지네이션과 임의의 최대 조회 일수 제한은 MVP 계약에 추가하지 않으며, 운영상 제한값은 후속 결정으로 보류한다.

성공은 HTTP 200과 `Cache-Control: no-store`를 반환한다. 목록은 상세 응답과 분리된 DTO를 사용하며 다음 `items` 래퍼로 반환한다. 빈 결과도 404가 아닌 HTTP 200과 `"items": []`를 반환한다.

```json
{
  "items": [
    {
      "id": 1,
      "title": "팀 회의",
      "startAt": "2026-08-01T01:00:00Z",
      "endAt": "2026-08-01T02:00:00Z",
      "location": "회의실 A",
      "version": 0
    }
  ]
}
```

목록 항목은 id·title·startAt·endAt·location·version만 포함한다. createdAt·updatedAt은 상세 조회에서만 반환한다. 모든 시각은 UTC ISO-8601 Z 표기이며 ownerUserId·userId·email·내부 해시·confirmation·변경 이력은 반환하지 않는다.

일정 상세 조회는 `GET /api/v1/schedules/{scheduleId}`를 사용한다. 현재 인증 사용자의 소유 일정만 조회하며 성공은 HTTP 200, `Cache-Control: no-store`와 2.18.10의 생성 성공 응답과 같은 id·title·startAt·endAt·location·createdAt·updatedAt·version을 반환한다. 일정이 없거나 다른 사용자의 일정이면 존재 여부를 구분하지 않고 동일한 HTTP 404 + SCHEDULE_NOT_FOUND 공통 오류 JSON을 반환한다. 소유자 식별값, confirmation과 변경 이력 내부 정보는 반환하지 않는다.

from·to 누락, 형식 오류, 오프셋 없는 시각은 기존 공통 검증 오류 계약에 따라 HTTP 422 + VALIDATION_ERROR와 fieldErrors를 반환한다. from이 to보다 같거나 늦으면 field `to`, reason `INVALID_TIME_RANGE`로 통일한다. scheduleId 형식 오류도 HTTP 422 + VALIDATION_ERROR로 처리한다. 미인증 요청은 HTTP 401 + UNAUTHORIZED JSON이다. 두 GET API는 인증이 필수이지만 상태를 변경하지 않으므로 CSRF 토큰을 요구하지 않으며 공개 경로에 추가하지 않는다.

홈은 사용자 시간대 기준 오늘 시작과 다음 날 시작, 월간 캘린더는 표시 월 시작과 다음 달 시작, 선택 날짜 목록은 해당 날짜 시작과 다음 날 시작을 각각 offset date-time의 from·to로 계산해 기간 조회 API를 호출한다. 서버와 DB는 UTC 절대 시각을 유지한다. 시간대 변경 뒤에는 사용자 정보, 오늘 일정, 월간 캘린더, 선택 날짜 목록과 열린 일정 상세를 무효화하거나 재조회하고 새 시간대로 표시와 날짜 포함 여부만 다시 계산한다.

기존 `idx_schedules_owner_start (owner_user_id, start_at)`는 소유자 필터와 시작 시각 범위 축소를 지원하므로 이번 단계에서 새 인덱스를 제안하지 않는다. end_at 겹침 조건과 최종 정렬 비용은 실제 실행 계획과 데이터 규모로 후속 관찰한다. 목록 조회는 소유 일정과 응답 필드를 한 번의 조회로 가져오며 일정별 추가 조회를 발생시키지 않아 N+1을 방지한다.

상태: 기술 설계 확정안

2.18.12 일정 수정·삭제 요청·응답 계약
일정 수정은 `PATCH /api/v1/schedules/{scheduleId}`를 사용하는 부분 수정이다. CALTALK_SESSION으로 인증된 현재 사용자의 일정만 `scheduleId + owner_user_id` 조건으로 한 번에 조회하며, 일정이 없거나 다른 사용자 소유이면 존재 여부를 구분하지 않고 HTTP 404 + SCHEDULE_NOT_FOUND를 반환한다. 요청 JSON은 title·startAt·endAt·location·version만 허용하고 version은 필수인 0 이상의 정수다. title·startAt·endAt·location 중 최소 하나는 필드로 전달해야 하며 version만 있는 요청과 계약 외 필드는 HTTP 422 + VALIDATION_ERROR로 거부한다.

PATCH에서 전달하지 않은 필드는 기존 값을 유지한다. title은 전달 시 null을 허용하지 않고 trim 후 1자 이상 200자 이하여야 한다. startAt과 endAt은 전달 시 null을 허용하지 않는 ISO-8601 offset date-time이며 UTC Instant로 변환한다. location은 필드 미전달이면 유지(KEEP), null이면 삭제(REMOVE), 문자열이면 trim 후 저장(SET)하고 trim 결과가 빈 문자열이면 REMOVE로 정규화한다. 최종 후보의 endAt은 startAt보다 반드시 늦어야 하며 관계 오류는 endAt의 INVALID_TIME_RANGE로 처리한다.

요청 version이 현재 schedules.version과 다르면 HTTP 409 + SCHEDULE_VERSION_CONFLICT, message “일정이 다른 곳에서 변경되었습니다.”, 빈 fieldErrors를 반환하고 최신 일정 본문은 포함하지 않는다. 클라이언트는 상세를 다시 조회한 뒤 사용자 입력을 유지한 상태로 재시도 여부를 결정하며 자동 덮어쓰지 않는다. version이 일치하고 정규화된 최종 후보가 기존 값과 완전히 같으면 HTTP 200 멱등 성공으로 현재 ScheduleResponse를 반환하고 updatedAt·version을 변경하거나 UPDATE 이력을 추가하지 않는다.

실제 변경이 있고 충돌이 없으면 현재 값 전체를 before, 최종 값 전체를 after로 하는 change_type UPDATE·source_channel PWA·현재 changed_by_user_id 이력을 일정 수정과 같은 트랜잭션에 저장한다. JPA `@Version` 증가와 `updatedAt` 갱신을 flush한 뒤 증가된 version과 UTC updatedAt이 포함된 ScheduleResponse를 HTTP 200, `Cache-Control: no-store`로 반환한다. location의 실제 null은 before/after에도 null로 기록한다.

최종 후보의 충돌은 현재 일정 자신을 제외하고 다음 조건으로 조회하며 startAt, id 오름차순으로 정렬한다.

```sql
WHERE existing.owner_user_id = :currentUserId
  AND existing.id <> :targetScheduleId
  AND existing.start_at < :candidateEndAt
  AND existing.end_at > :candidateStartAt
ORDER BY existing.start_at ASC, existing.id ASC
```

충돌이 있으면 일정과 이력을 변경하지 않고 HTTP 409 + SCHEDULE_CONFLICT, confirmationId와 conflicts를 반환한다. 기존 confirmation_requests 통합 모델에 origin_channel PWA, command_type UPDATE_EVENT, 현재 사용자, target_schedule_id, 검증한 요청 version을 target_schedule_version으로 저장한다. title·start_at·end_at은 요청에서 변경하지 않은 필드를 null로 저장하고 location은 KEEP·SET·REMOVE 및 SET일 때만 location_value를 저장하는 델타 규칙을 유지한다. candidate_fingerprint의 canonical 순서는 command_type, target_schedule_id, title, start_at, end_at, location_action, location_value이며 충돌 목록의 id+version으로 conflict_snapshot_hash를 계산한다. 새 요청은 PENDING·conflict_acknowledged=false·5분 만료다.

승인은 `POST /api/v1/confirmations/{confirmationId}/approve`의 2.8.3 절차를 재사용한다. confirmation 잠금, 사용자·상태·만료·대상 존재와 소유권, target_schedule_version, 저장 델타 적용 결과, 시간 범위와 최신 충돌 해시를 재검증한다. 대상이 사라지면 CONFIRMATION_TARGET_GONE, 대상 version 또는 충돌 목록이 바뀌면 기존 confirmation을 SUPERSEDED로 바꾸고 최신 기준의 새 confirmation과 CONFIRMATION_SUPERSEDED를 반환한다. 이 승인 경로에는 직접 PATCH·DELETE의 SCHEDULE_VERSION_CONFLICT를 사용하지 않고 기존 자동 재계산 계약을 유지한다. 조건이 같고 conflictAcknowledged=true이면 일정 UPDATE, 전체 before/after 이력, CONSUMED 전환을 같은 트랜잭션에서 한 번만 처리한다. STALE_CONFIRMATION은 계속 보류하며 새 코드로 추가하지 않는다.

일정 삭제는 `DELETE /api/v1/schedules/{scheduleId}?version={version}`을 사용한다. DELETE 본문과 If-Match 헤더는 사용하지 않으며 version은 필수인 0 이상의 정수다. 현재 사용자 일정만 `scheduleId + owner_user_id` 조건으로 조회하고 없음·타 사용자 소유는 동일한 HTTP 404 + SCHEDULE_NOT_FOUND로 처리한다. version 불일치는 PATCH와 같은 HTTP 409 + SCHEDULE_VERSION_CONFLICT를 반환한다. PWA의 삭제 확인 모달은 클라이언트 책임이며 직접 DELETE에 confirmation_requests를 만들지 않는다.

삭제 성공은 같은 트랜잭션에서 새 DELETE 이력을 생성하지 않고 일정을 즉시 하드 삭제하며 HTTP 204 No Content, 빈 본문, `Cache-Control: no-store`를 반환한다. 기존 CREATE·UPDATE 이력은 `schedule_change_history.schedule_id ON DELETE CASCADE`에 의해 함께 제거된다. change_type DELETE는 현재 MVP에서 사용하지 않으며 별도 감사 로그·soft delete·FK 변경을 추가하지 않는다. 향후 영구 삭제 감사가 필요하면 별도 감사 로그 구조를 검토한다. 같은 ID의 재삭제는 404 SCHEDULE_NOT_FOUND다.

PATCH와 DELETE는 공개 경로나 CSRF 예외에 추가하지 않는다. 미인증은 401 UNAUTHORIZED, CSRF 누락·불일치는 403 FORBIDDEN이다. scheduleId 또는 version의 누락·형식·범위 오류와 PATCH 필드 검증은 422 VALIDATION_ERROR를 사용한다. 성공 후 클라이언트는 열린 상세, 홈 오늘 일정, 월간 캘린더와 선택 날짜 목록을 무효화하거나 재조회한다. 수정은 전후 날짜 범위가 모두 갱신 대상이며, 삭제 성공은 상세를 닫고 안전한 목록이나 캘린더로 이동한다.

테스트는 각 부분 필드 수정, location KEEP·SET·REMOVE, 시간 검증, 계약 외 필드, 무변경 멱등 요청, version 일치·불일치, 자기 자신 제외 충돌, UPDATE_EVENT confirmation 생성·승인·SUPERSEDED·대상 소멸, UPDATE 전체 스냅샷 이력, 삭제 204·재삭제 404·버전 충돌·CASCADE 이력 제거, 소유권 은닉, 인증·CSRF와 캐시 갱신 범위를 포함한다.

상태: 기술 설계 확정안

2.19 보안·개인정보·요청 제한
2.19.1 최소 수집·최소 전송·개인정보 안내
LLM에는 명령 해석에 필요한 최소 정보만 전달, 이메일·내부 ID·인증 토큰 미전달(확정). 개인정보 처리 안내에는 "자연어 입력이 외부 API로 전달될 수 있다"는 사실만 반영한다(2.12.2). 운영 로그에 사용자 입력 원문을 무분별하게 남기지 않는다. API 키, DB 접속정보, 세션/CSRF 비밀값, HMAC 서버 비밀키는 환경변수로 관리한다.

2.19.2 요청 제한 저장 방식
Redis 등 별도 인프라를 추가하지 않는 원칙을 유지하며, 카운터의 저장 위치를 성격에 따라 구분한다.

분류	원칙
일반 API의 단기 속도 제한	단일 서버 MVP에서는 애플리케이션 메모리 기반 카운터 사용 가능. 재시작 시 초기화되어도 무방한 보조적 제한이다
서버 재시작 후에도 유지되어야 하는 보안 상태(로그인 실패 누적, 계정 잠금, 연결 코드 실패 횟수)	PostgreSQL에 저장
엔드포인트	제한 대상	기본값	저장 위치
로그인 실패(계정)	정규화 이메일로 식별한 user_id	15분당 5회 → 15분 잠금	PostgreSQL(login_security_state, 2.9.9)
로그인 실패(IP, 보조)	IP	15분당 20회	애플리케이션 메모리
회원가입(IP)	IP	1시간당 5회	애플리케이션 메모리
연결 코드 발급(사용자)	user_id	10분당 3회	PostgreSQL(connection_codes.issued_at 카운트, 기존 테이블 그대로 사용)
연결 코드 검증 실패(코드)	connection_codes.id	코드당 5회	PostgreSQL(connection_codes.fail_count, 기존 컬럼 그대로 사용)
연결 코드 검증 실패(봇 식별값, 보조)	external_user_hmac	1시간당 10회	애플리케이션 메모리
자연어 분석 요청(웹)	user_id	1시간당 20회	애플리케이션 메모리
카카오 스킬 요청	external_user_hmac	1시간당 30회	애플리케이션 메모리
IP 기반 보조 제한의 개인정보 최소화: 애플리케이션 메모리 카운터는 프로세스 밖으로 영속화되지 않으므로 원문 IP를 키로 사용해도 즉시 위험이 낮다. 다만 이 카운터 정보를 로그나 향후 영속 저장소에 남겨야 하는 경우에는 원문 IP 대신 HMAC-SHA-256(server_secret, ip) 값을 짧은 기간만 사용한다.

서버 재시작 시 초기화되는 것과 유지되어야 하는 것의 구분:

초기화되어도 되는 것: 위 표에서 "애플리케이션 메모리"로 표시된 모든 보조·단기 제한.
반드시 유지되어야 하는 것: 계정 잠금(login_security_state), 연결 코드 실패·발급 이력(connection_codes).
이 방식은 단일 인스턴스 MVP에서만 사용하며, 다중 인스턴스로 확장이 필요해지면 애플리케이션 메모리 카운터를 공용 제한 저장소로 옮기는 것을 재검토한다.
모든 기본값은 환경변수로 조정 가능하다. 로그인 실패·연결 코드 검증 실패는 계정/코드 존재 여부를 노출하지 않는 동일한 일반 오류로 응답한다(기존 결정 유지). 계정 기준 실패가 5회에 도달하면 15분간 잠그되 외부에는 INVALID_CREDENTIALS로 동일하게 응답한다. IP 기준 실패가 15분 동안 20회를 초과하면 HTTP 429 + RATE_LIMITED와 초 단위 Retry-After 헤더를 반환한다. 로그인 성공 시 해당 계정의 실패 횟수를 초기화하고 필요한 범위에서 해당 이메일·IP 조합의 보조 실패 기록도 초기화한다.

상태: 기술 설계 확정안

2.20 장애 및 예외 처리
장애 유형	대응 원칙
OpenAI API 장애/타임아웃(웹)	HTTP 503 + AI_SERVICE_UNAVAILABLE, DB 변경 없음
OpenAI API 장애/타임아웃(카카오)	카카오 규격 정상 응답 JSON 안의 실패 안내(18.2)
카카오 플랫폼 장애	실패 안내 반환, PWA/웹 경로는 정상 제공
DB 장애	성공하지 않은 변경을 성공으로 응답하지 않음
네트워크 지연	지연된 비동기 결과로 임의 변경 금지
중복 재시도	idempotency_records 기반 멱등성
상태: 확정

2.21 테스트 전략
회원가입 단위·MVC 테스트: 이메일 trim·소문자 정규화, 이메일 형식·254자 제한, 비밀번호 8~64자·공백 전용 거부, 비밀번호 확인 일치, 무인증 접근, 201 응답 필드와 민감 필드 부재, 422·409 오류 계약을 검증한다.
회원가입 통합 테스트: PostgreSQL에서 users 스키마와 기본 시간대 Asia/Seoul, 정규화 이메일 저장, BCrypt 해시와 원문 불일치, 해시 일치 검증, 중복 이메일 사전 조회와 DB 고유 제약 경쟁 경로의 DUPLICATE_EMAIL 변환, 자동 세션 미생성을 검증한다.
로그인 단위·MVC 테스트: 이메일 정규화·형식·254자 제한, 비밀번호 8~64자, 200 응답 필드와 민감 필드 부재, 422 입력 오류, 계정 부재·비밀번호 불일치·잠금의 동일한 401 INVALID_CREDENTIALS 응답을 검증한다.
로그인 세션·보안 테스트: CALTALK_SESSION의 HttpOnly·SameSite=Lax·Path=/와 Domain·Max-Age 미지정, 환경별 Secure, 12시간 유휴 만료, 기존 세션 ID 교체, 재로그인, JSON 401 UNAUTHORIZED·403 FORBIDDEN, HTML 리다이렉트 부재를 검증한다.
로그아웃 테스트: 인증 사용자 요청의 204와 빈 본문, 현재 세션 무효화, SecurityContext 제거, CALTALK_SESSION의 빈 값·Path=/·Max-Age=0·HttpOnly·SameSite=Lax·환경별 Secure·Domain 미지정, 시작 화면 이동, 기존 쿠키의 보호 API 401 UNAUTHORIZED, 유효한 CSRF 토큰을 포함한 미인증 재요청의 204, CSRF 누락·불일치의 403 FORBIDDEN JSON, HTML 리다이렉트 부재, 세션 ID·쿠키 값·토큰 비노출을 검증한다.
회원 탈퇴 테스트: 인증·CSRF와 currentPassword 필수·공백 전용 거부·64자 경계, 비밀번호 불일치의 401 INVALID_CREDENTIALS·빈 fieldErrors·민감정보 비노출, 현재 V1~V3 기준 confirmation_requests 전체 삭제 후 schedules 하드 삭제·schedule_change_history CASCADE·users 삭제와 다른 사용자 데이터 보존을 검증한다. DB 삭제 실패 시 전체 롤백과 세션 유지를 확인하고, 커밋 성공 뒤에만 현재 HTTP 세션 무효화·SecurityContext 제거·CALTALK_SESSION 삭제가 수행되는지, 204·빈 본문·Cache-Control no-store와 기존 쿠키의 보호 API 및 탈퇴 재요청 401을 검증한다.
현재 사용자 조회 테스트: 로그인 뒤 GET /api/v1/users/me의 200, email·timezone·UTC createdAt, Cache-Control no-store, id·password·passwordHash·token·refreshToken·sessionId·secret·roles·authorities 부재를 검증한다. 세션 없는 요청의 401 UNAUTHORIZED JSON·Content-Type·리다이렉트와 Location 부재, 로그아웃 뒤 기존 쿠키의 401, users 레코드가 없는 인증 세션의 401·세션 무효화·SecurityContext 제거·CALTALK_SESSION 삭제·사용자 존재 여부 비노출도 검증한다.
시간대 변경 단위·MVC 테스트: 인증된 PATCH /api/v1/users/me의 200, timezone 단일 요청 필드, trim 적용, 지역 기반 IANA Zone ID 허용, 빈 값·공백·존재하지 않는 ID·KST·GMT+9·UTC+09:00 거부, 422 VALIDATION_ERROR와 timezone의 INVALID_TIMEZONE fieldError, email·createdAt 유지, Cache-Control no-store와 민감 필드 부재를 검증한다.
시간대 변경 통합·보안 테스트: users.timezone만 변경되고 GET /api/v1/users/me에서 변경값이 확인되는지, 동일 값 재요청도 200인지, email·password_hash·created_at·사용자 ID와 일정 UTC 값 및 DB 스키마가 불변인지 검증한다. 미인증 401 UNAUTHORIZED, 사용자 없는 인증 세션 정리, CSRF 누락·불일치 403 FORBIDDEN, 다른 사용자 변경 불가와 마지막 정상 커밋 값의 최종 반영도 검증한다.
시간대 변경 클라이언트 테스트: 성공 시 사용자 정보·오늘 일정·월간 캘린더·선택 날짜 목록·열린 일정 상세를 무효화하거나 재조회하고 새 시간대 기준으로 모든 날짜·시각 표시와 오늘·선택 날짜 포함 여부를 다시 계산하는지 검증한다. 실패 시 기존 시간대와 일정 캐시를 유지하고 필드 검증 오류와 일반 서버 오류를 구분하는지 검증한다.
일정 생성 단위·MVC 테스트: title 필수·trim·공백 거부·200자 경계, location 선택·trim·빈 값의 null 변환·200자 경계, startAt·endAt 필수·offset date-time 형식, endAt > startAt, 과거 일정 허용과 계약 외 필드 거부를 검증한다. 입력 오류는 422 VALIDATION_ERROR와 title·location·endAt의 REQUIRED·MAX_LENGTH·INVALID_TIME_RANGE fieldError를 사용하고 입력 전체 원문을 노출하지 않는지 확인한다.
일정 생성 통합 테스트: 인증 후 충돌 없는 POST /api/v1/schedules의 201, Cache-Control no-store, Location 헤더, 현재 사용자의 owner_user_id, UTC Instant 저장, CREATE 변경 이력, location null·trim, 응답의 id·title·startAt·endAt·location·createdAt·updatedAt·version과 UTC Z 표기, 소유자·인증·민감 필드 부재를 PostgreSQL에서 검증한다.
일정 생성 충돌 테스트: 같은 사용자의 겹치는 범위는 최초 POST에서 DB 변경 없이 409 SCHEDULE_CONFLICT와 confirmationId·충돌 목록을 반환하고, 맞닿는 경계와 다른 사용자 일정은 충돌로 보지 않는지 확인한다. 승인 후 한 번만 생성되는지, 위조·타 사용자 confirmation 사용·만료·중복 승인·승인 대기 중 충돌 변경을 기존 공통 confirmation 계약대로 처리하는지 검증한다.
일정 충돌 응답 테스트: confirmationId가 숫자이고 conflicts가 startAt·id 순으로 정렬되며 각 항목에 id·title·UTC startAt·UTC endAt·location만 있는지 검증한다. 다른 사용자 일정과 owner_user_id·user_id·version·생성/수정 시각·후보/충돌 해시·내부 재계산 정보가 노출되지 않는지 확인한다.
confirmation 물리 스키마 테스트: user_id와 상태 5개, 5분 만료, 개별 후보 필드, CHAR(64) candidate_fingerprint·conflict_snapshot_hash, 부분 유니크 제약과 user/status·expires_at·target_schedule_id 인덱스, 세 FK의 NO ACTION을 검증한다.
변경 이력 물리 스키마 테스트: before/after 컬럼, source_channel·change_type CHECK, schedule_id FK의 ON DELETE CASCADE, changed_by_user_id FK의 NO ACTION과 두 복합 인덱스를 검증한다. CREATE는 before NULL·after 저장, UPDATE는 양쪽 저장을 확인하고, 일정 삭제 시 새 DELETE 이력이 생성되지 않는지 확인한다.
일정 생성 인증·트랜잭션 테스트: 미인증 401 UNAUTHORIZED, CSRF 누락·불일치 403 FORBIDDEN, 요청 userId·ownerUserId로 소유자를 바꿀 수 없음, 사용자 없는 인증 세션 정리, 일정·이력 저장 실패 시 전체 롤백, 일반 동일 요청 반복의 별도 일정 생성과 다른 사용자 일정 불변을 검증한다.
로그인 제한 테스트: 정규화 이메일로 식별한 계정의 15분·5회 잠금과 성공 후 초기화, IP의 15분·20회 제한 및 429 RATE_LIMITED·초 단위 Retry-After를 검증한다.
단위 테스트: 충돌 판정, 지속시간 유지, 낙관적 잠금 버전 비교, conflict_snapshot_hash/candidate_fingerprint 계산(정규화 규칙 포함), login_security_state의 15분 롤링 윈도·잠금 로직
통합 테스트: 확인 승인의 잠금→검증→소비/재계산 전체 흐름, PWA 충돌 확인이 자연어 흐름과 동일한 승인 엔드포인트를 공유하는지
동시성 테스트: 동일 confirmationId에 대한 동시 승인 요청 중 하나만 커밋되는지
동일 후보 동시 최초 생성 테스트(신규): 동일 user_id·candidate_fingerprint를 가진 완전히 동일한 최초 생성 요청 두 개가 PENDING 행이 아직 하나도 없는 상태에서 동시에 들어올 때, 부분 유니크 인덱스 위반이 발생해도 이것이 클라이언트에 HTTP 500으로 노출되지 않고, 두 요청 모두 결과적으로 동일한 하나의 confirmationId(또는 상태가 달라졌다면 새로 발급된 confirmationId)를 정상적으로 받는지 확인
자동 재계산 테스트: 대상 일정의 버전이 승인 대기 중 바뀌었을 때 기존 확인이 SUPERSEDED로 바뀌고 새 confirmationId·최신 후보·최신 충돌 목록이 자동 발급되는지, 기존 confirmationId가 이후 재사용 불가한지, 대상 일정 자체가 삭제된 경우에만 재입력 요청으로 이어지는지
동일 후보 재사용 재검증 테스트: 동일 candidate_fingerprint로 재요청 시 버전·충돌이 동일하면 기존 confirmationId가 재사용되고, 달라졌으면 기존이 SUPERSEDED로 바뀌며 새 확인이 발급되는지
삭제 테스트: 새 DELETE 이력을 생성하지 않고 하드 삭제하며 기존 schedule_change_history가 ON DELETE CASCADE로 같은 트랜잭션에서 함께 삭제되는지
요청 제한 테스트: 로그인 실패 누적 후 계정 잠금이 걸리는지, 애플리케이션 재시작(또는 그에 준하는 상태 초기화) 시뮬레이션 후에도 login_security_state·connection_codes.fail_count 기반 잠금이 유지되는 반면 메모리 기반 보조 제한만 초기화되는지
권한·시간대·멱등성·만료 테스트: 기존과 동일
AI 장애 응답 테스트: 웹은 503+AI_SERVICE_UNAVAILABLE, 카카오는 정상 스킬 응답 형태로 반환되는지
외부 API 어댑터 테스트: OpenAI/카카오 어댑터 모킹 검증
배포 환경 수동 검증: 실제 카카오 스킬 응답 시간, 동일 출처 프록시, 세션 쿠키 동작
상태: 기술 설계 확정안

2.22 배포·운영 설계
프런트엔드·백엔드 물리적 분리 배포, 운영은 동일 출처 리버스 프록시(2.7.1)
PostgreSQL 운영 환경(호스팅 방식): 보류
Spring Session 테이블은 운영에서 마이그레이션 관리 범위에 포함(2.7.10)
환경변수: DB 접속정보, OpenAI API 키, 세션/CSRF 비밀값, 카카오 관련 값, HMAC 서버 비밀키
CORS: 운영 불필요, 개발만 localhost:5173
상태 확인: 헬스체크 엔드포인트(경로는 구현 단계 결정)
장애 로그: 외부 API 실패, 인증 실패 기록(마스킹 원칙)
백업: 정기 스냅샷 수준, 자동 복구 훈련은 MVP 범위 아님
관리형 DB 백업 삭제 데이터 보관 기간: 호스팅 제공업체 선택 후 확인(보류)
단일 인스턴스 전제(19절의 메모리 기반 요청 제한과 일치), 다중 인스턴스 확장은 이 시점에 설계하지 않는다
상태: 원칙은 기술 설계 확정안, 호스팅 방식·백업 보관 기간은 보류

2.23 모노레포의 논리적 디렉터리 구조 제안

caltalk/
 ├─ apps/
 │   ├─ frontend/     (React + TypeScript + Vite, PWA)
 │   └─ backend/      (Java 21 + Spring Boot)
 ├─ docs/
 │   ├─ planning/
 │   └─ design/
 └─ README.md

apps/backend/src/main/java/.../caltalk/
 ├─ auth/  ├─ user/  ├─ schedule/  ├─ conversation/  ├─ ai/  ├─ kakao/  └─ common/
상태: 기술 설계 확정안(실제 생성은 다음 단계 이후)

2.24 기술 버전과 공식 출처
원칙: 프로젝트 공식 문서·공식 릴리스 페이지·공식 GitHub 저장소만 출처로 사용한다. 패치 버전이 자주 바뀌는 도구는 메이저·마이너까지만 제시하고, 정확한 패치 버전은 저장소 초기화 시점에 공식 출처로 다시 검증한 뒤 선택한다. Spring Boot BOM이 관리하는 Spring Security, Spring Session 등의 버전은 원칙적으로 개별 직접 고정하지 않는다.

기술	기준(메이저.마이너)	공식 출처	확인일	비고
Java	21(LTS, 유지)	OpenJDK — JDK 21 프로젝트	2026-07-25	정확한 패치·배포판은 배포 단계 결정
Spring Boot	4.1 계열	Spring 공식 블로그, Spring Boot 공식 릴리스 노트	2026-07-25	정확한 패치는 초기화 시점 재확인
Spring Security	버전 직접 고정 안 함(Boot BOM 관리)	Spring Boot 공식 문서	2026-07-25	—
Spring Session(JDBC)	버전 직접 고정 안 함, 방향(JDBC)만 확정	Spring Boot 공식 문서	2026-07-25	—
PostgreSQL	18 계열	PostgreSQL 공식 뉴스, 버전 정책	2026-07-25	정확한 마이너는 배포 시점 재확인
Node.js	개발 환경 기준 최신 Active LTS 라인	Node.js 공식 릴리스 안내	2026-07-25	저장소 초기화 시점 공식 일정 재확인
React	19 계열	react.dev — Versions	2026-07-25	패치는 초기화 시점 재확인
TypeScript	7 계열	TypeScript 공식 블로그	2026-07-25	매우 최근 메이저, 생태계 호환성 재확인
Vite	8 계열	Vite 공식 릴리스 페이지	2026-07-25	패치는 초기화 시점 재확인
React Router	8 계열	React Router 공식 문서	2026-07-25	요구사항·패키지 구조 재확인
TanStack Query	v5 계열	TanStack Query 공식 문서	2026-07-25	패치는 초기화 시점
date-fns	v4 계열	date-fns 공식 사이트	2026-07-25	시간대 최종 판단은 서버 기준
vite-plugin-pwa	v1 계열	vite-pwa/vite-plugin-pwa 공식 저장소	2026-07-25	캐시 전략 설정법 재확인
테스트 도구(백엔드)	JUnit 6 계열	JUnit 공식 릴리스 노트	2026-07-25	starter-test 기본 포함 버전 재확인
테스트 도구(프런트엔드)	Vitest 4 계열	Vitest 공식 블로그	2026-07-25	부속 라이브러리 버전은 확인 필요
OpenAI 공식 Java SDK	사용 가능(openai/openai-java)	OpenAI 공식 GitHub 저장소	2026-07-25	지원 범위 재확인
OpenAI 모델명 / API 데이터 보관 정책	확정하지 않음	OpenAI 공식 문서(구현 직전 확인)	—	추측 금지
상태: 기술 설계 확정안(개별 패치 버전과 카카오·OpenAI 정책은 확인 필요/PoC 대상)

2.25 기술 결정 표
#	항목	결정	이유	대안	상태
1	인증 방식	세션 쿠키	단순·안전	JWT	기술 설계 확정안
2	세션 저장소	Spring Session JDBC(PostgreSQL)	재시작에도 세션 유지	인메모리, Redis	기술 설계 확정안
3	배포 출처 구조	운영 동일 출처 프록시 / 개발 CORS	CSRF·쿠키 문제 최소화	완전 CORS	기술 설계 확정안
4	확인 요청 소비 순서	잠금(FOR UPDATE) → 재검증 → 소비	"확정 전 소비 금지" 원칙 구현	조건부 UPDATE 선소비	기술 설계 확정안
5	SUPERSEDED 이후 처리	서버가 자동으로 최신 상태 기준 새 후보 생성·발급	사용자가 매번 처음부터 다시 시작하지 않도록	사용자 전면 재입력 요구	기술 설계 확정안
6	동일 후보 재사용	재사용 전 버전·충돌 재검증, 다르면 SUPERSEDED 후 재발급	오래된 후보를 그대로 승인하는 위험 방지	무조건 재사용	기술 설계 확정안
7	동일 후보 동시 최초 생성 경쟁	부분 유니크 인덱스를 최종 방어선으로, 위반 시 재조회·재검증 후 응답	SELECT FOR UPDATE가 막지 못하는 구간 보완	애플리케이션 레벨 분산 락 도입	기술 설계 확정안
8	candidate_fingerprint 계산	7개 필드의 canonical JSON SHA-256 해시, 정규화 규칙 명시	채널·표현 방식 차이로 인한 오탐/누락 방지	변경된 필드만 해시	기술 설계 확정안
9	PWA 충돌 승인	통합 confirmation 모델(서버 발급 confirmationId)	클라이언트 플래그 조작 우회 방지	acknowledgeConflict 불리언	기술 설계 확정안
10	일정 삭제 방식	즉시 하드 삭제, status 컬럼 없음	휴지통·복구 UI 없음	소프트 삭제 후 보관	기술 설계 확정안
11	변경 이력 FK 정책	ON DELETE CASCADE	삭제된 일정 이력을 사용할 기능 없음	SET NULL 후 보존	기술 설계 확정안
12	요청 제한 저장 위치	단기 제한은 메모리, 계정 잠금·연결코드 실패는 PostgreSQL	Redis 없이도 필요한 지속성 확보	전부 Redis, 전부 메모리	기술 설계 확정안
13	로그인 보안 상태	login_security_state 신설 테이블	계정 정보(users)와 공격 방어 상태 분리	users에 컬럼 추가	기술 설계 확정안
14	OpenAI store: false 표현	응답 객체 저장 비활성화로만 설명, ZDR과 구분	정책 오인 방지	ZDR과 동일시	기술 설계 확정안
15	AI 장애 HTTP 처리(웹)	항상 503 + AI_SERVICE_UNAVAILABLE	모호한 표현 제거	상황별 선택	확정
16	AI 장애 처리(카카오)	카카오 규격 정상 응답 JSON 내 실패 안내	플랫폼 규격 준수	HTTP 503 그대로 반환	확정
17	기술 버전 출처	공식 문서·릴리스 페이지·공식 GitHub만 사용	신뢰성 있는 근거	비공식 집계 사이트	확정
18	Spring Security/Session 버전	개별 고정 안 함(Boot BOM 관리)	불필요한 버전 충돌 방지	직접 버전 고정	기술 설계 확정안
19	카카오 5초 대응	짧은 내부 타임아웃 + 즉시 실패 안내	콜백 비전제 원칙	콜백 필수 설계	PoC 검증 필요
20	카카오 요청 진위 검증 수단	서명/헤더/IP대역 여부 확인 후 결정	공식 미확인 상태에서 확정 금지	—	PoC 검증 필요
21	PostgreSQL 호스팅 방식	미정	배포 단계 결정	—	보류
22	관리형 DB 백업 보관 기간	미정	제공업체 선택 후 확정	—	보류
23	회원가입 계약	email·password·passwordConfirmation 요청, 201 응답, 422·409·429·500 공통 오류 구조	구현 전 입력·저장·응답 계약 일치	이름·시간대 가입 입력	기술 설계 확정안
24	로그인 계약	email·password 요청, 200 응답, Spring Security 세션과 CALTALK_SESSION, 일반화된 401 오류	세션 인증과 화면 계약 일치	JWT·자동 로그인	기술 설계 확정안
25	로그아웃 계약	POST, CSRF 보호, 멱등 204, 현재 세션·SecurityContext 종료와 CALTALK_SESSION 삭제	인증 상태 비노출과 화면 계약 일치	리다이렉트·전체 세션 종료	기술 설계 확정안
26	현재 사용자 조회 계약	GET /api/v1/users/me, 세션 principal 이메일로 DB 재조회, 200 email·timezone·createdAt, no-store	새로고침 인증 복원과 무효 세션 정리	사용자 ID·역할·JWT 노출	기술 설계 확정안
2.26 알려진 위험과 대응
위험	영향	대응
카카오 5초 제약과 OpenAI 응답 지연	스킬 응답 실패	짧은 내부 타임아웃 + 카카오 규격 정상 응답 내 실패 안내
카카오 요청 진위 검증 수단 부재 가능성	스킬 엔드포인트 오남용 위험	비밀 경로+빈도제한+해시매칭 다중 방어
SELECT ... FOR UPDATE 대기로 인한 지연	동시 승인 시도가 몰릴 경우 두 번째 요청이 잠시 대기	5분 유효시간 내에서는 영향 미미
동일 후보 동시 최초 생성 시 DB 고유 제약 위반 발생	처리하지 않으면 500 오류로 노출될 위험	애플리케이션이 이 위반을 정상 신호로 해석해 재조회·재검증으로 처리(2.8.2)
대상 일정이 승인 직전 삭제됨	재구성 불가로 사용자가 처음부터 다시 입력해야 함	명확한 안내 메시지(CONFIRMATION_TARGET_GONE)로 즉시 재시도 유도
애플리케이션 메모리 기반 요청 제한이 재시작 시 초기화됨	단기 속도 제한이 일시적으로 느슨해질 수 있음	계정 잠금·연결 코드 실패 등 핵심 보안 경계는 PostgreSQL에 두어 실질적 방어는 유지됨(19절)
단일 인스턴스 전제가 깨질 경우(다중 인스턴스 확장)	메모리 기반 카운터가 인스턴스마다 분리되어 제한이 느슨해짐	확장 시점에 공용 제한 저장소 재검토
카카오 원 요청 식별자 미확보	스킬 경로 멱등성 약화	PoC 결과에 따라 근사 중복 판정 방식 별도 검토
OpenAI 실제 데이터 보관 정책 미확정	개인정보 안내가 실제 정책과 어긋날 위험	구현 직전 공식 문서·계정 설정 재확인 후 안내 문구 확정
개인 개발 환경 제약	계획한 배포 구조가 실현 불가능할 수 있음	기획서 20.4 기준 범위 조정, PWA·웹 경로는 계속 개발
2.27 구현 순서와 기술적 의존관계

8. 회원가입/로그인/인증 (Spring Session JDBC 스키마·마이그레이션, login_security_state 포함)
        │
        ▼
9. PWA 캘린더 일정 CRUD (schedules: version, 충돌 로직, 즉시 하드 삭제)
        │
        ▼
10. 웹 자연어 일정 조회/등록/수정
     (conversation 모듈: confirmation_requests 통합 확인 모델 + 자동 재계산 로직 + OpenAI 어댑터)
        │
        ▼
11. 카카오 챗봇 사용자 연결 (kakao_user_links/connection_codes)
        │
        ▼
12~14. 카카오톡 일정 조회/등록/수정 (10의 확인 모델 재사용 + 11의 연결 상태)
        │
        ▼
15. 보안/예외처리/자동테스트/최소보안 강화
     (SUPERSEDED 자동 재계산·동일 후보 재검증·동시 최초 생성 경쟁·계정 잠금 지속성 테스트 포함)
        │
        ▼
16. 프론트엔드/백엔드/DB 배포 (동일 출처 프록시, Spring Session 마이그레이션 포함)
2.28 기술 설계 완료 기준
이 목록은 절 개수를 세어 맞추기 위한 것이 아니라, 문서가 실제로 정합적이고 승인 가능한 상태인지 검증하기 위한 체크리스트다.

 본문 절 번호가 2.1부터 2.29까지 누락 없이 연속되는지 확인했다.
 선택지가 있는 항목마다 하나의 결정과 이유가 명시되어 있다.
 기획서·시작 프롬프트·GitHub 가이드와 충돌하는 결정이 없다.
 확인 요청 소비 순서가 "검증 후 소비" 원칙을 실제로 만족한다.
 SUPERSEDED 발생 시 서버가 자동으로 새 후보를 계산·발급하며, 대상 일정이 사라진 경우에만 재입력을 요구한다.
 동일 후보 지문 재사용 전 버전·충돌을 재검증하며, 이 흐름이 부분 유니크 인덱스와 충돌하지 않는다.
 candidate_fingerprint가 명시된 7개 필드와 정규화 규칙에 따라 일관되게 계산된다.
 동일 후보의 동시 최초 생성 경쟁이 부분 유니크 인덱스를 통해 안전하게, 500 오류 없이 처리된다.
 요청 제한 카운터가 성격에 따라 메모리/PostgreSQL로 명확히 구분되어 있다.
 OpenAI store: false와 ZDR이 명확히 구분되어 있다.
 기술 버전 표가 공식 출처만 사용한다.
 확정 불가능한 항목이 "PoC 검증 필요" 또는 "보류"로 분리되어 있다.
 사용자가 이 v1.0 Final을 검토하고 최종 승인했다.
마지막 항목(사용자 최종 승인)만 이 응답 이후의 사용자 확인을 필요로 합니다.

2.29 다음 단계(화면·기능 명세서)에 전달할 사항
화면 후보: 회원가입/로그인, 월간 캘린더(+날짜별 목록), 일정 상세, 일정 등록/수정 폼(충돌 시 확인 다이얼로그), 웹 자연어 대화 화면, 카카오 연결 관리 화면, 계정 설정
회원가입: email, password, passwordConfirmation만 입력받고 이메일은 trim·소문자 정규화 후 최대 254자로 검증한다. 비밀번호는 8~64자이며 공백 전용을 거부하고 확인값과 정확히 일치시킨다. 성공은 201과 email·timezone·createdAt을 반환하며 자동 로그인과 세션 생성은 하지 않는다. 중복 이메일은 409 DUPLICATE_EMAIL, 입력 오류는 422 VALIDATION_ERROR로 처리한다.
로그인: email과 password만 입력받고 성공은 200과 email·timezone을 반환한다. 실패 원인은 401 INVALID_CREDENTIALS로 일반화하며, 세션 쿠키는 CALTALK_SESSION이고 유휴 만료는 12시간이다. 로그인 성공 후 검증된 내부 상대 복귀 경로가 있으면 우선 이동하고 없으면 홈으로 이동한다. 실패 시 이메일은 유지하고 비밀번호는 지운다.
로그아웃: POST /api/v1/auth/logout은 CSRF 보호 대상으로 유지한다. 유효한 CSRF 토큰이 있으면 인증 여부와 무관하게 현재 세션과 SecurityContext를 정리하고 CALTALK_SESSION을 삭제한 뒤 빈 본문의 204를 반환한다. 클라이언트는 인증 상태와 사용자 캐시를 초기화하고 로그인 화면이 아닌 시작 화면으로 이동하며, 서버 리다이렉트와 민감 인증 정보 노출은 허용하지 않는다.
현재 사용자: 앱 초기 진입과 새로고침에서 GET /api/v1/users/me를 호출한다. 200이면 email·timezone·createdAt을 메모리 상태에 복원하고, 401이면 공개 화면은 유지하며 보호 화면에서 시작 화면으로 이동한다. 응답은 no-store이며 사용자 없는 인증 세션은 무효화하고 SecurityContext와 CALTALK_SESSION을 정리한 뒤 일반화된 401 JSON을 반환한다.
화면별 상태: 로딩, 데이터 없음, 입력 오류(422), 권한 없음(403), 인증 만료(401), REPHRASE_REQUIRED, AI_SERVICE_UNAVAILABLE(웹 503), SCHEDULE_CONFLICT→확인 다이얼로그, CONFIRMATION_SUPERSEDED(→"정보가 바뀌어 다시 확인이 필요합니다"와 함께 최신 제안 내용을 자동으로 다시 보여주고 재확인만 받으면 됨), CONFIRMATION_TARGET_GONE(→"해당 일정을 찾을 수 없습니다. 처음부터 다시 시도해주세요")
계정 잠금 안내: 로그인 실패가 누적되어 잠긴 경우, 정확한 원인을 노출하지 않으면서도 "잠시 후 다시 시도해주세요" 수준의 문구로 안내
PWA 저장 충돌 확인 다이얼로그와 자연어 대화의 최종 확인이 내부적으로 동일한 서버 로직을 사용한다는 점
일정 삭제가 즉시·되돌릴 수 없는 삭제임을 명확히 경고하는 문구(휴지통 없음)
월간 그리드 전용 캘린더(일간 뷰 없음) 범위 유지

부록 A. 카카오톡 채널 챗봇 PoC 최종 기술 판정

PoC 본표 97개 개별 행을 재집계한 결과는 통과 50, 제한적 통과 14, 미실행 33, 기타 0이며 고유 ID 97개, 중복·누락 0이다. 최종 판정은 **PoC 조건부 종료**다.

핵심 기술 성과:

- 카카오 스킬 연동: 관리자센터 Test URL과 실제 개발 채널 호출, POST JSON·SkillResponse 2.0, simpleText·quickReplies·basicCard·버튼·웹링크, 테스트 도구 accountId와 실제 채널 botUserKey 차이를 KSK-001~008·010·012, KUI-001·004~006·008·012·013·017로 확인했다.
- 요청·응답 안정성: 고정 응답·연결 상태·실제 채널·버튼 반복, 3초·4초 지연, 5초 초과 시 화면 동작, 응답 크기 실측을 KPF-001·002·007~009, KID-001·002·004, KUI-018로 확인했다. KUI-018의 정확한 플랫폼 차단 경계와 KPF-009의 관리자센터 오류 증적은 미확인이다.
- 연결 코드·상태: 발급·만료·1회 사용·재사용 차단·재발급 무효화·오입력 5회·10분 3회 발급 제한·4번째 차단·연결 상태·재연결 거부·명시적 해제·해제 후 접근 차단을 KLC-001~007·009~015, KPF-002로 확인했다.
- 보안·예외: 테스트 헤더와 관리자 토큰, 잘못된 메서드·Content-Type, 1MiB 본문 제한, 식별값 누락, 식별값·발화·코드·헤더·토큰 비저장, 외부 오류 일반화와 서버 예외 내부정보 비노출을 KSE-001~008·010~014, KPF-011로 확인했다.
- 방어 로직: 사용자별 10초·5회 빈도 제한과 복구, HMAC 보조 지문의 3초 중복 상태 변경 방지, 안전 응답·서버 생존·health를 KSE-009, KID-008, KPF-011로 확인했다.

확인된 한계:

- 제한적 통과 14개에는 관리자센터 오류 이력 미제공, 정확한 플랫폼 경계 미확인, 요청 진위·자동 재시도 식별자 미확정, 두 번째 실제 사용자 부재, 단일 인스턴스 메모리 한계와 실제 DB 트랜잭션 미검증이 남아 있다.
- 미실행 33개는 정식 백엔드·DB/일정 CRUD·대화 상태·OpenAI·카카오 UI/payload 계약 또는 외부 사용자·플랫폼 기능이 필요하다. 현재 PoC에서 임의 모의 경로를 더 늘리는 것은 정식 구현 위험을 충분히 줄이지 못한다.
- KSK-011·KID-003·KPF-003은 각각 두 번째 실제 사용자, 플랫폼 재전송 기능, 실행 정의·전용 조회 경로가 없어 HOLD를 유지한다.

정식 개발 이관 원칙:

- 본 PoC에서 검증된 HTTP·payload·응답·보안·예외·빈도 제한·중복 방지 계약과 테스트 결과만 정식 구현의 인수 조건으로 옮긴다.
- 영속 상태, 일정 CRUD, pending·confirmation, OpenAI 구조화, Redis 기반 rate limit·분산 멱등성은 정식 아키텍처와 트랜잭션 경계 안에서 새로 구현하고 자동화 테스트로 재검증한다.
- 정식 저장소·브랜치 전략, 확정 백엔드 프레임워크, DB 스키마, 사용자 계정과 카카오 연결 식별 계약, 비밀 관리, 배포 환경과 운영 채널 전환 기준을 착수 전에 승인한다.

PoC 코드 처리 방향:

`server.js`는 카카오 연동·보안·예외·빈도 제한·보조 중복 방지 검증을 위한 메모리 Map 기반 단일 인스턴스 참고 구현이다. 재시작 시 상태가 초기화되고 실제 일정 CRUD·DB·OpenAI가 없으므로 정식 서비스 코드로 직접 승격하거나 계속 확장하지 않는다. 검증된 계약과 테스트 결과만 정식 구현에 이관하고, 임시 발화·지연·오류·모의 상태 변경 분기는 정식 코드에 복사하지 않는다.

종료·재개 원칙:

정식 개발 착수 조건이 충족되는 것을 전제로 PoC를 조건부 종료한다. 카카오 payload 또는 관리자센터 동작의 중대한 변경, 운영 채널에서 개발 채널과 다른 필수 동작, 두 사용자 격리 실패, 분산 rate limit·멱등성 설계 불가, 정식 DB/OpenAI 통합에서 핵심 계약 불충족이 발견되면 해당 범위만 PoC 또는 통합 검증으로 재개한다.
