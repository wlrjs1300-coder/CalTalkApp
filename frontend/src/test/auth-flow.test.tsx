import { QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login, logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { getCurrentUser } from '../api/user';
import { createQueryClient } from '../app/providers/queryClient';
import { currentUserQueryKey } from '../features/auth/authQuery';
import { ProtectedRoute, PublicOnlyRoute } from '../features/auth/routeGuards';
import { HomePage } from '../pages/HomePage';
import { LoginPage } from '../pages/LoginPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  signup: vi.fn(),
}));

vi.mock('../api/user', () => ({
  getCurrentUser: vi.fn(),
  updateTimezone: vi.fn(),
}));

const loginMock = vi.mocked(login);
const logoutMock = vi.mocked(logout);
const currentUserMock = vi.mocked(getCurrentUser);
const user = {
  email: 'member@example.com',
  timezone: 'Asia/Seoul',
  createdAt: '2026-01-01T00:00:00Z',
};

function renderLogin() {
  const queryClient = createQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<h1>홈 화면</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('authentication UI', () => {
  beforeEach(() => {
    loginMock.mockReset();
    logoutMock.mockReset();
    currentUserMock.mockReset();
  });

  it('shows client validation errors without submitting login', async () => {
    renderLogin();
    await userEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByText('이메일을 입력해 주세요.')).toBeVisible();
    expect(screen.getByText('비밀번호를 입력해 주세요.')).toBeVisible();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('refetches the current user and navigates after login', async () => {
    loginMock.mockResolvedValue({ email: user.email, timezone: user.timezone });
    currentUserMock.mockResolvedValue(user);
    renderLogin();

    await userEvent.type(screen.getByLabelText('이메일'), user.email);
    await userEvent.type(screen.getByLabelText('비밀번호'), 'example-password');
    await userEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('heading', { name: '홈 화면' })).toBeVisible();
    expect(currentUserMock).toHaveBeenCalledOnce();
  });

  it('shows a generalized invalid credentials message', async () => {
    loginMock.mockRejectedValue(
      new ApiError({ status: 401, code: 'INVALID_CREDENTIALS', message: 'internal message' }),
    );
    renderLogin();

    await userEvent.type(screen.getByLabelText('이메일'), user.email);
    await userEvent.type(screen.getByLabelText('비밀번호'), 'example-password');
    await userEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      '이메일 또는 비밀번호를 확인해 주세요.',
    );
  });

  it('redirects an unauthenticated protected route to login', async () => {
    currentUserMock.mockRejectedValue(
      new ApiError({ status: 401, code: 'UNAUTHORIZED', message: '로그인이 필요합니다.' }),
    );
    const queryClient = createQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<h1>보호 화면</h1>} />
            </Route>
            <Route path="/login" element={<h1>로그인 화면</h1>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('heading', { name: '로그인 화면' })).toBeVisible();
  });

  it('redirects an authenticated user away from an auth page', async () => {
    const queryClient = createQueryClient();
    queryClient.setQueryData(currentUserQueryKey, user);
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/login" element={<Outlet />} />
            </Route>
            <Route path="/" element={<h1>홈 화면</h1>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('heading', { name: '홈 화면' })).toBeVisible();
  });

  it('removes the current user cache and navigates after logout', async () => {
    logoutMock.mockResolvedValue(undefined);
    const queryClient = createQueryClient();
    queryClient.setQueryData(currentUserQueryKey, user);
    const removeQueries = vi.spyOn(queryClient, 'removeQueries');
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<h1>로그인 화면</h1>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('heading', { name: '로그인 화면' })).toBeVisible();
    await waitFor(() =>
      expect(removeQueries).toHaveBeenCalledWith({ queryKey: currentUserQueryKey }),
    );
  });
});
