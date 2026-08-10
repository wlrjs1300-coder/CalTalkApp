import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';

import { AppLayout } from '../components/layout/AppLayout';
import { logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/authQuery';
import { DialogShell } from '../features/schedule/components/DialogShell';
import { getDateKey, getDayRangeFromDate, scheduleQueryRange } from '../features/schedule/dateTime';
import { getHolidayName } from '../features/schedule/holidays';
import { useSchedules } from '../features/schedule/queries';
import { SettingsPanel } from '../features/user/SettingsPanel';
import { useMutation, useQueryClient } from '@tanstack/react-query';

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function DailySchedulePage() {
  const { date } = useParams();
  const navigate = useNavigate();
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();

  const [settingsOpen, setSettingsOpen] = useState(false);

  const logoutMutation = useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
      navigate('/welcome', { replace: true });
    },
  });

  const isValidDate = Boolean(date && DATE_PATTERN.test(date));
  const timezone = currentUser.data?.timezone ?? 'Asia/Seoul';

  const safeRange = useMemo(() => {
    if (!isValidDate || !date) {
      return scheduleQueryRange();
    }
    try {
      return getDayRangeFromDate(date, timezone);
    } catch {
      return scheduleQueryRange();
    }
  }, [date, isValidDate, timezone]);

  const scheduleQuery = useSchedules(safeRange.from, safeRange.to);
  const schedules = useMemo(() => {
    if (!isValidDate || !date || !scheduleQuery.data?.items) {
      return [];
    }
    return scheduleQuery.data.items
      .filter((item) => getDateKey(item.startAt, timezone) === date)
      .sort((left, right) => new Date(left.startAt).getTime() - new Date(right.startAt).getTime());
  }, [date, isValidDate, timezone, scheduleQuery.data]);

  const dayLabel = useMemo(() => {
    if (!isValidDate || !date) {
      return '일정';
    }
    const [year, month, day] = date.split('-');
    const sample = new Date(Number(year), Number(month) - 1, Number(day));
    if (Number.isNaN(sample.getTime())) return '일정';
    return new Intl.DateTimeFormat('ko-KR', {
      timeZone: timezone,
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'short',
    }).format(sample);
  }, [date, isValidDate, timezone]);

  const datePresentation = useMemo(() => {
    if (!isValidDate || !date) return null;
    const [year, month, day] = date.split('-').map(Number);
    const value = new Date(year, month - 1, day);
    if (Number.isNaN(value.getTime())) return null;
    return {
      yearMonth: new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long' }).format(value),
      day: String(day),
      weekday: new Intl.DateTimeFormat('ko-KR', { weekday: 'long' }).format(value),
      holidayName: getHolidayName(date),
    };
  }, [date, isValidDate]);

  const openDetail = (scheduleId: number) => {
    if (!date) return;
    navigate(`/day/${date}/event/${scheduleId}`);
  };

  if (!currentUser.data) return null;

  return (
    <AppLayout
      email={currentUser.data.email}
      onOpenSettings={() => setSettingsOpen(true)}
      onLogout={() => logoutMutation.mutate()}
      logoutPending={logoutMutation.isPending}
    >
      <section className="daily-schedule-page">
        <header className="daily-schedule-toolbar">
          <Link to="/" className="daily-back-button" aria-label="캘린더로 돌아가기">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 18-6-6 6-6" /></svg>
          </Link>
          <div className="daily-schedule-title-block">
            <p className="daily-schedule-date-label">하루 일정</p>
            <h1 className="daily-schedule-date-title">{dayLabel}</h1>
          </div>
          <Link to="/" className="daily-calendar-button" aria-label="캘린더 보기">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 2v3M18 2v3M3 9h18M5 4h14a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z" /></svg>
          </Link>
        </header>

        {!isValidDate ? (
          <div className="empty-state daily-empty-state">
            <h3>요청한 날짜 형식이 올바르지 않습니다.</h3>
            <p>달력에서 날짜를 다시 선택해 주세요.</p>
            <button type="button" className="primary-button compact-button" onClick={() => navigate('/')}>
              캘린더로 이동
            </button>
          </div>
        ) : (
          <div className="daily-schedule-content">
            {datePresentation ? (
              <div className="daily-date-hero" aria-label={dayLabel}>
                <div className="daily-date-card">
                  <span>{datePresentation.yearMonth}</span>
                  <strong>{datePresentation.day}</strong>
                  <span>{datePresentation.weekday}</span>
                </div>
                <div className="daily-date-summary">
                  {datePresentation.holidayName ? (
                    <span className="daily-holiday-badge">공휴일 · {datePresentation.holidayName}</span>
                  ) : null}
                  <p>{schedules.length ? '예정된 일정' : '여유로운 하루예요'}</p>
                  <strong>{schedules.length ? `${schedules.length}개의 일정` : '등록된 일정이 없어요'}</strong>
                  <span>{schedules.length ? '시간 순서대로 확인해 보세요.' : '새로운 계획을 추가해 보세요.'}</span>
                </div>
              </div>
            ) : null}
            {scheduleQuery.isPending ? <div className="status-state">일정을 불러오는 중입니다...</div> : null}
            {scheduleQuery.error ? (
              <div className="inline-state" role="alert">
                <p>
                  {scheduleQuery.error instanceof ApiError && scheduleQuery.error.status === 401
                    ? '로그인이 필요합니다.'
                    : scheduleQuery.error.message}
                </p>
                <button
                  type="button"
                  className="secondary-button compact-button"
                  onClick={() => scheduleQuery.refetch()}
                >
                  다시 시도
                </button>
              </div>
            ) : null}

            {schedules.length === 0 && !scheduleQuery.isPending ? (
              <div className="empty-state daily-empty-state">
                <span className="daily-empty-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" /></svg>
                </span>
                <h3>이날은 아직 비어 있어요</h3>
                <p>캘린더로 돌아가 새로운 일정을 추가해 보세요.</p>
                <Link to="/" className="primary-button compact-button daily-empty-cta">
                  일정 추가하기
                </Link>
              </div>
            ) : null}

            {schedules.length > 0 ? (
              <ul className="schedule-list daily-schedule-list" aria-label="선택 날짜의 일정 목록">
                {schedules.map((schedule) => {
                  const start = new Date(schedule.startAt);
                  const end = new Date(schedule.endAt);
                  const startTime = Number.isNaN(start.getTime())
                    ? ''
                    : new Intl.DateTimeFormat('ko-KR', {
                        timeZone: timezone,
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: false,
                      }).format(start);
                  const endTime = Number.isNaN(end.getTime())
                    ? ''
                    : new Intl.DateTimeFormat('ko-KR', {
                        timeZone: timezone,
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: false,
                      }).format(end);
                  const content = schedule.location?.trim() || '위치 정보 없음';

                  return (
                    <li className="schedule-item daily-schedule-item" key={schedule.id}>
                      <button
                        type="button"
                        className="schedule-item-button daily-schedule-item-button"
                        onClick={() => openDetail(schedule.id)}
                        aria-label={`${schedule.title} 상세 보기`}
                      >
                        <span className="daily-schedule-marker" aria-hidden="true" />
                        <span className="daily-schedule-time">
                          <span className="daily-schedule-time-main">{startTime}</span>
                          <span className="daily-schedule-time-sub">{endTime ? `~ ${endTime}` : ''}</span>
                        </span>
                        <span className="schedule-main">
                          <strong className="schedule-item-title">{schedule.title}</strong>
                          <span className="schedule-item-content">{content}</span>
                        </span>
                        <span className="daily-schedule-arrow" aria-hidden="true">
                          <svg viewBox="0 0 24 24"><path d="m9 18 6-6-6-6" /></svg>
                        </span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            ) : null}
          </div>
        )}
      </section>

      {settingsOpen ? (
        <DialogShell title="설정" description="필요한 연결만 빠르게 관리해요." onClose={() => setSettingsOpen(false)}>
          <SettingsPanel onSaved={() => setSettingsOpen(false)} />
        </DialogShell>
      ) : null}
    </AppLayout>
  );
}
