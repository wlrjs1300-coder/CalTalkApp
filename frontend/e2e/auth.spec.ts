import { expect, test } from './fixtures';
import {
  cleanupAccount,
  E2E_API_BASE_URL,
  login,
  signup,
  signupAndLogin,
  TEST_PASSWORD,
  uniqueEmail,
} from './helpers';

const expectedSecureCookie = process.env.E2E_EXPECT_SECURE_COOKIE === 'true';

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
  expect(sessionCookie).toMatchObject({
    httpOnly: true,
    secure: expectedSecureCookie,
    sameSite: 'Lax',
  });

  await page.reload();
  await expect(page.getByText(email)).toBeVisible();

  const csrfRequestPromise = page.waitForRequest(
    (request) => request.url().endsWith('/api/v1/users/me') && request.method() === 'PATCH',
  );
  await page.getByRole('button', { name: '설정' }).click();
  await page.getByLabel('지역 또는 시간대').fill('Asia/Tokyo');
  await page.getByRole('button', { name: '변경' }).click();
  const csrfRequest = await csrfRequestPromise;
  expect(csrfRequest.headers()['x-xsrf-token']).toBeTruthy();
  expect((await csrfRequest.allHeaders()).origin).toBe(new URL(page.url()).origin);
  const csrfCookie = (await context.cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN');
  expect(csrfCookie).toMatchObject({
    httpOnly: false,
    secure: expectedSecureCookie,
    sameSite: 'Lax',
  });

  await page.keyboard.press('Escape');

  await page.getByRole('button', { name: '로그아웃' }).click();
  await expect(page).toHaveURL(/\/login$/u);
  const rejectedReuseStatus = await page.evaluate(async (apiBaseUrl) => {
    const response = await fetch(`${apiBaseUrl}/api/v1/users/me`, {
      credentials: 'include',
    });
    return response.status;
  }, E2E_API_BASE_URL);
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
    const result = await page.evaluate(async (apiBaseUrl) => {
      const response = await fetch(`${apiBaseUrl}/api/v1/users/me`, {
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
    }, E2E_API_BASE_URL);
    expect(result.status).toBe(403);
    expect(result.contentType).toContain('application/json');
    expect(result.body.code).toBe('FORBIDDEN');
  } finally {
    await cleanupAccount(page, email);
  }
});
