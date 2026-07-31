1. 문서 확인
다음 여섯 문서를 기준 자료로 확인했다.
CalTalk_MVP_서비스_기획서_최종본.md
CalTalk_기술_설계서_v1.0_Final.md
CalTalk_프로젝트_시작_프롬프트_최종본.md
CalTalk_화면_기능_명세서_초안_v0.2.md
GitHub_공개저장소_업로드_커밋_푸시_점검가이드.md
CalTalk 화면·기능 명세서 v0.2 — 독립 검수 보고서.md
문서 내부의 버전과 상태를 기준으로 판단했으며, 파일명에 붙을 수 있는 중복 표시와 로컬 경로는 문서 버전 판단에 사용하지 않았다.
2. 독립 검수 의견 반영 판정표
검수 의견	판정	기준 문서 근거	판정 이유	최종 반영 위치
자연어 과거 일정 거부 상태 추가	반영	서비스 기획서 11.7, 기술 설계서 2.10	해석 실패가 아니라 정책상 거부이므로 REPHRASE_REQUIRED와 분리해야 한다. 다만 기술 설계의 확정 enum으로 표현하지 않고 화면·API 명세 제안 상태로 둔다.	SCR-CHAT-001, 공통 상태 사전, 수용 기준, 테스트, 구현 전달사항, UX 위험, PoC 전달사항
재질문 대기 10분 만료 처리 추가	반영	서비스 기획서 15.4, 기술 설계서 2.13	pending_commands 10분과 confirmation_requests 5분은 별도 계약이다.	SCR-CHAT-001, 공통 상태 사전, 수용 기준, 테스트
생략된 사용자 흐름 전문 복원	반영	문서 자기완결성 요구	최종 문서 하나만으로 전체 흐름을 파악할 수 있어야 한다.	사용자 흐름 18개
자연어 확인 취소 API 매핑 추가	반영	기술 설계서 2.4, 2.8.4, 2.18.1	PWA와 자연어 확인이 공통 confirmation 모델을 사용한다.	화면별 API 연결표
수정 후보 선택 API 매핑 추가	반영	기술 설계서 2.13, 2.18.1	후보 선택은 일정 ID를 URL에 노출하는 별도 엔드포인트가 아니라 POST /api/v1/chat/messages 재사용 제안으로 정리한다.	DLG-CHAT-TARGET-001, API 연결표
수용 기준 “22개”를 실제 개수로 정정	반영	실제 목록 직접 계산	고정 숫자에 맞추지 않고 최종 목록을 기준으로 재계산한다.	수용 기준, 최종 요약
시간대 변경 후 일정 캐시 무효화 확대	반영	기술 설계서 2.10, 프런트엔드 캐시 원칙	UTC 저장값은 유지되지만 날짜 구분과 표시가 달라지므로 일정 관련 캐시 전체를 갱신해야 한다.	SCR-SET-002, API 연결표, 테스트
DLG-EXPIRED-001 표시 방식 명확화	반영	화면별 UX 차이 허용, 서버 모델 공통 원칙	PWA는 다이얼로그, 웹 채팅은 만료 카드로 구체화한다.	UI 명세
연결 코드 발급 제한 안내 문구 추가	반영	기술 설계서 2.19.2	10분 3회 제한은 이미 확정됐으며 사용자에게 재시도 안내가 필요하다.	SCR-KAKAO-001
다음 날을 넘는 다중 일자 일정 상한 확정	미반영	기준 문서에서 보류	화면 명세 단계에서 임의 확정할 수 없다.	보류·확인 필요 항목
회원가입 비밀번호 길이 정책	반영	기술 설계서 2.7.5, 2.18.5	8자 이상 64자 이하이며 조합 강제 없이 공백 전용 값을 거부하는 것으로 확정했다.	SCR-AUTH-001, 공통 오류 사전, 테스트, 구현 전달사항
카카오 PoC 성공을 전제로 한 공개 기능 표현	미반영	서비스 기획서 20.4, 기술 설계서 2.25·2.26	실제 스킬과 응답 제약은 PoC 전이다.	랜딩, PoC 전달사항

3. CalTalk 화면·기능 명세서 v1.0 Final
3.1 문서 정보
문서명: CalTalk 화면·기능 명세서
버전: 1.0 Final
기준 시간대: Asia/Seoul
상태: 구현 착수 기준 명세
적용 범위: PWA 화면, 웹 자연어 대화 화면, 카카오 연결 관리, 공통 상태·오류, 사용자 흐름, 화면별 API 연결, 수용 기준과 테스트 기준
3.2 목적
CalTalk MVP의 화면과 기능을 하나의 문서로 통합하여 인증, PWA 일정 CRUD, 웹 자연어 일정 관리, 카카오 연결 및 후속 구현 단계에서 공통 기준으로 사용한다.
3.3 비범위
다음 기능은 포함하지 않는다.
반복·종일·공유·팀 일정
참석자와 첨부파일
외부 캘린더 연동
자연어 일정 삭제
선제 알림
카카오 로그인
자동 연결 상태 폴링과 실시간 push
완전한 오프라인 일정 조회·수정·동기화
일간 시간표형 캘린더
관리자 화면
다국어, 다크 모드, 테마 사용자 지정
실제 색상·폰트·브랜드 스타일 확정
3.4 사용자와 권한
사용자	허용 범위
비로그인 사용자	시작, 회원가입, 로그인, 개인정보 처리 안내
로그인 사용자	본인 일정, 웹 대화, 카카오 연결, 계정·시간대 설정, 탈퇴
카카오 미연결 사용자	연결 안내만 제공하며 일정 데이터는 반환하지 않음
카카오 연결 사용자	연결된 CalTalk 사용자의 일정 조회·생성·수정

모든 일정 접근은 서버가 인증 사용자 또는 검증된 카카오 연결의 내부 사용자 ID를 기준으로 소유권을 재검증한다.
3.5 정보 구조와 내비게이션
로그인 영역의 하단 탭은 다음 네 개로 고정한다.
홈
캘린더
대화
설정
카카오 연결 관리는 설정 하위 화면이다. 개인정보 처리 안내는 로그인 여부와 관계없이 접근 가능하다.
4. 최종 화면 목록
번호	ID	화면명	URL
1	SCR-LAND-001	시작 화면	/
2	SCR-AUTH-001	회원가입	/signup
3	SCR-AUTH-002	로그인	/login
4	SCR-HOME-001	홈·오늘 일정	/home
5	SCR-CAL-001	월간 캘린더	/calendar
6	SCR-CAL-002	선택 날짜 일정 목록	/calendar?date=YYYY-MM-DD
7	SCR-SCHED-001	일정 상세	/schedules/{id}
8	SCR-SCHED-002	일정 등록	/schedules/new
9	SCR-SCHED-003	일정 수정	/schedules/{id}/edit
10	SCR-CHAT-001	웹 자연어 일정 관리	/chat
11	SCR-KAKAO-001	카카오 연결 관리	/settings/kakao
12	SCR-SET-001	계정 설정	/settings/account
13	SCR-SET-002	시간대 설정	/settings/timezone
14	SCR-SET-003	회원 탈퇴 확인	/settings/account/delete
15	SCR-LEGAL-001	개인정보 처리 안내	/privacy

총 15개다. SCR-CAL-001과 SCR-CAL-002는 동일 라우트에 배치되는 두 화면 섹션이지만 각각 독립된 명세 ID를 유지한다.
5. 화면 공통 동작
5.1 보호 화면
로그인이 필요한 화면에서 세션이 만료되면 로그인 화면으로 이동한다. 재로그인 성공 후 안전한 내부 경로였던 경우 원래 화면으로 복귀한다.
앱 초기 진입과 새로고침에서는 GET /api/v1/users/me로 세션 인증 상태를 확인한다. 200이면 email·timezone·createdAt을 사용자 상태에 복원하고, 401이면 비로그인 상태로 초기화한다. 공개 화면은 현재 시작 화면을 유지하고 보호 화면은 시작 화면으로 이동하며 로그인 화면으로 무조건 이동하지 않는다. 401은 일반 서버 오류가 아니라 정상적인 인증 상태 판정 결과로 처리한다.
현재 사용자 응답은 Cache-Control: no-store로 저장하지 않으며 ETag와 Last-Modified를 사용하지 않는다.
5.2 로딩과 중복 실행
요청 중 실행 버튼을 비활성화한다.
조회 영역은 콘텐츠 위치를 유지하는 로딩 상태를 표시한다.
동일 확인 승인 재전송은 서버 멱등성 규칙에 따라 한 번만 반영한다.
지연된 AI 응답이 나중에 임의로 DB를 변경해서는 안 된다.
5.3 모바일
360px 전후 폭에서 가로 스크롤 없이 핵심 기능을 사용할 수 있어야 한다.
주요 액션의 터치 영역을 충분히 확보한다.
키보드 표시 시 입력 필드와 실행 버튼이 가려지지 않게 스크롤을 조정한다.
하단 내비게이션과 화면별 고정 액션이 겹치지 않게 한다.
5.4 접근성
입력 필드에는 연결된 label을 제공한다.
오류는 필드 및 오류 요약과 연결한다.
상태 변화는 적절한 live region으로 알린다.
색상만으로 선택·오류·충돌을 구분하지 않는다.
다이얼로그는 포커스를 가두고 닫힌 뒤 실행 버튼으로 포커스를 복귀시킨다.
6. 화면별 상세 명세
6.1 SCR-LAND-001 — 시작 화면
목적: 서비스를 소개하고 회원가입 또는 로그인으로 안내한다.
진입 조건: 비로그인.
진입 경로: 직접 URL, PWA 실행.
상단: 서비스명과 한 문장 소개.
본문: PWA 직접 일정 관리와 웹 자연어 대화를 중심으로 설명한다.
하단 액션: 회원가입, 로그인.
입력·표시 데이터: 없음.
정상 상태: 소개와 두 진입 버튼 표시.
로그인 사용자 접근: /home으로 전환.
서버·외부 API 상태: 없음.
접근성: 문서 제목과 주요 액션 순서 명확.
모바일: 세로 1열.
관련 API: 없음.
카카오 표시 원칙: PoC 전에는 완성된 기능으로 홍보하지 않고 “연결 시 이용 가능한 보조 기능” 수준으로만 표시한다.
완료 기준: 로그인 상태 분기와 두 인증 경로가 정상 동작한다.
6.2 SCR-AUTH-001 — 회원가입
목적: 이메일과 비밀번호로 계정을 만든다.
진입 조건: 비로그인.
입력: email, password, passwordConfirmation. 모두 필수이며 이름·닉네임·표시 이름·시간대는 입력받지 않는다.
이메일 검증: 앞뒤 공백 제거와 소문자 정규화 후 올바른 이메일 형식과 최대 254자를 검증한다.
비밀번호 검증: 8자 이상 64자 이하이며 문자·숫자·특수문자 조합은 강제하지 않는다. 공백만으로 구성된 값은 허용하지 않고 passwordConfirmation은 password와 정확히 일치해야 한다.
정상 상태: 빈 폼.
로딩: 가입 버튼 비활성화.
422 VALIDATION_ERROR: 공통 오류 JSON의 fieldErrors를 입력 필드와 연결한다.
409 DUPLICATE_EMAIL: 계정 노출을 확대하지 않는 서버 문구를 사용한다.
429 RATE_LIMITED: 잠시 후 재시도하도록 안내한다.
성공: HTTP 201 응답의 email, timezone, createdAt을 확인한 뒤 자동 로그인이나 세션 생성 없이 /login으로 이동해 “가입이 완료되었습니다. 로그인해주세요.”를 표시한다.
취소: /.
접근성: 비밀번호 표시·숨김, 오류 연결.
관련 API: POST /api/v1/auth/signup.
완료 기준: 성공 경로는 로그인 화면 이동 하나뿐이며 자동 로그인 대안은 없다.
6.3 SCR-AUTH-002 — 로그인
목적: 서버 세션을 발급받는다.
입력: email, password. 모두 필수다.
진입 사유별 안내: 가입 완료, 세션 만료.
이메일 검증: 앞뒤 공백 제거와 소문자 정규화 후 올바른 이메일 형식과 최대 254자를 검증한다.
비밀번호 검증: 8자 이상 64자 이하로 검증한다.
성공: HTTP 200 응답의 email과 timezone을 확인하고 서버 세션을 사용한다. 기존 세션이 있으면 세션 ID를 교체하며, 이미 로그인한 사용자도 새 인증 정보로 재인증하고 중복 로그인 오류를 반환하지 않는다.
실패: 계정 부재, 비밀번호 오류, 계정 잠금의 내부 차이를 노출하지 않고 HTTP 401 INVALID_CREDENTIALS와 “이메일 또는 비밀번호를 확인해주세요.”를 사용한다. 로그인 화면과 이메일 입력값은 유지하고 비밀번호 입력값은 삭제한다.
로딩: 로그인 버튼 비활성화.
이동: 검증된 서비스 내부 상대 복귀 경로가 있으면 우선 이동하고, 없으면 /home으로 이동한다. 외부 URL과 프로토콜 상대 URL은 복귀 경로로 허용하지 않는다.
세션: CALTALK_SESSION 쿠키를 사용하며 12시간 유휴 만료다. 쿠키는 HttpOnly=true, SameSite=Lax, Path=/이고 Domain과 Max-Age는 지정하지 않는다. Secure는 로컬 HTTP 개발환경에서 false, 운영 HTTPS 환경에서 true로 설정하며 운영에서 false를 허용하지 않는다. 자동 로그인, 로그인 상태 유지 체크박스와 동시 로그인 제한은 제공하지 않는다.
관련 API: POST /api/v1/auth/login.
완료 기준: 계정 존재 여부와 잠금 여부가 응답 문구 차이로 노출되지 않는다.
6.4 SCR-HOME-001 — 홈·오늘 일정
목적: 사용자 시간대 기준 오늘 일정을 빠르게 확인한다.
표시: 오늘 날짜, 시간순 일정 최대 3건, PWA 설치 안내, 카카오 연결 상태 진입점.
4건 이상: “전체 일정 보기”를 표시해 오늘 날짜의 SCR-CAL-002로 이동한다.
빈 상태: “오늘 등록된 일정이 없어요.”
주요 액션: 일정 등록, 전체 일정 보기, 캘린더, 대화, 카카오 설정.
로딩·오류: 일정 영역에서 표시하며 이전 사용자 데이터가 다른 세션에 남지 않게 한다.
관련 API: GET /api/v1/schedules.
완료 기준: 최대 3건만 노출하고 KPI나 통계를 추가하지 않는다.
6.5 SCR-CAL-001 — 월간 캘린더
목적: 월 단위로 일정 존재 날짜를 파악한다.
상단 고정: 연·월, 이전 달, 다음 달, 오늘.
본문: 요일 헤더와 7열 월간 그리드.
일정 표시: 날짜마다 존재 여부를 점과 접근성 텍스트로 표시하며 일정 제목을 그리드에 과도하게 나열하지 않는다.
날짜 선택: URL의 date를 갱신하고 SCR-CAL-002 위치로 스크롤한다.
그리드는 상단에 고정하지 않고 문서 흐름과 함께 스크롤한다.
관련 API: GET /api/v1/schedules.
완료 기준: 일간 시간표형 보기 없이 월간 그리드만 제공한다.
6.6 SCR-CAL-002 — 선택 날짜 일정 목록
목적: 선택 날짜의 본인 일정을 시간순으로 본다.
표시: 절대 날짜, 제목, 시작·종료, 장소 유무.
빈 상태: “이 날짜에는 일정이 없어요.”
액션: 일정 상세, 이 날짜에 일정 등록, 캘린더로 돌아가기.
등록 이동: 선택 날짜를 초기값으로 전달하되 URL에는 민감한 내부 식별값을 넣지 않는다.
관련 API: GET /api/v1/schedules.
완료 기준: 목록 공간이 월간 그리드 때문에 사용할 수 없을 정도로 압박되지 않는다.
6.7 SCR-SCHED-001 — 일정 상세
목적: 단일 일정 확인과 수정·삭제 진입.
표시: 제목, 연·월·일·요일, 시작, 종료, 장소 또는 “장소 없음”.
진입 조건: 로그인 및 본인 소유.
액션: 수정, 삭제, 뒤로가기.
404: 존재하지 않거나 삭제되었거나 다른 사용자 소유인 일정을 구분하지 않고 SCHEDULE_NOT_FOUND로 안내한 뒤 안전한 목록으로 이동.
삭제: DLG-DELETE-001을 연다.
관련 API: GET /api/v1/schedules/{scheduleId}, DELETE /api/v1/schedules/{scheduleId}?version={version}.
완료 기준: 다른 사용자의 일정 내용이 노출되지 않는다.
6.8 SCR-SCHED-002 — 일정 등록
입력: 제목, 날짜, 시작 시간, 종료 시간, 장소, 다음 날 종료 토글.
제목·날짜·시작·종료는 필수이고 장소는 선택이다.
서버 요청 필드는 title, startAt, endAt, location 네 개다. 날짜·시작 시간·종료 시간·다음 날 종료 토글은 UTC 오프셋을 포함한 startAt·endAt을 만들기 위한 화면 입력이며 allDay를 전송하지 않는다.
제목은 앞뒤 공백을 제거한 뒤 1자 이상 200자 이하여야 한다. 누락·빈 값·공백 전용 값은 REQUIRED, 200자 초과는 MAX_LENGTH 필드 오류로 표시한다.
장소는 앞뒤 공백을 제거하고 최대 200자다. 누락·null·빈 문자열을 허용하며 trim 후 빈 문자열은 null로 전송·저장하고, 200자 초과는 MAX_LENGTH 필드 오류로 표시한다.
startAt과 endAt은 UTC 오프셋을 포함한 ISO-8601 offset date-time으로 전송한다. 오프셋 없는 값과 누락은 저장하지 않으며, 종료가 시작과 같거나 빠르면 endAt의 INVALID_TIME_RANGE와 “종료 시각은 시작 시각보다 늦어야 합니다.”를 표시한다.
시작 시간 입력 시 종료 시간을 시작+1시간으로 자동 채운다.
사용자가 종료 시간을 직접 수정하기 전 시작 시간이 바뀌면 종료 시간도 같은 차이만큼 이동한다.
종료 시간 직접 수정 후 시작 시간 변경 시 동작은 최종 구현 결정 전 확인이 필요하다.
토글 OFF에서 종료가 시작 이하이면 저장을 차단한다.
토글 ON에서는 종료를 다음 날 시각으로 해석한다.
제목·장소는 최대 200자라는 기술 설계 제약을 적용한다.
PWA 직접 입력은 과거 일정도 허용하되 절대 날짜와 시간을 저장 전에 명확히 표시한다.
충돌 없음: 저장 클릭을 최종 확인으로 보고 즉시 저장.
충돌 있음: 409 SCHEDULE_CONFLICT의 confirmationId로 DLG-CONFLICT-001을 연다. 최초 요청에 클라이언트 승인 플래그를 보내지 않는다.
성공: 201 응답을 받으면 입력 화면을 닫고 성공 안내를 표시한 뒤 선택 날짜 일정 목록, 홈의 오늘 일정, 월간 캘린더를 무효화하거나 다시 조회한다. 생성 일정이 현재 범위에 포함되면 즉시 표시하고 UTC 응답을 사용자 시간대로 변환한다.
검증 실패: 입력 화면과 입력값을 유지하고 422 fieldErrors를 해당 필드에 표시한다. 서버 오류는 검증 오류와 구분한다.
충돌 응답: 입력 화면과 값을 유지한 채 숫자 confirmationId와 conflicts를 받는다. conflicts는 startAt 오름차순, 같으면 id 오름차순이며 각 항목의 id·title·UTC startAt·UTC endAt·location만 DLG-CONFLICT-001에 표시한다. 취소하면 생성하지 않고 “그대로 저장”을 선택하면 POST /api/v1/confirmations/{confirmationId}/approve에 conflictAcknowledged: true를 보내 기존 확인 절차를 진행한다. 일정 생성 API를 confirmationId와 함께 다시 호출하지 않는다.
인증 만료: 401이면 비로그인 상태로 전환하고 보호 화면 정책을 적용한다. CSRF 실패 403은 성공으로 처리하지 않는다.
관련 API: POST /api/v1/schedules, 승인·취소 API.
완료 기준: 서버로 보내는 종료 시간이 비어 있지 않고 생성 성공 뒤 관련 일정 화면이 새 UTC 저장값의 사용자 시간대 표시로 갱신된다.
6.9 SCR-SCHED-003 — 일정 수정
입력은 기존 저장값으로 초기화한다.
수정 가능 필드: 제목, 날짜, 시작, 종료, 장소.
저장 요청은 변경한 필드와 현재 상세 응답의 필수 version을 보내는 부분 수정이다. 변경할 필드 없이 version만 보내지 않는다.
장소를 수정하지 않으면 필드를 보내지 않고, 장소 삭제는 null 또는 trim 후 빈 문자열, 장소 설정은 trim한 문자열로 보낸다.
기존 일정이 자정을 넘으면 다음 날 종료 토글을 켠 상태로 연다.
날짜 변경 시 기존 당일·익일 관계를 유지한다.
충돌 없음: 즉시 수정.
충돌 있음: 공통 confirmation 모델로 전환.
승인 대기 중 대상 버전이나 충돌 목록이 바뀌면 최신 후보로 자동 갱신해 재확인을 받는다.
대상이 삭제됐으면 수정 폼으로 복구하지 않고 대상 없음 안내 후 목록으로 이동한다.
버전 충돌: 입력을 유지하고 최신 상세 재조회 안내를 표시하며 자동 덮어쓰지 않는다.
관련 API: PATCH /api/v1/schedules/{scheduleId}, 승인·취소 API.
완료 기준: 다른 사용자 일정과 삭제된 일정은 수정할 수 없다.
6.10 SCR-CHAT-001 — 웹 자연어 일정 관리
목적: 자연어로 일정 조회·생성·수정.
입력: 단일 메시지.
허용 명령: 조회, 생성 후보, 수정 후보.
자연어 삭제는 지원하지 않는다.
조회: 오늘, 내일, 특정 날짜, 상대 표현으로 지정된 하루.
누락값: STA-CHAT-MISSING-001로 재질문한다.
재질문 대기: 마지막 상호작용 후 10분.
10분 만료: 다음 응답을 이전 후보에 이어 붙이지 않고 새로운 요청으로 다시 해석한다. 화면에는 NEEDS_INPUT / PENDING_COMMAND_EXPIRED 안내를 인라인으로 표시한다.
수정 후보: 0건은 조건 재질문, 1건은 바로 확인, 2~5건은 DLG-CHAT-TARGET-001, 6건 이상은 조건 축소 재질문.
수정 대상 후보 선택은 별도 API를 신설하지 않고 진행 중인 자연어 대화의 후속 입력으로 처리하며 POST /api/v1/chat/messages를 재사용한다.
요청에는 내부 일정 ID 대신 불투명한 후보 선택값을 포함한다. 개념 요청 예시는 다음과 같다.
```json
{
  "message": null,
  "selection": {
    "type": "SCHEDULE_CANDIDATE",
    "value": "opaque-candidate-token"
  }
}
```
이 예시와 정확한 DTO 필드명은 실제 DTO 확정안이 아닌 화면·API 명세 제안이며 구현 단계에서 확정한다.
서버는 pending_commands의 존재 여부와 만료 여부, 후보 일정의 사용자 소유권, 후보가 아직 존재하는지와 선택된 후보가 현재 후보 집합에 포함되는지를 다시 검증한다.
후보 선택만으로 일정 DB 데이터는 변경하지 않으며, 검증 성공 시 자연어 수정 최종 확인 카드로 전환한다.
생성·수정: 최종 확인 전 DB 변경 없음.
확인 대기: 5분.
과거 일정: 생성 또는 과거 시각으로의 수정 요청을 거부한다. 화면·API 제안 상태 PAST_DATETIME_REJECTED를 사용하며 PWA 직접 입력을 안내한다.
날짜나 시간 자체가 모호하면 NEEDS_INPUT을 사용하고 과거 정책 거부와 구분한다.
AI 장애: 일정 DB를 변경하지 않고 직접 입력 이동 버튼을 제공한다.
취소: confirmation 취소 API 호출 후 카드를 종료한다.
관련 API: POST /api/v1/chat/messages, 승인·취소 API.
완료 기준: 확인 전 변경, 중복 변경, 과거 자연어 변경이 발생하지 않는다.
6.11 SCR-KAKAO-001 — 카카오 연결 관리
표시: 연결 여부, 연결 시각, 연결 코드 발급 또는 연결 해제 액션.
미연결: 코드 발급 버튼과 입력 절차 안내.
발급 코드: UI 카운트다운은 참고 표시이고 서버 expires_at이 최종 기준이다.
발급 제한: “연결 코드는 10분에 3번까지 발급할 수 있어요. 잠시 후 다시 시도해주세요.”
연결 과정: PWA에서 자동 폴링하거나 실시간 push하지 않는다.
사용자는 카카오톡에서 코드를 입력한 뒤 “연결 상태 확인”을 직접 누른다.
상태 응답에는 connected, linkedAt만 사용한다.
외부 식별값, 봇 ID, 채널 ID, HMAC, 내부 사용자 ID는 노출하지 않는다.
연결 해제: 대기 명령과 확인 요청도 무효화하고 이후 카카오 일정 접근을 차단한다.
관련 API: POST /api/v1/kakao/link-codes, GET /api/v1/kakao/link, POST /api/v1/kakao/links/revoke.
완료 기준: 연결 코드를 카카오 로그인으로 표현하지 않는다.
6.12 SCR-SET-001 — 계정 설정
표시: 이메일, 시간대, 카카오 연결 관리, 개인정보 처리 안내, 로그아웃, 회원 탈퇴 진입.
이메일은 표시 전용이며 변경 기능은 포함하지 않는다.
로그아웃은 유효한 CSRF 토큰과 함께 POST /api/v1/auth/logout으로 요청한다. 인증 상태와 관계없이 성공은 HTTP 204이며 본문과 서버 리다이렉트가 없다.
로그아웃 성공: 클라이언트 인증 상태와 사용자 캐시를 초기화하고 로그인 화면으로 직접 이동하지 않은 채 시작 화면으로 이동한다. 브라우저 뒤로가기로 보호 화면이 재노출되지 않도록 보호 화면 기록과 캐시를 안전하게 처리한다.
서버는 현재 세션과 SecurityContext를 종료하고 CALTALK_SESSION을 빈 값, Path=/, Max-Age=0, HttpOnly=true, SameSite=Lax, 환경별 Secure, Domain 미지정으로 삭제한다. 이미 세션이 없는 요청도 유효한 CSRF 토큰이 있으면 204로 처리한다.
CSRF 토큰 누락·불일치는 403 FORBIDDEN 공통 JSON 오류로 표시하며 로그아웃 성공으로 간주하거나 화면을 이동하지 않는다.
관련 API: GET /api/v1/users/me, POST /api/v1/auth/logout.
완료 기준: 로그아웃 이후 이전 사용자 데이터와 보호 화면이 남지 않고, 기존 세션 쿠키를 사용한 보호 API는 HTML 리다이렉트 없이 401 UNAUTHORIZED JSON을 반환한다.
6.13 SCR-SET-002 — 시간대 설정
목적: 일정 표시와 상대 날짜 해석 기준을 변경한다.
입력: 지원 시간대 선택.
기본값: 현재 저장된 사용자 시간대.
시간대 변경 API: PATCH /api/v1/users/me.
요청: 앞뒤 공백을 제거한 IANA Time Zone ID인 timezone 하나만 전송한다. 이메일·비밀번호·사용자 ID·생성 시각·역할 정보는 전송하지 않는다.
허용 예: Asia/Seoul, Asia/Tokyo, America/New_York, Europe/London.
거부 예: 빈 값, 공백 전용 값, 존재하지 않는 Zone ID, KST, GMT+9, UTC+09:00, Seoul.
저장 중: 저장 액션을 비활성화해 동일 화면의 중복 제출을 막는다.
성공: HTTP 200과 Cache-Control no-store 응답의 email·timezone·createdAt을 사용자 상태에 반영하고 성공 안내를 표시한다. 같은 시간대 재저장도 성공으로 처리한다.
입력 실패: HTTP 422 VALIDATION_ERROR의 timezone·INVALID_TIMEZONE fieldError를 시간대 입력에 연결하고 “올바른 시간대를 선택해주세요.”를 표시한다.
인증 실패: HTTP 401 UNAUTHORIZED이면 인증 상태를 초기화하고 보호 화면 정책에 따라 시작 화면으로 이동한다. 사용자 없는 인증 세션도 같은 상태로 처리하며 404로 구분하지 않는다.
CSRF 실패: HTTP 403 FORBIDDEN이면 기존 시간대를 유지하고 저장 성공으로 처리하지 않는다.
일반 서버 실패: 입력 오류와 구분되는 공통 오류를 표시하고 기존 시간대와 일정 캐시를 유지한다.
별도 캐시 API는 추가하지 않는다.
저장 성공 후 프런트엔드는 다음 데이터를 무효화하거나 다시 조회한다.
GET /api/v1/users/me 사용자 정보
홈 화면의 오늘 일정
월간 캘린더 일정
선택 날짜 일정 목록
현재 열려 있는 일정 상세의 표시값
그 후 사용자 시간대 기준으로 날짜, 요일, 시작 시간, 종료 시간, 오늘 및 선택 날짜 기준을 다시 계산한다.
기존 일정의 UTC 절대 저장값은 변경하지 않는다.
완료 기준: users.timezone만 바뀌고 저장된 일정의 UTC 절대값은 그대로이며 표시와 날짜 구분만 새 시간대 기준으로 갱신된다.
6.14 SCR-SET-003 — 회원 탈퇴 확인
경고: 계정, 일정, 변경 이력, 카카오 연결, 연결 코드, 대기 명령, 확인 요청, 사용자 범위 멱등성 기록과 로그인 보안 상태가 즉시 삭제된다.
확인 체크박스: 삭제 범위와 비가역성을 명시한다.
체크 전 “계정과 데이터 삭제” 버튼 비활성화.
비밀번호 재입력은 요구하지 않는다.
성공: 자동 로그아웃 후 시작 화면.
실패: 계정을 유지하고 체크 상태도 유지한 채 오류를 표시한다.
관련 API: DELETE /api/v1/users/me 화면 명세 제안.
완료 기준: 일부만 삭제된 성공 상태가 발생하지 않는다.
6.15 SCR-LEGAL-001 — 개인정보 처리 안내
접근: 로그인 여부와 무관.
내용:수집·이용 정보의 범주
일정 데이터 처리 목적
자연어 입력이 외부 API로 전달될 수 있다는 사실
이메일·내부 ID·인증 토큰은 명령 해석에 불필요하므로 외부 AI 요청에서 제외한다는 원칙
카카오 식별값을 HMAC으로 변환해 저장한다는 설명
회원 탈퇴 시 삭제 범위
관리형 DB 백업 보관 기간은 제공업체 확정 후 반영한다는 사실

금지 표현: 완전 익명, 복호화 가능한 암호화라는 오해를 주는 표현.
관련 API: 없음.
완료 기준: 비로그인 상태에서도 접근 가능하다.
7. 다이얼로그·시트·상태 UI 17개
ID	이름·유형	위치·표시 조건	내용과 액션	종료 조건·상태	DB 변경
DLG-CONFLICT-001	충돌 확인 다이얼로그	등록·수정 409 SCHEDULE_CONFLICT	정렬된 conflicts의 제목·UTC 시각을 사용자 시간대로 변환해 표시하고 장소가 있으면 함께 표시, 그대로 저장·다른 시간·취소	별도 승인 API 성공, 취소, 5분 만료, CONSUMED·SUPERSEDED·CANCELLED 상태	승인 성공 때만
DLG-DELETE-001	삭제 확인 다이얼로그	상세에서 삭제	되돌릴 수 없음, 삭제·취소	성공 또는 취소	삭제 성공 시 하드 삭제
DLG-CHAT-CONFIRM-CREATE-001	생성 확인 바텀시트	자연어 후보 완성	제목·절대 날짜·시간·장소·충돌, 등록·취소	승인·취소·5분 만료	승인 성공 때만
DLG-CHAT-CONFIRM-UPDATE-001	수정 확인 바텀시트	수정 후보 완성	대상, 전후 값, 충돌, 수정·취소	승인·취소·5분 만료	승인 성공 때만
DLG-CHAT-TARGET-001	후보 인라인 카드	후보 2~5개	제목·절대 시간, 후보 선택·취소	선택 또는 취소	없음
STA-CHAT-MISSING-001	재질문 메시지	필수값 부족	필요한 값 질문, 답변 입력	값 확보·취소·10분 만료	없음
DLG-EXPIRED-001	만료 UI	confirmation 5분 경과	PWA는 다이얼로그, 채팅은 기존 카드 만료 상태	닫기·새 요청	없음
DLG-SUPERSEDED-001	최신 후보 재확인	승인 시 상태 변경 감지	“정보가 바뀌어 다시 확인이 필요해요”, 최신 후보 표시	새 확인 승인·취소	재승인 전 없음
DLG-TARGET-GONE-001	대상 없음 안내	승인 대기 중 대상 삭제	처음부터 다시 시도 안내	닫기	없음
DLG-SESSION-EXPIRED-001	세션 만료	보호 요청 401	재로그인 안내	로그인 이동	없음
DLG-FORBIDDEN-001	권한 없음	403	접근 불가, 안전한 화면 이동	이동	없음
DLG-SERVER-ERROR-001	서버 오류	5xx	성공으로 오인하지 않는 오류, 재시도	재시도·이동	실패 요청은 없음
DLG-AI-UNAVAILABLE-001	AI 장애 배너	503	직접 일정 관리 가능 안내, 직접 입력 이동	재시도·이동	없음
STA-EMPTY-001	빈 상태	일정 0건	날짜별 빈 상태 문구와 등록 CTA	데이터 생성·날짜 변경	없음
STA-LOADING-001	로딩 상태	요청 진행	진행 상태, 중복 실행 방지	성공·실패	없음
DLG-PWA-INSTALL-001	설치 안내	설치 가능·미설치	설치, 나중에	선택·설치 가능 상태 종료	없음
STA-OFFLINE-001	오프라인 배너	네트워크 단절	오프라인 동기화 미지원, 재연결 안내	연결 복구	없음

모든 UI는 키보드 조작, 명확한 포커스, 스크린리더 상태 안내, 360px 모바일 표시를 지원해야 한다.
8. 공통 상태·오류·메시지 사전
HTTP/채널	코드·상태	사용자 메시지 원칙	화면 처리
200	정상	요청 결과 표시	화면 갱신
200 제안	PAST_DATETIME_REJECTED	“이미 지난 시간에는 자연어로 일정을 만들거나 과거 시각으로 변경할 수 없어요. 다른 시간을 말씀해주시거나 캘린더에서 직접 입력해주세요.”	채팅 인라인 메시지, DB 변경 없음
200 제안	NEEDS_INPUT / PENDING_COMMAND_EXPIRED	“이전 대화가 오래되어 처음부터 다시 확인할게요.”	현재 메시지를 새 요청으로 재해석
200	NEEDS_INPUT	누락된 제목·날짜·시간 질문	재질문 상태
200	REPHRASE_REQUIRED	지원 예시와 재표현 요청	DB 변경 없음
401	UNAUTHORIZED	다시 로그인 안내, JSON 오류 응답	로그인 이동, HTML 리다이렉트 금지
401	INVALID_CREDENTIALS	“이메일 또는 비밀번호를 확인해주세요.”	이메일 유지, 비밀번호 삭제, 로그인 폼 유지
403	FORBIDDEN	“요청을 처리할 권한이 없습니다.”	fieldErrors 빈 배열, 데이터 내용 미노출, HTML 리다이렉트 금지
404	SCHEDULE_NOT_FOUND	일정을 찾을 수 없음	목록 이동
404	CONFIRMATION_NOT_FOUND	만료됐거나 이미 처리된 요청	확인 UI 종료
404	CONFIRMATION_TARGET_GONE	대상 일정이 없어 처음부터 재시도 필요	대상 없음 UI
409	SCHEDULE_CONFLICT	겹치는 일정 경고	확인 다이얼로그
409	SCHEDULE_VERSION_CONFLICT	“일정이 다른 곳에서 변경되었습니다.”	입력 유지, 최신 상세 재조회 후 재시도
409	CONFLICT_ACKNOWLEDGEMENT_REQUIRED	최신 충돌 재확인 필요	같은 UI 갱신
409	CONFIRMATION_SUPERSEDED	정보 변경으로 최신 후보 재확인 필요	새 confirmationId로 갱신
409	중복·상태 변경	이미 처리됐거나 상태가 바뀜	성공으로 중복 표시 금지
409	DUPLICATE_EMAIL	이미 사용할 수 없는 이메일이라는 안전한 안내	회원가입 폼 유지
422	VALIDATION_ERROR	입력한 내용을 다시 확인해주세요.	fieldErrors를 필드와 연결하고 입력값 보존
429	RATE_LIMITED	잠시 후 재시도	실행 제한
503	AI_SERVICE_UNAVAILABLE	직접 캘린더 기능은 계속 사용 가능	AI 장애 배너
500	SERVER_ERROR	저장 성공으로 표시하지 않음	재시도
네트워크	연결 실패	연결 확인 안내	입력 보존, 임의 재실행 금지
오프라인	OFFLINE	완전한 오프라인 일정 기능 미지원	전역 배너

REST API 오류 JSON은 timestamp, status, code, message, fieldErrors를 사용한다. timestamp는 UTC ISO-8601 문자열이며 fieldErrors의 각 항목은 field, code, message로 구성한다. 필드 오류가 없으면 빈 배열을 사용한다. 스택 트레이스, SQL 메시지, 내부 클래스명과 입력 비밀번호는 반환하지 않는다.
GET /api/v1/users/me의 세션 없음·미인증·사용자 레코드 없음은 모두 401 UNAUTHORIZED와 “인증이 필요합니다.”로 일반화한다. HTML 리다이렉트와 Location 헤더를 사용하지 않으며 이메일·세션 ID·쿠키 값·인증 객체 정보를 노출하지 않는다. 예상하지 못한 오류는 500 SERVER_ERROR를 사용하고 내부 정보와 사용자 이메일을 반환하지 않는다.

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

로그인 자격 증명 실패는 같은 구조로 다음 JSON을 사용한다.

```json
{
  "timestamp": "2026-07-30T00:00:00Z",
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호를 확인해주세요.",
  "fieldErrors": []
}
```

CSRF 실패는 같은 구조로 HTTP 403, code FORBIDDEN, message “요청을 처리할 권한이 없습니다.”, 빈 fieldErrors를 사용한다.

카카오 채널에서는 동일한 의미의 상태를 카카오 스킬 규격의 정상 응답 JSON 내부 안내 문구로 변환한다.
9. 사용자 흐름 18개
9.1 첫 방문 → 회원가입 → 로그인 → 오늘 일정
시작 조건: 비로그인.
행동: 회원가입 제출 후 로그인 화면에서 다시 로그인.
시스템: email, password, passwordConfirmation만 제출한다. 이메일을 trim·소문자 정규화하고 BCrypt 해시와 기본 시간대 Asia/Seoul을 저장한다. 가입 시 사용자만 생성하고 자동 로그인이나 세션 생성을 하지 않는다. 로그인 성공 시에만 세션을 발급한다.
API: 회원가입, 로그인, 사용자 정보, 오늘 일정 조회.
DB 변경: 가입 성공 시 사용자 생성.
실패: 422 VALIDATION_ERROR, 409 DUPLICATE_EMAIL, 429 RATE_LIMITED, 로그인 공통 실패.
완료: 홈에 오늘 일정 최대 3건 표시.

로그인 요청은 email과 password만 사용한다. 성공 시 200과 email·timezone을 받고 CALTALK_SESSION 서버 세션을 생성한다. 안전한 내부 상대 복귀 경로가 있으면 해당 경로로 이동하고 없으면 홈으로 이동한다. 실패 시 401 INVALID_CREDENTIALS로 원인을 일반화하고 이메일은 유지하며 비밀번호는 삭제한다.
9.2 월간 캘린더 → 날짜 선택 → 일정 등록
날짜 선택 후 목록으로 스크롤하고 등록 화면에 날짜를 초기화한다.
저장 시 충돌이 없으면 즉시 생성하고, 있으면 confirmation을 발급한다.
실패: 422, 401, 409, 네트워크 오류.
완료: 일정 상세에 저장값 표시.
9.3 일정 등록 — 충돌 없음
title, startAt, endAt, location만 제출하며 화면의 날짜·시간을 UTC 오프셋이 포함된 시각으로 구성한다.
서버가 소유권과 시간 규칙을 검증하고 충돌이 없으면 생성과 이력을 한 트랜잭션으로 반영한다.
201, Cache-Control no-store, Location과 일정 상세 응답을 받으면 입력 화면을 닫고 성공 안내 후 선택 날짜·오늘 일정·월간 캘린더를 다시 조회한다.
완료: 현재 사용자 소유의 단일 일정이 생성되고 UTC 응답이 사용자 시간대로 표시된다.
9.4 일정 등록 — 충돌 있음
서버는 최초 요청에서 DB를 변경하지 않고 confirmationId와 충돌 목록을 반환한다.
화면은 입력값을 유지하고 DLG-CONFLICT-001을 표시한다. 취소하면 생성하지 않으며 “그대로 저장”은 POST /api/v1/confirmations/{confirmationId}/approve를 호출한다.
서버가 충돌과 confirmation의 소유권·만료·소비 상태를 재검증하고 유효한 승인 한 번에만 생성한다.
실패: 만료, superseded, 중복 승인.
완료: 유효한 승인 한 번에만 생성된다.
9.5 일정 상세 → 수정
기존 값을 불러와 수정한다.
충돌이 없으면 즉시 수정하고, 있으면 확인 절차로 전환한다.
승인 대기 중 값이 바뀌면 최신 후보를 자동 발급한다.
대상 삭제 시 재입력을 안내한다.
완료: 한 번만 수정되고 변경 이력이 기록된다.
9.6 일정 상세 → 삭제
삭제 확인에서 비가역성을 고지한다.
사용자가 확인한 DELETE 요청에서 본인 소유와 version을 재검증하고 즉시 하드 삭제한다.
관련 변경 이력은 CASCADE 정책에 따라 삭제된다.
실패 시 성공 화면을 표시하지 않는다.
9.7 자연어 일정 조회
메시지를 구조화하고 사용자 시간대의 특정 하루를 조회한다.
본인 일정만 시간순으로 반환한다.
날짜가 모호하면 재질문한다.
DB 변경 없음.
9.8 자연어 일정 생성
필수값을 수집하고 종료가 없으면 시작+1시간을 제안한다.
과거 시각이면 전용 정책 거부.
충돌과 절대 시각을 확인 카드에 표시한다.
승인 전 DB 변경 없음.
승인 성공 시 한 번만 생성.
9.9 자연어 일정 수정
본인 일정에서 후보를 찾는다.
0건 재질문, 1건 바로 확인, 2~5건 선택, 6건 이상 조건 축소.
언급하지 않은 값은 유지하고 시작 시간만 바꾸면 기존 지속시간을 유지한다.
승인 시 대상·버전·충돌을 재검증한다.
9.10 확인 대기 중 데이터 변경
승인 시 대상 버전 또는 충돌 상태가 달라졌으면 기존 confirmation을 SUPERSEDED로 바꾼다.
서버가 최신 후보를 자동 계산하고 새 confirmation을 반환한다.
사용자는 전체 요청을 다시 입력하지 않고 최신 후보만 재확인한다.
9.11 확인 대기 중 대상 삭제
서버는 기존 확인을 실행하지 않는다.
CONFIRMATION_TARGET_GONE을 반환한다.
사용자는 처음부터 새 요청을 시작한다.
DB 추가 변경 없음.
9.12 AI 장애 → 직접 입력
OpenAI 요청 실패 또는 타임아웃 시 웹은 503 상태를 표시한다.
자연어 후보와 일정 DB를 변경하지 않는다.
사용자는 캘린더나 직접 등록 화면으로 이동할 수 있다.
9.13 카카오 연결 코드 발급 → 연결 상태 확인
로그인 사용자가 1회용 코드를 발급받아 카카오톡에 입력한다.
서버가 만료·사용 여부·실패 횟수·연결 충돌을 검증한다.
사용자가 PWA의 “연결 상태 확인”을 눌러 수동 갱신한다.
완료: connected=true, linkedAt 표시.
9.14 카카오 연결 해제
사용자가 해제를 확인한다.
연결과 관련 대기 명령·확인 요청을 무효화한다.
이후 해당 카카오 식별값으로 일정에 접근할 수 없다.
PWA 계정과 일정 자체는 삭제하지 않는다.
9.15 세션 만료 → 재로그인
12시간 비활동 후 보호 요청에서 401을 받는다.
로그인 화면으로 이동하고 안전한 복귀 경로를 보존한다.
로그인 성공 후 원래 화면으로 복귀한다.
실패한 변경을 자동 재전송하지 않는다.
9.16 앱 초기화·새로고침 → 인증 상태 복원
앱 진입 시 GET /api/v1/users/me를 호출한다. 200이면 email·timezone·createdAt을 사용자 상태에 저장하고 기존 화면을 계속 표시한다.
401이면 사용자 상태를 비로그인으로 초기화한다. 공개 화면은 시작 화면을 유지하고 보호 화면은 시작 화면으로 이동하며 로그인 화면으로 강제 이동하거나 일반 서버 오류를 표시하지 않는다.
응답은 Cache-Control: no-store로 캐시하지 않는다.

9.17 로그아웃 → 시작 화면
인증 사용자가 유효한 CSRF 토큰과 함께 POST /api/v1/auth/logout을 요청한다.
서버는 현재 세션과 SecurityContext를 종료하고 CALTALK_SESSION을 삭제한 뒤 본문과 리다이렉트 없는 204를 반환한다.
클라이언트는 인증 상태와 사용자 캐시를 초기화하고 시작 화면으로 이동하며, 로그인 화면으로 직접 이동하지 않는다. 뒤로가기로 보호 화면이 재노출되지 않도록 처리한다.
이미 세션이 없어도 유효한 CSRF 토큰이 있으면 같은 204를 반환한다. CSRF 토큰 누락·불일치는 403 FORBIDDEN JSON으로 처리하고 성공 화면 이동을 하지 않는다.

9.18 회원 탈퇴
사용자가 삭제 범위 체크박스를 확인한다.
서버가 계정 및 관련 데이터를 한 트랜잭션으로 삭제한다.
성공 시 세션을 종료하고 시작 화면으로 이동한다.
서버 오류 시 계정과 데이터는 유지되고 오류만 표시한다.
10. 수용 기준
Given 비로그인 사용자, When 유효한 회원가입을 제출하면, Then 201과 정규화된 email·Asia/Seoul timezone·UTC createdAt을 받고 자동 로그인 없이 로그인 화면으로 이동한다.
Given 가입 직후, When 로그인 화면에 진입하면, Then 가입 완료 안내가 표시된다.
Given 잘못된 이메일·비밀번호·확인값, When 회원가입을 제출하면, Then 422 VALIDATION_ERROR와 필드별 오류를 받는다.
Given 정규화 후 같은 이메일이 이미 존재, When 회원가입을 제출하면, Then 409 DUPLICATE_EMAIL을 받고 사용자가 추가되지 않는다.
Given 회원가입 성공 또는 실패, When 응답을 확인하면, Then password·passwordConfirmation·passwordHash·token·secret이 포함되지 않는다.
Given 유효한 로그인 요청, When 인증에 성공하면, Then 200과 email·timezone을 받고 CALTALK_SESSION 세션을 생성한다.
Given 계정 부재·비밀번호 오류·계정 잠금, When 로그인하면, Then 모두 401 INVALID_CREDENTIALS와 동일한 메시지를 받는다.
Given 로그인 입력 형식 오류, When 제출하면, Then 422 VALIDATION_ERROR와 필드별 오류를 받는다.
Given 이미 로그인한 사용자, When 새 인증 정보로 로그인하면, Then 기존 세션 ID를 교체하고 200을 받는다.
Given 인증되지 않은 사용자, When 보호 API를 호출하면, Then HTML 리다이렉트 없이 401 UNAUTHORIZED JSON을 받는다.
Given 권한이 부족한 사용자, When 보호 API를 호출하면, Then 403 FORBIDDEN JSON을 받는다.
Given 로그인 성공 후 안전한 내부 상대 복귀 경로, When 이동하면, Then 해당 경로를 우선 사용하고 외부·프로토콜 상대 URL은 거부한다.
Given 로그인 세션, When 앱 초기 진입 또는 새로고침에서 GET /api/v1/users/me를 호출하면, Then 200과 email·timezone·UTC createdAt을 받고 Cache-Control no-store로 사용자 상태를 복원한다.
Given 세션이 없는 공개 화면, When GET /api/v1/users/me가 401 UNAUTHORIZED를 반환하면, Then 일반 오류 없이 비로그인 상태로 초기화하고 시작 화면을 유지한다.
Given 세션이 없는 보호 화면, When GET /api/v1/users/me가 401 UNAUTHORIZED를 반환하면, Then 로그인 화면으로 강제 이동하지 않고 시작 화면으로 이동한다.
Given 인증 principal의 사용자가 DB에 없음, When GET /api/v1/users/me를 호출하면, Then 세션·SecurityContext·CALTALK_SESSION을 정리하고 사용자 존재 여부 노출 없이 401 UNAUTHORIZED JSON을 받는다.
Given 인증된 사용자와 유효한 CSRF 토큰, When 로그아웃하면, Then 현재 세션과 SecurityContext가 종료되고 CALTALK_SESSION이 삭제되며 빈 본문의 204를 받는다.
Given 로그아웃 성공, When 클라이언트가 응답을 처리하면, Then 인증 상태와 사용자 캐시를 초기화하고 로그인 화면이 아닌 시작 화면으로 이동하며 뒤로가기로 보호 화면을 재노출하지 않는다.
Given 로그아웃된 기존 세션 쿠키, When 보호 API를 호출하면, Then HTML 리다이렉트 없이 401 UNAUTHORIZED JSON을 받는다.
Given 세션이 없는 사용자와 유효한 CSRF 토큰, When 로그아웃을 다시 요청하면, Then 로그인 상태를 노출하지 않고 빈 본문의 204를 받는다.
Given CSRF 토큰이 없거나 일치하지 않는 로그아웃 요청, When 서버가 처리하면, Then 성공으로 간주하지 않고 HTML 리다이렉트 없이 403 FORBIDDEN 공통 JSON을 받는다.
Given 오늘 일정이 4건 이상, When 홈을 열면, Then 3건과 전체 보기 액션만 표시된다.
Given 월간 캘린더, When 스크롤하면, Then 상단 월 이동 바만 고정되고 그리드는 스크롤된다.
Given 날짜 선택, When 선택이 완료되면, Then 해당 날짜 목록으로 이동한다.
Given 직접 등록에서 종료를 입력하지 않은 초기 상태, When 시작을 정하면, Then 종료는 시작+1시간으로 채워진다.
Given 토글 OFF에서 종료가 시작 이하, When 저장하면, Then 저장이 차단된다.
Given 과거 절대 날짜를 PWA에서 선택, When 저장하면, Then 명확한 절대 시각 확인 후 허용된다.
Given 충돌 없는 직접 저장, When 저장하면, Then 즉시 한 번 반영된다.
Given 충돌 있는 직접 저장, When 최초 요청하면, Then DB 변경 없이 confirmation이 반환된다.
Given 인증된 사용자와 유효한 CSRF 토큰, When 유효한 title·startAt·endAt·location으로 충돌 없는 일정을 생성하면, Then 201과 Cache-Control no-store·일정 Location·상세 응답을 받는다.
Given 제목과 장소에 앞뒤 공백이 있음, When 생성하면, Then trim된 제목·장소가 저장되고 빈 장소는 null로 저장된다.
Given offset date-time 입력, When 생성하면, Then 오프셋을 반영한 UTC Instant가 저장되고 응답은 Z 표기를 사용한다.
Given 제목 누락·공백·200자 초과 또는 장소 200자 초과, When 생성하면, Then 422 VALIDATION_ERROR와 해당 필드의 REQUIRED 또는 MAX_LENGTH를 받는다.
Given 종료가 시작과 같거나 빠름, When 생성하면, Then 422와 endAt의 INVALID_TIME_RANGE를 받고 DB는 변경되지 않는다.
Given description·allDay·userId 등 계약 외 필드, When 생성 요청에 포함하면, Then 422로 거부되고 소유자와 일정 데이터는 변경되지 않는다.
Given 같은 사용자 일정과 시간 범위가 겹침, When 최초 생성하면, Then 409 SCHEDULE_CONFLICT와 숫자 confirmationId·정렬된 conflicts를 받고 DB는 변경되지 않는다.
Given SCHEDULE_CONFLICT 응답, When conflicts를 확인하면, Then id·title·startAt·endAt·location만 있고 다른 사용자 일정·소유자·version·내부 해시는 없다.
Given 기존 일정 종료와 새 일정 시작이 맞닿음 또는 다른 사용자의 일정만 겹침, When 생성하면, Then 충돌로 판정하지 않는다.
Given 유효한 본인 confirmationId, When 별도 승인 API에 conflictAcknowledged true로 그대로 저장을 승인하면, Then 기존 공통 확인 검증 뒤 일정이 한 번만 생성된다.
Given PENDING confirmation 생성 후 5분 경과 또는 CONSUMED·CANCELLED 상태, When 승인하면, Then CONFIRMATION_NOT_FOUND로 종료되고 DB는 변경되지 않는다.
Given 승인 시 최신 충돌 목록이 변경됨, When 서버가 재검증하면, Then 기존 confirmation을 SUPERSEDED로 전환하고 새 confirmationId와 최신 후보·충돌 목록을 반환한다.
Given 다른 사용자의 confirmationId, When 승인하면, Then 403 FORBIDDEN을 받고 일정·이력·confirmation 상태는 변경되지 않는다.
Given 일정 생성 성공, When 클라이언트가 처리하면, Then 입력 화면을 닫고 성공 안내 후 선택 날짜·오늘 일정·월간 캘린더를 재조회해 사용자 시간대로 표시한다.
Given 동일 승인을 중복 실행, When 서버가 처리하면, Then DB 변경은 한 번만 발생한다.
Given 자연어 필수값 누락, When 분석되면, Then 재질문하고 DB는 변경되지 않는다.
Given 재질문 후 10분 경과, When 새 메시지를 보내면, Then 이전 후보를 폐기하고 새 요청으로 재해석한다.
Given 자연어 과거 생성 요청, When 분석되면, Then PAST_DATETIME_REJECTED 안내 후 DB를 변경하지 않는다.
Given 수정 후보 0건, When 응답하면, Then 조건 재질문이 표시된다.
Given 후보 1건, When 응답하면, Then 선택 없이 확인으로 진행한다.
Given 후보 2~5건, When 응답하면, Then 본인 일정 후보 선택 UI가 표시된다.
Given 후보 6건 이상, When 응답하면, Then 목록 대신 조건 축소 재질문이 표시된다.
Given 확인 후 5분 경과, When 승인하면, Then 만료 처리되고 DB는 변경되지 않는다.
Given 승인 대기 중 대상 변경, When 승인하면, Then 최신 후보가 자동 발급된다.
Given 승인 대기 중 대상 삭제, When 승인하면, Then 대상 없음 안내 후 재입력을 요구한다.
Given AI 장애, When 메시지를 보내면, Then 503 안내와 직접 입력 이동을 제공한다.
Given 카카오 코드 입력 완료, When 상태 확인을 누르면, Then 수동 조회 결과로만 연결 표시가 갱신된다.
Given 시간대 변경, When 저장하면, Then UTC 값은 유지되고 사용자 및 일정 캐시가 새 기준으로 갱신된다.
Given 탈퇴 체크 미선택, When 화면을 보면, Then 삭제 버튼은 비활성화된다.
Given 탈퇴 처리 오류, When 응답하면, Then 계정과 체크 상태가 유지된다.
Given 존재하지 않거나 다른 사용자 소유인 일정 URL, When 접근하면, Then 두 경우를 구분하지 않는 404 SCHEDULE_NOT_FOUND 응답을 받는다.
최종 수용 기준은 61개다.
11. 화면별 API 연결
11.1 확정 API
POST /api/v1/auth/signup
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET /api/v1/users/me
PATCH /api/v1/users/me
GET /api/v1/schedules
GET /api/v1/schedules/{scheduleId}
POST /api/v1/schedules
PATCH /api/v1/schedules/{scheduleId}
DELETE /api/v1/schedules/{scheduleId}
POST /api/v1/chat/messages
POST /api/v1/confirmations/{confirmationId}/approve
POST /api/v1/confirmations/{confirmationId}/cancel
POST /api/v1/kakao/link-codes
POST /api/v1/kakao/links/revoke
11.2 화면·API 명세 제안
DELETE /api/v1/users/me
GET /api/v1/kakao/link
중복 제거 후 총 17종(확정 15종, 화면·API 명세 제안 2종)이다.
화면·UI	액션	API	캐시 무효화 또는 재조회	일정 DB 변경	결과
SCR-AUTH-001	가입	POST /api/v1/auth/signup	—	없음	users에 정규화 email·BCrypt password_hash·Asia/Seoul timezone·UTC created_at 저장, 201 응답 후 로그인 화면 이동
SCR-AUTH-002	로그인	POST /api/v1/auth/login	GET /api/v1/users/me 사용자 정보 재조회	없음	200 응답과 CALTALK_SESSION 생성 후 안전한 복귀 경로 또는 홈 이동
SCR-HOME-001	오늘 일정	GET /api/v1/schedules	—	—	—
SCR-CAL-001/002	월·일 일정	GET /api/v1/schedules	—	—	—
SCR-SCHED-001	상세	GET /api/v1/schedules/{scheduleId}	—	없음	200과 Cache-Control no-store, 본인 일정 상세 또는 존재 여부를 숨긴 404 SCHEDULE_NOT_FOUND
SCR-SCHED-001	삭제	DELETE /api/v1/schedules/{scheduleId}?version={version}	성공 시 열린 상세·홈 오늘 일정·월간 캘린더·선택 날짜 목록 무효화 또는 재조회	현재 사용자 일정 하드 삭제, history는 ON DELETE CASCADE	204 No Content와 Cache-Control no-store, 상세 닫기 후 안전한 목록 이동
SCR-SCHED-002	등록	POST /api/v1/schedules	성공 시 선택 날짜 일정 목록, 홈의 오늘 일정, 월간 캘린더를 무효화하거나 재조회	충돌 없을 때 일정과 CREATE 이력을 한 트랜잭션으로 생성	201, Cache-Control no-store, Location과 일정 상세를 받고 입력 화면 닫기·성공 안내·사용자 시간대 표시
SCR-SCHED-003	수정	PATCH /api/v1/schedules/{scheduleId}	성공 시 상세 응답 반영, 수정 전후 범위의 홈 오늘 일정·월간 캘린더·선택 날짜 목록 무효화 또는 재조회	충돌 없으면 일정과 UPDATE 이력을 한 트랜잭션으로 수정	200과 Cache-Control no-store 또는 충돌 confirmation
SCR-CHAT-001	메시지	POST /api/v1/chat/messages	—	—	—
DLG-CHAT-TARGET-001	수정 대상 후보 선택	POST /api/v1/chat/messages 재사용	해당 없음	없음	자연어 수정 최종 확인 카드로 전환
DLG-CONFLICT-001	승인	POST /api/v1/confirmations/{confirmationId}/approve	—	—	—
DLG-CONFLICT-001	취소·다른 시간	POST /api/v1/confirmations/{confirmationId}/cancel	—	—	—
DLG-CHAT-CONFIRM-*	승인	POST /api/v1/confirmations/{confirmationId}/approve	—	—	—
DLG-CHAT-CONFIRM-*	취소	POST /api/v1/confirmations/{confirmationId}/cancel	—	—	—
SCR-KAKAO-001	코드 발급	POST /api/v1/kakao/link-codes	—	—	—
SCR-KAKAO-001	상태 확인	GET /api/v1/kakao/link 제안	—	—	—
SCR-KAKAO-001	연결 해제	POST /api/v1/kakao/links/revoke	—	—	—
앱 공통	인증 상태 복원	GET /api/v1/users/me	응답을 HTTP 캐시에 저장하지 않고 사용자 메모리 상태 갱신	없음	200이면 email·timezone·createdAt 복원, 401이면 공개 화면 유지 또는 보호 화면에서 시작 화면 이동
SCR-SET-001	사용자 정보	GET /api/v1/users/me	Cache-Control no-store	없음	email·timezone·createdAt만 반환
SCR-SET-001	로그아웃	POST /api/v1/auth/logout	인증 상태와 사용자 캐시 초기화	없음	유효한 CSRF 토큰이면 인증 여부와 무관하게 현재 세션·SecurityContext 종료와 CALTALK_SESSION 삭제 후 204, 시작 화면 이동
SCR-SET-002	시간대 저장	PATCH /api/v1/users/me	GET /api/v1/users/me 사용자 정보, 홈 화면의 오늘 일정, 월간 캘린더 일정, 선택 날짜 일정 목록, 현재 열려 있는 일정 상세의 표시값을 무효화하거나 다시 조회하고 사용자 시간대 기준으로 날짜·요일·시작 시간·종료 시간·오늘 및 선택 날짜 기준을 다시 계산	없음	200과 email·timezone·createdAt, Cache-Control no-store를 받고 기존 일정의 UTC 절대 저장값을 유지한 채 새 시간대 기준으로 표시 갱신
SCR-SET-003	탈퇴	DELETE /api/v1/users/me 제안	—	—	—

11.3 일정 조회 계약
기간별 일정 조회는 `GET /api/v1/schedules?from={from}&to={to}` 하나를 홈의 오늘 일정, 월간 캘린더, 선택 날짜 일정 목록에서 공통 사용한다. from과 to는 오프셋을 포함한 필수 ISO-8601 date-time이며 시작 포함·종료 미포함인 `[from, to)`를 뜻한다. from은 to보다 빨라야 하고 서버는 두 값을 UTC Instant로 변환한다. 오프셋 없는 시각, 누락·형식 오류, from >= to는 HTTP 422 + VALIDATION_ERROR와 fieldErrors로 처리하며 관계 오류의 field는 to, reason은 INVALID_TIME_RANGE로 통일한다. 임의의 페이지네이션이나 최대 조회 일수는 추가하지 않고 조회 범위 제한은 후속 결정으로 보류한다.

서버는 현재 인증 사용자의 일정에 `startAt < to AND endAt > from` 겹침 조건을 적용한다. 요청 시작 경계에 정확히 끝나는 일정은 제외하고 요청 시작 경계에 정확히 시작하는 일정은 포함한다. 요청 종료 경계에 정확히 시작하는 일정은 제외하고 요청 종료 경계에 정확히 끝나는 일정은 포함한다. 여러 날짜에 걸친 일정도 겹침 조건을 만족하면 포함한다. 결과는 startAt, endAt, id 오름차순으로 정렬한다. userId·ownerUserId·email로 조회 대상을 지정할 수 없다.

성공은 HTTP 200, `Cache-Control: no-store`와 `{ "items": [...] }`를 반환한다. 빈 결과는 `{ "items": [] }`이며 404를 사용하지 않는다. 목록 항목은 id·title·startAt·endAt·location·version만 포함하는 별도 DTO이고, createdAt·updatedAt은 상세에만 포함한다. 시각은 UTC ISO-8601 Z 문자열이며 소유자·사용자 이메일·내부 해시·confirmation·변경 이력은 노출하지 않는다.

일정 상세는 `GET /api/v1/schedules/{scheduleId}`를 사용하고 HTTP 200, `Cache-Control: no-store`와 id·title·startAt·endAt·location·createdAt·updatedAt·version을 반환한다. scheduleId 형식 오류는 HTTP 422 + VALIDATION_ERROR다. 일정이 없거나 다른 사용자 소유이면 존재 여부를 구분하지 않고 동일한 HTTP 404 + SCHEDULE_NOT_FOUND를 반환한다. 두 GET API는 세션 인증이 필수이고 공개 경로가 아니지만 CSRF 토큰은 요구하지 않는다.

홈은 사용자 시간대의 오늘 시작·다음 날 시작, 월간 캘린더는 표시 월 시작·다음 달 시작, 선택 날짜 목록은 선택일 시작·다음 날 시작을 offset date-time의 from·to로 계산한다. 일정 상세는 일정 ID로 조회한다. 시간대 변경 후 사용자 정보와 이 네 일정 화면 데이터를 무효화하거나 재조회하고 날짜·요일·시각·오늘 여부·선택 날짜 포함 여부를 새 시간대로 다시 계산하되 DB의 UTC 절대 시각은 변경하지 않는다.

11.4 일정 수정·삭제 계약
일정 수정은 `PATCH /api/v1/schedules/{scheduleId}`의 부분 수정이다. 요청은 title·startAt·endAt·location·version만 허용하고 version은 필수인 0 이상의 정수이며, 나머지 네 필드 중 하나 이상을 전달한다. 전달하지 않은 필드는 기존 값을 유지한다. title·startAt·endAt은 전달 시 null을 허용하지 않고 title은 trim 후 필수·최대 200자, 시각은 offset date-time이다. location 미전달은 유지, null 또는 trim 후 빈 문자열은 삭제, 그 외 문자열은 trim 후 설정한다. 최종 endAt은 startAt보다 늦어야 하고 계약 외 필드와 검증 오류는 422 VALIDATION_ERROR다.

현재 사용자 일정만 scheduleId와 소유자 조건으로 조회한다. 없음·타 사용자 소유는 동일한 404 SCHEDULE_NOT_FOUND다. 요청 version 불일치는 409 SCHEDULE_VERSION_CONFLICT와 빈 fieldErrors만 반환하며 최신 일정은 포함하지 않는다. 입력은 유지하고 상세 재조회를 안내하며 자동 덮어쓰지 않는다. version이 일치하지만 정규화된 값이 모두 같으면 200 멱등 성공으로 처리하고 updatedAt·version·이력을 변경하지 않는다.

실제 변경과 UPDATE 전체 before/after 스냅샷은 한 트랜잭션으로 저장한다. 성공은 증가한 version과 갱신된 UTC updatedAt을 포함한 ScheduleResponse, HTTP 200과 `Cache-Control: no-store`다. 충돌 검사는 자신을 제외한 현재 사용자 일정에 `startAt < candidateEndAt AND endAt > candidateStartAt`을 적용하고 startAt·id 순으로 정렬한다. 충돌 시 DB를 변경하지 않고 409 SCHEDULE_CONFLICT와 confirmationId·conflicts를 반환한다.

수정 confirmation은 기존 UPDATE_EVENT 델타 모델을 사용한다. target_schedule_id와 검증한 target_schedule_version, 변경된 title·start_at·end_at만 저장하고 location_action KEEP·SET·REMOVE를 구분하며 SET일 때만 location_value를 저장한다. PENDING·5분 만료·후보 지문·충돌 해시를 유지한다. 승인 중 대상 version이나 충돌 목록이 바뀌면 직접 수정의 버전 오류 대신 기존 CONFIRMATION_SUPERSEDED와 최신 confirmation을 사용하고, 대상이 사라지면 CONFIRMATION_TARGET_GONE을 사용한다. 유효한 승인만 일정·UPDATE 이력·CONSUMED를 한 트랜잭션으로 반영한다.

일정 삭제는 `DELETE /api/v1/schedules/{scheduleId}?version={version}`을 사용한다. version은 필수인 0 이상의 정수이며 DELETE 본문과 If-Match는 사용하지 않는다. 직접 PWA 삭제는 DLG-DELETE-001에서 비가역성을 확인한 뒤 호출하고 서버 confirmation은 만들지 않는다. 없음·타 사용자 소유는 404 SCHEDULE_NOT_FOUND, version 불일치는 409 SCHEDULE_VERSION_CONFLICT다.

삭제 성공은 HTTP 204 No Content, 빈 본문과 `Cache-Control: no-store`이며 새 DELETE 이력을 만들지 않고 즉시 하드 삭제한다. 기존 CREATE·UPDATE 이력은 schedule_change_history의 schedule_id ON DELETE CASCADE에 따라 같은 트랜잭션에서 함께 삭제된다. change_type DELETE는 현재 MVP에서 사용하지 않고 별도 감사 로그와 soft delete는 추가하지 않는다. 향후 영구 삭제 감사가 필요하면 별도 감사 로그 구조를 검토한다. 재삭제는 404다. PATCH·DELETE는 세션 인증과 CSRF 토큰이 필수이며 미인증은 401, CSRF 실패는 403이다.

수정 성공 뒤 상세 응답을 반영하고 전후 날짜 범위의 홈 오늘 일정·월간 캘린더·선택 날짜 목록을 무효화하거나 재조회한다. 수정 충돌은 기존 다이얼로그에서 사용자 시간대로 표시하고 승인·다른 시간·취소를 제공한다. 삭제 성공 뒤 상세를 닫고 안전한 목록으로 이동하며 상세와 세 일정 목록 캐시를 무효화한다. 테스트는 부분 수정, location 세 상태, 무변경, version, 충돌·confirmation·SUPERSEDED, UPDATE 이력, 삭제 204·CASCADE·재삭제, 소유권·CSRF를 포함한다.

12. 테스트 체크리스트

정상 회원가입·로그인

로그인 후 GET /api/v1/users/me HTTP 200

현재 사용자 email 반환

현재 사용자 timezone 반환

현재 사용자 createdAt 반환

현재 사용자 Cache-Control no-store

현재 사용자 응답에 password 없음

현재 사용자 응답에 passwordHash 없음

현재 사용자 응답에 token 없음

현재 사용자 응답에 sessionId 없음

현재 사용자 응답에 roles 없음

현재 사용자 응답에 authorities 없음

세션 없는 현재 사용자 조회 HTTP 401

현재 사용자 미인증 오류 code UNAUTHORIZED

현재 사용자 미인증 오류 JSON Content-Type

현재 사용자 미인증 응답 HTML 리다이렉트 없음

현재 사용자 미인증 응답 Location 헤더 없음

로그인 후 로그아웃과 기존 쿠키의 현재 사용자 조회 HTTP 401

DB 사용자가 없는 인증 세션의 현재 사용자 조회 HTTP 401

DB 사용자가 없는 인증 세션 무효화

DB 사용자가 없는 인증 세션 SecurityContext 제거

DB 사용자가 없는 인증 세션 CALTALK_SESSION 삭제

현재 사용자 조회에서 사용자 존재 여부 비노출

인증된 사용자 로그아웃 HTTP 204

로그아웃 응답 본문 없음

로그아웃 시 현재 세션 무효화

로그아웃 시 SecurityContext 제거

CALTALK_SESSION 빈 값·Path=/·Max-Age=0·HttpOnly·SameSite=Lax·환경별 Secure·Domain 미지정 삭제

로그아웃 후 시작 화면 이동과 뒤로가기 보호 화면 재노출 방지

로그아웃 후 기존 세션 쿠키의 보호 API HTTP 401 UNAUTHORIZED JSON

유효한 CSRF 토큰을 포함한 미인증 재로그아웃 HTTP 204

로그아웃 CSRF 토큰 누락 HTTP 403 FORBIDDEN JSON

로그아웃 CSRF 토큰 불일치 HTTP 403 FORBIDDEN JSON

로그아웃 응답과 오류 응답의 HTML 리다이렉트 없음

로그아웃 응답의 세션 ID·쿠키 값·CSRF 토큰 노출 없음

회원가입 후 자동 로그인 없음

로그인 요청 email·password와 200 email·timezone 응답

로그인 이메일 trim·소문자 정규화·형식·254자 제한

로그인 비밀번호 8~64자 제한

계정 부재·비밀번호 오류·잠금의 401 INVALID_CREDENTIALS 응답 통일

CALTALK_SESSION과 HttpOnly·SameSite=Lax·Path=/·환경별 Secure

세션 12시간 유휴 만료와 기존 세션 ID 교체

계정 기준 15분·5회와 성공 후 초기화

IP 기준 15분·20회 및 429 RATE_LIMITED·Retry-After

보호 API의 JSON 401 UNAUTHORIZED·403 FORBIDDEN과 HTML 리다이렉트 부재

안전한 내부 상대 복귀 경로 검증과 외부 URL 차단

회원가입 요청 필드는 email, password, passwordConfirmation만 사용

이메일 trim·소문자 정규화와 254자 제한

비밀번호 8~64자, 공백 전용 거부와 확인값 일치

회원가입 201 응답의 email·timezone·createdAt과 민감 필드 부재

중복 이메일 409 DUPLICATE_EMAIL

회원가입 요청 제한 429 RATE_LIMITED

회원가입 서버 오류 500 SERVER_ERROR

로그인 실패·잠금 문구 통일

로딩과 중복 클릭

빈 일정

422 필드 오류

401 세션 만료와 복귀

403 다른 사용자 일정 접근

404 일정 없음

409 일정 충돌

409 confirmation 상태 변경

429 요청 제한

503 AI 장애

네트워크 지연·단절

오프라인 안내

360px 모바일

키보드와 포커스 이동

제목·장소 200자 경계

종료=시작

다음 날 종료

자정 경계

PWA 과거 일정 허용

자연어 과거 일정 거부

재질문 10분 만료

확인 5분 만료

동일 확인 중복 승인

CONFIRMATION_SUPERSEDED

CONFIRMATION_TARGET_GONE

수정 후보 0건

수정 후보 1건

수정 후보 2~5건

수정 후보 6건 이상

시간대 변경 후 모든 관련 캐시 갱신

기존 일정 UTC 값 불변

인증 후 PATCH /api/v1/users/me HTTP 200

시간대 변경 요청 필드는 timezone 하나

유효한 지역 기반 IANA 시간대 저장

timezone 앞뒤 공백 제거

시간대 변경 응답의 email·timezone·createdAt

시간대 변경 응답 Cache-Control no-store

동일 시간대 재요청 HTTP 200

빈 timezone HTTP 422

공백 timezone HTTP 422

존재하지 않는 Zone ID HTTP 422

KST HTTP 422

GMT+9 HTTP 422

UTC+09:00 HTTP 422

시간대 오류 code VALIDATION_ERROR

시간대 fieldErrors의 timezone·INVALID_TIMEZONE

시간대 변경 미인증 HTTP 401 UNAUTHORIZED

시간대 변경 CSRF 누락 HTTP 403 FORBIDDEN

시간대 변경 CSRF 불일치 HTTP 403 FORBIDDEN

시간대 응답에 민감 정보 없음

다른 사용자 시간대 변경 불가

시간대 변경 뒤 email·createdAt 불변

시간대 변경 뒤 users.timezone 외 사용자 컬럼 불변

시간대 변경 뒤 DB 스키마 불변

시간대 변경 실패 시 기존 시간대·일정 캐시 유지

동시 시간대 변경의 마지막 정상 처리 값 반영

인증 후 POST /api/v1/schedules HTTP 201

일정 생성 요청 필드 title·startAt·endAt·location

일정과 CREATE 변경 이력 DB 저장

현재 사용자 owner_user_id 저장

입력 오프셋의 UTC Instant 변환 저장

일정 응답 시각 UTC Z 표기

일정 생성 Location 헤더

일정 생성 Cache-Control no-store

location 누락·빈 값의 null 저장

title·location trim 저장

일정 생성 응답 version

제목 누락 HTTP 422 REQUIRED

제목 공백 HTTP 422 REQUIRED

제목 200자 초과 HTTP 422 MAX_LENGTH

장소 200자 초과 HTTP 422 MAX_LENGTH

startAt 누락 HTTP 422

endAt 누락 HTTP 422

오프셋 없는 일정 시각 HTTP 422

종료와 시작 동일 HTTP 422 INVALID_TIME_RANGE

종료가 시작보다 빠름 HTTP 422 INVALID_TIME_RANGE

description·allDay·userId 등 계약 외 필드 HTTP 422

같은 사용자 겹침 일정 HTTP 409 SCHEDULE_CONFLICT

종료·시작 경계가 맞닿는 일정은 충돌 아님

confirmation 승인 후 겹침 일정 생성

SCHEDULE_CONFLICT confirmationId 숫자

conflicts의 startAt·id 정렬

conflicts 항목의 id·title·startAt·endAt·location

conflicts에서 다른 사용자·소유자·version·내부 해시 미노출

별도 승인 API와 conflictAcknowledged true

confirmation 5분 만료

confirmation 상태 PENDING·CONSUMED·SUPERSEDED·EXPIRED·CANCELLED

confirmation user_id 소유권

candidate_fingerprint 검증

conflict_snapshot_hash 재계산

target_schedule_version 검증

CONSUMED·CANCELLED 승인 방지

SUPERSEDED 자동 후보 발급

confirmation 물리 타입·CHECK·부분 유니크 제약

confirmation user_id/status·expires_at·target_schedule_id 인덱스

history before/after 컬럼 유지

history source_channel·change_type CHECK

history schedule_id FK ON DELETE CASCADE

history changed_by_user_id FK NO ACTION

schedules owner_user_id FK NO ACTION

다른 사용자 일정은 충돌 대상 아님

confirmation 위조·타 사용자 사용 방지

일정 생성 미인증 HTTP 401 UNAUTHORIZED

일정 생성 CSRF 누락 HTTP 403 FORBIDDEN

일정 생성 CSRF 불일치 HTTP 403 FORBIDDEN

userId·ownerUserId로 일정 소유자 변경 불가

일정 응답에 소유자·인증·민감정보 없음

동일 일반 요청 반복의 별도 생성과 충돌 확인 적용

카카오 미연결

연결 코드 만료

연결 코드 재사용

연결 코드 시도 초과

연결 코드 발급 제한

수동 연결 상태 확인

자동 폴링·push 없음

연결 해제 후 접근 차단

개인정보 안내 비로그인 접근

탈퇴 체크박스

탈퇴 서버 오류 시 데이터 유지

실제 식별값·토큰·코드의 URL 비노출
총 175개 점검 항목이다.
13. 구현 단계 전달사항
13.1 인증 구현
회원가입은 email, password, passwordConfirmation만 받고 이름과 시간대는 받지 않는다. 이메일은 trim·소문자 정규화 후 형식과 최대 254자를 검증한다.
비밀번호는 8자 이상 64자 이하이고 공백 전용 값을 거부하며 조합은 강제하지 않는다. 확인값은 저장하지 않고 BCrypt 해시만 저장한다.
가입 성공은 201과 email·timezone·createdAt을 반환하되 자동 로그인과 세션 생성은 하지 않는다. 422 VALIDATION_ERROR, 409 DUPLICATE_EMAIL, 429 RATE_LIMITED, 500 SERVER_ERROR와 공통 오류 JSON을 적용한다.
로그인은 email과 password만 받고 성공은 200과 email·timezone을 반환한다. CALTALK_SESSION 서버 세션을 생성하고 기존 세션이 있으면 ID를 교체하며, 유휴 만료는 12시간으로 설정한다.
로그인 실패 내부 분기는 401 INVALID_CREDENTIALS로 일반화한다. 계정 제한은 15분·5회, IP 제한은 15분·20회를 유지하고 IP 초과 시 429 RATE_LIMITED와 Retry-After를 반환한다.
보호 API의 미인증·권한 부족은 HTML 리다이렉트 없이 각각 401 UNAUTHORIZED·403 FORBIDDEN JSON으로 응답한다.
로그인 성공 후 내부 상대 복귀 경로를 검증해 우선 이동하고 없으면 홈으로 이동한다. 실패 시 이메일은 유지하고 비밀번호는 삭제한다.
세션과 CSRF 쿠키 갱신을 함께 검증한다.
로그아웃은 CSRF 예외에 추가하지 않는다. 유효한 CSRF 토큰이 있으면 인증 여부와 관계없이 현재 세션과 SecurityContext를 종료하고 CALTALK_SESSION을 삭제한 뒤 빈 본문의 204를 반환한다. 토큰 누락·불일치는 403 FORBIDDEN 공통 JSON으로 처리한다.
클라이언트는 로그아웃 성공 시 인증 상태와 사용자 캐시를 초기화하고 로그인 화면이 아닌 시작 화면으로 이동한다. 서버 리다이렉트를 사용하지 않으며 뒤로가기로 보호 화면이 재노출되지 않도록 처리한다.
현재 사용자 조회는 세션 principal의 정규화 이메일로 users를 다시 조회하고 200에서 email·timezone·createdAt만 반환한다. id·비밀번호·토큰·세션·역할 정보는 반환하지 않으며 Cache-Control no-store를 적용한다.
세션이 없거나 인증되지 않았거나 principal에 해당하는 사용자가 없으면 401 UNAUTHORIZED 공통 JSON을 반환한다. 사용자 없는 세션은 무효화하고 SecurityContext와 CALTALK_SESSION을 정리하며 HTML 리다이렉트와 사용자 존재 여부 노출을 금지한다.
프런트엔드는 앱 초기 진입과 새로고침에서 GET /api/v1/users/me로 인증 상태를 복원한다. 401이면 공개 화면은 유지하고 보호 화면은 시작 화면으로 이동하며 로그인 화면으로 무조건 이동하지 않는다.
13.2 PWA 일정 CRUD
종료 시간 자동 채움과 다음 날 토글을 구현한다.
충돌 시 클라이언트 승인 플래그가 아니라 서버 confirmation을 사용한다.
일정 조회는 인증된 `GET /api/v1/schedules?from={from}&to={to}`와 `GET /api/v1/schedules/{scheduleId}`를 사용한다. 기간 조회는 현재 사용자의 일정만 겹침 조건으로 한 번에 조회해 startAt·endAt·id 순으로 반환하고 일정별 추가 조회를 만들지 않는다. 목록은 items 래퍼, 빈 결과는 200, 상세의 없음·타 사용자 소유는 동일한 404 SCHEDULE_NOT_FOUND이며 GET에는 CSRF 토큰을 요구하지 않는다.
일정 수정은 `PATCH /api/v1/schedules/{scheduleId}`에 변경 필드와 필수 version을 보내며 location 미전달·null/빈 값·문자열을 KEEP·REMOVE·SET으로 구분한다. 직접 version 불일치는 SCHEDULE_VERSION_CONFLICT, 수정 confirmation 승인 중 대상 version·충돌 변경은 기존 CONFIRMATION_SUPERSEDED 자동 재계산을 사용한다. 실제 수정은 전체 before/after UPDATE 이력과 함께 저장하고 무변경은 version·updatedAt·이력을 유지한다.
일정 삭제는 `DELETE /api/v1/schedules/{scheduleId}?version={version}`을 사용하고 PWA 확인 모달 외 서버 confirmation은 만들지 않는다. 성공은 204와 빈 본문이며 하드 삭제와 ON DELETE CASCADE로 변경 이력도 제거한다. PATCH·DELETE는 인증·CSRF 보호를 적용하고 없음·타 사용자 일정은 같은 404로 숨긴다.
일정 생성은 POST /api/v1/schedules에 title·startAt·endAt·location만 전송한다. title은 trim 후 필수·최대 200자, location은 trim 후 최대 200자이며 빈 값은 null로 처리한다. offset 없는 시각과 endAt이 startAt보다 늦지 않은 요청은 422 VALIDATION_ERROR로 거부하고 PWA 과거 일정은 허용한다.
세션 principal의 이메일로 현재 사용자를 조회해 owner_user_id를 정하고 요청의 사용자 식별 필드와 계약 외 필드는 거부한다. 사용자 없는 인증 세션은 현재 사용자 조회와 같은 경로로 정리한다. 생성은 세션 인증과 CSRF 보호를 적용하고 일정·CREATE 이력을 하나의 트랜잭션에서 저장한다.
충돌 없음은 201, Cache-Control no-store, Location과 id·title·startAt·endAt·location·createdAt·updatedAt·version을 반환한다. 시각은 UTC Z 표기이며 소유자·인증·민감정보는 반환하지 않는다.
같은 사용자 일정과 겹치면 최초 POST는 DB를 변경하지 않고 409 SCHEDULE_CONFLICT와 confirmationId·충돌 목록을 반환한다. 경계가 맞닿거나 다른 사용자 일정만 겹치면 충돌이 아니다. 승인은 기존 confirmation 엔드포인트를 사용하고 위조·소유권·만료·중복 소비를 재검증한다.
SCHEDULE_CONFLICT는 공통 오류 필드에 숫자 confirmationId와 startAt·id 순으로 정렬된 conflicts를 추가한다. conflicts는 id·title·UTC startAt·UTC endAt·location만 노출한다. 승인 요청은 POST /api/v1/confirmations/{confirmationId}/approve와 conflictAcknowledged true를 사용하며 5분 만료와 상태 5개, 후보 지문·충돌 해시·대상 버전 재검증 및 SUPERSEDED 자동 발급을 유지한다.
confirmation_requests는 user_id와 기존 개별 후보 컬럼을 유지하고, schedule_change_history는 changed_by_user_id·source_channel·before/after 구조와 schedule_id ON DELETE CASCADE를 유지한다. schedules.owner_user_id와 confirmation의 FK 및 history.changed_by_user_id는 NO ACTION으로 구현한다.
성공 후 입력 화면을 닫고 안내를 표시하며 선택 날짜 일정 목록, 홈의 오늘 일정, 월간 캘린더를 무효화하거나 다시 조회한다. 검증·충돌 실패는 입력값을 유지하고 별도 캐시 API를 만들지 않는다.
삭제는 즉시 하드 삭제이며 휴지통이 없다.
13.3 웹 자연어 일정 관리
PAST_DATETIME_REJECTED는 화면·API 명세 제안이며 구현 시 최종 응답 enum 채택 여부를 확정한다.
PENDING_COMMAND_EXPIRED reason 필드와 값도 구현 시 최종 채택 여부를 확정한다.
10분 재질문 만료와 5분 confirmation 만료를 분리한다.
수정 대상 후보 선택은 별도 API를 신설하지 않고 진행 중인 자연어 대화의 후속 입력으로 처리하며 POST /api/v1/chat/messages를 재사용한다.
요청에는 내부 일정 ID 대신 불투명한 후보 선택값을 포함하고, 정확한 DTO 필드명은 화면·API 명세 제안으로 구현 단계에서 확정한다.
서버는 pending_commands의 존재 여부와 만료 여부, 후보 일정의 사용자 소유권, 후보의 현재 존재 여부와 선택된 후보의 현재 후보 집합 포함 여부를 다시 검증한다.
후보 선택만으로 일정 DB 데이터는 변경하지 않으며, 검증 성공 시 자연어 수정 최종 확인 카드로 전환한다.
13.4 카카오 연결과 채널
실제 식별값을 화면이나 응답에 노출하지 않는다.
자동 폴링과 실시간 push를 추가하지 않는다.
카카오 응답에서는 동일 상태를 스킬 규격 정상 JSON 내부 문구로 변환한다.
13.5 안정화
시간대 변경은 PATCH /api/v1/users/me로 처리하며 별도 캐시 API를 추가하지 않는다. timezone 하나만 받고 trim 후 지역 기반 IANA Zone ID인지 Java ZoneId로 검증한다. 빈 값·공백·존재하지 않는 ID·약어·고정 오프셋은 422 VALIDATION_ERROR와 timezone의 INVALID_TIMEZONE fieldError로 거부한다.
PATCH는 CALTALK_SESSION 인증과 CSRF 보호를 적용한다. 성공은 동일 값 재요청을 포함해 200, Cache-Control no-store와 email·timezone·createdAt을 반환한다. 미인증 또는 사용자 없는 인증 세션은 401 UNAUTHORIZED, CSRF 누락·불일치는 403 FORBIDDEN으로 처리한다.
하나의 트랜잭션에서 현재 사용자의 users.timezone만 변경하고 email·password_hash·created_at·사용자 ID·일정 UTC 값과 DB 스키마는 변경하지 않는다. 동시 요청은 마지막 정상 처리 값을 최종 상태로 사용한다.
저장 성공 후 GET /api/v1/users/me 사용자 정보, 홈 화면의 오늘 일정, 월간 캘린더 일정, 선택 날짜 일정 목록, 현재 열려 있는 일정 상세의 표시값을 무효화하거나 다시 조회한다.
그 후 사용자 시간대 기준으로 날짜, 요일, 시작 시간, 종료 시간, 오늘 및 선택 날짜 기준을 다시 계산하며 기존 일정의 UTC 절대 저장값은 변경하지 않는다.
confirmation 동시 승인, 최초 동일 후보 생성 경쟁, 대상 변경·삭제를 테스트한다.
AI 장애가 PWA 직접 CRUD 장애로 전파되지 않게 한다.
14. 알려진 UX 위험과 대응
위험	대응
자연어 과거 일정 거부를 해석 실패로 오해	정책상 제한임을 분명히 말하고 직접 입력 경로 제공
10분 만료와 5분 만료 혼동	각각 “대화가 오래됨”과 “확인 요청 만료”로 다른 문구 사용
최신 후보 자동 갱신이 임의 변경처럼 보임	변경 이유를 먼저 알리고 전후 내용을 재표시
충돌을 오류로 오해	경고와 진행 가능한 액션을 함께 표시
삭제 실수	비가역 문구와 파괴적 버튼 분리
캘린더가 모바일 화면을 독점	월간 그리드를 일반 스크롤에 포함
AI 장애를 전체 서비스 장애로 오해	직접 일정 기능은 계속 사용할 수 있다고 안내
카카오 코드를 로그인으로 오해	모든 문구를 “연결”로 통일
수동 연결 확인이 번거로움	버튼 옆에 실행 시점을 구체적으로 안내
시간대 변경 후 일정 자체가 바뀐 것으로 오해	UTC 저장값은 바뀌지 않는다고 설명
연결 코드 제한의 원인 불명확	10분 3회 제한과 재시도 안내 제공

15. PoC 단계 전달사항
카카오 채널·챗봇 관리자 접근 가능 여부를 실제 확인한다.
공중망 HTTPS 스킬 서버 호출을 검증한다.
실제 요청·응답 JSON과 사용자 식별값 위치를 확인한다.
카카오 응답 시간 제한 안에서 OpenAI 호출이 가능한지 측정한다.
콜백을 전제로 설계하지 않는다.
카카오 요청 진위 검증에 사용할 공식 서명·헤더·IP 정보가 있는지 확인한다.
카카오 원 요청 식별자를 멱등성 키로 사용할 수 있는지 확인한다.
PAST_DATETIME_REJECTED와 PENDING_COMMAND_EXPIRED를 카카오 응답 문구에 어떻게 매핑할지 검증한다.
PoC 성공 전 랜딩에서 카카오 기능을 완성된 기능처럼 홍보하지 않는다.
PoC가 실패하거나 범위 조정 조건에 해당하면 PWA와 웹 자연어 기능을 유지하고 카카오 공개 문구만 비활성화한다.
16. 보류·확인 필요 항목
다음 날을 넘는 다중 일자 일정 허용 여부와 최대 기간
계정 설정의 비밀번호 변경 기능 포함 여부
화면 명세 제안 API 3종의 최종 채택
수정 후보 선택 DTO의 정확한 필드명
PAST_DATETIME_REJECTED의 최종 응답 enum 채택 여부
PENDING_COMMAND_EXPIRED reason 필드와 값의 최종 채택 여부
종료 시간 직접 수정 후 시작 시간 변경 규칙
카카오 PoC 실패 시 공개 문구를 제어할 배포 설정 방식
관리형 PostgreSQL 호스팅과 백업 보관 기간
정확한 색상·폰트·브랜드 스타일
STALE_CONFIRMATION 오류 코드 도입 여부(기존 오류 사전에 없으므로 CONFIRMATION_NOT_FOUND·CONFIRMATION_SUPERSEDED·CONFLICT_ACKNOWLEDGEMENT_REQUIRED 계약을 우선 사용)
17. 명세 완료 기준

화면 15개 정의

다이얼로그·시트·상태 UI 17개 정의

공통 상태와 오류 정의

사용자 흐름 18개 전문 수록

수용 기준 61개 작성

API 경로를 /api/v1 전체 경로로 통일

중복 제거 API 17종 재계산

테스트 체크 항목 175개 작성

자연어 과거 일정 거부 반영

재질문 10분 만료와 확인 5분 만료 분리

자연어 확인 취소와 후보 선택 API 매핑

시간대 변경 캐시 무효화 범위 보완

MVP 제외 기능 유지

카카오 PoC 미완료 상태 유지

실제 구현이나 테스트 완료로 표현하지 않음

실제 식별값·연결 코드·내부 경로 미포함

보류 항목의 구현 단계 결정

카카오 PoC 수행

사용자 최종 승인
18. v0.2 대비 변경 요약
자연어 과거 일정 정책 거부 상태를 추가했다.
재질문 10분 만료를 confirmation 5분 만료와 분리했다.
생략됐던 사용자 흐름을 전문으로 복원했다.
자연어 확인 취소 API와 수정 후보 선택 API 매핑을 추가했다.
시간대 변경 후 일정 관련 캐시 전체 갱신을 명시했다.
일정 생성의 요청·검증·UTC 저장·소유권·충돌 확인·201 응답·재조회 계약을 확정했다.
DLG-EXPIRED-001의 화면별 표시 형식을 구체화했다.
연결 코드 발급 제한 안내 문구를 추가했다.
모든 API를 /api/v1 전체 경로로 통일했다.
수용 기준, 테스트, 상태, 화면·API 요약 개수를 실제 목록에서 다시 계산했다.
화면 15개와 UI 17개 및 MVP 범위는 유지했다.
19. 최종 목록과 개수
화면: 15개
다이얼로그·시트·상태 UI: 17개
API: 17종(확정 14종, 화면·API 명세 제안 3종)

사용자 흐름: 18개
수용 기준: 61개
테스트 체크 항목: 175개
공통 상태·오류 사전: 21개 행
20. 최종 자체 평가
20.1 정합성
서비스 기획서의 MVP 범위와 기술 설계서의 인증, 소유권, confirmation, 충돌, 만료, 멱등성, 시간대, 카카오 연결 원칙을 유지했다.
20.2 완전성
화면, 상태 UI, 사용자 흐름, API 매핑, 수용 기준, 테스트, 구현 전달사항을 이 문서 안에 수록했다. 이전 버전의 실제 내용을 참조해야만 이해할 수 있는 생략 표현은 사용하지 않았다.
20.3 구현 가능성
인증, PWA CRUD, 웹 자연어, 카카오 연결, 안정화 단계별 입력으로 사용할 수 있다. 다만 화면 명세 단계에서 확정 권한이 없는 정책과 PoC 결과는 보류 상태로 남겼다.
20.4 최종 판정
A. 화면·기능 명세서 v1.0 Final로 승인 요청 가능한 상태
남은 미충족 항목은 사용자 승인, 구현 단계 결정사항, 카카오 PoC 및 실제 구현·검증이다.
21. 다음 작업 두 가지
이 화면·기능 명세서 v1.0 Final을 검토하고 공식 승인한다.
승인 후 5단계인 카카오톡 채널 챗봇 PoC 계획과 기술검증 명세를 작성한다.
