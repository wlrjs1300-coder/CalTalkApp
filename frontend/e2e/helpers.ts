import { randomUUID } from 'node:crypto';
import type { Page, TestInfo } from '@playwright/test';
import { expect } from '@playwright/test';

export const TEST_PASSWORD = 'E2e-password-2026!';
export const E2E_API_BASE_URL = process.env.E2E_API_BASE_URL ?? 'http://localhost:8080';

export function uniqueEmail(testInfo: TestInfo): string {
  return `e2e-${testInfo.workerIndex}-${Date.now()}-${randomUUID().slice(0, 8)}@example.test`;
}

export function uniqueTitle(prefix: string): string {
  return `${prefix}-${randomUUID().slice(0, 8)}`;
}

export async function signup(page: Page, email: string): Promise<void> {
  await page.goto('/signup');
  await page.getByLabel('이메일').fill(email);
  await page.getByLabel('비밀번호', { exact: true }).fill(TEST_PASSWORD);
  await page.getByLabel('비밀번호 확인').fill(TEST_PASSWORD);
  await page.getByRole('button', { name: '회원가입' }).click();
  await expect(page).toHaveURL(/\/login$/u);
}

export async function login(page: Page, email: string, password = TEST_PASSWORD): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('이메일').fill(email);
  await page.getByLabel('비밀번호').fill(password);
  await page.getByRole('button', { name: '로그인' }).click();
  await expect(page).toHaveURL(/\/$/u);
  await expect(page.getByText(email)).toBeVisible();
}

export async function signupAndLogin(page: Page, email: string): Promise<void> {
  await signup(page, email);
  await login(page, email);
}

export async function cleanupAccount(page: Page, email: string): Promise<void> {
  try {
    if (!page.url().endsWith('/')) await login(page, email);
    await page.evaluate(
      async ({ apiBaseUrl, currentPassword }) => {
        await fetch(`${apiBaseUrl}/api/v1/csrf`, { credentials: 'include' });
        const token = document.cookie
          .split('; ')
          .find((entry) => entry.startsWith(`${'XSRF-TOKEN'}${'='}`))
          ?.split('=')
          .slice(1)
          .join('=');
        if (!token) throw new Error('CSRF token was not issued for account cleanup.');
        const response = await fetch(`${apiBaseUrl}/api/v1/users/me`, {
          method: 'DELETE',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': decodeURIComponent(token),
          },
          body: JSON.stringify({ currentPassword }),
        });
        if (!response.ok) throw new Error(`Account cleanup failed with HTTP ${response.status}.`);
      },
      { apiBaseUrl: E2E_API_BASE_URL, currentPassword: TEST_PASSWORD },
    );
  } catch {
    // Cleanup is best-effort so it never hides the original test failure.
  }
}

export async function createSchedule(
  page: Page,
  values: { title: string; startAt: string; endAt: string; location?: string },
) {
  await page.getByRole('button', { name: '새 일정' }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel('제목').fill(values.title);
  await dialog.getByLabel('시작').fill(values.startAt);
  await dialog.getByLabel('종료').fill(values.endAt);
  if (values.location !== undefined) await dialog.getByLabel(/장소/u).fill(values.location);
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/schedules') && response.request().method() === 'POST',
  );
  await dialog.getByRole('button', { name: '저장' }).click();
  return responsePromise;
}

export async function openSchedule(page: Page, title: string): Promise<void> {
  await page.getByRole('button', { name: new RegExp(title, 'u') }).click();
  await expect(page.getByRole('dialog')).toContainText(title);
}
