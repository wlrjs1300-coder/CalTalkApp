import { QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { approveConfirmation } from '../api/confirmation';
import { ApiError } from '../api/errors';
import {
  createSchedule,
  deleteSchedule,
  getSchedule,
  getSchedules,
  updateSchedule,
  type ScheduleDetail,
} from '../api/schedule';
import { updateTimezone } from '../api/user';
import { createQueryClient } from '../app/providers/queryClient';
import { currentUserQueryKey } from '../features/auth/authQuery';
import { ScheduleWorkspace } from '../features/schedule/components/ScheduleWorkspace';
import { scheduleKeys } from '../features/schedule/queries';
import { TimezoneForm } from '../features/user/TimezoneForm';

vi.mock('../api/schedule', () => ({
  getSchedules: vi.fn(),
  getSchedule: vi.fn(),
  createSchedule: vi.fn(),
  updateSchedule: vi.fn(),
  deleteSchedule: vi.fn(),
}));

vi.mock('../api/confirmation', () => ({ approveConfirmation: vi.fn() }));
vi.mock('../api/user', () => ({ updateTimezone: vi.fn(), getCurrentUser: vi.fn() }));

const getSchedulesMock = vi.mocked(getSchedules);
const getScheduleMock = vi.mocked(getSchedule);
const createScheduleMock = vi.mocked(createSchedule);
const updateScheduleMock = vi.mocked(updateSchedule);
const deleteScheduleMock = vi.mocked(deleteSchedule);
const approveMock = vi.mocked(approveConfirmation);
const updateTimezoneMock = vi.mocked(updateTimezone);

const detail: ScheduleDetail = {
  id: 7,
  title: '팀 회의',
  startAt: '2026-08-10T00:00:00.000Z',
  endAt: '2026-08-10T01:00:00.000Z',
  location: '회의실',
  version: 4,
  createdAt: '2026-08-01T00:00:00Z',
  updatedAt: '2026-08-01T00:00:00Z',
};

function listOf(...items: ScheduleDetail[]) {
  return {
    items: items.map((item) => ({
      id: item.id,
      title: item.title,
      startAt: item.startAt,
      endAt: item.endAt,
      location: item.location,
      version: item.version,
    })),
  };
}

function renderWorkspace() {
  const queryClient = createQueryClient();
  const result = render(
    <QueryClientProvider client={queryClient}>
      <ScheduleWorkspace timeZone="Asia/Seoul" />
    </QueryClientProvider>,
  );
  return { ...result, queryClient };
}

async function openCreateForm() {
  await userEvent.click(screen.getByRole('button', { name: '새 일정' }));
  await userEvent.type(screen.getByLabelText('제목'), '새 일정');
  await userEvent.type(screen.getByLabelText('시작'), '2026-08-11T09:00');
  await userEvent.type(screen.getByLabelText('종료'), '2026-08-11T10:00');
}

async function openDetail() {
  await userEvent.click(await screen.findByRole('button', { name: /팀 회의/ }));
  expect(await screen.findByRole('heading', { name: '일정 상세' })).toBeVisible();
}

describe('schedule workspace', () => {
  beforeEach(() => {
    getSchedulesMock.mockReset();
    getScheduleMock.mockReset();
    createScheduleMock.mockReset();
    updateScheduleMock.mockReset();
    deleteScheduleMock.mockReset();
    approveMock.mockReset();
    updateTimezoneMock.mockReset();
    getSchedulesMock.mockResolvedValue({ items: [] });
    getScheduleMock.mockResolvedValue(detail);
  });

  it('shows loading and then an empty state', async () => {
    let resolveList!: (value: { items: [] }) => void;
    getSchedulesMock.mockReturnValueOnce(new Promise((resolve) => (resolveList = resolve)));
    renderWorkspace();
    expect(screen.getByText('일정을 불러오는 중…')).toBeVisible();
    resolveList({ items: [] });
    expect(await screen.findByText('조회 기간에 등록된 일정이 없습니다.')).toBeVisible();
  });

  it('renders the backend list order and opens detail', async () => {
    getSchedulesMock.mockResolvedValue(listOf(detail));
    getScheduleMock.mockResolvedValue(detail);
    renderWorkspace();
    await openDetail();
    expect(screen.getByRole('dialog')).toHaveTextContent('회의실');
  });

  it('creates a schedule with timezone-converted UTC values', async () => {
    createScheduleMock.mockResolvedValue(detail);
    renderWorkspace();
    await openCreateForm();
    await userEvent.click(screen.getByRole('button', { name: '저장' }));
    await waitFor(() =>
      expect(createScheduleMock).toHaveBeenCalledWith({
        title: '새 일정',
        startAt: '2026-08-11T00:00:00.000Z',
        endAt: '2026-08-11T01:00:00.000Z',
        location: null,
      }),
    );
    expect(await screen.findByText('일정을 생성했습니다.')).toBeVisible();
  });

  it('opens conflict UI for create 409 and approves the confirmation', async () => {
    createScheduleMock.mockRejectedValue(
      new ApiError({
        status: 409,
        code: 'SCHEDULE_CONFLICT',
        message: '충돌',
        confirmationId: 91,
        conflicts: [detail],
      }),
    );
    approveMock.mockResolvedValue(detail);
    renderWorkspace();
    await openCreateForm();
    await userEvent.click(screen.getByRole('button', { name: '저장' }));
    expect(await screen.findByRole('heading', { name: '겹치는 일정 확인' })).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: '충돌 확인 후 저장' }));
    expect(approveMock).toHaveBeenCalledWith(91);
    expect(await screen.findByText('충돌을 확인하고 일정을 저장했습니다.')).toBeVisible();
    expect(getScheduleMock).not.toHaveBeenCalled();
  });

  it('uses a replacement confirmation ID after superseded response', async () => {
    createScheduleMock.mockRejectedValue(
      new ApiError({ status: 409, code: 'SCHEDULE_CONFLICT', message: '충돌', confirmationId: 91 }),
    );
    approveMock
      .mockRejectedValueOnce(
        new ApiError({
          status: 409,
          code: 'CONFIRMATION_SUPERSEDED',
          message: '교체',
          confirmationId: 92,
          conflicts: [detail],
        }),
      )
      .mockResolvedValueOnce(detail);
    renderWorkspace();
    await openCreateForm();
    await userEvent.click(screen.getByRole('button', { name: '저장' }));
    await userEvent.click(await screen.findByRole('button', { name: '충돌 확인 후 저장' }));
    expect(await screen.findByText(/최신 확인 요청으로 교체/)).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: '충돌 확인 후 저장' }));
    expect(approveMock).toHaveBeenLastCalledWith(92);
    expect(getScheduleMock).not.toHaveBeenCalled();
  });

  it('includes version and omits unchanged location on update', async () => {
    getSchedulesMock.mockResolvedValue(listOf(detail));
    getScheduleMock.mockResolvedValue(detail);
    updateScheduleMock.mockResolvedValue({ ...detail, title: '변경된 회의', version: 5 });
    renderWorkspace();
    await openDetail();
    await userEvent.click(screen.getByRole('button', { name: '수정' }));
    const title = screen.getByLabelText('제목');
    await userEvent.clear(title);
    await userEvent.type(title, '변경된 회의');
    await userEvent.click(screen.getByRole('button', { name: '저장' }));
    await waitFor(() =>
      expect(updateScheduleMock).toHaveBeenCalledWith(7, { version: 4, title: '변경된 회의' }),
    );
  });

  it('sends null when update clears location', async () => {
    getSchedulesMock.mockResolvedValue(listOf(detail));
    getScheduleMock.mockResolvedValue(detail);
    updateScheduleMock.mockResolvedValue({ ...detail, location: null, version: 5 });
    renderWorkspace();
    await openDetail();
    await userEvent.click(screen.getByRole('button', { name: '수정' }));
    await userEvent.clear(screen.getByLabelText('장소 (선택)'));
    await userEvent.click(screen.getByRole('button', { name: '저장' }));
    await waitFor(() =>
      expect(updateScheduleMock).toHaveBeenCalledWith(7, { version: 4, location: null }),
    );
  });

  it('deletes with the current version after confirmation', async () => {
    getSchedulesMock.mockResolvedValue(listOf(detail));
    getScheduleMock.mockResolvedValue(detail);
    deleteScheduleMock.mockResolvedValue(undefined);
    renderWorkspace();
    await openDetail();
    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    expect(deleteScheduleMock).toHaveBeenCalledWith(7, 4);
    expect(await screen.findByText('일정을 삭제했습니다.')).toBeVisible();
  });

  it('shows a version conflict returned by delete', async () => {
    getSchedulesMock.mockResolvedValue(listOf(detail));
    getScheduleMock.mockResolvedValue(detail);
    deleteScheduleMock.mockRejectedValue(
      new ApiError({ status: 409, code: 'SCHEDULE_VERSION_CONFLICT', message: '충돌' }),
    );
    renderWorkspace();
    await openDetail();
    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('일정이 변경되었습니다.');
  });

  it('shows a network error and removes auth cache on schedule 401', async () => {
    getSchedulesMock.mockRejectedValueOnce(
      new ApiError({ status: 0, code: 'NETWORK_ERROR', message: '네트워크 오류' }),
    );
    const first = renderWorkspace();
    expect(await screen.findByRole('alert')).toHaveTextContent('네트워크 오류');
    first.unmount();

    getSchedulesMock.mockRejectedValueOnce(
      new ApiError({ status: 401, code: 'UNAUTHORIZED', message: '인증 필요' }),
    );
    const second = renderWorkspace();
    const removeQueries = vi.spyOn(second.queryClient, 'removeQueries');
    await waitFor(() =>
      expect(removeQueries).toHaveBeenCalledWith({ queryKey: currentUserQueryKey }),
    );
  });
});

describe('timezone form', () => {
  it('updates current user cache and invalidates schedule queries', async () => {
    updateTimezoneMock.mockResolvedValue({
      email: 'member@example.com',
      timezone: 'Asia/Tokyo',
      createdAt: '2026-01-01T00:00:00Z',
    });
    const queryClient = createQueryClient();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    render(
      <QueryClientProvider client={queryClient}>
        <TimezoneForm current="Asia/Seoul" />
      </QueryClientProvider>,
    );
    const input = screen.getByLabelText('표시 시간대');
    await userEvent.clear(input);
    await userEvent.type(input, 'Asia/Tokyo');
    await userEvent.click(screen.getByRole('button', { name: '변경' }));
    expect(await screen.findByText(/표시 시간만 바뀝니다/)).toBeVisible();
    expect(queryClient.getQueryData(currentUserQueryKey)).toMatchObject({ timezone: 'Asia/Tokyo' });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: scheduleKeys.all });
  });
});
