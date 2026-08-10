import { QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getSocialProviders, login, logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { getCurrentUser } from '../api/user';
import { createQueryClient } from '../app/providers/queryClient';
import { currentUserQueryKey } from '../features/auth/authQuery';
import { ProtectedRoute, PublicOnlyRoute } from '../features/auth/routeGuards';
import { HomePage } from '../pages/HomePage';
import { LoginPage } from '../pages/LoginPage';
import { SignupPage } from '../pages/SignupPage';
import { WelcomePage } from '../pages/WelcomePage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  signup: vi.fn(),
  getSocialProviders: vi.fn(),
  socialLoginUrl: (provider: string) => `http://localhost:8080/oauth2/authorization/${provider}`,
}));

vi.mock('../api/user', () => ({
  getCurrentUser: vi.fn(),
  updateTimezone: vi.fn(),
}));

const loginMock = vi.mocked(login);
const logoutMock = vi.mocked(logout);
const currentUserMock = vi.mocked(getCurrentUser);
const socialProvidersMock = vi.mocked(getSocialProviders);
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

function renderSignup() {
  const queryClient = createQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/signup']}>
        <Routes>
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/login" element={<h1>로그인 화면</h1>} />
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
    socialProvidersMock.mockReset();
    socialProvidersMock.mockResolvedValue([]);
  });

  it('introduces CalTalk before authentication on the first visit', async () => {
    render(
      <MemoryRouter initialEntries={['/welcome']}>
        <Routes>
          <Route path="/welcome" element={<WelcomePage />} />
          <Route path="/login" element={<h1>로그인 화면</h1>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: /카톡으로 간편하게/ })).toBeVisible();
    await userEvent.click(screen.getByRole('link', { name: 'CalTalk 시작하기' }));
    expect(await screen.findByRole('heading', { name: '로그인 화면' })).toBeVisible();
  });

  it('shows client validation errors without submitting login', async () => {
    renderLogin();
    await userEvent.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByText('이메일을 입력해 주세요.')).toBeVisible();
    expect(screen.getByText('비밀번호를 입력해 주세요.')).toBeVisible();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it('uses user-facing copy and toggles password visibility', async () => {
    renderLogin();
    expect(screen.getByRole('heading', { name: /복잡한 일정을/ })).toBeVisible();
    const password = screen.getByLabelText('비밀번호');
    expect(password).toHaveAttribute('type', 'password');
    await userEvent.click(screen.getByRole('button', { name: '비밀번호 표시' }));
    expect(password).toHaveAttribute('type', 'text');
    expect(screen.queryByText(/서버 세션|source of truth|UTC 절대 시각/u)).not.toBeInTheDocument();
  });

  it('offers honest account recovery guidance', async () => {
    renderLogin();

    await userEvent.click(screen.getByRole('button', { name: '이메일 찾기' }));
    expect(screen.getByRole('dialog')).toHaveTextContent('가입할 때 사용한 이메일');
    await userEvent.click(screen.getByRole('button', { name: '확인' }));

    await userEvent.click(screen.getByRole('button', { name: '비밀번호 찾기' }));
    expect(screen.getByRole('dialog')).toHaveTextContent(
      '비밀번호 재설정 기능을 준비하고 있습니다',
    );
  });

  it('shows the configured social login providers', async () => {
    socialProvidersMock.mockResolvedValue([
      { id: 'kakao', name: '카카오' },
      { id: 'naver', name: '네이버' },
      { id: 'google', name: 'Google' },
    ]);
    renderLogin();

    expect(await screen.findByRole('link', { name: '카카오로 로그인' })).toHaveAttribute(
      'href',
      'http://localhost:8080/oauth2/authorization/kakao',
    );
    expect(screen.getByRole('link', { name: '네이버로 로그인' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Google로 로그인' })).toBeVisible();
  });

  it('shows password requirements while signing up', async () => {
    renderSignup();

    await userEvent.type(screen.getByLabelText('비밀번호', { exact: true }), 'password123');
    expect(
      screen.getByText(
        (_content, element) =>
          element?.matches('.password-checks > .is-complete') === true &&
          element.textContent?.includes('8~64자') === true,
      ),
    ).toBeVisible();
    await userEvent.type(screen.getByLabelText('비밀번호 확인'), 'password123');
    expect(
      screen.getByText(
        (_content, element) =>
          element?.matches('.password-checks > .is-complete') === true &&
          element.textContent?.includes('비밀번호 일치') === true,
      ),
    ).toBeVisible();
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

  it('redirects an unauthenticated visitor to the welcome screen', async () => {
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
            <Route path="/welcome" element={<h1>CalTalk 소개</h1>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('heading', { name: 'CalTalk 소개' })).toBeVisible();
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
            <Route path="/welcome" element={<h1>CalTalk 소개</h1>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('heading', { name: 'CalTalk 소개' })).toBeVisible();
    await waitFor(() =>
      expect(removeQueries).toHaveBeenCalledWith({ queryKey: currentUserQueryKey }),
    );
  });
});
