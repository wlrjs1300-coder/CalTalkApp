# Render 무료 배포 가이드

## 배포 구조

CalTalk의 검증·시연 환경은 저장소 루트의 `render.yaml`로 다음 리소스를 생성합니다.

- `caltalk-frontend`: Docker Free Web Service
- `caltalk-backend`: Docker Free Web Service
- `caltalk-postgres`: Free Render Postgres
- `caltalk-redis`: Free Render Key Value

브라우저와 카카오톡은 공개 frontend origin만 사용합니다. Frontend Nginx가 `/api`,
`/oauth2`, `/login/oauth2`, `/actuator` 요청을 공개 backend origin으로 전달합니다.
무료 Web Service는 private network 요청을 받을 수 없기 때문에 backend도 Web Service로
배포하지만, 사용자가 직접 backend URL을 사용할 필요는 없습니다.

## 무료 플랜 제한

- Free Web Service는 15분 동안 요청이 없으면 중지되며 첫 요청의 응답이 늦어질 수 있습니다.
- 두 Web Service가 워크스페이스의 월 750 free instance hours를 함께 사용합니다.
- Free Postgres는 생성 30일 후 만료되고 백업과 managed connection pooling을 제공하지 않습니다.
- Free Key Value는 재시작 시 데이터가 사라집니다. 로그인 세션과 진행 중인 챗봇 명령도 초기화될 수 있습니다.
- 카카오톡 callback이 backend cold start를 기다리지 못해 첫 요청이 실패할 수 있습니다.
- OpenAI API, 도메인 등록 및 Route 53 비용은 Render 무료 플랜과 별개입니다.

이 구성은 포트폴리오와 기능 검증용입니다. 실제 사용자에게 상시 서비스를 제공하기 전에는
backend와 PostgreSQL을 유료 또는 다른 지속형 인프라로 이전해야 합니다.

## Blueprint 생성

1. Render Dashboard에서 `New > Blueprint`를 선택합니다.
2. GitHub의 `wlrjs1300-coder/CalTalkApp` 저장소를 연결합니다.
3. branch는 `develop`, Blueprint path는 `render.yaml`을 선택합니다.
4. 생성될 리소스 네 개가 모두 `Free`, region이 `Singapore`인지 확인합니다.
5. 아래 `sync: false` 항목만 Dashboard에서 입력하고 `Deploy Blueprint`를 실행합니다.

`render.yaml`은 다음 값을 자동 연결합니다.

- Postgres connection string → backend `DB_URL`
- Postgres user/password → backend `DB_USERNAME`, `DB_PASSWORD`
- Key Value connection string → backend `REDIS_URL`
- Frontend Render hostname → backend `CORS_ALLOWED_ORIGINS`, `FRONTEND_ORIGIN`
- Backend Render hostname → frontend `BACKEND_ORIGIN`

Backend는 Render의 `postgresql://user:password@host/database` URL에서 인증정보를 분리하고
시작 시 `jdbc:postgresql://host:5432/database`로 변환합니다. 인증은 별도로 연결된
`DB_USERNAME`과 `DB_PASSWORD`를 사용합니다.
Backend와 frontend는 protocol이 없는 Render hostname을 HTTPS origin으로 변환합니다.

## 최초 입력이 필요한 비밀값

아래 값은 로컬 `backend/.env`에서 확인하되 GitHub, 문서, 채팅 또는 스크린샷에 노출하지 않습니다.

```text
SOCIAL_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
NAVER_CLIENT_ID
NAVER_CLIENT_SECRET
KAKAO_CHATBOT_SKILL_SECRET
OPENAI_API_KEY
WEB_PUSH_VAPID_PUBLIC_KEY
WEB_PUSH_VAPID_PRIVATE_KEY
WEB_PUSH_VAPID_SUBJECT=mailto:<운영 이메일>
CUSTOM_FRONTEND_ORIGIN=https://caltalk.<보유 도메인>
```

`KAKAO_IDENTITY_HMAC_SECRET`은 Blueprint가 안전한 임의 값으로 생성합니다.
Render에서는 사용자 PC의 Ollama에 접근할 수 없으므로 무료 검증 환경은
`OLLAMA_ENABLED=false`, `OPENAI_FALLBACK_ENABLED=true`로 실행됩니다.
AWS 서브도메인을 아직 결정하지 않았다면 `CUSTOM_FRONTEND_ORIGIN`은 비워 둡니다. 나중에
추가하면 해당 주소를 로그인 복귀 origin으로 사용하고 Render 기본 주소와 함께 CORS에 허용합니다.

## 배포 확인

Frontend의 실제 `onrender.com` URL을 확인한 다음 아래 경로를 순서대로 검사합니다.

```text
https://<frontend-host>/healthz
https://<frontend-host>/actuator/health
https://<frontend-host>/
https://<frontend-host>/welcome
```

`/healthz`는 `ok`, `/actuator/health`는 `UP`이어야 합니다. 최초 접근에서는 frontend와
backend가 차례로 깨어나므로 일시적인 502/504가 발생할 수 있습니다. 잠시 후 다시 확인합니다.

## 소셜 로그인 callback

각 플랫폼 개발자 콘솔에 아래 운영 callback을 추가합니다.

```text
https://<frontend-host>/login/oauth2/code/google
https://<frontend-host>/login/oauth2/code/kakao
https://<frontend-host>/login/oauth2/code/naver
```

AWS 서브도메인을 연결한 뒤에는 `<frontend-host>` 대신 최종 사용자 도메인의 callback도 추가합니다.

## 카카오톡 챗봇

챗봇 관리자센터의 운영 스킬 URL과 Test URL을 다음 주소로 변경하고 저장·배포합니다.

```text
https://<frontend-host>/api/v1/kakao/skill
```

AWS 서브도메인 연결 후에는 최종 주소로 한 번 더 변경합니다. 무료 backend가 중지된 상태의
첫 요청은 callback 제한 시간 안에 응답하지 못할 수 있으므로 연속 두 번째 요청도 확인합니다.

## AWS Route 53 서브도메인

1. Frontend 서비스의 `Settings > Custom Domains`에서 사용자 서브도메인을 추가합니다.
2. Render가 안내하는 DNS target을 확인합니다.
3. Route 53 Public Hosted Zone에 CNAME을 추가합니다.
4. Render의 도메인 검증과 managed TLS 발급이 완료될 때까지 기다립니다.

예시:

```text
Name: caltalk
Type: CNAME
Value: <frontend-host>.onrender.com
TTL: 300
```

DNS 값에는 `https://`나 URL path를 넣지 않습니다.

## PWA 알림 재등록

Push subscription은 origin별로 분리됩니다. 운영 주소 또는 AWS 서브도메인에서 다시 로그인한 뒤
`설정 > 알림 허용 > 테스트 발송`을 실행합니다. localhost에서 만든 구독은 운영 origin으로
자동 이전되지 않습니다.

## 30일 운영 체크

Free Postgres 만료 전에 필요한 데이터를 내보냅니다. 계속 운영할 경우 만료 전에 Postgres를
유료로 업그레이드하거나 다른 영구 PostgreSQL로 이전합니다. 무료 검증 환경에서는 데이터가
영구 보존된다고 안내하면 안 됩니다.
