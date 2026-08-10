import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { TouchEvent, WheelEvent } from 'react';
import type { ScheduleListItem } from '../../../api/schedule';
import { CalendarIcon, ListIcon } from '../../../components/common/Icons';
import { getDateKey } from '../dateTime';
import { getHolidayName } from '../holidays';

interface ScheduleCalendarProps {
  schedules?: ScheduleListItem[];
  timeZone: string;
  onSelect: (id: number) => void;
  onDateSelect?: (dateKey: string) => void;
}

const weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const miniWeekDays = ['일', '월', '화', '수', '목', '금', '토'];
const MONTH_FORMATTER = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: 'long',
});

function monthText(date: Date) {
  return MONTH_FORMATTER.format(date);
}

function localDateKey(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

function truncateText(value: string, maxLength: number) {
  if (!value) return value;
  if (maxLength <= 0) return '';

  const normalized = value.replace(/\s+/g, ' ').trim();
  const segmenter = typeof Intl.Segmenter === 'function'
    ? new Intl.Segmenter('ko-KR', { granularity: 'grapheme' })
    : null;
  const segments = segmenter
    ? [...segmenter.segment(normalized)].map((segment) => segment.segment)
    : [...normalized];

  const visibleCount = segments.filter((char) => char.trim() !== '').length;
  if (visibleCount <= maxLength) {
    return normalized;
  }

  let kept: string[] = [];
  let charCount = 0;
  for (const segment of segments) {
    if (segment.trim() !== '') {
      charCount += 1;
      if (charCount > maxLength) {
        break;
      }
    }
    kept.push(segment);
  }

  return `${kept.join('').trim()}...`;
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

const DRAG_DECAY = 0.97;
const WHEEL_STEP_MIN_MOVEMENT = 0.35;
const TOUCH_GAIN = 0.34;
const SNAP_VELOCITY = 0.05;
const MAX_OFFSET = 0.06;
const RETURN_VELOCITY = 0.72;

export function ScheduleCalendar({ schedules, timeZone, onSelect, onDateSelect }: ScheduleCalendarProps) {
  const [monthCursor, setMonthCursor] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selectedDateKey, setSelectedDateKey] = useState<string | null>(null);
  const [monthTransition, setMonthTransition] = useState<'previous' | 'next' | null>(null);
  const [viewMode, setViewMode] = useState<'month' | 'year'>('month');
  const [contentMode, setContentMode] = useState<'calendar' | 'list'>('calendar');
  const [yearCursor, setYearCursor] = useState(() => new Date().getFullYear());

  const calendarPanelRef = useRef<HTMLElement | null>(null);
  const calendarGridRef = useRef<HTMLOListElement | null>(null);
  const touchStartY = useRef<number | null>(null);
  const touchLastY = useRef<number | null>(null);
  const translateYRef = useRef(0);
  const velocityRef = useRef(0);
  const rafRef = useRef<number | null>(null);
  const pendingMonthStepsRef = useRef(0);
  const panelHeightRef = useRef(0);
  const lastFrameRef = useRef(0);
  const yearWheelLockedRef = useRef(false);
  const yearWheelResetRef = useRef<number | null>(null);

  const monthTitle = useMemo(() => {
    const formatter = new Intl.DateTimeFormat('ko-KR', {
      timeZone,
      year: 'numeric',
      month: 'long',
    });
    return formatter.format(monthCursor);
  }, [monthCursor, timeZone]);
  const prevMonthLabel = useMemo(
    () => monthText(new Date(monthCursor.getFullYear(), monthCursor.getMonth() - 1, 1)),
    [monthCursor],
  );
  const nextMonthLabel = useMemo(
    () => monthText(new Date(monthCursor.getFullYear(), monthCursor.getMonth() + 1, 1)),
    [monthCursor],
  );

  useEffect(() => {
    const now = new Date();
    setMonthCursor(new Date(now.getFullYear(), now.getMonth(), 1));
  }, [timeZone]);

  useEffect(() => {
    const panel = calendarPanelRef.current;
    if (panel) {
      panelHeightRef.current = panel.clientHeight || 400;
    }
  }, [monthCursor]);

  const schedulesByDate = useMemo(() => {
    const map = new Map<string, ScheduleListItem[]>();
    for (const schedule of schedules ?? []) {
      const key = getDateKey(schedule.startAt, timeZone);
      const list = map.get(key);
      if (!list) {
        map.set(key, [schedule]);
      } else {
        list.push(schedule);
      }
    }
    for (const list of map.values()) {
      list.sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());
    }
    return map;
  }, [schedules, timeZone]);

  const firstDay = useMemo(() => new Date(monthCursor.getFullYear(), monthCursor.getMonth(), 1), [monthCursor]);
  const startOffset = firstDay.getDay();
  const todayKey = useMemo(() => getDateKey(new Date().toISOString(), timeZone), [timeZone]);

  const dateFormatter = useMemo(
    () =>
      new Intl.DateTimeFormat('en-CA', {
        timeZone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      }),
    [timeZone],
  );
  const days = useMemo(() => {
    const totalCells = 42;
    const items: {
      date: number;
      key: string;
      isCurrentMonth: boolean;
      isWeekend: boolean;
      isToday: boolean;
      isHoliday: boolean;
      holidayName: string | null;
    }[] = [];

    for (let index = 0; index < totalCells; index += 1) {
      const dayDate = new Date(firstDay.getFullYear(), firstDay.getMonth(), 1 - startOffset + index);
      const key = dateFormatter.format(dayDate);
      const dayNumber = dayDate.getDate();
      const holidayName = getHolidayName(key);
      items.push({
        date: dayNumber,
        key,
        isHoliday: Boolean(holidayName),
        holidayName,
        isCurrentMonth: dayDate.getMonth() === monthCursor.getMonth(),
        isWeekend: dayDate.getDay() === 0 || dayDate.getDay() === 6,
        isToday: key === todayKey,
      });
    }

    return items;
  }, [firstDay, startOffset, monthCursor, dateFormatter, todayKey]);

  const applyGridTransform = useCallback((translateY: number) => {
    const grid = calendarGridRef.current;
    if (!grid) return;
    const panelHeight = Math.max(1, panelHeightRef.current);
    const clamped = clamp(translateY, -panelHeight * MAX_OFFSET, panelHeight * MAX_OFFSET);
    const offset = Math.min(Math.abs(clamped) / panelHeight, 1);
    const opacity = 1 - offset * 0.2;

    grid.style.transform = `translateY(${-clamped}px)`;
    grid.style.opacity = opacity.toFixed(3);
  }, []);

  const setMonth = useCallback((delta: number) => {
    setMonthTransition(delta > 0 ? 'next' : 'previous');
    setMonthCursor((current) => new Date(current.getFullYear(), current.getMonth() + delta, 1));
  }, []);

  const stopAnimation = useCallback((resetToCenter = false) => {
    if (rafRef.current !== null) {
      window.cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    if (resetToCenter && calendarGridRef.current) {
      calendarGridRef.current.style.transform = 'translateY(0px)';
      calendarGridRef.current.style.opacity = '1';
    }
    translateYRef.current = 0;
    velocityRef.current = 0;
    pendingMonthStepsRef.current = 0;
  }, []);

  const animate = useCallback(() => {
    const panelHeight = Math.max(1, panelHeightRef.current);
    const rowHeight = panelHeight / 6;
    const threshold = Math.max(16, rowHeight * 0.25);
    const now = performance.now();
    const last = lastFrameRef.current || now;
    const dt = Math.min(48, now - last);
    const normalized = dt / 16.666;

    let offset = translateYRef.current + velocityRef.current * normalized;
    const velocity = velocityRef.current * Math.pow(DRAG_DECAY, normalized);

    const monthSteps = offset > 0 ? Math.floor(offset / threshold) : Math.ceil(offset / threshold);
    if (monthSteps !== 0) {
      pendingMonthStepsRef.current += monthSteps;
      offset -= monthSteps * threshold;
    }

    translateYRef.current = clamp(offset, -threshold, threshold);
    velocityRef.current = velocity;
    applyGridTransform(translateYRef.current);

    const isStillMoving = Math.abs(velocityRef.current) > SNAP_VELOCITY;
    if (!isStillMoving) {
      if (Math.abs(translateYRef.current) > 1) {
        translateYRef.current *= RETURN_VELOCITY;
        applyGridTransform(translateYRef.current);
        lastFrameRef.current = now;
        rafRef.current = window.requestAnimationFrame(animate);
        return;
      }

      if (pendingMonthStepsRef.current !== 0) {
        setMonth(pendingMonthStepsRef.current);
        pendingMonthStepsRef.current = 0;
      }
      stopAnimation(true);
      return;
    }

    lastFrameRef.current = now;
    rafRef.current = window.requestAnimationFrame(animate);
  }, [applyGridTransform, setMonth, stopAnimation]);

  const startScrollMomentum = useCallback(
    (deltaY: number, deltaMode: number) => {
      const scale = deltaMode === 1 ? 16 : deltaMode === 2 ? 800 : 1;
      const scaledDelta = deltaY * scale;
      const absDelta = Math.abs(scaledDelta);
      if (absDelta < WHEEL_STEP_MIN_MOVEMENT) {
        return;
      }
      setMonth(scaledDelta > 0 ? 1 : -1);
    },
    [setMonth],
  );

  const onCalendarWheelNative = useCallback(
    (event: WheelEvent) => {
      if (Math.abs(event.deltaY) < 0.5) {
        return;
      }
      startScrollMomentum(event.deltaY, event.deltaMode);
    },
    [startScrollMomentum],
  );

  useEffect(() => {
    return () => {
      if (rafRef.current !== null) {
        window.cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
      lastFrameRef.current = 0;
    };
  }, []);

  const onCalendarTouchStart = useCallback((event: TouchEvent<HTMLElement>) => {
    const touch = event.touches[0];
    if (!touch) return;
    if (rafRef.current !== null) {
      window.cancelAnimationFrame(rafRef.current);
      rafRef.current = null;
    }
    touchStartY.current = touch.clientY;
    touchLastY.current = touch.clientY;
    velocityRef.current = 0;
    translateYRef.current = 0;
    pendingMonthStepsRef.current = 0;
  }, []);

  const onCalendarTouchMove = useCallback(
    (event: TouchEvent<HTMLElement>) => {
      const touch = event.touches[0];
      if (!touch || touchStartY.current === null) return;
      const nextY = touch.clientY;
      const prevY = touchLastY.current;
      if (prevY === null) {
        touchLastY.current = nextY;
        return;
      }

      const delta = nextY - prevY;
      touchLastY.current = nextY;
      const direction = delta * -1;
      translateYRef.current += direction * TOUCH_GAIN;
      const panelHeight = Math.max(1, panelHeightRef.current);
      const rowHeight = panelHeight / 6;
      const threshold = Math.max(24, rowHeight * 0.35);
      translateYRef.current = clamp(translateYRef.current, -threshold, threshold);
      velocityRef.current = (velocityRef.current * 0.4) + direction * 0.45;

      if (velocityRef.current > 999) velocityRef.current = 999;
      if (velocityRef.current < -999) velocityRef.current = -999;

      applyGridTransform(translateYRef.current);
      if (rafRef.current === null) {
        lastFrameRef.current = performance.now();
        rafRef.current = window.requestAnimationFrame(animate);
      }
    },
    [applyGridTransform, animate],
  );

  const onCalendarTouchEnd = useCallback(() => {
    if (touchStartY.current === null) return;
    touchStartY.current = null;
    touchLastY.current = null;
    if (Math.abs(velocityRef.current) < SNAP_VELOCITY && rafRef.current !== null) {
      stopAnimation(true);
      return;
    }
    if (rafRef.current === null) {
      lastFrameRef.current = performance.now();
      rafRef.current = window.requestAnimationFrame(animate);
    }
  }, [animate, stopAnimation]);

  const onMonthArrow = useCallback(
    (delta: number) => {
      setMonth(delta);
      stopAnimation(true);
    },
    [setMonth, stopAnimation],
  );

  const openYearView = useCallback(() => {
    stopAnimation(true);
    setYearCursor(monthCursor.getFullYear());
    setViewMode('year');
  }, [monthCursor, stopAnimation]);

  const openMonth = useCallback((month: number) => {
    setMonthCursor(new Date(yearCursor, month, 1));
    setMonthTransition(null);
    setViewMode('month');
  }, [yearCursor]);

  const returnToMonthForYear = useCallback(() => {
    setMonthCursor((current) => new Date(yearCursor, current.getMonth(), 1));
    setMonthTransition(null);
    setViewMode('month');
  }, [yearCursor]);

  const yearMonths = useMemo(() => Array.from({ length: 12 }, (_, month) => {
    const firstWeekday = new Date(yearCursor, month, 1).getDay();
    const dayCount = new Date(yearCursor, month + 1, 0).getDate();
    const cells = Array.from({ length: firstWeekday + dayCount }, (_, index) => {
      if (index < firstWeekday) return null;
      const day = index - firstWeekday + 1;
      const key = localDateKey(yearCursor, month, day);
      return {
        day,
        key,
        isToday: key === todayKey,
        isHoliday: Boolean(getHolidayName(key)),
        hasSchedule: (schedulesByDate.get(key)?.length ?? 0) > 0,
      };
    });
    return { month, cells };
  }), [schedulesByDate, todayKey, yearCursor]);

  const monthScheduleGroups = useMemo(() => {
    const monthPrefix = `${monthCursor.getFullYear()}-${String(monthCursor.getMonth() + 1).padStart(2, '0')}`;
    return [...schedulesByDate.entries()]
      .filter(([key]) => key.startsWith(monthPrefix))
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([key, items]) => {
        const date = new Date(`${key}T12:00:00`);
        return {
          key,
          dayLabel: new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' }).format(date),
          holidayName: getHolidayName(key),
          items,
        };
      });
  }, [monthCursor, schedulesByDate]);

  const listTimeFormatter = useMemo(() => new Intl.DateTimeFormat('ko-KR', {
    timeZone,
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  }), [timeZone]);

  const onYearWheel = useCallback((event: WheelEvent<HTMLElement>) => {
    if (Math.abs(event.deltaY) < 12) return;
    if (yearWheelResetRef.current !== null) window.clearTimeout(yearWheelResetRef.current);
    yearWheelResetRef.current = window.setTimeout(() => {
      yearWheelLockedRef.current = false;
      yearWheelResetRef.current = null;
    }, 220);
    if (yearWheelLockedRef.current) return;
    yearWheelLockedRef.current = true;
    setYearCursor((year) => year + (event.deltaY > 0 ? 1 : -1));
  }, []);

  useEffect(() => () => {
    if (yearWheelResetRef.current !== null) window.clearTimeout(yearWheelResetRef.current);
  }, []);

  return (
    <section className="calendar-view" aria-label="calendar">
      {viewMode === 'month' ? (
        <div className="calendar-view-heading">
          <button className="calendar-year-trigger" type="button" onClick={openYearView}>
            <span className="calendar-year-trigger-icon" aria-hidden="true">‹</span>
            <span className="calendar-year-trigger-label">{monthCursor.getFullYear()}년</span>
          </button>
          <button
            className="calendar-content-toggle"
            type="button"
            onClick={() => setContentMode((mode) => mode === 'calendar' ? 'list' : 'calendar')}
            aria-label={contentMode === 'calendar' ? '이번 달 일정 목록 보기' : '월간 캘린더 보기'}
            title={contentMode === 'calendar' ? '일정 목록' : '캘린더'}
          >
            {contentMode === 'calendar' ? <ListIcon /> : <CalendarIcon />}
          </button>
        </div>
      ) : null}
      <div className={`calendar-frame${viewMode === 'year' ? ' is-year-view' : ''}`}>
        {viewMode === 'year' ? (
          <section
            className="year-calendar calendar-view-enter-year"
            aria-label={`${yearCursor}년 연간 캘린더`}
            onWheelCapture={onYearWheel}
          >
            <header className="year-calendar-header">
              <button type="button" onClick={() => setYearCursor((year) => year - 1)} aria-label={`${yearCursor - 1}년 보기`}>
                <span aria-hidden="true">‹</span>
                <strong className="year-calendar-button-label">{yearCursor - 1}년</strong>
              </button>
              <button
                className="year-calendar-current"
                type="button"
                onClick={returnToMonthForYear}
                aria-label={`${yearCursor}년 ${monthCursor.getMonth() + 1}월 캘린더로 돌아가기`}
              >
                <CalendarIcon />
                <strong>{yearCursor}년</strong>
              </button>
              <button type="button" onClick={() => setYearCursor((year) => year + 1)} aria-label={`${yearCursor + 1}년 보기`}>
                <strong className="year-calendar-button-label">{yearCursor + 1}년</strong>
                <span aria-hidden="true">›</span>
              </button>
            </header>
            <div className="year-calendar-grid" key={yearCursor}>
              {yearMonths.map(({ month, cells }) => (
                <button
                  className={`year-month${month === monthCursor.getMonth() && yearCursor === monthCursor.getFullYear() ? ' is-active' : ''}`}
                  type="button"
                  key={month}
                  onClick={() => openMonth(month)}
                  aria-label={`${yearCursor}년 ${month + 1}월 월간 보기`}
                >
                  <strong>{month + 1}월</strong>
                  <span className="year-month-weekdays" aria-hidden="true">
                    {miniWeekDays.map((day) => <i key={day}>{day}</i>)}
                  </span>
                  <span className="year-month-days" aria-hidden="true">
                    {cells.map((cell, index) => cell ? (
                      <i
                        key={cell.key}
                        className={`${cell.isToday ? ' is-today' : ''}${cell.isHoliday ? ' is-holiday' : ''}${cell.hasSchedule ? ' has-schedule' : ''}`}
                      >
                        {cell.day}
                      </i>
                    ) : <i key={`blank-${index}`} />)}
                  </span>
                </button>
              ))}
            </div>
          </section>
        ) : (
          <>
        <header className="calendar-toolbar calendar-view-enter-month">
          <div className="calendar-toolbar-slot calendar-toolbar-slot-start">
            <button
              className="calendar-toolbar-badge"
              type="button"
              onClick={() => onMonthArrow(-1)}
              aria-label={`${prevMonthLabel} 이전 달`}
            >
              <span aria-hidden="true">◀</span>
              <span>{prevMonthLabel}</span>
            </button>
          </div>

          <div className="calendar-toolbar-slot calendar-toolbar-slot-center">
            <span className="calendar-toolbar-badge">{monthTitle}</span>
          </div>

          <div className="calendar-toolbar-slot calendar-toolbar-slot-end">
            <button
              className="calendar-toolbar-badge"
              type="button"
              onClick={() => onMonthArrow(1)}
              aria-label={`${nextMonthLabel} 다음 달`}
            >
              <span>{nextMonthLabel}</span>
              <span aria-hidden="true">▶</span>
            </button>
          </div>
        </header>

        {contentMode === 'calendar' ? <section
          className="calendar-grid-panel calendar-view-enter-month"
          ref={calendarPanelRef}
          aria-label="일정 캘린더"
          onWheelCapture={onCalendarWheelNative}
          onTouchStart={onCalendarTouchStart}
          onTouchMove={onCalendarTouchMove}
          onTouchEnd={onCalendarTouchEnd}
        >
          <div className="calendar-weekdays" role="row">
            {weekDays.map((day) => (
              <div role="columnheader" key={day}>
                {day}
              </div>
            ))}
          </div>
          <ol
            key={`${monthCursor.getFullYear()}-${monthCursor.getMonth()}`}
            className={`calendar-grid${monthTransition ? ` calendar-month-enter-${monthTransition}` : ''}`}
            ref={calendarGridRef}
          >
            {days.map((day) => {
              const dayItems = schedulesByDate.get(day.key) ?? [];
              const displayItems = dayItems.slice(0, 2);
              const isSelected = day.key === selectedDateKey;
              return (
                <li
                  key={`${day.key}-${day.date}`}
                  className={`calendar-cell ${day.isCurrentMonth ? '' : 'is-outside'} ${day.isWeekend ? 'is-weekend' : ''} ${
                    isSelected ? 'is-selected' : ''
                  } ${day.isHoliday ? 'is-holiday' : ''}`}
                >
                  <button
                    className="calendar-cell-button"
                    type="button"
                    aria-label={`${monthCursor.getMonth() + 1}월 ${day.date}일${day.holidayName ? ` ${day.holidayName}` : ''} 일정`}
                    aria-pressed={isSelected}
                    onClick={() => {
                      if (onDateSelect) {
                        onDateSelect(day.key);
                        return;
                      }
                      setSelectedDateKey(day.key);
                    }}
                  >
                    <div className="calendar-day-header">
                      <span
                        className={[
                          'calendar-day',
                          day.isCurrentMonth ? 'calendar-day-current' : 'calendar-day-outside',
                          day.isHoliday ? 'calendar-day-holiday' : '',
                          day.isToday ? 'calendar-day-today' : '',
                        ]
                          .filter(Boolean)
                          .join(' ')}
                      >
                        {day.date}
                      </span>
                      {day.holidayName ? <span className="calendar-holiday-name">{day.holidayName}</span> : null}
                    </div>
                    <div className="calendar-events" aria-label={`${dayItems.length}개 일정`}>
                      {displayItems.length > 0 ? (
                        displayItems.map((item) => (
                        <div
                          key={item.id}
                          className="calendar-item"
                          role="button"
                          tabIndex={0}
                            onClick={(event) => {
                              event.stopPropagation();
                              onSelect(item.id);
                            }}
                            onKeyDown={(event) => {
                              if (event.key === 'Enter' || event.key === ' ') {
                                event.preventDefault();
                                event.stopPropagation();
                                onSelect(item.id);
                              }
                            }}
                          >
                            <span className="calendar-item-title" title={item.title}>
                              {truncateText(item.title, 8)}
                            </span>
                          </div>
                        ))
                      ) : (
                        <span className="calendar-no-event" />
                      )}
                      {dayItems.length > 2 ? <span className="calendar-more">+ {dayItems.length - 2}</span> : null}
                    </div>
                  </button>
                </li>
              );
            })}
          </ol>
        </section> : (
          <section className="calendar-month-list calendar-view-enter-month" aria-label={`${monthTitle} 일정 목록`}>
            {monthScheduleGroups.length > 0 ? monthScheduleGroups.map((group) => (
              <section className="calendar-month-list-group" key={group.key}>
                <header>
                  <strong>{group.dayLabel}</strong>
                  {group.holidayName ? <span>{group.holidayName}</span> : null}
                </header>
                <div className="calendar-month-list-items">
                  {group.items.map((item) => (
                    <button type="button" key={item.id} onClick={() => onSelect(item.id)}>
                      <span className="calendar-month-list-time">
                        {listTimeFormatter.format(new Date(item.startAt))}
                        <i aria-hidden="true">–</i>
                        {listTimeFormatter.format(new Date(item.endAt))}
                      </span>
                      <span className="calendar-month-list-copy">
                        <strong>{item.title}</strong>
                        {item.location ? <small>{item.location}</small> : null}
                      </span>
                      <span className="calendar-month-list-arrow" aria-hidden="true">›</span>
                    </button>
                  ))}
                </div>
              </section>
            )) : (
              <div className="calendar-month-list-empty">
                <strong>{monthTitle}</strong>
                <p>등록된 일정이 없습니다.</p>
              </div>
            )}
          </section>
        )}
          </>
        )}
      </div>
    </section>
  );
}
