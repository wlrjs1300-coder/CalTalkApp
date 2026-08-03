# CalTalk UI/UX 전면 재설계

## 기존 문제와 목표

기존 화면은 기능 검증에는 충분했지만 개발자 문구, 약한 브랜드 인상, 설정 우선의 홈 구조와 단순 상태 UI 때문에 서비스 목적과 주요 행동이 선명하지 않았습니다. 이번 재설계는 일정 확인과 생성이 가장 먼저 보이고, 시간대와 일정 충돌 기능을 사용자 언어로 이해할 수 있는 포트폴리오 대표 화면을 목표로 했습니다.

## 정보 구조 변경

- 로그인·회원가입: 브랜드 소개와 단일 인증 행동을 균형 있게 배치
- App Shell: compact top navigation에 설정, 사용자 정보와 로그아웃 배치
- 홈: 오늘 날짜와 일정 목록을 먼저 표시하고 시간대는 설정 dialog로 이동
- 일정: 목록 → 상세 → 수정/삭제의 위계를 분리하고 전체 항목을 큰 클릭 영역으로 제공
- 충돌: 내부 처리 용어 대신 겹치는 일정, 시간 다시 수정, 그래도 저장이라는 행동 언어 사용

## 디자인 시스템

`global.css`에 primary/neutral/state 색상, 7단계 typography, 8px 기반 spacing, radius, shadow와 motion token을 정의했습니다. 차분한 teal primary와 gray-green surface를 사용하고 shadow와 장식은 핵심 영역에만 제한했습니다. hover, active, focus-visible, disabled, invalid, success와 reduced-motion 상태를 포함합니다.

## 주요 화면 개선

인증 화면은 서비스 가치가 즉시 읽히는 카피, password 표시/숨김, placeholder, autocomplete와 pending 상태를 제공합니다. 홈은 일정 생성 CTA와 compact list를 중심으로 재구성했습니다. 목록은 날짜·시간·장소·긴 제목을 반응형으로 처리하며 skeleton, 행동 가능한 empty/error 상태를 제공합니다. 일정 form은 timezone 맥락, 근접 validation과 모바일 sticky action footer를 사용합니다.

상세 화면은 일시와 장소만 우선 표시하고 수정과 삭제의 시각적 위계를 분리했습니다. 삭제 dialog는 대상 제목과 되돌릴 수 없음을 명확히 안내합니다. 성공 결과는 화면 맥락 안의 일관된 status notice로 전달합니다.

## 충돌 확인 UX

CalTalk의 핵심 기능인 일정 겹침을 warning summary와 기존 일정 목록으로 표현합니다. 기존 일정의 시간과 장소를 비교할 수 있으며 사용자는 `시간 다시 수정` 또는 `그래도 저장`을 선택합니다. 서버에서 최신 정보가 반환되면 “일정 정보가 바뀌어 다시 확인이 필요합니다”라고 안내하면서 기존 승인/replacement 로직과 최대 3회 제한은 유지합니다.

## 반응형 전략

Desktop은 넓은 여백과 compact list로 정보 밀도를 유지합니다. Tablet에서는 보조 요소를 축소하고, 560px 이하에서는 날짜를 먼저 보여주는 일정 카드와 full-height dialog, 16px input, 44px 이상 touch target 및 safe-area를 고려한 sticky footer를 사용합니다. 최소 지원 폭은 360px입니다.

## 접근성

semantic heading과 label, `role=dialog`, `aria-modal`, `aria-describedby`, alert/status live semantics와 visible focus를 적용했습니다. Dialog는 focus trap, Escape 닫기, body scroll lock과 닫힌 뒤 trigger focus 복원을 지원합니다. 상태는 색상뿐 아니라 icon, 제목과 문장으로 함께 구분합니다.

## 캡처 위치

Before/after 이미지는 실제 배포 완료 후 `docs/assets/ui-ux/`에 로그인, 회원가입, 일정 홈, 생성 form, 충돌 dialog, 모바일 홈과 모바일 form 순서로 추가합니다. 현재 문서에는 검증되지 않은 캡처를 포함하지 않습니다.

## 테스트와 남은 제한사항

기존 API/인증/CSRF/session 계약과 모든 테스트 목적을 유지하면서 변경된 사용자 locator를 반영했습니다. Password visibility, empty CTA와 dialog focus restore 검증을 추가했습니다. 실제 기기 키보드, screen reader 조합별 탐색, Pretendard webfont self-hosting과 배포 환경의 visual regression baseline은 후속 운영 과제입니다.
