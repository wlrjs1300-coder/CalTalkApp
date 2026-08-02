import { expect, test } from './fixtures';
import {
  cleanupAccount,
  login,
  signup,
  signupAndLogin,
  TEST_PASSWORD,
  uniqueEmail,
} from './helpers';

test('signs up, rejects duplicates and bad credentials, restores and ends the server session', async ({
  page,
  context,
}, testInfo) => {
  const email = uniqueEmail(testInfo);
  try {
    await signup(page, email);

    await page.goto('/signup');
    await page.getByLabel('이메일').fill(email);
    await page.getByLabel('비밀번호', { exact: true }).fill(TEST_PASSWORD);
    await page.getByLabel('비밀번호 확인').fill(TEST_PASSWORD);
    await page.getByRole('button', { name: '회원가입' }).click();
    await expect(page.getByRole('alert')).toBeVisible();

    await login(page, email, `${TEST_PASSWORD}-wrong`);
  } catch (error) {
    if (
      await page
        .getByText(email)
        .isVisible()
        .catch(() => false)
    )
      throw error;
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByRole('main')).not.toContainText(TEST_PASSWORD);
  }

  await login(page, email);
  const sessionCookie = (await context.cookies()).find(
    (cookie) => cookie.name === 'CALTALK_SESSION',
  );
  expect(sessionCookie).toMatchObject({ httpOnly: true, secure: false, sameSite: 'Lax' });

  await page.reload();
  await expect(page.getByText(email)).toBeVisible();

  const csrfRequestPromise = page.waitForRequest(
    (request) => request.url().endsWith('/api/v1/users/me') && request.method() === 'PATCH',
  );
  await page.getByLabel('표시 시간대').fill('Asia/Tokyo');
  await page.getByRole('button', { name: '변경' }).click();
  const csrfRequest = await csrfRequestPromise;
  expect(csrfRequest.headers()['x-xsrf-token']).toBeTruthy();
  expect((await csrfRequest.allHeaders()).origin).toBe('http://localhost:5173');
  const csrfCookie = (await context.cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN');
  expect(csrfCookie).toMatchObject({ httpOnly: false, secure: false, sameSite: 'Lax' });

  await page.getByRole('button', { name: '로그아웃' }).click();
  await expect(page).toHaveURL(/\/login$/u);
  const rejectedReuseStatus = await page.evaluate(async () => {
    const response = await fetch('http://localhost:8080/api/v1/users/me', {
      credentials: 'include',
    });
    return response.status;
  });
  expect(rejectedReuseStatus).toBe(401);
  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/u);

  await cleanupAccount(page, email);
});

test('rejects a state-changing request without the X-XSRF-TOKEN header as JSON', async ({
  page,
}, testInfo) => {
  const email = uniqueEmail(testInfo);
  try {
    await signupAndLogin(page, email);
    const result = await page.evaluate(async () => {
      const response = await fetch('http://localhost:8080/api/v1/users/me', {
        method: 'PATCH',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ timezone: 'Asia/Tokyo' }),
      });
      return {
        status: response.status,
        contentType: response.headers.get('content-type'),
        body: (await response.json()) as { code?: string },
      };
    });
    expect(result.status).toBe(403);
    expect(result.contentType).toContain('application/json');
    expect(result.body.code).toBe('FORBIDDEN');
  } finally {
    await cleanupAccount(page, email);
  }
});
