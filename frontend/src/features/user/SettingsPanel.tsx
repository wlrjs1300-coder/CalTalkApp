import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createPortal } from 'react-dom';

import type { ChatPreferences } from '../../api/types';
import { updateChatPreferences } from '../../api/user';
import { sendTestPush, showDeviceTestNotification, subscribeCurrentDevice, unsubscribeCurrentDevice } from '../../api/push';
import { currentUserQueryKey, useCurrentUser } from '../auth/authQuery';
import { KakaoLinkPanel } from './KakaoLinkPanel';

const STYLE_OPTIONS: Array<{ value: ChatPreferences['replyStyle']; label: string; description: string }> = [
  { value: 'CONCISE', label: '간결형', description: '결과와 핵심 정보만 짧게' },
  { value: 'STANDARD', label: '기본형', description: '가독성과 친근함의 균형' },
  { value: 'ASSISTANT', label: '비서형', description: '다음 행동까지 차분하게 안내' },
  { value: 'BUSINESS', label: '비즈니스형', description: '정중하고 명확한 문장' },
  { value: 'FRIENDLY', label: '친근형', description: '조금 더 부드러운 말투' },
];

const DEFAULTS: ChatPreferences = {
  replyStyle: 'STANDARD', replyDensity: 'STANDARD', replyLayout: 'BALANCED', emojiLevel: 'MINIMAL', timeFormat: 'TWELVE_HOUR',
  confirmCreate: true, confirmUpdate: true, defaultDurationMinutes: 60, defaultReminderMinutes: [1440], defaultQueryRange: 'TODAY',
  dailySummaryEnabled: false, dailySummaryTime: '08:00', weeklySummaryEnabled: false, weeklySummaryDay: 1, weeklySummaryTime: '18:00',
};

const HELP = {
  style: { title: '답장 컨셉', body: '카카오톡에서 CalTalk이 사용하는 문장 길이와 말투를 정합니다. 일정 처리 결과는 동일하며 표현 방식만 달라집니다.' },
  density: { title: '정보량', body: '일정 조회 답장에 어느 정도의 정보를 포함할지 정합니다. 자세히를 선택하면 종료 시간과 등록된 장소도 함께 보여줍니다.' },
  layout: { title: '문단 구성', body: '일정을 짧은 카드로 압축하거나, 기본 문단으로 표시하거나, 항목 사이를 넓게 구분해 표시합니다.' },
  emoji: { title: '아이콘·이모지', body: '카카오톡 답장에서 일정, 시간, 장소와 처리 결과를 빠르게 알아볼 수 있도록 의미가 분명한 아이콘만 사용합니다.' },
  time: { title: '시간 표시', body: '카카오톡 답장에 시간을 오전·오후 방식으로 표시할지, 00시부터 23시까지의 24시간 방식으로 표시할지 정합니다.' },
  duration: { title: '기본 일정 길이', body: '종료 시간을 말하지 않았을 때 자동으로 적용할 일정 길이입니다. 예를 들어 “오후 2시에 회의 추가”라고 말했을 때 사용됩니다.' },
  reminder: { title: '기본 일정 알림', body: '새 일정에 자동으로 적용할 카카오톡 알림 시점을 복수로 선택합니다. 일정마다 별도로 변경하거나 알림을 끌 수도 있습니다.' },
  range: { title: '기본 조회 범위', body: '“일정 알려줘”처럼 날짜를 말하지 않았을 때 CalTalk이 기본으로 확인할 기간을 정합니다.' },
  confirmation: { title: '처리 전 확인', body: '카카오톡에서 일정을 실제로 저장하거나 변경하기 전에 내용을 다시 확인할지 정합니다. 삭제는 안전을 위해 설정과 관계없이 항상 확인합니다.' },
  link: { title: '카카오톡 연동', body: '현재 CalTalk 계정과 카카오톡 사용자를 연결합니다. 연결 후 카카오톡에서 일정 조회·추가·수정·삭제 기능을 사용할 수 있습니다.' },
} as const;

type HelpKey = keyof typeof HELP;

function InfoButton({ topic, onOpen }: { topic: HelpKey; onOpen: (topic: HelpKey, rect: DOMRect) => void }) {
  return <button type="button" className="settings-info-button" aria-label={`${HELP[topic].title} 설명 보기`} onClick={(event) => onOpen(topic, event.currentTarget.getBoundingClientRect())}>i</button>;
}

function Choice<T extends string | number>({ value, current, label, onChange }: {
  value: T; current: T; label: string; onChange: (value: T) => void;
}) {
  return <button type="button" className={value === current ? 'settings-choice is-selected' : 'settings-choice'} onClick={() => onChange(value)}>{label}</button>;
}

function SettingsTabIcon({ tab }: { tab: 'reply' | 'schedule' | 'connection' }) {
  if (tab === 'reply') return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 6.5h14v9H9l-4 3v-12Z" /><path d="M8.5 10h7M8.5 13h4.5" /></svg>;
  if (tab === 'schedule') return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="5" y="6.5" width="14" height="12" rx="2" /><path d="M8 4.5v4M16 4.5v4M5 10.5h14M8.5 14h2M13.5 14h2" /></svg>;
  return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M8.5 12.5 6.8 14.2a3 3 0 0 0 4.2 4.2l2.1-2.1M15.5 11.5l1.7-1.7A3 3 0 1 0 13 5.6l-2.1 2.1M9 15l6-6" /><path d="M18 16.5v3M16.5 18h3" /></svg>;
}

const RANGE_OPTIONS: Array<{ value: ChatPreferences['defaultQueryRange']; label: string; description: string }> = [
  { value: 'TODAY', label: '오늘', description: '오늘 하루의 일정' },
  { value: 'THREE_DAYS', label: '앞으로 3일', description: '오늘부터 3일 동안' },
  { value: 'THIS_WEEK', label: '이번 주', description: '이번 주 월요일부터 일요일' },
  { value: 'NEXT_FIVE', label: '다음 일정 5개', description: '날짜와 관계없이 가까운 순서' },
];

function RangePicker({ value, onChange }: {
  value: ChatPreferences['defaultQueryRange'];
  onChange: (value: ChatPreferences['defaultQueryRange']) => void;
}) {
  const [open, setOpen] = useState(false);
  const selected = RANGE_OPTIONS.find((option) => option.value === value) ?? RANGE_OPTIONS[0];
  return (
    <div className="settings-range-picker" onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOpen(false);
    }}>
      <button type="button" className="settings-range-trigger" aria-haspopup="listbox" aria-expanded={open} onClick={() => setOpen((current) => !current)}>
        <span><strong>{selected.label}</strong></span><i aria-hidden="true">⌄</i>
      </button>
      {open ? <div className="settings-range-menu" role="listbox">
        {RANGE_OPTIONS.map((option) => <button type="button" role="option" aria-selected={option.value === value} className={option.value === value ? 'is-selected' : ''} key={option.value} onClick={() => { onChange(option.value); setOpen(false); }}>
          <span><strong>{option.label}</strong><small>{option.description}</small></span><span className="settings-range-check" aria-hidden="true">✓</span>
        </button>)}
      </div> : null}
    </div>
  );
}

export function SettingsPanel({ onSaved }: { onSaved?: () => void }) {
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'reply' | 'schedule' | 'connection'>('reply');
  const [preferences, setPreferences] = useState<ChatPreferences>(() => ({
    ...DEFAULTS,
    ...currentUser.data?.chatPreferences,
  }));
  const [helpPopover, setHelpPopover] = useState<{ topic: HelpKey; top: number; left: number; above: boolean } | null>(null);
  const [pushState, setPushState] = useState<'idle' | 'pending' | 'enabled' | 'disabled'>('idle');
  const [pushError, setPushError] = useState<string>();
  const [pushNotice, setPushNotice] = useState<string>();
  const mutation = useMutation({
    mutationFn: () => updateChatPreferences(preferences),
    onSuccess: (user) => {
      queryClient.setQueryData(currentUserQueryKey, user);
      onSaved?.();
    },
  });
  const saved: ChatPreferences = { ...DEFAULTS, ...currentUser.data?.chatPreferences };
  const changed = JSON.stringify(saved) !== JSON.stringify(preferences);
  const plainPreviewHeading = {
    CONCISE: '일정 등록 완료', STANDARD: '일정을 등록했어요.', ASSISTANT: '요청하신 일정을 등록했어요.',
    BUSINESS: '일정이 등록되었습니다.', FRIENDLY: '일정을 등록했어요.',
  }[preferences.replyStyle];
  const previewHeading = preferences.emojiLevel === 'NONE'
    ? plainPreviewHeading
    : `✅ ${plainPreviewHeading}`;
  const previewTime = preferences.timeFormat === 'TWENTY_FOUR_HOUR'
    ? '8월 10일 10:00 – 11:00' : '8월 10일 오전 10:00 – 오전 11:00';
  const timePrefix = preferences.emojiLevel === 'BALANCED' ? '🕒 ' : '';
  const locationPrefix = preferences.emojiLevel === 'BALANCED' ? '📍 ' : '';
  const previewLines = preferences.replyLayout === 'COMPACT'
    ? <>팀 주간 회의<br />{timePrefix}{previewTime}</>
    : preferences.replyLayout === 'SECTIONED'
      ? <>• 일정 1<br />팀 주간 회의<br />{timePrefix}시간 · {previewTime}{preferences.replyDensity === 'DETAILED' ? <><br />{locationPrefix}장소 · 3층 회의실</> : null}</>
      : <>팀 주간 회의<br />{timePrefix}{previewTime}{preferences.replyDensity === 'DETAILED' ? <><br />{locationPrefix}3층 회의실 · 1시간</> : null}</>;
  const patch = <K extends keyof ChatPreferences>(key: K, value: ChatPreferences[K]) =>
    setPreferences((current) => ({ ...current, [key]: value }));
  const toggleReminder = (minutes: 60 | 1440 | 4320 | 10080) => setPreferences((current) => ({
    ...current,
    defaultReminderMinutes: current.defaultReminderMinutes.includes(minutes)
      ? current.defaultReminderMinutes.filter((value) => value !== minutes)
      : [...current.defaultReminderMinutes, minutes],
  }));
  const openHelp = (topic: HelpKey, rect: DOMRect) => {
    const width = Math.min(300, window.innerWidth - 32);
    const above = rect.bottom + 180 > window.innerHeight;
    setHelpPopover({
      topic,
      top: above ? rect.top - 9 : rect.bottom + 9,
      left: Math.max(16, Math.min(rect.left - 12, window.innerWidth - width - 16)),
      above,
    });
  };
  const enablePush = async () => {
    setPushState('pending'); setPushError(undefined);
    try { await subscribeCurrentDevice(); setPushState('enabled'); }
    catch (error) { setPushState('idle'); setPushError(error instanceof Error ? error.message : '알림을 설정하지 못했습니다.'); }
  };
  const disablePush = async () => {
    setPushState('pending'); setPushError(undefined);
    try { await unsubscribeCurrentDevice(); setPushState('disabled'); }
    catch (error) { setPushState('idle'); setPushError(error instanceof Error ? error.message : '알림을 해제하지 못했습니다.'); }
  };
  const testPush = async () => {
    setPushState('pending'); setPushError(undefined); setPushNotice(undefined);
    try {
      await showDeviceTestNotification();
      const result = await sendTestPush();
      if (result.sent === 0) throw new Error(result.failed > 0
        ? `푸시 서버 전송에 실패했습니다${result.error ? ` (${result.error})` : ''}.`
        : '먼저 이 기기에서 알림을 허용해 주세요.');
      setPushState('enabled');
      setPushNotice('기기 알림과 서버 원격 푸시를 각각 발송했어요.');
    } catch (error) { setPushState('idle'); setPushError(error instanceof Error ? error.message : '테스트 알림을 보내지 못했습니다.'); }
  };

  return (
    <div className="settings-panel">
      <div className="settings-tabs" role="tablist" aria-label="설정 분류">
        {([
          ['reply', '답장 설정'],
          ['schedule', '일정 설정'],
          ['connection', '연결·알림'],
        ] as const).map(([value, label]) => (
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === value}
            className={activeTab === value ? 'is-active' : ''}
            key={value}
            onClick={() => setActiveTab(value)}
          >
            <SettingsTabIcon tab={value} />
            <span>{label}</span>
          </button>
        ))}
      </div>
      <div className="reply-settings">
        <section className="settings-group" hidden={activeTab !== 'reply'}>
          <div className="settings-group-heading">
            <div className="settings-heading-row"><h3>답장 컨셉</h3><InfoButton topic="style" onOpen={openHelp} /></div>
            <p>CalTalk이 답하는 말투를 선택해요.</p>
          </div>
          <div className="reply-style-grid">
            {STYLE_OPTIONS.map((option) => (
              <button type="button" key={option.value} className={preferences.replyStyle === option.value ? 'reply-style-card is-selected' : 'reply-style-card'} onClick={() => patch('replyStyle', option.value)}>
                <strong>{option.label}</strong><span>{option.description}</span>
              </button>
            ))}
          </div>
        </section>

        <section className="settings-preview" aria-label="답장 미리보기" hidden={activeTab !== 'reply'}>
          <span>답장 미리보기</span>
          <div><strong>{previewHeading}</strong><p>{previewLines}</p></div>
        </section>

        <section className="settings-group settings-compact-grid" hidden={activeTab !== 'reply'}>
          <div><div className="settings-heading-row"><h3>문단 구성</h3><InfoButton topic="layout" onOpen={openHelp} /></div><div className="settings-choice-row">
            <Choice value="COMPACT" current={preferences.replyLayout} label="압축형" onChange={(value) => patch('replyLayout', value)} /><Choice value="BALANCED" current={preferences.replyLayout} label="균형형" onChange={(value) => patch('replyLayout', value)} /><Choice value="SECTIONED" current={preferences.replyLayout} label="구분형" onChange={(value) => patch('replyLayout', value)} />
          </div></div>
          <div><div className="settings-heading-row"><h3>정보량</h3><InfoButton topic="density" onOpen={openHelp} /></div><div className="settings-choice-row">
            <Choice value="ESSENTIAL" current={preferences.replyDensity} label="핵심만" onChange={(value) => patch('replyDensity', value)} /><Choice value="STANDARD" current={preferences.replyDensity} label="표준" onChange={(value) => patch('replyDensity', value)} /><Choice value="DETAILED" current={preferences.replyDensity} label="자세히" onChange={(value) => patch('replyDensity', value)} />
          </div></div>
          <div><div className="settings-heading-row"><h3>아이콘·이모지</h3><InfoButton topic="emoji" onOpen={openHelp} /></div><div className="settings-choice-row">
            <Choice value="NONE" current={preferences.emojiLevel} label="사용 안 함" onChange={(value) => patch('emojiLevel', value)} /><Choice value="MINIMAL" current={preferences.emojiLevel} label="최소" onChange={(value) => patch('emojiLevel', value)} /><Choice value="BALANCED" current={preferences.emojiLevel} label="적절히" onChange={(value) => patch('emojiLevel', value)} />
          </div></div>
          <div><div className="settings-heading-row"><h3>시간 표시</h3><InfoButton topic="time" onOpen={openHelp} /></div><div className="settings-choice-row">
            <Choice value="TWELVE_HOUR" current={preferences.timeFormat} label="오전·오후" onChange={(value) => patch('timeFormat', value)} /><Choice value="TWENTY_FOUR_HOUR" current={preferences.timeFormat} label="24시간" onChange={(value) => patch('timeFormat', value)} />
          </div></div>
        </section>

        <section className="settings-group settings-compact-grid settings-schedule-basics" hidden={activeTab !== 'schedule'}>
          <div><div className="settings-heading-row"><h3>기본 일정 길이</h3><InfoButton topic="duration" onOpen={openHelp} /></div><div className="settings-choice-row">
            <Choice value={30} current={preferences.defaultDurationMinutes} label="30분" onChange={(value) => patch('defaultDurationMinutes', value)} /><Choice value={60} current={preferences.defaultDurationMinutes} label="1시간" onChange={(value) => patch('defaultDurationMinutes', value)} /><Choice value={120} current={preferences.defaultDurationMinutes} label="2시간" onChange={(value) => patch('defaultDurationMinutes', value)} />
          </div></div>
          <div><div className="settings-heading-row"><h3>기본 일정 알림</h3><InfoButton topic="reminder" onOpen={openHelp} /></div><div className="settings-choice-row settings-reminder-choices">
            {([[10080, '1주일 전'], [4320, '3일 전'], [1440, '1일 전'], [60, '1시간 전']] as const).map(([minutes, label]) => (
              <button type="button" key={minutes} className={preferences.defaultReminderMinutes.includes(minutes) ? 'settings-choice is-selected' : 'settings-choice'} onClick={() => toggleReminder(minutes)}>{label}</button>
            ))}
          </div></div>
          <div><div className="settings-heading-row"><h3>기본 조회 범위</h3><InfoButton topic="range" onOpen={openHelp} /></div><RangePicker value={preferences.defaultQueryRange} onChange={(value) => patch('defaultQueryRange', value)} /></div>
        </section>

        <section className="settings-group settings-confirmations" hidden={activeTab !== 'schedule'}>
          <div className="settings-heading-row"><h3>처리 전 확인</h3><InfoButton topic="confirmation" onOpen={openHelp} /></div>
          <label><span><strong>일정 추가 전 확인</strong><small>등록 내용을 한 번 확인한 뒤 저장해요.</small></span><input type="checkbox" checked={preferences.confirmCreate} onChange={(event) => patch('confirmCreate', event.target.checked)} /></label>
          <label><span><strong>일정 수정 전 확인</strong><small>변경 전 기존 일정과 새 내용을 비교해요.</small></span><input type="checkbox" checked={preferences.confirmUpdate} onChange={(event) => patch('confirmUpdate', event.target.checked)} /></label>
          <p>일정 삭제는 안전을 위해 항상 확인합니다.</p>
        </section>

        <section className="settings-group settings-confirmations" hidden={activeTab !== 'schedule'}>
          <div className="settings-heading-row"><h3>일정 요약 알림</h3></div>
          <label><span><strong>일간 요약</strong><small>오늘 일정을 선택한 시각에 정리해 드려요.</small></span><input type="checkbox" checked={preferences.dailySummaryEnabled} onChange={(event) => patch('dailySummaryEnabled', event.target.checked)} /></label>
          {preferences.dailySummaryEnabled ? <div className="settings-choice-row">
            {['07:00', '08:00', '09:00'].map((time) => <button type="button" key={time} className={preferences.dailySummaryTime === time ? 'settings-choice is-selected' : 'settings-choice'} onClick={() => patch('dailySummaryTime', time)}>{time}</button>)}
          </div> : null}
          <label><span><strong>주간 요약</strong><small>한 주의 일정을 선택한 요일에 미리 보여드려요.</small></span><input type="checkbox" checked={preferences.weeklySummaryEnabled} onChange={(event) => patch('weeklySummaryEnabled', event.target.checked)} /></label>
          {preferences.weeklySummaryEnabled ? <>
            <div className="settings-choice-row">
              {[[1, '월요일'], [7, '일요일']].map(([day, label]) => <button type="button" key={day} className={preferences.weeklySummaryDay === day ? 'settings-choice is-selected' : 'settings-choice'} onClick={() => patch('weeklySummaryDay', day as number)}>{label}</button>)}
            </div>
            <div className="settings-choice-row">
              {['08:00', '18:00', '20:00'].map((time) => <button type="button" key={time} className={preferences.weeklySummaryTime === time ? 'settings-choice is-selected' : 'settings-choice'} onClick={() => patch('weeklySummaryTime', time)}>{time}</button>)}
            </div>
          </> : null}
        </section>

        <section className="settings-push" aria-labelledby="device-push-title" hidden={activeTab !== 'connection'}>
          <div className="settings-push-intro">
            <span className="settings-push-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none"><path d="M7.6 9.8a4.4 4.4 0 0 1 8.8 0c0 5 2.1 5.5 2.1 6.7H5.5c0-1.2 2.1-1.7 2.1-6.7Z" /><path d="M10 19h4" /></svg>
            </span>
            <div>
              <div className="settings-push-title-row">
                <h3 id="device-push-title">이 기기 알림</h3>
                <span className={`settings-push-status is-${pushState}`}>{pushState === 'enabled' ? '사용 중' : pushState === 'disabled' ? '해제됨' : pushState === 'pending' ? '처리 중' : '설정 필요'}</span>
              </div>
              <p>일정 알림과 요약을 지금 사용하는 기기에서 받아보세요.</p>
            </div>
          </div>
          <div className="settings-push-actions">
            <button type="button" className="settings-push-primary" disabled={pushState === 'pending'} onClick={enablePush}>{pushState === 'pending' ? '처리 중…' : pushState === 'enabled' ? '알림 다시 설정' : '이 기기에서 알림 받기'}</button>
            <button type="button" className="settings-push-secondary" disabled={pushState === 'pending'} onClick={testPush}>테스트 알림</button>
          </div>
          <button type="button" className="settings-push-release" disabled={pushState === 'pending'} onClick={disablePush}>이 기기 알림 해제</button>
          <div className="settings-push-feedback" aria-live="polite">
            {pushState === 'enabled' && !pushNotice ? <span>알림을 받을 준비가 완료됐어요.</span> : null}
            {pushState === 'disabled' ? <span>이 기기에서는 더 이상 알림을 받지 않아요.</span> : null}
            {pushNotice ? <span>{pushNotice}</span> : null}
            {pushError ? <span className="settings-push-error" role="alert">{pushError}</span> : null}
          </div>
        </section>

        <div className="settings-save-bar">
          <span>{mutation.isSuccess && !changed ? '설정이 저장됐어요.' : changed ? '변경사항이 있어요.' : '현재 설정이 적용 중이에요.'}</span>
          <button type="button" disabled={!changed || mutation.isPending} onClick={() => mutation.mutate()}>{mutation.isPending ? '저장 중…' : '설정 저장'}</button>
        </div>

        <section className="settings-link-section" hidden={activeTab !== 'connection'}>
          <div className="settings-heading-row"><h3>카카오톡 연동</h3><InfoButton topic="link" onOpen={openHelp} /></div>
          <KakaoLinkPanel showTitle={false} />
        </section>
      </div>

      {helpPopover ? createPortal(<>
        <button type="button" className="settings-popover-scrim" aria-label="설명 닫기" onClick={() => setHelpPopover(null)} />
        <aside
          className={helpPopover.above ? 'settings-info-popover is-above' : 'settings-info-popover'}
          role="tooltip"
          style={{ top: helpPopover.top, left: helpPopover.left }}
        >
          <strong>{HELP[helpPopover.topic].title}</strong>
          <p>{HELP[helpPopover.topic].body}</p>
        </aside>
      </>, document.body) : null}
    </div>
  );
}
