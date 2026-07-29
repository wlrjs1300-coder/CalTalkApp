CalTalk 카카오톡 채널 챗봇 PoC 계획서 v0.1
1. 문서 확인
다음 5개 문서를 처음부터 끝까지 기준 자료로 확인했다.
CalTalk_MVP_서비스_기획서_최종본.md
CalTalk_기술_설계서_v1.0_Final.md
CalTalk_프로젝트_시작_프롬프트_최종본.md
CalTalk_화면_기능_명세서_v1.0_Final.md
GitHub_공개저장소_업로드_커밋_푸시_점검가이드.md
파일명의 (1), (2)는 문서 버전으로 해석하지 않았다. 문서 내부 기준으로 서비스 기획서, 기술 설계서, 화면·기능 명세서는 각각 1.0 Final 상태다.
기준은 다음과 같다.
서비스 범위: PWA가 본체이며 카카오톡은 보조 채널이다.
기술 원칙: 카카오 요청은 채널 어댑터를 거쳐 공통 일정 도메인 서비스로 전달한다.
안전 원칙: 생성·수정은 확인 전 DB를 변경하지 않으며 사용자 연결과 소유권을 재검증한다.
공개 원칙: 실제 식별값, 연결 코드, 비밀 URL, 인증값, 내부 경로, 원문 로그를 공개하지 않는다.
진행 순서: 현재는 5단계이며 저장소 초기화와 정식 구현은 아직 수행하지 않는다.
2. 현재 단계와 작업 범위
현재 단계는 5단계 — 카카오톡 채널 챗봇 PoC 계획 및 기술검증이다.
이번 단계의 목적은 카카오 전체 일정 기능을 완성하는 것이 아니라 다음 가능성을 최소 범위에서 증명하는 것이다.
카카오가 외부 스킬 서버를 호출할 수 있는가
실제 요청 페이로드에서 필요한 입력과 사용자 식별값을 얻을 수 있는가
카카오 규격 응답이 실제 말풍선으로 표시되는가
일반 스킬의 5초 제한 안에서 CalTalk의 최소 흐름을 처리할 수 있는가
일회용 연결 코드와 연결 사용자 접근 통제가 가능한가
버튼·퀵리플라이·카드로 필요한 확인 UX를 구성할 수 있는가
공식적인 요청 인증 수단과 요청 고유 식별자가 제공되는가
이번 단계에서는 다음을 하지 않는다.
운영용 일정 CRUD 구현
정식 PWA 또는 백엔드 프로젝트 생성
저장소·브랜치·모노레포 초기화
운영 채널 연결
실제 계정이나 개인정보를 사용한 테스트
카카오 로그인 또는 소셜 로그인 구현
콜백을 MVP 필수 조건으로 채택
PostgreSQL 전체 스키마 구현
운영 배포 또는 이후 개발 단계 선행
PoC에 필요한 최소 서버는 폐기 가능한 일회성 기술검증 자원으로만 취급한다.
3. 최신 카카오 공식 문서 확인 결과
확인일: 2026-07-27

근거 상태는 다음과 같이 구분한다.

- [OFFICIAL-CONFIRMED]: 카카오 공식 문서에서 직접 확인된 사실
- [OFFICIAL-INDIRECT]: 공식 문서의 예시 또는 서로 다른 공식 문서의 간접 비교로 확인한 사실
- [POC-VERIFY]: 공개 공식 문서만으로 확정할 수 없어 관리자센터 또는 실제 호출로 확인할 사항
- [PROJECT-DECISION]: 카카오 플랫폼의 보장이 아니라 CalTalk가 선택한 설계·보안·운영 규칙

공식 문서에서 확인되지 않은 기능은 없거나 지원되지 않는다고 단정하지 않는다.

| 문서 ID | 공식 문서명 | 카카오 공식 URL | 관련 절 또는 제목 | 확인일 | 직접 확인한 사실 | 근거 상태 | 연결 검증 ID | 비고 |
|---|---|---|---|---|---|---|---|---|
| DOC-SKILL-001 | 스킬 만들기 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 스킬 서버 이해하기, 스킬 테스트 | 2026-07-27 | 요청은 HTTP POST와 JSON body를 사용하고, 공인 IP 또는 공중망 도메인이 필요하며, 고정 타임아웃은 5초다. | [OFFICIAL-CONFIRMED] | KSK-001~005, KPF-001~009 | 실제 호출로 재확인 |
| DOC-PAYLOAD-001 | 응답 타입별 JSON 포맷 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | SkillPayload | 2026-07-27 | intent, userRequest, bot, action, contexts, flow와 사용자·발화·파라미터 필드를 설명한다. | [OFFICIAL-CONFIRMED] | KSK-001~012 | 실제 개발 채널 값은 별도 확인 |
| DOC-RESPONSE-001 | 응답 타입별 JSON 포맷 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | SkillResponse, 응답 타입 | 2026-07-27 | version 2.0, template, outputs, quickReplies와 말풍선 JSON 형식을 설명한다. | [OFFICIAL-CONFIRMED] | KUI-001~017 | 표시 결과는 실제 채널 확인 |
| DOC-UI-001 | 응답 설정 | https://kakaobusiness.gitbook.io/main/tool/chatbot/main_notions/setup_answer | 응답 사이즈 | 2026-07-27 | 각 말풍선의 플랫폼 계산 응답 크기가 30,720바이트를 넘으면 발송되지 않으며 봇테스트에서 확인한다. | [OFFICIAL-CONFIRMED] | KUI-001~018 | JSON 원문 크기와 다를 수 있음 |
| DOC-ERROR-001 | 스킬 오류 내역 확인하기 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 오류 타입, 세부 오류 메시지 | 2026-07-27 | 운영 채널 오류는 서버 연결 오류와 말풍선 가이드 위반 등으로 관리자센터에서 확인한다. | [OFFICIAL-CONFIRMED] | KUI-018, KPF-010~015 | Event API 오류 코드와 혼용 금지 |
| DOC-CALLBACK-001 | AI 챗봇 콜백 개발 가이드 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/ai_chatbot_callback_guide | 개요, SkillPayload | 2026-07-27 | AI 챗봇 콜백 URL은 약 1분 유효하고 1회 사용한다. | [OFFICIAL-CONFIRMED] | KPF-009 | 일반 스킬 요청 규칙으로 확대 금지 |
| DOC-BOTTEST-001 | 봇테스트 | https://kakaobusiness.gitbook.io/main/tool/chatbot/main_notions/bot_test | 봇테스트 화면, 제공 정보 | 2026-07-27 | 관리자센터 웹에서 발화와 응답 형태 및 상세 정보를 확인할 수 있다. | [OFFICIAL-CONFIRMED] | KSK-010~012, KUI 전체 | 실제 채널과 구분 |
| DOC-BOT-001 | 봇 설정 | https://kakaobusiness.gitbook.io/main/tool/chatbot/main_notions/bot_setting | 기본 정보 | 2026-07-27 | 운영·개발 채널 연결과 앱 키 설정 항목을 설명한다. | [OFFICIAL-CONFIRMED] | KSK-002, KSK-012 | 로그인 후 UI는 재확인 |
| DOC-CHANNEL-001 | 튜토리얼 1단계 | https://kakaobusiness.gitbook.io/main/tool/chatbot/tutorial/make_chatbot/tutorial_1 | 카카오톡 채널 연결하기 | 2026-07-27 | 챗봇과 개발·운영 채널 연결 절차를 설명한다. | [OFFICIAL-CONFIRMED] | KSK-002 | 운영 채널은 PoC 비범위 |
| DOC-AUTH-001 | 스킬 만들기 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 스킬 생성·수정 설정 | 2026-07-27 | URL·Test URL과 헤더값·테스트 헤더값을 입력할 수 있다. | [OFFICIAL-CONFIRMED] | KSE-001~004 | 암호학적 서명이 아님 |
| DOC-AUTH-002 | 공개 공식 문서 부재 조사 기록 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide | 스킬 개발 가이드 공개 범위 조사 | 2026-07-27 | 서명, 타임스탬프, nonce, 고정 인증 헤더, 공식 발신 IP 대역, mTLS 등의 제공 보장을 확인하지 못했다. | 공식 문서에서 확인 불가 / [POC-VERIFY] | KSE-001~014 | 기능이 없다는 판정이 아님 |
| DOC-IDEMP-001 | 공개 공식 문서 부재 조사 기록 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | SkillPayload 공개 필드 조사 | 2026-07-27 | 일반 스킬 요청별 고유 ID, 재전송 ID, 공식 재시도 계약의 제공 보장을 확인하지 못했다. | 공식 문서에서 확인 불가 / [POC-VERIFY] | KID-001~008 | 기능이 없다는 판정이 아님 |

공식 확인 수치:

| 항목 | 공식 기준 | 상태 |
|---|---|---|
| 일반 스킬 고정 타임아웃 | 5초 | [OFFICIAL-CONFIRMED] |
| SkillResponse version | `"2.0"` | [OFFICIAL-CONFIRMED] |
| `template.outputs` | 1개 이상 3개 이하 | [OFFICIAL-CONFIRMED] |
| `template.quickReplies` | 최대 10개 | [OFFICIAL-CONFIRMED] |
| `simpleText.text` | 최대 1,000자, 500자 초과 시 전체 보기 또는 표시 변화 가능 | [OFFICIAL-CONFIRMED] |
| `basicCard.title` / `description` | 최대 50자 / 230자 | [OFFICIAL-CONFIRMED] |
| 단일형 / 캐러셀형 `listCard.items` | 최대 5개 / 최대 4개 | [OFFICIAL-CONFIRMED] |
| 버튼 가로 / 세로 배치 | 최대 2개 / 최대 3개 | [OFFICIAL-CONFIRMED] |
| AI 챗봇 콜백 URL | 약 1분 유효, 1회 사용 | [OFFICIAL-CONFIRMED] |
| 각 말풍선 응답 크기 | 플랫폼 계산 크기 30,720바이트 초과 시 발송되지 않음 | [OFFICIAL-CONFIRMED] |

[OFFICIAL-CONFIRMED] 카카오 공식 응답 설정 문서는 챗봇 관리자센터 또는 스킬 서버에서 설정한 각 말풍선의 플랫폼 계산 응답 크기가 30,720바이트를 넘으면 사용자에게 발송되지 않는다고 설명한다. 스킬 서버의 JSON 원문 바이트와 플랫폼이 계산한 실제 응답 크기는 다를 수 있으므로 봇테스트에서 실측한다.

요청 JSON 근거:

| 경로 | 공식 확인 내용 | PoC 처리 |
|---|---|---|
| `userRequest`, `userRequest.utterance`, `userRequest.user`, `userRequest.timezone`, `userRequest.lang`, `userRequest.params` | SkillPayload 구성과 상세 필드 | 실제 호출의 키 구조와 값을 마스킹해 확인 |
| `userRequest.user.id` | 현재 type은 `botUserKey` | 인증 수단으로 표현하지 않고 원문을 장기 저장하지 않으며 반복 안정성은 실제 호출로 검증 |
| `userRequest.user.properties.plusfriendUserKey` | 카카오톡 채널 사용자 식별키 | 개발 채널 요청에서 실제 전달되는지는 [POC-VERIFY] |
| `userRequest.user.properties.appUserId` | 봇 설정에서 앱 키를 설정한 경우에만 제공 | 제공 조건을 실제 호출로 확인하며 비공식 별칭은 사용하지 않고 공식 필드명 `appUserId`만 사용 |
| `intent`, `bot`, `action`, `contexts`, `flow` | SkillPayload의 상위 필드 | 존재·형태를 실제 호출로 확인 |

`bot.id`, `intent.id`, `action.id`, block 관련 ID는 요청별 고유 ID로 간주하지 않는다. [OFFICIAL-INDIRECT] 일반 스킬 문서와 콜백 문서의 한국어 예시값이 각각 `ko`와 `kr`로 다르게 나타나므로, 특정 값을 하드코딩하지 않고 실제 일반 스킬 요청의 값을 증적으로 확인한다.

응답 JSON 근거:

| 항목 | 공식 문서 ID | 공식 URL | 관련 절 | 필수 필드 | 선택 필드 | 제한 | PoC 검증 ID | 증적 유형 | 실제 표시 검증 |
|---|---|---|---|---|---|---|---|---|---|
| `version` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | SkillResponse | `"2.0"` | 없음 | 고정 값 | KUI-001 | RES | 필요 |
| `template`, `outputs` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | SkillResponse | template, outputs | quickReplies | outputs 1~3 | KUI-001, KUI-016 | RES, UI | 필요 |
| `simpleText` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | SimpleText | text | 없음 | 1,000자; 500자 경계 표시 확인 | KUI-001~003 | RES, UI | 필요 |
| `basicCard` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | BasicCard | 카드 객체 | title, description, thumbnail, buttons | title 50자, description 230자 | KUI-008 | RES, UI | 필요 |
| `listCard` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | ListCard | header, items | buttons | 단일 5개, 캐러셀 4개 | KUI-009~011 | RES, UI | 필요 |
| `quickReplies` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | QuickReply | label, action | messageText, blockId, extra | 최대 10개 | KUI-004~007, KUI-017 | RES, UI | 필요 |
| `buttons`, `buttonLayout` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | Button | label, action별 필수값 | 배치 옵션 | 가로 2개, 세로 3개 | KUI-012~013 | RES, UI | 필요 |
| `webLinkUrl` | DOC-RESPONSE-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | Button `webLink` | action=`webLink`, webLinkUrl | 없음 | 공식 action·필드명 준수 | KUI-012 | RES, UI | 필요 |
| `useCallback`, `callbackUrl` | DOC-CALLBACK-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/ai_chatbot_callback_guide | SkillPayload, CallbackRequest | 콜백 사용 시 관련 값 | 일반 스킬에서는 비필수 | AI 챗봇 콜백에만 약 1분·1회 | KPF-009 | REQ, RES, TIM | 필요 |
4. PoC 핵심 질문
질문별 근거 상태와 실제 판정은 분리하며, 공식 문서만으로 확정되지 않는 항목은 [POC-VERIFY], CalTalk가 선택한 규칙은 [PROJECT-DECISION]으로 기록한다.
ID	질문	판정 방법
Q1	카카오가 공중망 HTTPS 서버를 호출하는가	실제 채널 요청 수신
Q2	발화·시간대·사용자 식별값을 얻는가	마스킹된 페이로드 구조 확인
Q3	정상 JSON이 말풍선으로 표시되는가	실제 카카오톡 화면 확인
Q4	단순 응답을 5초 내 반환하는가	서버·화면 시간 측정
Q5	일회용 연결 코드를 처리할 수 있는가	정상·만료·재사용 등 상태 테스트
Q6	연결 사용자만 접근할 수 있는가	연결 전후·해제 후 비교
Q7	어떤 UI가 실제 사용 가능한가	퀵리플라이·카드·버튼 비교
Q8	3분기 선택이 가능한가	버튼 또는 퀵리플라이 실제 선택
Q9	요청 진위를 공식 수단으로 확인할 수 있는가	사용자 정의 헤더와 추가 공식 수단 조사
Q10	콜백 없이 핵심 흐름이 가능한가	일반 스킬 5초 내 성공률
Q11	요청 전용 멱등성 ID가 있는가	반복·재전송 페이로드 비교
Q12	실패를 정상 응답 JSON으로 안내할 수 있는가	장애 시나리오별 말풍선 확인

현재는 모두 계획 완료 상태이며 실제 통과로 판정된 항목은 없다.
5. PoC 범위
PoC-1. 고정 응답 스킬 연결
임시 HTTPS 엔드포인트
카카오 POST 요청 수신
헤더명과 JSON 필드 구조 확인
고정 simpleText 응답
실제 채널 말풍선 확인
관리자센터 오류 내역 확인
서버 처리시간과 화면 체감시간 측정
PoC-2. 요청 식별정보
userRequest.utterance
userRequest.timezone
userRequest.lang
userRequest.user.id
선택적 properties
동일 사용자 반복 호출
서로 다른 테스트 사용자 비교
관리자센터 테스트와 실제 채널 호출 차이
원문 미보관과 HMAC 또는 마스킹
PoC-3. 연결 코드 최소 흐름 [PROJECT-DECISION]
8자리, 혼동 문자 제외
5분 유효
한 번만 사용
코드당 실패 5회
사용자당 활성 코드 1개
10분당 발급 3회
HMAC 검증 [PROJECT-DECISION]
재발급 시 기존 코드 무효화
연결과 해제
연결 전·해제 후 일정 기능 접근 차단
실제 일정 DB 없이 모의 접근 결과만 반환
PoC-4. 응답 UI
simpleText
quickReplies
basicCard
listCard
웹 링크 버튼
3분기 선택
2~5건 후보 표시
6건 이상 재질문
절대 날짜·요일·시간 표시
PoC-5. 응답시간
각 시나리오를 최소 10회 측정한다.
고정 응답
연결 상태 확인
메모리 조회
일정 조회 모의 응답
OpenAI 포함 구조화 요청
의도적 지연과 내부 타임아웃
PoC-6. 실패 처리
4xx·5xx
잘못된 JSON
카카오 응답 규격 위반
지연
OpenAI 타임아웃
미연결 사용자
만료·오입력·재사용 코드
시도 횟수 초과
지원하지 않는 명령
과거 자연어 일정 요청
재질문 대기 만료
확인 요청 만료
가능한 모든 애플리케이션 실패는 HTTP 오류 대신 카카오 규격의 HTTP 200 응답 안에서 안전한 안내로 변환한다. 단, 인증 헤더가 틀리거나 요청 구조 자체가 카카오 요청으로 인정될 수 없는 경우의 HTTP 상태는 별도로 측정한다.
6. PoC 비범위
정식 회원가입·로그인
실제 PWA 화면
운영용 Spring Security와 Spring Session
PostgreSQL 전체 스키마
실제 일정 CRUD
자연어 수정 전체 과정
confirmation 동시성 전체 구현
카카오 로그인
운영 채널
운영 배포
Google Calendar
반복·종일·공유 일정
선제 메시지와 푸시 알림
관리자 기능
결제
콜백 필수 구조
공개 저장소 및 Git 작업
정식 애플리케이션으로의 자동 편입
7. PoC 구현 방식 비교와 권장안
배포 방식과 콜백 비필수 원칙은 [PROJECT-DECISION]이며 카카오 플랫폼이 보장하는 구현 방식으로 표현하지 않는다.
방식	스택 일치	준비 시간	HTTPS	로그·원문 확인	시간 측정	비밀 관리	정리	공개 위험	비용	권장
A. 최소 Spring Boot 서버	높음	중간	배포 방식에 따름	좋음	좋음	좋음	중간	분리 필요	환경에 따름	조건부
B. 최소 Node.js 서버	낮음	짧음	배포 방식에 따름	좋음	좋음	좋음	쉬움	분리 필요	환경에 따름	빠른 예비검증용
C. 서버리스 함수	중간	짧음	기본 제공 가능	플랫폼 의존	콜드 스타트 영향	좋음	쉬움	설정 유출 주의	소액 또는 무료	조건부
D. 로컬 터널 HTTPS	언어 무관	가장 짧음	제공 가능	매우 좋음	터널 지연 혼입	로컬 관리	쉬움	임시 URL 유출 위험	무료 가능	1차 연결 시험용
E. 임시 클라우드 배포	구성에 따름	중간	안정적 제공 가능	좋음	실제 환경에 가까움	환경변수 사용	자원 삭제 필요	공개 설정 주의	소액 가능	최종 권장

최종 권장안은 E. 임시 클라우드 배포에 최소 Spring Boot 서버를 올리는 방식이다.
이유:
CalTalk의 확정 백엔드 스택과 일치한다.
터널 자체의 지연과 연결 불안정이 5초 측정에 섞이는 것을 줄인다.
사용자 정의 헤더, 요청 크기, 타임아웃, JSON 직렬화를 실제 백엔드 환경에 가깝게 검증할 수 있다.
임시 자원을 별도로 삭제하면 정식 저장소와 분리할 수 있다.
PoC 결과를 이후 정식 코드에 자동 편입하지 않아도 된다.
단, 클라우드 준비가 지연될 경우 D 방식은 “카카오가 요청을 보내는지”만 확인하는 사전 시험에 사용할 수 있다. 공식 성능 판정은 E 방식에서 수행한다.
8. 최소 PoC 서버 논리 설계
예시 엔드포인트:
POST https://example.test/<masked-skill-path>
GET https://example.test/<masked-health-path>
두 경로는 설계 예시다. 실제 경로는 추측하기 어려운 임시 경로를 사용하고 기록·스크린샷에서는 가린다.
처리 흐름:
요청 수신
→ 허용 메서드·Content-Type·본문 크기 확인
→ 관리자센터 사용자 정의 인증 헤더 검증
→ JSON 구조 검증
→ userRequest.utterance 추출
→ userRequest.user.id 존재 확인
→ 식별값 HMAC 변환
→ 테스트 시나리오 결정
→ 카카오 SkillResponse 2.0 생성
→ 처리시간·결과 코드 기록
→ HTTP 200 JSON 반환
내부 구성:
요청 검증기
민감값 마스킹기
시나리오 라우터
임시 연결 상태 저장소
연결 코드 검증기
카카오 응답 생성기
시간 측정기
구조화 로그 기록기
최소 시나리오:
ping
echo
identity-check
connection-code
quick-reply
delayed-response
safe-failure
로그 원칙:
발화 원문을 기본적으로 저장하지 않는다.
식별값은 HMAC 또는 앞뒤 일부만 남긴 마스킹 값으로 기록한다.
사용자 정의 인증 헤더 값은 기록하지 않는다.
전체 요청 헤더를 저장하지 않는다.
연결 코드 원문을 저장하지 않는다.
각 요청에는 서버 내부에서 생성한 임시 추적 ID만 부여한다.
PoC 종료 후 로그와 임시 상태를 삭제한다.
9. 연결 코드 PoC 계획
연결 코드 8자리·5분 유효·1회 사용, 코드당 실패 5회, 사용자당 10분에 3회 발급, HMAC 저장과 연결 완료·해제 시 대기 상태 폐기는 모두 [PROJECT-DECISION]이다.
이미 CalTalk 계정과 연결된 카카오 사용자가 새 연결 코드를 입력하면 기존 연결을 자동 덮어쓰지 않는다. 동일 계정 재연결도 기존 연결 상태를 안내하고, 다른 계정 연결 시에는 “이미 다른 CalTalk 계정과 연결되어 있습니다. 기존 연결을 해제한 뒤 다시 시도해 주세요.”라고 안내한다. 두 경우 모두 코드를 소비하지 않으며, 다른 계정으로 연결하려면 기존 연결을 먼저 명시적으로 해제해야 한다. [PROJECT-DECISION]
최소 발급 수단 비교
방식	장점	위험	판정
로컬 테스트 스크립트	단순, 외부 공격면 없음	원격 상태와 연결 과정 필요	적합
임시 테스트 페이지	사용 편의성	인증 없는 공개 페이지가 될 위험	제외
제한된 CLI	외부 HTTP 발급 API 불필요	서버 접근 권한 필요	최종 선택
수동 DB 시드	구현이 빠름	실수·원문 저장·상태 불일치 위험	제외
테스트 전용 HTTP API	자동화 용이	공격면과 삭제 누락 위험	제외

선택안은 임시 서버 관리 환경에서만 실행할 수 있는 제한된 CLI다.
CLI는 다음 동작만 수행한다.
테스트 사용자 별칭을 입력받는다.
8자리 코드를 한 번 표시한다.
서버에는 코드 원문 대신 HMAC, 만료시각, 실패 횟수, 사용 상태만 저장한다.
같은 사용자에게 새 코드를 발급하면 이전 코드를 즉시 무효화한다.
발급 내역은 10분당 3회로 제한한다.
PoC 종료 명령으로 모든 코드와 연결 상태를 삭제한다.
연결 테스트 순서:
코드 발급
카카오톡에 코드 입력
형식·만료·사용 상태 확인
사용자 식별값 HMAC과 테스트 사용자 연결
모의 일정 기능 접근 허용 확인
연결 해제
기존 대기 상태 폐기
재접근 차단 확인
실제 계정, 이메일, 일정 또는 개인정보는 사용하지 않는다.
10. 요청 진위 검증 계획
카카오 공식 기능과 CalTalk 자체 방어를 구분한다.

| 구분 | 항목 | 상태 | 검증 |
|---|---|---|---|
| 카카오 공식 설정 | URL·Test URL, 헤더값·테스트 헤더값 입력 기능 | [OFFICIAL-CONFIRMED] | 실제 개발 채널에서 설정 헤더 전달, 운영값·테스트값 분리, 교체 후 이전 값 거부를 [POC-VERIFY] |
| 공개 공식 문서에서 확인 불가 | 요청 서명, 서명 타임스탬프, nonce, 카카오 고정 인증 헤더, 공식 발신 IP 대역, mTLS, 클라이언트 인증서, 관리자센터 별도 Secret, 일반 스킬 요청별 토큰·고유 ID·재전송 ID, 공식 재시도 계약, 일반 스킬 전용 webhook 검증 절차, HTTPS 강제 여부, 허용 포트, 인증서 종류, 리다이렉트 허용 여부 | [POC-VERIFY] | 제공되지 않는다고 단정하지 않고 관리자센터와 실제 호출로 확인 |

사용자 정의 헤더는 카카오가 생성한 암호학적 서명이 아니다.

| CalTalk 자체 방어 | 상태 | 한계 |
|---|---|---|
| 추측하기 어려운 임시 경로 | [PROJECT-DECISION] | 비밀 경로는 강한 인증 수단이 아니다. |
| 요청 JSON 엄격 검증 | [PROJECT-DECISION] | 형식 검증이며 발신자 인증이 아니다. |
| 메모리 기반 요청 제한 | [PROJECT-DECISION] | PoC 자원 보호 목적이다. |
| 사용자 식별값 HMAC 저장 | [PROJECT-DECISION] | HMAC은 저장된 사용자 식별값 원문을 보호하기 위한 방식이며, 카카오 요청의 진위를 보장하지 않는다. |
| 로그 마스킹 | [PROJECT-DECISION] | 공개·운영 노출을 줄인다. |
| 응답 정보 최소화 | [PROJECT-DECISION] | 정보 노출을 줄인다. |
| 외부 오류 메시지 일반화 | [PROJECT-DECISION] | 내부 구조 노출을 줄인다. |

비밀 경로·요청 제한·로그 마스킹은 다중 방어이며 카카오의 공식 보장이 아니다. IP 제한은 카카오 공식 발신 대역과 변경 정책이 확인된 경우에만 검토한다.

검증 순서:
1. 관리자센터 운영 헤더값·테스트 헤더값 설정 항목을 확인한다.
2. 실제 개발 채널과 관리자센터 테스트에서 설정 헤더가 전달되는지 비교한다.
3. 누락·오류·이전 헤더 값을 거부하고 민감값이 로그·오류·화면에 노출되지 않는지 확인한다.
4. 공개 문서에서 확인 불가한 항목은 실제 페이로드와 관리자센터에서 추가 확인하되, 문서에 없는 필드를 신뢰하지 않는다.

최종 판정은 `공식 강한 검증 가능`, `제한적 검증만 가능`, `공식 검증 수단 확인 불가`, `범위 조정 필요` 중 하나로만 기록한다. 계획 단계 판정은 미실행이다.
11. 멱등성 식별자 검증 계획
DOC-IDEMP-001은 존재하는 멱등성 기능 명세가 아니라 공개 공식 문서 부재 조사 기록이다. 일반 스킬 요청별 고유 ID, 재전송 ID, 공식 재시도 계약은 확인한 공개 공식 문서에서 제공 보장을 찾지 못했으며, 이는 해당 기능이 없다는 판정이 아니다. [POC-VERIFY]

`bot.id`, `intent.id`, `action.id`, block 관련 ID와 `userRequest.user.id`는 요청별 고유 ID로 간주하지 않는다.

검증 항목:
- 같은 발화 연속 전송, 같은 버튼 반복 클릭, 관리자센터 재전송, 실제 채널 반복에서 변하는 필드를 비교한다.
- `flow.trigger`와 `action.clientExtra`의 동작을 비교한다.
- 고유 ID 후보가 관찰되면 범위, 충돌 가능성, 반복·재전송 안정성을 검증한다.
- 고유 ID를 확정하지 못하면 조회는 DB를 변경하지 않고, 생성·수정은 서버 발급 confirmation ID의 1회 소비로 보호한다.
- 사용자 식별값 HMAC + 정규화 발화 해시 + 짧은 시간창은 보조 중복 감지일 뿐 완전한 멱등성 보장으로 표현하지 않는다.

모든 항목의 실제 측정값은 실행 전이며 판정은 미실행이다.
12. 5초 제한·OpenAI 응답시간 검증 계획
일반 스킬 고정 타임아웃 5초는 DOC-SKILL-001의 [OFFICIAL-CONFIRMED] 기준이며, 내부 목표시간·타임아웃·실패 전환은 [PROJECT-DECISION]이고 실제 값은 TIM 증적으로 측정한다.
시나리오	목표 서버 시간	내부 타임아웃	실패 전환	지연 결과	DB 변경
A. 고정 응답	500ms 이하	1초	즉시 안전 안내	없음	없음
B. 연결 상태	800ms 이하	1.5초	재시도 안내	폐기	없음
C. 단순 모의 조회	1초 이하	1.5초	조회 실패 안내	폐기	없음
D. OpenAI 구조화	3.5초 이하	3초	약 3.2초에 실패 응답 준비	결과 폐기	없음
E. 조회+OpenAI	3.8초 이하	외부 호출 2.8초	약 3.5초에 실패 전환	결과 폐기	없음
F. 확인 승인 모의	1초 이하	1.5초	실행 실패 안내	실행 금지	실제 일정 변경 없음
G. 장애·타임아웃	4초 이전 응답	3~3.5초	안전 실패 말풍선	전부 폐기	없음

안전 여유 원칙:
카카오 제한 5초를 내부 목표로 사용하지 않는다.
정상 응답은 서버 기준 4초 이전 반환을 목표로 한다.
외부 호출은 약 3초 안에 종료시켜 실패 응답 생성 시간을 확보한다.
화면 체감시간과 서버 처리시간을 따로 기록한다.
평균뿐 아니라 최댓값과 95백분위 값을 확인한다.
각 주요 시나리오는 최소 10회 수행한다.
OpenAI 요청이 안정적이지 않으면:
콜백을 필수 조건으로 전환하지 않는다.
카카오에서는 즉시 실패 안내와 PWA·웹 대화 경로를 제공한다.
카카오 자연어 생성·수정을 제외할 수 있다.
단순 조회 또는 연결 관리만 유지할 수 있다.
PWA와 웹 자연어 기능은 그대로 진행한다.
늦게 도착한 OpenAI 결과는 일정이나 연결 상태를 변경하지 않고 폐기한다.
13. 카카오 응답 UI 검증 계획
필드·개수·글자 수 제한은 DOC-RESPONSE-001과 DOC-UI-001의 [OFFICIAL-CONFIRMED] 기준을 사용하고, 가독성·줄바꿈·클릭 결과는 실제 채널에서 [POC-VERIFY]한다.
용도	1차 후보	검증 내용
고정 응답·오류	simpleText	길이, 줄바꿈, 실제 표시
3분기 선택	quickReplies	메시지/블록 연결, extra 전달, 재클릭
단일 일정 확인	basicCard 또는 textCard	제목·절대시간·장소·버튼 가독성
후보 2~5건	listCard	최대 5개, 각 후보 구분, 불투명 값 전달
상세 이동	webLink 버튼	내부 식별값 없는 안전한 링크
6건 이상	simpleText+재질문	후보 목록을 표시하지 않고 조건 축소

3분기 선택은 다음 두 방식을 비교한다.
퀵리플라이 3개
세로 카드 버튼 3개
선택 기준:
모바일에서 잘리지 않는가
정확한 문구가 표시되는가
내부 값은 노출하지 않고 extra 또는 동등한 데이터로 전달할 수 있는가
재클릭과 만료를 서버가 통제할 수 있는가
실제 채널과 관리자센터 테스트에서 동일하게 동작하는가
14. 실패·장애 검증 계획
일반 스킬 오류는 실제 관리자센터 오류 내역에 표시된 값을 증적으로 기록하며, 카카오 Event API 오류 코드를 일반 챗봇 스킬 오류 코드처럼 사용하지 않는다.

| 검증 ID | 시나리오 | 공식 오류 분류 | 공식 문서 ID | 공식 URL | 서버 로그 증적 ID | 관리자센터 오류 증적 ID | 카카오 화면 증적 ID | CalTalk 처리 | DB 변경 여부 | 판정 |
|---|---|---|---|---|---|---|---|---|---|---|
| KPF-011 | 서버 예외 | 스킬 서버 내부 오류 | DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | RES-KPF-011-01, SEC-KPF-011-01 | 확인 대상 | UI-KPF-011-01 | 내부 정보 없는 안전 안내와 서버 생존·후속 처리 확인 | 없음 | 통과 |
| KPF-014 | 네트워크 단절 | 스킬 서버 연결 오류 | DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | RES-KPF-014-01 | ERR-KPF-014-01 | UI-KPF-014-01 | 복구 후 정상 응답 확인 | 없음 | 제한적 통과 |
| KPF-009 | 5초 초과 | 타임아웃 | DOC-SKILL-001, DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 실행 전 | 실행 전 | 실행 전 | 늦은 결과 폐기 | 없음 | 미실행 |
| KUI-015 | 잘못된 JSON | 말풍선 가이드 위반 | DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | RES-KUI-015-01 | ERR-KUI-015-01 | UI-KUI-015-01 | 안전한 말풍선과 원문 비노출 확인 | 없음 | 제한적 통과 |
| KUI-002~017 | 말풍선 필드 제한 위반 | 말풍선 가이드 위반 | DOC-RESPONSE-001, DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 실행 전 | 실행 전 | 실행 전 | 유효 응답으로 교정 | 없음 | 미실행 |
| KUI-018 | 말풍선 크기 제한 위반 | 말풍선 사이즈 초과 | DOC-UI-001, DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/main_notions/setup_answer | RES-KUI-018-01 | ERR-KUI-018-01 | UI-KUI-018-01 | 네 크기 표시 성공, 정확한 차단 경계는 미확인 | 없음 | 제한적 통과 |
| KPF-012 | 스킬 서버 4xx | 실제 오류 내역 확인 | DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | RES-KPF-012-01 | ERR-KPF-012-01 | UI-KPF-012-01 | 안전한 말풍선과 내부 오류 원문 비노출 확인 | 없음 | 제한적 통과 |
| KPF-013 | 스킬 서버 5xx | 실제 오류 내역 확인 | DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | RES-KPF-013-01 | ERR-KPF-013-01 | UI-KPF-013-01 | 안전한 말풍선과 내부 오류 원문 비노출 확인 | 없음 | 제한적 통과 |
| KPF-010 | OpenAI 장애 | CalTalk 내부 오류 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 내부 타임아웃 후 일반화된 안내 | 없음 | 미실행 |
| KDM-001 | 연결되지 않은 사용자 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 연결 안내 | 없음 | 미실행 |
| KLC-002 | 만료 코드 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 재발급 안내 | 없음 | 미실행 |
| KLC-005~007 | 잘못된 코드·시도 초과 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 일반 실패·제한 안내 | 없음 | 미실행 |
| KDM-003 | 지원하지 않는 명령 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 지원 범위 안내 | 없음 | 미실행 |
| KDM-004~005 | 과거 일정 자연어 요청 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 생성·수정 거부 | 없음 | 미실행 |
| KDM-007 | pending_commands 10분 만료 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 재입력 안내 | 없음 | 미실행 |
| KDM-008 | confirmation 5분 만료 | CalTalk 도메인 상태 | [PROJECT-DECISION] | 해당 없음 | 실행 전 | 실행 전 | 실행 전 | 재확인 안내 | 없음 | 미실행 |

연결 완료·해제 시 기존 대기 명령과 확인 요청을 폐기하는 것은 [PROJECT-DECISION]이다. 가능한 애플리케이션 실패는 카카오 규격의 안전한 응답으로 변환하되 인증·구조 오류의 HTTP 상태는 실제 호출로 측정한다.
15. PoC 성공 기준
공식 기준 충족과 실제 호출 성공을 분리해 증적 ID로 판정하며, 계획 단계의 모든 항목은 미실행이다.
Must
기준	검증 방법	통과 기준	실패 영향	증거
카카오 POST 수신	실제 채널 발화	서버 수신 확인	PoC 중단	마스킹 로그
정상 JSON 표시	simpleText 반환	실제 말풍선 표시	카카오 기능 불가	화면 캡처
발화 추출	페이로드 확인	입력과 일치	자연어 기능 불가	마스킹 구조
사용자 식별값	반복·사용자 비교	존재하고 사용자 구분 가능	연결 기능 불가	HMAC 비교표
고정 응답 시간	10회 측정	전부 5초 내, 목표 4초 내	구조 재검토	시간표
안전한 실패	장애 주입	정상 응답 안에서 안내	운영 불가	응답 캡처
연결 코드 정상 흐름	발급·입력·사용	1회만 성공	카카오 일정 기능 제외	상태 기록
기연결 사용자 재연결	동일·다른 계정 코드 입력	기존 연결 유지, 코드 미소비, 명시적 해제 안내	계정 오연결 위험	상태 기록
미연결 차단	연결 전 요청	일정 정보 0건	보안 실패	결과표
해제 후 차단	해제 후 요청	접근 불가	보안 실패	결과표
로그 마스킹	로그 검토	원문 비밀값 0건	공개·운영 금지	점검표

Conditional
기준	통과 기준	실패 시 조정
OpenAI 포함 요청	반복 측정에서 5초 내 안정적	카카오 AI 생성·수정 제외
3분기 선택	세 선택이 정확히 구분됨	텍스트 재질문으로 축소
요청 고유 ID	공식 의미와 재전송 안정성 확인	confirmation 중심 멱등성 유지
요청 인증	사용자 정의 헤더가 실제 채널에서 안정적으로 전달	다중 방어 후 제한적 운영 또는 범위 축소
후보 2~5건	잘림·오선택 없이 표시	후보 조건 재질문 방식 사용

Optional
콜백 승인과 실채널 검증
카드 표현 고도화
링크 버튼
다중 카드 표현
관리자센터 테스트 자동화
Optional 실패는 Must 성공 판정을 막지 않는다.
16. 중단·범위 조정 기준
상황	판정
공중망 서버 호출 불가	PoC 중단, 관리자센터·URL 조건 재확인
HTTPS 등록 또는 인증서 문제	임시 배포 방식을 교체하고 재검증
사용자 식별값 없음	카카오 연결·일정 기능 제외
식별값 불안정	카카오 연결 기능 제외
인증 헤더 미전달이나 검증 불가	제한적 방어 평가 후 카카오 기능 축소
고정 응답도 지속적으로 5초 초과	PoC 중단
OpenAI 요청만 초과	카카오 자연어 생성·수정 제외, 단순 조회 검토
연결 코드 보안 규칙 구현 불가	연결이 필요한 모든 일정 기능 제외
3분기 UI 불가	텍스트 기반 재질문으로 축소
요청 고유 ID 없음	confirmation 1회 소비로 변경 요청 보호
콜백 사용 불가	일반 스킬 범위만 유지
개인 개발 환경 제약	카카오 기능 축소 또는 중단

어떤 경우에도 PWA와 웹 자연어 일정 관리는 계속 진행한다. 카카오 실패 시 PWA·웹 기능을 유지하는 것은 [PROJECT-DECISION]이다.
17. 실제 검증 순서
각 단계 결과는 18장의 검증 ID와 19장의 결과 템플릿 및 20장의 증적 ID로 연결한다. 실행 전 실제 측정값은 실행 전, 판정은 미실행이다.
각 단계는 이전 단계가 통과한 뒤에만 진행한다.
17.1 공식 문서 재확인
목적: 테스트 당일 규격 변화 확인
관리자센터: 공지와 스킬 설정 항목 확인
서버 준비: 없음
입력 발화: 없음
예상 요청·응답: 없음
확인 화면: 공식 가이드, 관리자센터
증거: 출처와 확인일
성공: URL·헤더·테스트 기능 확인
중단: 스킬 생성 권한 없음
다음 조건: 설정 항목 기록 완료
17.2 임시 HTTPS 서버 준비
목적: 외부 수신 기반 마련
관리자센터: 아직 URL 등록하지 않음
서버 준비: health와 스킬 엔드포인트
입력 발화: 없음
예상 요청: 외부 health 조회
예상 응답: 일반 상태 응답
확인 화면: 배포 상태
증거: 민감값 없는 상태 기록
성공: 외부 HTTPS 접근 성공
중단: 인증서 또는 외부 접근 실패
다음 조건: 연속 health 성공
17.3 Health 확인
목적: 스킬 등록 전 서버 상태 확인
관리자센터: 없음
서버 준비: DB·외부 API에 의존하지 않는 health
입력: health 요청
예상 응답: 정상 상태
확인 화면: 상태 응답
증거: 시각·상태 코드
성공: 안정적인 응답
중단: 간헐적 실패
다음 조건: 반복 성공
17.4 고정 simpleText
목적: 응답 JSON 자체 검증
관리자센터: Test URL과 임시 인증 헤더 설정
서버 준비: 고정 응답
입력 발화: ping
예상 요청: POST JSON
예상 응답: SkillResponse 2.0
확인 화면: 응답 미리보기
증거: 마스킹 요청 구조와 미리보기
성공: 규격 오류 없음
중단: JSON 가이드 위반
다음 조건: 관리자센터 테스트 성공
17.5 실제 카카오톡 호출
목적: 테스트 도구가 아닌 실제 채널 경로 확인
관리자센터: 블록에 스킬 연결·필요 범위 배포
서버 준비: 동일 고정 응답
입력 발화: ping
예상 요청: 실제 채널 POST
예상 응답: 고정 말풍선
확인 화면: 개발 채널 대화
증거: 가림 처리 화면과 시간
성공: 말풍선 표시
중단: 실제 채널에서 서버 미호출
다음 조건: 반복 호출 성공
17.6 요청 JSON 확인
목적: 필요한 필드 확인
관리자센터: 동일 설정 유지
서버 준비: 필드 존재 여부만 기록
입력 발화: 비식별 테스트 문장
예상 요청: userRequest, action, bot, flow
예상 응답: 수신 필드 목록
확인 화면: 대화와 마스킹 로그
증거: 키 구조만 남긴 샘플
성공: 발화·시간대·사용자 ID 확인
중단: 사용자 ID 없음
다음 조건: 민감값 없는 샘플 확보
17.7 사용자 식별값 안정성
목적: 연결 키 사용 가능성 확인
관리자센터: 변경 없음
서버 준비: HMAC 비교
입력 발화: 동일 사용자가 동일 발화 5회, 다른 사용자 각 3회
예상 요청: 사용자별 ID
예상 응답: 동일 고정 응답
확인 화면: 비교 결과표
증거: HMAC 값만 사용
성공: 동일 사용자 유지·다른 사용자 구분
중단: 값 누락 또는 충돌
다음 조건: 안정성 판정
17.8 Quick Replies
목적: 제한 선택 UX 확인
관리자센터: 퀵리플라이 응답 사용
서버 준비: 세 선택지 반환
입력 발화: 선택 테스트
예상 요청: 선택 후 후속 발화·clientExtra
예상 응답: 선택 결과
확인 화면: 실제 채널
증거: 각 선택 화면
성공: 세 선택 구분
중단: 후속 값 손실
다음 조건: 3개 모두 성공
17.9 카드와 버튼
목적: 일정 확인·링크 표시 가능성 확인
관리자센터: 카드 응답 연결
서버 준비: 가상 일정 데이터
입력 발화: 카드 테스트
예상 요청: 일반 스킬 요청
예상 응답: 카드·버튼
확인 화면: 실제 채널
증거: 가림 처리 캡처
성공: 날짜·시간과 버튼 가독성 확보
중단: 잘림 또는 잘못된 액션
다음 조건: 적합 UI 선정
17.10 연결 코드
목적: 1회용 연결 검증
관리자센터: 코드 발화 블록
서버 준비: 제한 CLI와 임시 상태
입력 발화: 테스트 코드
예상 요청: 코드가 포함된 발화
예상 응답: 연결 성공 또는 일반 실패
확인 화면: 대화와 상태표
증거: 원문 코드를 가린 기록
성공: 정상 코드 1회만 연결
중단: 재사용 또는 타 사용자 연결 성공
다음 조건: 전체 코드 케이스 통과
17.11 연결 해제
목적: 해제 후 접근 차단
관리자센터: 해제 발화
서버 준비: 연결과 대기 상태 제거
입력 발화: 연결 해제
예상 요청: 연결 사용자 요청
예상 응답: 해제 완료
확인 화면: 대화
증거: 전후 접근 결과
성공: 이후 모의 일정 접근 차단
중단: 연결 상태 잔존
다음 조건: 재연결도 정상
17.12 응답시간 측정
목적: 5초 내 안정성 평가
관리자센터: 동일 블록 사용
서버 준비: 시나리오별 시간 계측
입력: 고정·연결·조회 모의 각 10회
예상 요청·응답: 시나리오별 정상 응답
확인 화면: 대화와 측정표
증거: 수신·처리·응답·화면 시각
성공: 전부 5초 내, 목표 4초 내
중단: 고정 응답 반복 초과
다음 조건: 기본 성능 판정
17.13 지연·장애 응답
목적: 안전 실패 확인
관리자센터: 오류 내역 확인 가능 상태
서버 준비: 지연·예외 분기
입력 발화: 지연 테스트, 실패 테스트
예상 요청: 정상 페이로드
예상 응답: 제한 전 안전 실패 또는 의도한 오류
확인 화면: 대화·오류 내역
증거: 상태와 시간
성공: 일정 변경 없이 명확한 안내
중단: 뒤늦은 성공 또는 민감값 노출
다음 조건: 실패 계약 확인
17.14 요청 진위 검증
목적: 헤더 인증 수준 판정
관리자센터: 사용자 정의 헤더 설정·교체
서버 준비: 정상·누락·오류 헤더 분기
입력 발화: 인증 테스트
예상 요청: 정상 헤더 포함
예상 응답: 정상 요청만 허용
확인 화면: 결과표
증거: 헤더 값이 아닌 존재·검증 결과
성공: 정상 전달 및 잘못된 값 차단
중단: 헤더 미전달
다음 조건: 방어 수준 판정
17.15 멱등성 식별자
목적: 요청 전용 ID 존재 확인
관리자센터: 재전송·반복 실행
서버 준비: 키 이름과 해시 비교
입력: 동일 발화·동일 버튼 반복
예상 요청: 반복 페이로드
예상 응답: 고정 응답
확인 화면: 비교표
증거: 마스킹된 차이 목록
성공: 고유 ID 의미와 안정성 확인
중단: 없음—미제공도 유효한 결과
다음 조건: 멱등성 전략 판정
17.16 OpenAI 모의 또는 최소 연동
목적: 5초 내 자연어 구조화 가능성 평가
관리자센터: 일반 스킬 유지
서버 준비: 먼저 지연 모의, 이후 최소 호출
입력 발화: 비식별 가상 일정 문장
예상 요청: 일반 페이로드
예상 응답: 구조화 결과 요약 또는 안전 실패
확인 화면: 대화와 측정표
증거: 입력 원문 없는 시간 기록
성공: 반복적으로 안전 여유 확보
중단: 지연 결과가 상태를 변경함
다음 조건: 카카오 기능 범위 결정
17.17 결과 판정
목적: Must·Conditional·Optional 평가
관리자센터: 오류 내역 최종 확인
서버 준비: 결과 집계
입력: 없음
예상 요청·응답: 없음
확인 화면: 결과표
증거: 각 기준의 캡처·로그·시간표
성공: 모든 Must 충족
중단: 보안 Must 실패
다음 조건: 사용자 승인
17.18 임시 자원 정리
목적: 민감정보와 비용 제거
관리자센터: 임시 스킬·URL·헤더 제거 또는 비활성화
서버 준비: 코드·연결·로그·환경변수·배포 자원 삭제
입력: 없음
예상 요청: 기존 URL 접근 실패
예상 응답: 서비스 미존재
확인 화면: 관리자센터와 배포 상태
증거: 삭제 완료 체크표
성공: 임시 자원과 원문 로그가 남지 않음
중단: 삭제 확인 불가
다음 조건: 5단계 최종 승인
18. 테스트 케이스 전체 목록과 추적성
공식 문서 ID 열에는 카카오 공식 근거가 있으면 DOC-* ID를 기록하고, CalTalk 자체 결정이면 [PROJECT-DECISION]을 기록한다. 공식 URL 열에는 공식 근거 URL을 기록하며, 공식 문서가 없거나 프로젝트 결정이면 '해당 없음 — PROJECT-DECISION' 또는 '공개 공식 문서에서 확인 불가 — POC-VERIFY'처럼 사유를 기록한다.

기존 97개 테스트 케이스와 ID를 유지한다. 계획 단계의 실제 측정값은 실행 전, 판정은 미실행으로 통일한다.

| 검증 ID | 검증 목적 | 사전조건 | 기대값 | 검증 유형 | 근거 상태 태그 | 공식 문서 ID | 공식 URL | 관련 절 | 공식 기준값 | 실제 측정값 | 증적 ID | 판정 | 판정 이유 | 후속 조치 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| KSK-001 | 관리자센터 Test URL 호출 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001, DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 공중망 HTTPS 스킬 호출 및 관리자센터 스킬 테스트 성공 | REQ-KSK-001-01, RES-KSK-001-01, CFG-KSK-001-01 | 통과 | 관리자센터와 서버 수신 기록에서 HTTPS 호출·응답 확인 | 완료 |
| KSK-002 | 실제 개발 채널 호출 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001, DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실제 개발 채널에서 카카오톡 말풍선 응답 성공 | REQ-KSK-002-01, RES-KSK-002-01, UI-KSK-002-01 | 통과 | 개발 채널 화면과 서버 수신 기록에서 확인 | 완료 |
| KSK-003 | POST 메서드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001, DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 스킬 요청이 HTTP POST로 수신됨 | REQ-003 | 통과 | 서버 요청 로그에서 POST 메서드를 확인함 | 증적 정리 후 공개 가능 여부 재검토 |
| KSK-004 | JSON Content-Type 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001, DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 스킬 요청 JSON 본문 수신 성공 | REQ-004 | 통과 | 서버 로그에서 JSON 요청 본문 파싱 성공을 확인함 | 증적 정리 후 공개 가능 여부 재검토 |
| KSK-005 | 필수 구조 수신 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001, DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | userRequest 필수 구조와 사용자 속성 수신 성공 | REQ-005 | 통과 | 서버 로그에서 userRequest 구조와 관련 속성을 확인함 | 증적 정리 후 공개 가능 여부 재검토 |
| KSK-006 | 사용자 발화 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | userRequest.utterance 수신 성공 | REQ-KSK-006-01 | 통과 | 서버 요청 로그에서 필드 수신 확인 | 완료 |
| KSK-007 | 시간대 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | timezone이 Asia/Seoul로 전달됨 | REQ-KSK-007-01 | 통과 | 서버 요청 로그에서 값 확인 | 완료 |
| KSK-008 | 언어 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | lang이 ko로 전달됨 | REQ-KSK-008-01 | 통과 | 서버 요청 로그에서 값 확인 | 완료 |
| KSK-009 | 사용자 ID 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-PAYLOAD-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | user.id 수신, user.type botUserKey, plusfriendUserKey 수신, isFriend true 확인; appUserId는 현재 환경에서 미제공 | REQ-KSK-009-01, SEC-KSK-009-01 | 제한적 통과 | 개발 채널 요청에서 제공 필드는 확인했으며 appUserId 미제공은 실패로 판정하지 않음 | appUserId 제공 조건은 별도 환경에서 재검토 |
| KSK-010 | 동일 사용자 반복 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-PAYLOAD-001, DOC-BOTTEST-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 동일 사용자의 user.id와 plusfriendUserKey가 각각 3회 반복 요청에서 안정적으로 유지됨 | REQ-KSK-010-01, SEC-KSK-010-01 | 통과 | 원문을 기록하지 않은 비교 로그에서 3회 일치 확인 | 완료 |
| KSK-011 | 다른 사용자 구분 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-PAYLOAD-001, DOC-BOTTEST-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | HOLD — 서로 다른 실제 카카오 사용자 2명이 필요하나 현재 테스트 계정을 확보하지 못함 | 실행 전 | 미실행 | 서로 다른 실제 사용자 2명을 확보하지 못해 실행하지 않음 | 두 번째 실제 카카오 계정 확보 후 검증 |
| KSK-012 | 테스트 도구와 실제 채널 차이 검증 | 관리자센터 테스트 도구와 실제 개발 채널에서 동일 스킬 서버 호출 | 양쪽의 요청 속성·응답·실제 표시 비교 | 실제 호출로 확인 | [POC-VERIFY] | DOC-PAYLOAD-001, DOC-BOTTEST-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 테스트 도구: userType accountId, timezone Asia/Seoul, lang null, propertyKeys 없음, isFriend null, 처리시간 0ms, simpleText·quickReplies 정상. 실제 채널: userType botUserKey, timezone Asia/Seoul, lang ko, botUserKey·isFriend·plusfriendUserKey 계열 속성 키 존재, isFriend true, 처리시간 0ms, 말풍선·quickReplies 3개 정상 표시 | REQ-KSK-012-01, RES-KSK-012-01, UI-KSK-012-01 | 핵심 응답 동작 동일, 사용자 식별 타입·속성 차이 확인 | 통과 | 두 환경 모두 동일 스킬 서버를 정상 호출하고 표시 기능에 문제가 없음을 확인 | 관리자센터 accountId를 실제 채널 botUserKey와 동일 사용자 식별자로 취급하지 않음 |
| KUI-001 | simpleText 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | SkillResponse version 2.0 검증 및 simpleText 실제 말풍선 표시 성공 | RES-KUI-001-01, UI-KUI-001-01 | 통과 | 응답 구조와 실제 개발 채널 화면에서 확인 | 완료 |
| KUI-002 | 500자 경계 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-003 | 1,000자 경계 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-004 | quickReplies 3개 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | quickReplies 3개 표시 성공 | RES-KUI-004-01, UI-KUI-004-01 | 통과 | 실제 개발 채널에서 3개 선택지 표시 확인 | 완료 |
| KUI-005 | 퀵리플라이 메시지 연결 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 그대로 저장·다른 시간 입력·취소 선택값의 재호출 성공 | REQ-KUI-005-01, RES-KUI-005-01, UI-KUI-005-01 | 통과 | 세 선택값 각각의 후속 스킬 호출과 응답 확인 | 완료 |
| KUI-006 | 퀵리플라이 블록 연결 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 관리자센터 봇테스트 블록 연결 성공 | CFG-KUI-006-01, UI-KUI-006-01 | 통과 | 관리자센터 봇테스트에서 블록 연결 동작 확인 | 완료 |
| KUI-007 | clientExtra 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-008 | basicCard 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | BasicCard 제목·설명·줄바꿈 정상 표시 | RES-KUI-008-01, UI-KUI-008-01 | 통과 | 실제 채널에서 카드 구성과 줄바꿈 확인 | 완료 |
| KUI-009 | listCard 2건 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-010 | listCard 5건 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-011 | 후보 6건 재질문 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-012 | 링크 버튼 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | webLink 버튼 표시 및 외부 웹페이지 이동 성공; 사용자 발화와 서버 재호출 없음 | RES-KUI-012-01, UI-KUI-012-01, REQ-KUI-012-01 | 통과 | 외부 이동과 스킬 재호출 비발생 확인 | 완료 |
| KUI-013 | 세로 버튼 3개 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | BasicCard 세로 버튼 3개 표시·클릭 성공 | RES-KUI-013-01, UI-KUI-013-01 | 통과 | 세로 버튼 3개의 표시와 동작 확인 | 완료 |
| KUI-014 | 절대 날짜·요일·시간 가독성 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-015 | 잘못된 응답 JSON 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | HTTP 200으로 잘못된 JSON 반환; 정상 말풍선 및 깨진 원문 미표시; 관리자센터 오류 내역 없음 | RES-KUI-015-01, UI-KUI-015-01, ERR-KUI-015-01 | 제한적 통과 | 사용자 화면 안전성은 확인했으나 관리자센터 오류 증적 없음 | 오류 기록 조건 추가 확인 |
| KUI-016 | 출력 3개 경계 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KUI-017 | 퀵리플라이 10개 경계 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서만으로 확정 가능 + 실제 표시 별도 검증 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-RESPONSE-001, DOC-UI-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | quickReplies 10개 표시; 10번째 항목 접근·클릭·발화 재전송 성공 | RES-KUI-017-01, UI-KUI-017-01, REQ-KUI-017-01 | 통과 | 최대 10개 표시와 마지막 항목의 후속 호출 확인 | 완료 |
| KUI-018 | 최대 응답 크기 탐색 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-ERROR-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 해당 문서·프로젝트 기준 | ASCII simpleText 30000·31000·40000·60000자 모두 실제 표시 및 전체보기 제공; 관리자센터 오류 내역 없음; 공식 30720바이트의 플랫폼 계산값과 서버 text UTF-8 바이트가 동일하지 않음 | RES-KUI-018-01, UI-KUI-018-01, ERR-KUI-018-01 | 제한적 통과 | 네 크기의 표시는 확인했으나 정확한 차단 경계 미확인 | 플랫폼 계산 기준 추가 탐색 |
| KLC-001 | 정상 코드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 8자리 코드 발급, 유효시간 300초, 정상 입력 후 계정 연결 성공; 상태는 PoC 메모리에 저장되고 서버 재시작 시 초기화됨 | REQ-KLC-001-01, RES-KLC-001-01, SEC-KLC-001-01 | 통과 | 발급 응답·연결 상태·재시작 전후 서버 로그에서 확인 | PoC 메모리 방식은 운영 저장소 설계 시 교체 |
| KLC-002 | 만료 코드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 유효시간 300초 경과 후 만료 코드 거부 성공 | REQ-KLC-002-01, RES-KLC-002-01, TIM-KLC-002-01 | 통과 | 서버 응답과 처리 로그에서 만료 거부 확인 | 완료 |
| KLC-003 | 이미 사용된 코드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 정상 사용 완료 코드의 재사용 차단 성공 | REQ-KLC-003-01, RES-KLC-003-01 | 통과 | 동일 코드 재입력에 대한 거부 응답 확인 | 완료 |
| KLC-004 | 재발급으로 무효화된 코드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 재발급 후 이전 코드 무효화 및 연결 거부 성공 | REQ-KLC-004-01, RES-KLC-004-01 | 통과 | 재발급 전 코드 입력 시 무효화 응답 확인 | 완료 |
| KLC-005 | 오입력 1회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 첫 오입력 후 남은 시도 4 확인 | RES-KLC-005-01, SEC-KLC-005-01 | 통과 | 민감값 없는 검증 로그에서 확인 | 완료 |
| KLC-006 | 오입력 4회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 연속 오입력에서 남은 시도 3→2→1 확인 | RES-KLC-006-01, SEC-KLC-006-01 | 통과 | 민감값 없는 검증 로그에서 순차 감소 확인 | 완료 |
| KLC-007 | 오입력 5회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 5번째 오입력에서 남은 시도 0, CODE_ATTEMPTS_EXCEEDED, 활성 코드 무효화 확인; 이후 원래 정상 코드도 연결 실패 | RES-KLC-007-01, SEC-KLC-007-01 | 통과 | 민감값 없는 결과·남은 횟수·무효화 로그와 후속 연결 실패 확인 | 완료 |
| KLC-008 | 다른 테스트 사용자의 코드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KLC-009 | 동일 식별값 재연결 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 동일 계정의 기존 연결 상태를 안내하고 기존 연결 유지·코드 미소비 | 동일 카카오 사용자가 CalTalk 계정 A와 연결된 상태에서 같은 계정 A의 새 코드를 입력하자 ALREADY_CONNECTED로 거부되고 기존 연결 A 유지·코드 미소비·미무효화 확인; 연결 해제 후 동일 코드로 연결 성공, 최종 연결 계정 A 유지 | REQ-KLC-009-01, RES-KLC-009-01, SEC-KLC-009-01 | 통과 | 동일 계정도 자동 갱신하지 않으며 거부 코드를 보존하고 명시적 해제 후 동일 코드 사용 가능함을 민감정보 원문 없이 확인 | 완료 |
| KLC-010 | 다른 사용자와 이미 연결된 식별값 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 다른 계정 코드 입력을 거부하고 기존 연결 유지·코드 미소비·명시적 해제 안내 | 기연결 상태에서 다른 계정 코드 입력 시 ALREADY_CONNECTED, 기존 연결 유지 및 코드 미소비 확인; 기존 연결 해제 후 동일 코드로 새 계정 연결 성공 | REQ-KLC-010-01, RES-KLC-010-01, SEC-KLC-010-01 | 통과 | 자동 덮어쓰기 차단, 거부 코드 보존, 명시적 해제 후 정상 사용을 응답·연결 상태에서 확인 | 완료 |
| KLC-011 | 사용자당 활성 코드 1개 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 재발급 시 기존 활성 코드 무효화 성공 | REQ-KLC-011-01, RES-KLC-011-01 | 통과 | 재발급 후 이전 활성 코드 거부 확인 | 완료 |
| KLC-012 | 10분당 발급 3회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 동일 사용자에게 10분 내 3회까지 코드 발급 성공 | RES-KLC-012-01, TIM-KLC-012-01 | 통과 | 발급 횟수와 제한 시간창을 서버 응답에서 확인 | 완료 |
| KLC-013 | 4번째 발급 차단 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 10분 내 추가 발급에서 ISSUE_RATE_LIMITED와 retryAfterSeconds 확인 | RES-KLC-013-01, TIM-KLC-013-01 | 통과 | 제한 응답의 결과와 재시도 대기 필드 확인 | 완료 |
| KLC-014 | 연결 해제 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 연결 해제 성공 | REQ-KLC-014-01, RES-KLC-014-01 | 통과 | 해제 응답과 서버 상태에서 확인 | 완료 |
| KLC-015 | 해제 후 접근 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 연결 해제 후 미연결 상태 확인 성공 | REQ-KLC-015-01, RES-KLC-015-01 | 통과 | 해제 후 상태 조회 응답에서 확인 | 완료 |
| KLC-016 | 연결 후 기존 대기 상태 폐기 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KLC-017 | 해제 후 기존 확인 상태 폐기 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KSE-001 | 정상 사용자 정의 헤더 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 관리자센터 확인 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-AUTH-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 테스트 헤더 전달 성공; 사용자 설정 공유값이며 카카오 암호학적 서명이 아님 | CFG-KSE-001-01, SEC-KSE-001-01 | 제한적 통과 | 개발 채널 요청에서 전달은 확인했으나 요청 진위를 보장하는 서명으로 표현할 수 없음 | 추가 요청 진위 방어 수단 검토 |
| KSE-002 | 헤더 누락 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 관리자센터 확인 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-AUTH-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 헤더 누락·잘못된 헤더 HTTP 401; 올바른 헤더와 실제 개발 채널 요청 정상 처리 | REQ-KSE-002-01, RES-KSE-002-01, SEC-KSE-002-01 | 통과 | 헤더 검증 실패·성공 경로 확인 | 완료 |
| KSE-003 | 잘못된 헤더 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 관리자센터 확인 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-AUTH-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 테스트 헤더 값 변경 후 개발 채널 요청 반영 성공 | CFG-KSE-003-01, SEC-KSE-003-01 | 통과 | 헤더 원문 없이 변경값 검증 결과를 서버 로그에서 확인 | 완료 |
| KSE-004 | 이전 헤더 값 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 관리자센터 확인 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-AUTH-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 테스트 헤더 원상 복구 후 개발 채널 요청 성공 | CFG-KSE-004-01, SEC-KSE-004-01 | 통과 | 헤더 원문 없이 복구 후 정상 검증 결과 확인 | 완료 |
| KSE-005 | 잘못된 메서드 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | GET 스킬 요청 HTTP 404; 서버와 health 정상 유지 | REQ-KSE-005-01, RES-KSE-005-01 | 통과 | 허용하지 않은 메서드 거부와 서버 건전성 확인 | 완료 |
| KSE-006 | 잘못된 Content-Type 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | text/plain HTTP 415; application/json HTTP 200 정상 처리 | REQ-KSE-006-01, RES-KSE-006-01 | 통과 | Content-Type 거부·허용 경계 확인 | 완료 |
| KSE-007 | 과대 본문 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 1MiB 초과 요청 본문 감지; “요청 크기가 너무 큽니다.” 안전 응답; 서버 정상 유지 | REQ-KSE-007-01, RES-KSE-007-01 | 통과 | 본문 제한 적용과 서버 건전성 확인 | 완료 |
| KSE-008 | 필수 필드 누락 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | userRequest.user.id 누락 시 예외 없이 “사용자 식별 정보를 확인할 수 없습니다.” 안전 응답 | REQ-KSE-008-01, RES-KSE-008-01 | 통과 | 필수 식별정보 누락의 안전 처리 확인 | 완료 |
| KSE-009 | 요청 빈도 초과 검증 | 실제 카카오톡 개발 채널에서 동일 사용자가 일반 비변경 발화를 요청 사이 1초 미만으로 6회 전송하고 10초 이상 대기한 뒤 다음 요청 전송; 단일 Node.js 인스턴스, 사용자별 메모리 Map 기반 10초 고정 시간창, 5회 허용, 6회차부터 제한 | 동일 사용자의 허용 한도·초과 차단·시간창 후 복구·서버 건전성·로그 비노출과 사용자별 독립 제한 정책 검증 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | HMAC 처리된 사용자 키를 Map 식별자로 사용하고 원문은 저장하지 않음; 서버 재시작 시 제한 상태 초기화 | 1~5회차 일반 정상 응답; 6회차 HTTP 200 정상 Kakao SkillResponse로 “요청이 너무 많습니다. 잠시 후 다시 시도해주세요.” 안전 안내 표시; 10초 이상 대기 후 다음 요청은 제한 로그 없이 일반 스킬 응답으로 정상 복구; 서버 프로세스·GET /health 정상; activeCodes 0·linkedUsers 0; 오류·타임아웃·응답 누락 없음 | REQ-KSE-009-01~07, RES-KSE-009-01~07, TIM-KSE-009-01, SEC-KSE-009-01, UI-KSE-009-01; 제한 로그는 rateLimitExceeded true, rateLimitCategory KAKAO_SKILL_USER, requestCount 6, limit 5, windowSeconds 10, retryAfterSeconds 5이며 발화·사용자 식별값·HMAC 키·헤더/토큰 원문 또는 일부와 전체 req.body·req.headers·process.env는 미출력 | 제한적 통과 | 동일 사용자에 대한 5회 허용, 6회차 제한, 10초 후 복구, 서버 생존과 로그 비노출을 확인했으나 두 번째 실제 사용자가 없어 사용자별 독립 제한을 실제 검증하지 못함 | 단일 Node.js 인스턴스의 메모리 기반 제한만 검증했으며 다중 서버·Redis·분산 환경 제한은 미검증이다. 서버 재시작 시 제한 상태가 초기화된다 |
| KSE-010 | 로그 식별값 마스킹 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 사용자 식별값 원문이 서버 로그에서 마스킹됨 | SEC-KSE-010-01 | 통과 | 서버 로그에서 원문 비노출 확인 | 완료 |
| KSE-011 | 로그 발화 원문 비저장 검증 | 수정 전 요청 수신 로그의 userRequest.utterance 원문 출력 제거 후 실제 카카오톡 개발 채널에서 재검증 | 발화 원문·일부·마스킹 문자열·해시를 기록하지 않고 최소 메타데이터와 기존 민감정보 마스킹만 유지 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 발화 원문 비저장, 사용자 식별값·헤더값·요청 ID 후보 마스킹 유지, 기존 응답 기능 유지 | 실제 개발 채널 요청 정상; utterancePresent true, utteranceLength 17, utteranceUtf8Bytes 25, 서버 내부 elapsedMs 1ms; 발화 원문 미출력; 사용자 식별값·헤더값·요청 ID 후보 마스킹 유지; 오류·타임아웃 없음; 서버 정상 유지 | REQ-KSE-011-01, RES-KSE-011-01, SEC-KSE-011-01 | 통과 | 발화 원문이 서버 로그에 남지 않고 운영에 필요한 존재 여부·문자 수·UTF-8 바이트 수만 기록되며 응답 기능과 기존 스킬 동작이 유지됨 | 기본 로그에 발화 원문·일부·마스킹 문자열·해시를 추가하지 않는 정책 유지 |
| KSE-012 | 코드 원문 비저장 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 연결 코드 원문이 서버 로그에서 마스킹됨 | SEC-KSE-012-01 | 통과 | 서버 로그에서 원문 비노출 확인 | 완료 |
| KSE-013 | 헤더·토큰 비저장 검증 | 수정 전 testHeaderMasked가 테스트 헤더 원문의 앞·뒤 일부를 남기는 문제를 확인하고 해당 로그 필드 제거 후 재검증 | 스킬 헤더·관리자 토큰의 정상·오류·누락 분기와 로그·코드의 원문·부분 원문·전체 헤더 비저장 확인 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 헤더·토큰 원문·부분 원문·해시와 전체 req.headers 비저장, 기존 기능·마스킹 유지 | testHeaderMasked 제거, testHeaderReceived·testHeaderLength 유지, 사용자 식별값·요청 ID 후보 마스킹 유지; 스킬 헤더 정상 요청 성공, 잘못된 값·누락 요청 HTTP 401, 복구 후 실제 카카오톡 요청 정상; 관리자 토큰 누락·잘못된 값 HTTP 401, 정상 값 HTTP 201; 인증값 원문·부분 원문·해시와 전체 req.headers 미출력; request body 전체와 process.env 전체를 console.log 또는 console.error로 출력하는 코드가 없고 인증 실패·예외 처리에서도 request body 전체나 환경변수 전체를 로그에 남기지 않음; 정상 관리자 호출의 연결 코드 원문은 응답으로만 반환되고 로그에는 미출력; 파일·DB 저장 로직 없음, 콘솔 로그만 사용; 서버 재시작 후 activeCodes 0·linkedUsers 0 복구; 오류·타임아웃 없음 | REQ-KSE-013-01, RES-KSE-013-01, SEC-KSE-013-01 | 통과 | 스킬 헤더와 관리자 토큰의 정상·오류·누락 분기가 정상 동작하고 서버 로그와 코드에 인증값 원문·부분 원문·전체 헤더가 저장되지 않음을 확인함 | 외부 PowerShell 또는 운영 환경의 콘솔 보존 정책은 PoC 코드 감사 범위 밖으로 관리 |
| KSE-014 | 외부 오류 일반화 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 내부 오류를 카카오 규격의 안전한 응답으로 변환하고 내부 오류명 비노출 | RES-KSE-014-01, UI-KSE-014-01, SEC-KSE-014-01 | 통과 | 실제 응답과 사용자 화면에서 안전 문구 및 오류명 비노출 확인 | 완료 |
| KPF-001 | 고정 응답 10회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 고정 응답 10회 연속 성공; 서버 처리시간 0~1ms; 타임아웃·오류 0건; 사용자 식별값 10회 동일 | REQ-KPF-001-01, RES-KPF-001-01, TIM-KPF-001-01, SEC-KPF-001-01 | 통과 | 10회 모두 안정적으로 동일 사용자의 정상 응답 확인 | 완료 |
| KPF-002 | 연결 상태 10회 검증 | 실제 카카오톡 개발 채널, 동일 사용자, 서버 재시작 후 linkedUsers 0인 미연결 상태 유지, server.js·관리자센터 설정 변경 없음 | 연결 상태 조회용 발화를 10회 반복해 서버 도달·응답·처리시간·상태 일관성 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 연결 상태 시나리오 목표 서버 시간 800ms 이하, 실제 화면 5초 이내 | 10회 모두 서버 도달 및 실제 카카오톡 말풍선 정상 표시; 10회 모두 동일한 미연결 안내; 서버 내부 elapsedMs 0ms 8회·1ms 2회, 최대 1ms; 오류·타임아웃·응답 누락 없음; 테스트 중 서버 재시작·연결·연결 해제 없음; 사용자 유형·마스킹된 사용자 식별값·시간대·언어가 일관되고 테스트 헤더 10회 모두 정상 수신 | REQ-KPF-002-01, RES-KPF-002-01, TIM-KPF-002-01, UI-KPF-002-01, SEC-KPF-002-01 | 통과 | 10회 모두 800ms 목표와 실제 화면 5초 기준을 충족함. elapsedMs는 전체 네트워크 왕복시간이 아닌 서버 내부 측정값이므로 실제 말풍선 표시와 함께 판정했으며 이번 결과는 미연결 상태 조회 경로의 안정성에 한정됨 | 연결 사용자 상태 조회 경로는 필요 시 별도 테스트 |
| KPF-003 | 메모리 조회 10회 검증 | 비변경 메모리 조회 대상, 사용자 상태, 정확한 발화·응답과 판정 기준 확정 및 필요 시 최소 전용 분기 구현 | 단순 모의 메모리 조회 10회의 응답시간 안정성 검증 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 목표 서버 시간 1초 이하, 내부 타임아웃 1.5초, 일반 스킬 5초 제한과 목표 4초 기준 | HOLD — 정확한 조회 대상·사용자 상태·발화·응답과 KPF-003 전용 통과·제한적 통과·실패 기준이 정의되지 않았고 server.js에 전용 발화·분기·응답이 없음. getConnectionStatus()는 KPF-002 범위이며 GET /health도 대체 증적 근거가 없어 임의 발화나 기존 경로를 사용하면 테스트 목적이 왜곡됨 | 실행 전 | 미실행 | 비변경 메모리 조회 대상, 정확한 발화, 응답 및 판정 기준이 정의되지 않았고 전용 구현 경로도 없어 현재 환경에서 검증할 수 없음 | 조회 대상·발화·응답과 통과·제한적 통과·실패 기준을 확정하고 필요 시 최소 전용 분기를 구현한 뒤 10회 반복 검증 |
| KPF-004 | 일정 조회 모의 10회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KPF-005 | OpenAI 구조화 10회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KPF-006 | DB 조회+OpenAI 모의 10회 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KPF-007 | 의도적 3초 지연 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 3초 지연 응답 실제 말풍선 표시 성공; 약 3014ms | TIM-KPF-007-01, UI-KPF-007-01 | 통과 | 의도한 지연 후 실제 채널 표시 성공 | 완료 |
| KPF-008 | 의도적 4초 지연 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 4초 지연 응답 실제 말풍선 표시 성공; 약 4007ms | TIM-KPF-008-01, UI-KPF-008-01 | 통과 | 의도한 지연 후 실제 채널 표시 성공 | 완료 |
| KPF-009 | 5초 초과 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [POC-VERIFY] | DOC-SKILL-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/make_skill | 관련 문서 절 | 해당 문서·프로젝트 기준 | 6초 지연 응답이 실제 카카오톡에 표시되지 않았고 서버 응답 시도는 약 6007ms; 관리자센터 오류 내역 기록 없음 | TIM-KPF-009-01, UI-KPF-009-01, ERR-KPF-009-01 | 제한적 통과 | 사용자 말풍선 미표시는 확인했으나 관리자센터 오류 내역으로 원인을 교차 확인하지 못함 | 관리자센터 오류 기록 조건 추가 확인 |
| KPF-010 | OpenAI 타임아웃 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [PROJECT-DECISION] | DOC-ERROR-001 + [PROJECT-DECISION] | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KPF-011 | 서버 예외 검증 | 실제 카카오톡 개발 채널에서 전용 테스트 요청을 사용하고 기존 요청 처리 try/catch 범위 안에서 서버 종료를 유발하지 않는 비민감 고정 내부 예외를 동기 방식으로 1회 발생 | 내부 오류 원문 없이 일반 안전 안내를 반환하고 서버 생존·후속 요청·로그 비노출 확인 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [PROJECT-DECISION] | DOC-ERROR-001 + [PROJECT-DECISION] | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 5초 이내 안전 응답, 내부 정보 비노출, 서버 프로세스와 후속 처리 정상 유지 | 의도적 예외 정상 발생; 5초 이내 “요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.” 안전 안내 표시; 내부 오류 메시지·Error·스택·파일 경로·내부 함수명과 헤더·토큰·사용자 식별값·터널 정보 미노출; 로그에는 errorOccurred true, errorCategory KPF011_TEST, 오류 처리 elapsedMs 2ms만 기록되고 전체 Error·스택·발화 원문·request body·req.headers·환경변수 및 민감정보 원문 미출력; 기존 사용자·요청 ID 마스킹 유지; 서버 프로세스 유지; 직후 일반 요청 말풍선과 quickReplies 3개 정상 표시; GET /health 정상; activeCodes 0·linkedUsers 0; 오류·타임아웃·응답 누락 없음; 국소 테스트 분기 외 기존 기능 변경 없음 | REQ-KPF-011-01, RES-KPF-011-01, TIM-KPF-011-01, UI-KPF-011-01, SEC-KPF-011-01 | 통과 | 기존 예외 처리 경로가 내부 정보 없이 안전 응답을 반환하고 서버 생존·후속 처리·health 정상과 로그 비노출 정책을 모두 충족함 | 전용 테스트 분기는 실제 검증 용도로만 관리 |
| KPF-012 | 4xx 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [PROJECT-DECISION] | DOC-ERROR-001 + [PROJECT-DECISION] | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 해당 문서·프로젝트 기준 | HTTP 400; 정상 말풍선 및 내부 오류 원문 미표시; 관리자센터 오류 내역 없음 | RES-KPF-012-01, UI-KPF-012-01, ERR-KPF-012-01 | 제한적 통과 | 사용자 화면 안전성은 확인했으나 관리자센터 오류 증적 없음 | 오류 기록 조건 추가 확인 |
| KPF-013 | 5xx 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [PROJECT-DECISION] | DOC-ERROR-001 + [PROJECT-DECISION] | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 해당 문서·프로젝트 기준 | HTTP 500; 정상 말풍선 및 내부 오류 원문 미표시; 관리자센터 오류 내역 없음 | RES-KPF-013-01, UI-KPF-013-01, ERR-KPF-013-01 | 제한적 통과 | 사용자 화면 안전성은 확인했으나 관리자센터 오류 증적 없음 | 오류 기록 조건 추가 확인 |
| KPF-014 | 네트워크 단절 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [PROJECT-DECISION] | DOC-ERROR-001 + [PROJECT-DECISION] | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 해당 문서·프로젝트 기준 | 터널 단절 시 실제 말풍선 미표시; 내부 URL·오류 원문 미노출; 관리자센터 오류 내역 없음; 복구 후 정상 응답 | UI-KPF-014-01, ERR-KPF-014-01, RES-KPF-014-01 | 제한적 통과 | 장애·복구와 비노출은 확인했으나 관리자센터 오류 증적 없음 | 오류 기록 조건 추가 확인 |
| KPF-015 | 지연 결과 폐기 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 문서 확인 + 실제 호출 필요 | [OFFICIAL-CONFIRMED] + [PROJECT-DECISION] | DOC-ERROR-001 + [PROJECT-DECISION] | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/check_skill_error_history | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-001 | 미연결 사용자 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-002 | 연결 사용자 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 계정 연결 후 연결 상태 확인 성공 | REQ-KDM-002-01, RES-KDM-002-01 | 통과 | 연결 상태 조회 응답과 서버 상태에서 확인 | 완료 |
| KDM-003 | 지원하지 않는 명령 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-004 | 과거 일정 생성 요청 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-005 | 과거 시각으로 수정 요청 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-006 | 모호한 날짜·시간 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-007 | 재질문 대기 10분 만료 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-008 | 확인 5분 만료 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-009 | 후보 선택 0건 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-010 | 후보 선택 1건 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-011 | 후보 선택 2~5건 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-012 | 후보 선택 6건 이상 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KDM-013 | 동일 확인 반복 실행 모의 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 프로젝트 결정 + 실제 호출 필요 | [PROJECT-DECISION] | [PROJECT-DECISION] | 해당 없음 | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KID-001 | 동일 발화 연속 전송 검증 | 관리자센터 스킬 테스트 도구에서 동일 JSON과 발화를 5회 연속 전송 | 각 요청의 도달·응답·처리시간과 ID 후보 비교 | 실제 호출로 확인 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 5회 모두 서버 도달·정상 응답, 처리시간 0ms, 오류·타임아웃 없음. userType·사용자 식별값·발화는 동일하고 headerXRequestId는 매회 다름. intentId·actionId·blockId·cloudflareRay는 5회 동일 | REQ-KID-001-01~05, RES-KID-001-01~05, TIM-KID-001-01~05 | 일반 반복 호출은 별도 요청으로 처리되고 x-request-id가 요청별 구분 후보로 관찰됨 | 제한적 통과 | 반복 호출의 요청별 ID 차이와 응답 안정성은 확인했으나 플랫폼 재전송 시 식별자 유지 여부는 미확인 | intent.id·action.id·block.id·cloudflareRay를 단독 멱등성 키로 사용하지 않고, KID-001만으로 완전한 멱등성·중복 처리 정책을 확정하지 않음 |
| KID-002 | 동일 버튼 반복 클릭 검증 | 실제 카카오톡 개발 채널, 동일한 미연결 사용자, 기존 퀵리플라이 사용, server.js·관리자센터 설정 변경 없음 | 준비용 일반 발화 후 표시된 동일 저장 퀵리플라이 버튼 클릭 절차를 총 5회 반복해 요청 구분·응답 안정성·ID 후보 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 반복 요청의 고유 ID 의미와 안정성 확인 | 준비 발화 5회와 버튼 발화 5회 등 총 10회 모두 서버 도달; 버튼 클릭 5회 모두 사용자 발화로 정상 전송되고 카카오톡 말풍선 정상 표시; 오류·타임아웃·응답 누락 없음. 버튼 요청 5회는 utterancePresent true, utteranceLength 6, utteranceUtf8Bytes 16, userType botUserKey, 동일한 마스킹 사용자 식별값, testHeaderReceived true, testHeaderLength 44로 일관됨. headerXRequestId는 5회 모두 서로 다르고 cloudflareRay·intentId·botId·actionId·blockId는 동일함 | REQ-KID-002-01~05, RES-KID-002-01~05, UI-KID-002-01~05, SEC-KID-002-01 | 제한적 통과 | 동일 버튼 5회 반복 클릭의 요청 구분과 응답 안정성은 확인했으나 플랫폼 자동 재시도·동일 요청 재전송 시 요청 식별자 유지 여부는 미확인 | 각 클릭은 별도 요청으로 처리되며 x-request-id는 일반 버튼 반복 요청의 구분 후보임. intent.id·action.id·block.id·bot.id는 요청별 고유 ID가 아니고 cloudflareRay도 단독 멱등성 키로 사용하지 않음. KID-002는 사용자가 동일 버튼을 반복 클릭해 생성한 신규 요청의 안정성만 검증하므로 단독 결과만으로 서비스의 완전한 멱등성·중복 처리 정책을 확정하지 않으며 다른 KID 계열 테스트 결과와 함께 판단함 |
| KID-003 | 관리자센터 재전송 검증 | 관리자센터 스킬 테스트 화면에서 기존 요청 대상 재전송 기능 확인 | 원 요청과 재전송 요청의 요청 ID·페이로드 유지 여부 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 관리자센터에서 기존 요청을 대상으로 하는 명시적 재전송 기능 필요 | `클립보드로 복사`, `스킬서버로 전송`, `응답 초기화` 버튼은 확인했으나 명시적 재전송·다시 요청·요청 다시 보내기 기능은 확인하지 못함. `스킬서버로 전송` 재클릭과 동일 JSON의 PowerShell·curl 전송은 새 요청이므로 KID-001 범위이며 KID-003 증적이 아님 | CFG-KID-003-01 | 미실행 | HOLD — 기존 요청 재전송 기능을 확인할 수 없어 원 요청과 재전송 요청 사이의 요청 ID·페이로드 유지 여부를 검증할 수 없음 | 관리자센터에 원 요청 대상 재전송 기능이 확인될 때 재검증 |
| KID-004 | 실제 채널 반복 검증 | 실제 카카오톡 개발 채널, 동일한 미연결 사용자, server.js·관리자센터·PowerShell 설정 변경 없음 | 상태를 변경하지 않는 동일 일반 텍스트 발화를 약 1~2초 간격으로 정확히 5회 전송해 botUserKey 기반 실제 채널 요청의 ID·페이로드 안정성 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 통과: 5회 모두 서버 도달·정상 말풍선 표시·오류·타임아웃·응답 누락 없음, 동일 발화 메타데이터와 사용자·시간대·언어·친구 상태 일관, headerXRequestId로 요청별 구분 가능, 블록·intent·action·bot ID 유지·변경 양상 설명 및 KID-001과 비교 가능. 제한적 통과: 5회 정상이나 요청별 고유 ID의 공식 의미 또는 자동 재시도 시 headerXRequestId 유지 여부를 확인하지 못하고 완전한 멱등성·중복 처리 정책을 확정하지 못함. 실패·재검증: 요청 누락, 말풍선 미표시, 비정상 응답 차이, 서버 오류·타임아웃, 설명 없는 사용자·시간대·언어 변화, 발화 또는 민감정보 원문 로그 노출 | 5회 모두 서버 도달·정상 카카오톡 말풍선 표시, 오류·타임아웃·응답 누락 없음. 서버 처리시간 0ms 4회·1ms 1회, 최대 1ms. utterancePresent true, utteranceLength 13, utteranceUtf8Bytes 21, timezone Asia/Seoul, lang ko, userType botUserKey, 동일한 마스킹 사용자 식별값·propertyKeys, isFriend true, testHeaderReceived true, testHeaderLength 44가 5회 일관됨. 동일 발화 5회는 서버에 5개의 개별 요청으로 도달했고 headerXRequestId가 5회 모두 달라 요청별로 구분됐으므로, 동일 요청 재사용이 아니라 각각 새로 생성된 별도 요청으로 처리됨. cloudflareRay·intentId·botId·actionId·blockId는 동일함 | REQ-KID-004-01~05, RES-KID-004-01~05, TIM-KID-004-01~05, UI-KID-004-01~05, SEC-KID-004-01 | 제한적 통과 | 실제 채널 일반 텍스트 반복 요청의 안정성과 요청별 ID 차이는 확인했으나 이 결과는 플랫폼 자동 재시도·원 요청 재전송 시 식별자 유지 여부를 증명하지 않음 | intent.id·action.id·block.id·bot.id는 요청별 고유 ID가 아니고 cloudflareRay도 단독 멱등성 키로 사용하지 않음. KID-001의 accountId 기반 관리자센터 반복과 달리 botUserKey 기반 실제 채널 동작을 확인했으나 KID-004 단독으로 완전한 멱등성·중복 처리 정책을 확정하지 않으며 원상 복구는 불필요 |
| KID-005 | 요청별 ID 후보 비교 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 서로 다른 요청 3회에서 X-Request-Id가 존재하고 각각 달랐음; bodyRequestId·userRequestRequestId·X-Correlation-Id·traceparent는 미확인, Cloudflare Ray는 카카오 원 요청 ID가 아님 | REQ-KID-005-01, REQ-KID-005-02, REQ-KID-005-03 | 제한적 통과 | 요청 상관관계 후보는 확인했으나 재전송 안정성은 미검증 | X-Request-Id 단독 멱등성 키 사용 결정 보류 |
| KID-006 | flow.trigger 비교 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KID-007 | 동일 clientExtra 반복 검증 | 해당 시나리오 준비 | 문서 기준과 실제 결과 비교 | 실제 호출만으로 확인 가능 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 해당 문서·프로젝트 기준 | 실행 전 | 실행 전 | 미실행 | 실행 전이므로 판정하지 않음 | 실행 결과에 따라 기록 |
| KID-008 | 원 요청 ID 미제공 시 보조 중복 판정 검증 | 실제 카카오톡 개발 채널, 동일 사용자, 단일 Node.js 인스턴스, 메모리 Map 기반 HMAC 보조 지문, 중복 시간창 3초, 모의 상태 변경 카운터 | 재시도 안정성이 확인된 원 요청 ID가 없을 때 짧은 시간의 동일 상태 변경 요청을 보조 지문으로 중복 판정해 재실행을 방지하는 PoC 검증 | 실제 호출로 확인 | [POC-VERIFY] | DOC-IDEMP-001 | https://kakaobusiness.gitbook.io/main/tool/chatbot/skill_guide/answer_json_format | 관련 문서 절 | 최초 요청 상태 변경 1회, 3초 이내 동일 지문 요청 변경 미적용, 3초 이후 동일 요청 새 처리; 공식 원 요청 식별자의 대체가 아닌 PoC 방어 수단 | 최초 요청은 HTTP 200 정상 SkillResponse의 “요청 처리가 완료되었습니다.” 말풍선, duplicateDetected false, duplicateCategory KAKAO_SKILL_FINGERPRINT, dedupeWindowSeconds 3, stateMutationApplied true, mockMutationCount 1, elapsedMs 3. 3초 이내 두 번째 요청은 HTTP 200의 “이미 처리된 요청입니다.” 말풍선, duplicateDetected true, 같은 category/window, stateMutationApplied false, mockMutationCount 1, elapsedMs 1. 3초 이후 세 번째 요청은 HTTP 200 처리 완료 말풍선, duplicateDetected false, 같은 category/window, stateMutationApplied true, mockMutationCount 2, elapsedMs 1. 모든 말풍선 정상 표시, 서버·GET /health 정상, activeCodes 0·linkedUsers 0 | REQ-KID-008-01~03, RES-KID-008-01~03, TIM-KID-008-01~03, UI-KID-008-01~03, SEC-KID-008-01; 세 요청의 headerXRequestId는 모두 다르고 사용자·시간대·언어·친구 상태·블록 관련 정보는 일관됨. 발화·정규화 발화·지문 전체/일부·HMAC 키·사용자 식별값·clientExtra 전체·req.body/req.headers/process.env 전체·헤더/토큰/연결 코드 원문은 미출력 | 제한적 통과 | 최초 적용, 3초 이내 중복 미적용, 3초 이후 재적용과 mockMutationCount 1→1→2를 확인했고 x-request-id가 달라도 보조 지문으로 중복 상태 변경을 방지함. 다만 실제 플랫폼 자동 재시도 대신 사용자가 새로 보낸 동일 요청과 모의 상태 변경만 검증했으며 실제 일정 CRUD·DB 트랜잭션, 두 번째 실제 사용자 독립성, 다중 서버·Redis·분산 중복 방지는 미검증임 | 단일 인스턴스 메모리 Map 검증에 한정하며 서버 재시작 시 중복 Map과 카운터가 초기화됨 |
### 18.1 실제 PoC 수행 결과 요약

- 호출·응답: 공중망 HTTPS 호출, 관리자센터 스킬 테스트, SkillResponse 2.0, simpleText, 봇테스트 블록 연결, 실제 개발 채널 응답을 확인했다.
- 요청 속성: utterance, user.id, botUserKey, plusfriendUserKey, Asia/Seoul, ko, isFriend=true를 확인했다. appUserId는 현재 환경에서 제공되지 않았으며 실패로 판정하지 않는다.
- 반복 안정성: 동일 사용자의 user.id와 plusfriendUserKey가 각각 3회 안정적이었다.
- quickReplies: 3개 표시와 각 선택값의 재호출을 확인했다.
- 5초 초과: 약 6007ms의 서버 응답 시도는 확인됐으나 실제 카카오톡 말풍선은 표시되지 않았고 관리자센터 오류 내역에도 기록되지 않아 제한적 통과로 판정했다.
- 오류 처리: 내부 오류를 카카오 규격의 안전한 응답으로 변환했으며 내부 오류명은 사용자에게 노출되지 않았다.
- 요청 진위: 테스트 헤더 전달, 값 변경 반영, 원상 복구를 확인했다. 이는 사용자 설정 공유값이며 카카오의 암호학적 서명이 아니다.
- 요청 상관관계: X-Request-Id는 존재했고 서로 다른 요청 3회에서 각각 달랐다. 재전송 안정성은 미검증이므로 요청 상관관계에만 사용할 수 있고 단독 멱등성 키 사용은 보류한다. bodyRequestId, userRequestRequestId, X-Correlation-Id, traceparent는 확인되지 않았으며 Cloudflare Ray는 카카오 원 요청 ID가 아니다.
- 연결 코드: 8자리 발급, 300초 유효, 정상 연결·상태 확인·재사용 차단·해제·해제 후 미연결·만료 거부·재발급 시 이전 코드 무효화를 확인했다. 동일 계정 재연결도 ALREADY_CONNECTED로 자동 갱신을 거부해 기존 연결을 유지하며, 거부 코드는 소비·무효화되지 않아 연결 해제 후 동일 코드로 다시 연결할 수 있음을 확인했다.
- 테스트 집계: 통과 50, 제한적 통과 14, 미실행 33, 전체 97.
- 오입력·발급 제한: 남은 시도 4→3→2→1→0, 5번째 실패의 CODE_ATTEMPTS_EXCEEDED, 활성 코드 무효화, 사용자당 10분 3회 제한, ISSUE_RATE_LIMITED와 retryAfterSeconds를 확인했다.
- 보안·상태: 사용자 식별값과 연결 코드 원문은 서버 로그에서 마스킹됐다. 연결 코드와 연결 상태는 PoC 메모리에만 저장되며 서버 재시작 시 초기화된다.
- 증적은 ID만 기록하며 실제 파일명·원본 경로·식별값·코드·토큰·URL은 기록하지 않는다. 공개 가능 여부는 증적 정리 후 재검토한다.
19. 결과 기록 템플릿
요청·응답 원문과 실제 URL·헤더값·사용자 키·연결 코드는 본문에 붙이지 않고 증적 ID로 연결한다.

| 항목 | 기록값 |
|---|---|
| 검증 ID | |
| 검증 항목명 | |
| 근거 상태 태그 | |
| 검증 유형 | |
| 공식 문서 ID | |
| 공식 문서 URL | |
| 관련 공식 절 | |
| 공식 기준값 | |
| 테스트 환경 | |
| 입력 발화 | 민감정보 없는 요약 |
| 요청 JSON 증적 ID | |
| 응답 JSON 증적 ID | |
| 응답 HTTP 상태 | |
| 서버 처리시간 증적 ID | |
| 카카오 화면 증적 ID | |
| 관리자센터 오류 증적 ID | |
| 실제 측정값 | 실행 전 |
| 기대값과의 차이 | |
| 판정 | 미실행 |
| 판정 이유 | |
| 재현 절차 | |
| 재시도 결과 | |
| 일정 DB 변경 여부 | |
| 민감정보 마스킹 확인 | |
| 공개 가능 여부 | |
| 후속 조치 | |
| 담당·확인자 | |
| 검증 일시 | |
20. 증적 관리
증적 ID:

- 공식 문서: `DOC-<CATEGORY>-NNN`
- 요청 JSON: `REQ-<검증ID>-NN`
- 응답 JSON: `RES-<검증ID>-NN`
- 처리시간: `TIM-<검증ID>-NN`
- 카카오 화면: `UI-<검증ID>-NN`
- 관리자센터 오류: `ERR-<검증ID>-NN`
- 설정 화면: `CFG-<검증ID>-NN`
- 보안·마스킹: `SEC-<검증ID>-NN`

각 증적 메타데이터:

| 검증 ID | 공식 문서 ID | 실행 일시 | 테스트 환경 | 원본 파일명 | 마스킹본 파일명 | 민감정보 포함 여부 | 공개 가능 여부 | 보관 위치 | 삭제 예정일 | 정리 여부 |
|---|---|---|---|---|---|---|---|---|---|---|
| 실행 전 | 실행 전 | 실행 전 | 실행 전 | 실행 전 | 실행 전 | 실행 전 | 실행 전 | 논리적 보관 위치만 기록 | 실행 전 | 미정리 |

실제 로컬 절대경로는 기록하지 않는다. 실제 사용자 식별값, 연결 코드, 사용자 정의 헤더, 내부·비밀 URL, 봇·블록·채널 ID와 관리자 계정 정보를 가린다. 화면 캡처만으로 성공을 판단하지 않고 카카오 화면, 서버 수신 기록, 관리자센터 오류 내역을 대조한다. 원문 발화와 사용자 ID 원문은 장기 저장하지 않으며 PoC 종료 후 원본 로그와 임시 상태를 삭제한다.
21. 내부·공개 산출물 구분
내부 작업용
PoC 계획서
단계별 실행 체크리스트
결과 기록표
마스킹 전 원본 로그
실패 로그 요약
임시 URL·헤더 설정 기록
마스킹된 요청·응답 샘플
임시 자원 정리 체크리스트
인수인계 상태 요약
내부 문서는 공개 저장소에 넣지 않는다.
공개 가능 후보
검증한 기능과 미검증 기능의 요약
응답시간 집계
채택한 카카오 기능 범위
공식 규격에 기반한 개념 구조도
민감정보를 제거한 요청·응답 필드 목록
PoC 한계와 범위 조정 결과
공개 후보도 공개 전에 개인정보, 식별값, 비밀 경로, 메타데이터를 다시 검사한다.
22. 알려진 위험과 대응
위험	영향	대응
공식 가이드와 관리자센터 UI 차이	계획과 실제 설정 불일치	테스트 당일 재확인
HTTPS 강제 조건의 문서 명시 부족	URL 등록 실패 가능성	HTTPS만 사용하고 관리자센터에서 직접 검증
고유 요청 ID 미제공	완전한 요청 멱등성 어려움	confirmation 1회 소비와 보조 중복 감지
공식 서명 제공 보장 확인 불가	위조 요청 위험	사용자 정의 헤더와 다중 방어
사용자 식별값 변화	계정 연결 실패	반복·다사용자·재연결 시험
5초 제한	OpenAI 요청 실패	짧은 내부 타임아웃과 기능 축소
클라우드 콜드 스타트	측정 왜곡	워밍·비워밍 결과 분리
터널 지연	성능 오판	터널은 사전 연결 시험에만 사용
UI 제한	후보·확인 정보 잘림	2~5건만 표시하고 6건 이상 재질문
연결 코드 노출	계정 오연결	HMAC, 1회 사용, 짧은 만료, 로그 금지
테스트 로그 잔존	개인정보·비밀 노출	종료 단계에서 삭제 검증
임시 코드의 정식 편입	품질·보안 부채	PoC 자원 폐기, 정식 구현 별도 설계
콜백 승인 불가	장기 처리 불가	콜백 비전제, 카카오 기능 축소
개발 채널과 실제 운영 차이	운영 시 재현 실패	운영 전 별도 검증 필요로 기록
공식 문서 URL 변경	근거 추적 단절	PoC 실행 직전, 구현 단계, 배포 단계에서 공식 문서 재확인
관리자센터 UI 또는 로그인 제한	공개 URL 감사 제한	설정 화면 증적과 확인일 기록
문서 수치와 관리자센터 실측 차이	잘못된 기준 적용	차이를 증적으로 기록하고 범위 재판정
일반 스킬·AI 콜백·채널 API 혼동	잘못된 보장 확대	각 문서 ID와 적용 범위를 분리

23. 5단계 완료 기준
다음 항목이 모두 판정되어야 5단계를 완료할 수 있다.
최신 공식 문서 확인(공식 URL 변경 가능성, 관리자센터 UI 변경 가능성, 로그인 후 화면의 공개 감사 제한, 문서 수치와 실측 차이, 테스트 봇과 운영 봇 차이를 포함)
테스트 봇과 개발 채널 연결 상태 확인
공중망 스킬 서버 호출 성공 여부
HTTPS 등록·호출 조건 확인
요청 JSON 구조 확인
사용자 발화 확인
사용자 식별값 확인
식별값 안정성과 사용자 구분 확인
기본 응답 말풍선 출력 확인
고정 응답 5초 내 처리 측정
연결 코드 정상·만료·재사용·실패 제한 검증
연결 해제와 이후 접근 차단 검증
퀵리플라이·카드·버튼 검증
3분기 선택 방식 결정
실패 응답 검증
요청 진위 검증 수준 판정
원 요청 멱등성 식별자 확보 여부 판정
OpenAI 포함 요청의 성능 판정
콜백 필요성 판정
카카오 조회·등록·수정 유지 범위 판정
실제 수행 증거의 마스킹 확인
임시 자원과 로그 정리
PoC 결과에 따른 MVP 카카오 범위 확정
사용자 최종 승인
현재는 계획만 작성됐으므로 위 항목을 완료로 표시하지 않는다. 일반 스킬과 AI 챗봇 콜백, 카카오디벨로퍼스 채널 API와 챗봇 스킬을 혼동하지 않으며, 공식 문서에 없는 기능은 PoC 성공 전에 확정하지 않는다. PoC 실행 직전과 구현·배포 단계에서 공식 문서를 다시 확인한다.

[PROJECT-DECISION] 사용자 식별값 HMAC 저장, 연결 코드 8자리·5분 유효·1회 사용, 코드당 실패 5회, 사용자당 10분에 3회 발급, 연결 완료·해제 시 기존 대기 명령과 확인 요청 폐기, 콜백을 MVP 필수 전제로 두지 않는 원칙, 카카오 실패 시 PWA·웹 기능 유지, 비밀 경로·요청 제한·로그 마스킹 다중 방어는 카카오 플랫폼의 보장이 아니라 CalTalk가 선택한 규칙이다.
24. 현재 상태 블록
기준 문서 확인: 5개 기준 문서 확인 완료
현재 단계: 5단계 카카오톡 채널 챗봇 PoC 조건부 종료
완료된 단계: 1단계 MVP 기획, 2단계 카카오 환경 사전 확인, 3단계 기술 설계, 4단계 화면·기능 명세
현재 단계 상태: 본표 97개 중 통과 50·제한적 통과 14·미실행 33으로 핵심 기술 가능성 검증 완료, 정식 개발 착수 조건부 종료
현재 버전: CalTalk 카카오톡 채널 챗봇 PoC 계획서 v0.1
현재 브랜치: 없음 — 저장소 미생성
마지막 완료 작업: PoC 최종 집계·제한적 통과 분석·미실행 33개 이관과 조건부 종료 판정
확정 사항: 공중망 스킬 연동, POST JSON, SkillResponse 2.0, 일반 스킬 5초, 사용자 식별 차이, 주요 UI·연결·보안·예외·단일 인스턴스 방어 계약
정식 개발 검증 필요: 실제 DB·일정 CRUD·대화 상태·OpenAI·Redis 분산 제한·멱등성·다중 사용자·운영 채널
보류 사항: KSK-011·KID-003·KPF-003 HOLD, 운영 채널 전환, 플랫폼 재전송·오류 이력 경계
알려진 위험: 암호학적 요청 진위 미확정, 자동 재시도 ID 미확정, 단일 인스턴스 메모리 한계, OpenAI·DB 통합 미검증
다음 단계: 26.7의 착수 조건 승인 후 정식 개발 Phase 1
다음 작업 한 가지: 정식 저장소·브랜치 보호·필수 CI 정책을 확정하고 Phase 1 인수 테스트를 등록한다.
25. 다음 작업 한 가지
정식 저장소와 보호된 기본 브랜치를 만들기 전에 브랜치 전략·PR 리뷰·필수 CI·비밀 관리 원칙을 승인하고 Phase 1 테스트 ID를 인수 조건으로 등록한다.

[HOLD]: 현재 단계에서 필요한 권한, 관리자센터 상태, 공식 문서 근거, 선행 검증 결과 또는 외부 승인 조건이 부족해 결정할 수 없는 항목을 표시한다.

- [POC-VERIFY]는 이번 PoC에서 실제 검증할 수 있는 항목에 사용한다.
- [HOLD]는 외부 승인·권한·선행 조건을 기다려야 해 현재 검증할 수 없는 항목에 사용한다.
- 현재 적용 항목이 없어도 정의는 유지한다.

26. PoC 최종 판정 및 정식 개발 이관 계획

26.1 본표 직접 집계

97개 개별 테스트 행을 직접 집계한 결과는 고유 ID 97개, 통과 50, 제한적 통과 14, 미실행 33, 기타 0, 합계 97이며 중복·누락은 0이다. 기존 97개 테스트의 판정은 유지한다.

26.2 핵심 검증 성과

| 영역 | 실제 확인 범위 | 완료·제한적 완료 테스트 ID | 미확인 범위 |
|---|---|---|---|
| 카카오 스킬 연동 | 관리자센터 Test URL, 실제 개발 채널 말풍선, POST JSON·SkillResponse 2.0, simpleText·quickReplies·basicCard·버튼·웹링크, accountId와 botUserKey 차이 | KSK-001~008, KSK-010, KSK-012, KUI-001, KUI-004~006, KUI-008, KUI-012~013, KUI-017 | 운영 채널 차이, listCard·clientExtra·flow.trigger 경계 |
| 요청·응답 안정성 | 고정·연결 상태·실제 채널·버튼 반복, 3초·4초 지연, 5초 초과 화면 동작, 응답 크기 실측 | KPF-001~002, KPF-007~009, KID-001~002, KID-004, KUI-018 | OpenAI 포함 성능, 정확한 응답 차단 경계, 관리자센터 오류 이력 |
| 연결 코드·연결 상태 | 발급·만료·1회 사용·재사용 차단·재발급 무효화·오입력·발급 제한·연결 상태·재연결 거부·해제 | KLC-001~007, KLC-009~015, KPF-002 | 두 실제 사용자 격리, 영속 DB와 대기·확인 상태 연동 |
| 보안 | 헤더·관리자 토큰, 메서드·Content-Type·본문 크기·필수 식별값, 식별값·발화·코드·헤더·토큰 비저장, 예외 내부정보 비노출 | KSE-001~008, KSE-010~014, KPF-011 | 암호학적 요청 진위, 운영 비밀 관리·외부 로그 보존 |
| 방어 로직 | 사용자별 빈도 제한, HMAC 보조 지문 중복 방지, 안전 응답·서버 생존·health | KSE-009, KID-008, KPF-011 | Redis 분산 제한·멱등성, 자동 재시도, 실제 DB 트랜잭션 |

26.3 제한적 통과 14개 분석

| 테스트 ID | 분류 | 제한 사유 | 현재 PoC에서 확인한 범위 | 확인하지 못한 범위 | 정식 개발 후속 검증 |
|---|---|---|---|---|---|
| KSK-009 | 기타 | appUserId 미제공 | 실제 채널의 user.id·botUserKey·친구 정보 | appUserId 제공 조건 | 계정 연동 환경별 payload 회귀 |
| KUI-015 | 관리자센터 오류 이력 미제공 | 잘못된 JSON의 오류 이력 없음 | 사용자 화면 원문 비노출·안전 동작 | 관리자센터 기록 조건 | 운영형 스킬 오류 이력 교차 확인 |
| KUI-018 | 정확한 플랫폼 경계 미확인 | 표시 크기와 공식 계산 기준 차이 | 네 응답 크기의 실제 표시 | 정확한 차단·계산 경계 | 확정 UI/payload로 경계 자동화 |
| KSE-001 | 플랫폼 기능 한계 | 사용자 정의 헤더는 서명이 아님 | 헤더 전달·변경 | 암호학적 요청 진위 | 게이트웨이·서명 또는 다중 방어 확정 |
| KSE-009 | 두 번째 사용자 부재·단일 인스턴스 한계 | 사용자 독립·분산 제한 미검증 | 단일 사용자 5회 허용·6회 제한·복구 | 두 사용자·Redis·다중 노드 | Redis 원자 카운터와 격리·부하 테스트 |
| KPF-009 | 관리자센터 오류 이력 미제공 | 5초 초과 오류 기록 없음 | 6초 응답의 말풍선 미표시 | 플랫폼 오류 분류 | 운영형 오류 이력과 지연 결과 폐기 확인 |
| KPF-012 | 관리자센터 오류 이력 미제공 | 4xx 오류 기록 없음 | 사용자 화면 안전 응답 | 플랫폼 오류 분류 | 4xx 계약·오류 이력 통합 테스트 |
| KPF-013 | 관리자센터 오류 이력 미제공 | 5xx 오류 기록 없음 | 내부 오류 비노출·안전 화면 | 플랫폼 오류 분류 | 5xx 계약·관측성·알림 검증 |
| KPF-014 | 관리자센터 오류 이력 미제공 | 단절 오류 기록 없음 | 단절·복구·내부 정보 비노출 | 플랫폼 오류 분류 | 배포 환경 장애·복구와 오류 이력 확인 |
| KID-001 | 실제 자동 재시도 미재현 | 관리자센터 신규 반복만 확인 | 반복 요청별 ID 후보와 응답 안정성 | 원 요청 자동 재시도 ID 유지 | 정식 멱등성 저장소와 재시도 시뮬레이션 |
| KID-002 | 실제 자동 재시도 미재현 | 버튼 신규 클릭만 확인 | 버튼 반복 요청 안정성 | 동일 원 요청 재전송 | 상태 변경 API 멱등성 통합 테스트 |
| KID-004 | 실제 자동 재시도 미재현 | 실제 채널 신규 반복만 확인 | botUserKey 요청 안정성·ID 차이 | 자동 재시도 식별자 | 게이트웨이 재시도·중복 전달 시험 |
| KID-005 | 실제 자동 재시도 미재현 | 상관관계 ID만 관찰 | 요청별 x-request-id 차이 | 재전송 안정 ID | 공식 ID 계약 또는 서버 멱등키 확정 |
| KID-008 | 자동 재시도·DB·단일 인스턴스 한계 | 사용자 재전송과 모의 변경만 확인 | 3초 보조 지문 중복 방지·재처리 | 플랫폼 자동 재시도·DB 트랜잭션·두 사용자·분산 | 영속 멱등성·동시성·다중 노드 검증 |

26.4 미실행 33개 정식 개발 이관표

분류 집계는 A 6개, B 7개, C 6개, D 10개, E 3개, F 1개, G 0개로 합계 33개다.

| 테스트 ID | 테스트명 | 현재 판정 | 이관 분류 | 미실행 원인 | 필요한 구현 | 필요한 외부 조건 | 권장 수행 단계 | 우선순위 | 완료 기준 |
|---|---|---|---|---|---|---|---|---|---|
| KSK-011 | 다른 사용자 구분 검증 | 미실행 HOLD | E | 두 번째 실제 사용자 없음 | 사용자 격리·식별 계약 | 실제 계정 2개 | Phase 5 | 높음 | 두 사용자 식별·상태 완전 분리 |
| KUI-002 | 500자 경계 검증 | 미실행 | D | UI 경계 미검증 | 확정 응답 생성기 | 카카오 UI 계약 | Phase 4 | 중간 | 500자 화면·응답 규격 일치 |
| KUI-003 | 1,000자 경계 검증 | 미실행 | D | UI 경계 미검증 | 확정 응답 생성기 | 카카오 UI 계약 | Phase 4 | 중간 | 1,000자 경계 동작 확정 |
| KUI-007 | clientExtra 검증 | 미실행 | D | payload 계약·경로 미구현 | clientExtra 생성·파싱 | 관리자센터 블록 설정 | Phase 4 | 높음 | 왕복 값·비노출·선택 처리 성공 |
| KUI-009 | listCard 2건 검증 | 미실행 | D | listCard 미구현 | 2건 카드 렌더링 | 카카오 UI 계약 | Phase 4 | 중간 | 2건 정상 표시·선택 |
| KUI-010 | listCard 5건 검증 | 미실행 | D | listCard 미구현 | 5건 카드 렌더링 | 카카오 UI 계약 | Phase 4 | 중간 | 5건 정상 표시·선택 |
| KUI-011 | 후보 6건 재질문 검증 | 미실행 | D | 후보 UI 계약 미확정 | 재질문·축약 UI | 후보 표시 계약 | Phase 4 | 높음 | 6건 이상 안전 재질문 |
| KUI-014 | 절대 날짜·요일·시간 가독성 검증 | 미실행 | D | 실제 일정 UI 없음 | 날짜·시간 포맷터 | 실제 채널 화면 | Phase 4 | 중간 | 절대값·요일·시간 가독성 승인 |
| KUI-016 | 출력 3개 경계 검증 | 미실행 | D | 출력 조합 경계 미검증 | 3-output 응답 생성 | 카카오 UI 계약 | Phase 4 | 중간 | 허용 경계 정상 표시 |
| KLC-008 | 다른 테스트 사용자의 코드 검증 | 미실행 | E | 두 번째 실제 사용자 없음 | 코드 소유권 검증 | 실제 계정 2개 | Phase 5 | 높음 | 타 사용자 코드 사용 차단 |
| KLC-016 | 연결 후 기존 대기 상태 폐기 검증 | 미실행 | A | 대화 대기 상태 미구현 | 연결 이벤트·pending 폐기 | 정식 백엔드 | Phase 2 | 높음 | 연결 시 기존 pending 원자 폐기 |
| KLC-017 | 해제 후 기존 확인 상태 폐기 검증 | 미실행 | A | confirmation 상태 미구현 | 해제 이벤트·confirmation 폐기 | 정식 백엔드 | Phase 2 | 높음 | 해제 시 기존 confirmation 원자 폐기 |
| KPF-003 | 메모리 조회 10회 검증 | 미실행 HOLD | F | 대상·발화·응답·판정 미정 | 비변경 조회 계약·전용 경로 | 계약 승인 | Phase 1 | 낮음 | 정의 승인 후 10회 목표 충족 |
| KPF-004 | 일정 조회 모의 10회 검증 | 미실행 | A | 일정 조회 백엔드 없음 | 일정 조회 API·측정 | 정식 백엔드·DB | Phase 1 | 높음 | 10회 정합·성능 목표 충족 |
| KPF-005 | OpenAI 구조화 10회 검증 | 미실행 | C | OpenAI 계약·구현 없음 | 구조화 출력·검증기 | OpenAI 환경 | Phase 3 | 높음 | 10회 스키마·의미 검증 성공 |
| KPF-006 | DB 조회+OpenAI 모의 10회 검증 | 미실행 | C | DB·OpenAI 통합 없음 | 조회·프롬프트·구조화 통합 | DB·OpenAI 환경 | Phase 3 | 높음 | 10회 정합·성능 목표 충족 |
| KPF-010 | OpenAI 타임아웃 검증 | 미실행 | C | OpenAI 호출 없음 | 타임아웃·취소·안전 응답 | OpenAI 환경 | Phase 3 | 높음 | 제한 내 안전 실패·상태 무변경 |
| KPF-015 | 지연 결과 폐기 검증 | 미실행 | C | 비동기 대화 상태 없음 | 요청 세대·결과 폐기 | OpenAI·상태 저장소 | Phase 3 | 높음 | 늦은 결과가 최신 상태를 변경하지 않음 |
| KDM-001 | 미연결 사용자 검증 | 미실행 | A | 정식 권한·명령 경로 없음 | 연결 확인 게이트 | 정식 사용자·연결 | Phase 1 | 높음 | 미연결 상태 변경 차단·안내 |
| KDM-003 | 지원하지 않는 명령 검증 | 미실행 | C | 자연어 분류기 없음 | 의도 분류·안전 fallback | OpenAI 계약 | Phase 3 | 중간 | 미지원 명령 무변경·안내 |
| KDM-004 | 과거 일정 생성 요청 검증 | 미실행 | B | 일정 생성·시간 규칙 없음 | 생성 API·과거 시각 검증 | DB·일정 도메인 | Phase 1 | 높음 | 과거 생성 거부·DB 무변경 |
| KDM-005 | 과거 시각으로 수정 요청 검증 | 미실행 | B | 일정 수정·시간 규칙 없음 | 수정 API·버전 검증 | DB·일정 도메인 | Phase 1 | 높음 | 과거 수정 거부·원본 유지 |
| KDM-006 | 모호한 날짜·시간 검증 | 미실행 | C | 자연어 모호성 처리 없음 | 구조화·재질문 계약 | OpenAI 환경 | Phase 3 | 높음 | 임의 저장 없이 재질문 |
| KDM-007 | 재질문 대기 10분 만료 검증 | 미실행 | A | pending 상태·TTL 없음 | pending 저장·만료 | 정식 상태 저장소 | Phase 2 | 높음 | 10분 만료 후 재사용 차단 |
| KDM-008 | 확인 5분 만료 검증 | 미실행 | A | confirmation·TTL 없음 | confirmation 저장·만료 | 정식 상태 저장소 | Phase 2 | 높음 | 5분 만료 후 실행 차단 |
| KDM-009 | 후보 선택 0건 검증 | 미실행 | B | 일정 조회·후보 상태 없음 | 검색·후보 상태 | DB·일정 CRUD | Phase 2 | 중간 | 0건 안전 안내·무변경 |
| KDM-010 | 후보 선택 1건 검증 | 미실행 | B | 일정 조회·후보 상태 없음 | 단일 후보 선택 | DB·일정 CRUD | Phase 2 | 중간 | 1건 정확 선택·확인 |
| KDM-011 | 후보 선택 2~5건 검증 | 미실행 | B | 다중 후보 상태 없음 | 후보 목록·선택 토큰 | DB·일정 CRUD | Phase 2 | 높음 | 2~5건 정확 표시·선택 |
| KDM-012 | 후보 선택 6건 이상 검증 | 미실행 | B | 대량 후보 정책 없음 | 축약·재질문·선택 상태 | DB·일정 CRUD | Phase 2 | 높음 | 6건 이상 안전 축약·재질문 |
| KDM-013 | 동일 확인 반복 실행 모의 검증 | 미실행 | B | 영속 멱등성·트랜잭션 없음 | confirmation 소비·멱등성 | DB·일정 CRUD | Phase 2 | 높음 | 동일 확인의 상태 변경 1회 |
| KID-003 | 관리자센터 재전송 검증 | 미실행 HOLD | E | 원 요청 재전송 기능 없음 | 재전송 관측·시뮬레이터 | 플랫폼 기능 또는 도구 | Phase 5 | 중간 | 원 요청/재전송 ID 유지 확인 |
| KID-006 | flow.trigger 비교 검증 | 미실행 | D | payload 계약·로그 미구현 | trigger 파싱·비교 | 관리자센터 flow 설정 | Phase 4 | 중간 | 트리거 유지·변화 규칙 확정 |
| KID-007 | 동일 clientExtra 반복 검증 | 미실행 | D | clientExtra 경로 미구현 | 안정 직렬화·반복 비교 | 관리자센터 블록 설정 | Phase 4 | 높음 | 동일 값 반복·멱등 입력 검증 |

26.5 단계별 이관

| 단계 | 포함 테스트 ID | 선행 구현 | 테스트 시작 조건 | 완료 기준 |
|---|---|---|---|---|
| Phase 1 정식 기반·사용자·연결·DB·CRUD | KPF-003~004, KDM-001, KDM-004~005 | 정식 저장소·백엔드, 사용자/연결 계약, DB 스키마, 일정 조회·생성·수정 API | 로컬·CI DB 마이그레이션과 인증 흐름 가동 | 조회 성능·권한·과거 시각 규칙 및 DB 무결성 통과 |
| Phase 2 대화 상태·후보·확인·중복 | KLC-016~017, KDM-007~013 | pending·confirmation·후보·TTL·소비·멱등성 모델 | 트랜잭션·동시성 테스트 가능한 상태 저장소 | 만료·폐기·선택·반복 실행이 원자적으로 정합 |
| Phase 3 OpenAI·자연어·타임아웃 | KPF-005~006, KPF-010, KPF-015, KDM-003, KDM-006 | 구조화 출력 스키마, 프롬프트, 취소·세대 관리 | 격리된 OpenAI 테스트 환경과 비용·비밀 통제 | 구조화·모호성·타임아웃·지연 폐기 자동화 통과 |
| Phase 4 카카오 UI·payload 경계 | KUI-002~003, KUI-007, KUI-009~011, KUI-014, KUI-016, KID-006~007 | listCard·clientExtra·flow.trigger·날짜 포맷 계약 | 관리자센터 개발 블록과 실제 채널 준비 | UI 경계·가독성·payload 반복 계약 승인 |
| Phase 5 부하·보안·분산·외부 조건 | KSK-011, KLC-008, KID-003 | Redis rate limit·영속 멱등성·다중 사용자·운영 관측성 | 실제 사용자 2명, 플랫폼 재전송 수단 또는 시뮬레이터, 배포 환경 | 사용자 격리·분산 중복 방지·운영 채널 전환 기준 충족 |

26.6 PoC 종료 판정

최종 판정은 **B. PoC 조건부 종료**다.

- 검증 완료된 핵심 가설: 카카오 스킬의 관리자센터·실제 채널 연동, 주요 말풍선 UI, 연결 코드와 연결 상태, 입력·인증·로그 보안, 예외 안전성, 시간 제한, 단일 인스턴스 빈도 제한과 보조 중복 방지가 기술적으로 가능하다.
- 남은 핵심 위험: 실제 DB 트랜잭션과 일정 CRUD, pending·confirmation, OpenAI 구조화·타임아웃, Redis 분산 제한·멱등성, 두 사용자 격리, 운영 채널·플랫폼 오류/재시도 경계다.
- PoC에서 더 검증하지 않는 이유: 미실행 33개는 대부분 정식 도메인·영속 저장·OpenAI·확정 UI 계약 또는 외부 사용자·플랫폼 기능에 의존하며 Node.js 모의 분기 추가는 실제 서비스 위험을 대표하지 못한다.
- 정식 개발 착수 조건: 26.7의 저장소·아키텍처·상태·보안·배포 계약을 승인하고 Phase 1 테스트를 CI 인수 조건으로 등록한다.
- 재개 조건: 카카오 계약의 중대한 변경, 운영 채널 차이, 사용자 격리 실패, 분산 제한·멱등성 설계 불가, DB/OpenAI 통합에서 핵심 PoC 계약 불충족이 발견될 때 해당 범위만 재개한다.

26.7 정식 개발 착수 조건

- 정식 저장소를 만들고 보호된 기본 브랜치, 짧은 기능 브랜치, PR 리뷰와 필수 CI 전략을 확정한다.
- Spring Boot 또는 승인된 백엔드 프레임워크와 모듈 경계·버전 정책을 확정한다.
- 사용자·일정·카카오 연결·pending·confirmation·멱등성·감사 필드의 DB 스키마와 마이그레이션을 승인한다.
- 사용자 계정과 botUserKey/accountId의 연결·해제·재연결·격리 계약을 승인한다.
- 로컬·CI·스테이징·운영 환경변수와 비밀 저장소, 회전·접근·로그 비노출 정책을 적용한다.
- Redis 또는 동등한 영속·분산 저장소로 rate limit과 멱등성의 원자성·TTL·장애 정책을 설계한다.
- 일정 조회·생성·수정·삭제 API와 권한·시간대·버전·충돌·트랜잭션 계약을 구현한다.
- OpenAI 구조화 출력 스키마, store 정책, 타임아웃·취소·재질문·지연 결과 폐기 계약을 승인한다.
- pending·confirmation·후보 선택·만료·소비·SUPERSEDED 상태 모델을 구현한다.
- 단위·통합·계약·보안·동시성·부하·카카오 실제 채널 자동화 테스트를 CI에 등록한다.
- 스테이징·관측성·백업·복구·롤백이 있는 배포 환경과 운영 채널 전환·심사·승인 기준을 정한다.

26.8 `server.js` 처리 방침

`server.js`는 카카오 연동·보안·예외·빈도 제한·보조 중복 방지 검증용 PoC이며 메모리 Map 기반 단일 인스턴스 전용이다. 재시작 시 상태가 초기화되고 실제 일정 CRUD·DB·OpenAI가 없다. 정식 서비스 코드로 직접 승격하거나 계속 확장하지 않고 검증용 참고 구현으로 동결한다. 검증된 계약과 테스트 결과만 정식 구현에 이관하며 임시 발화·지연·오류·모의 상태 변경 분기는 정식 개발 코드에 복사하지 않는다.
