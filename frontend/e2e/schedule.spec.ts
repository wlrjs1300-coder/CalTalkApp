import { expect, test } from './fixtures';
import {
  cleanupAccount,
  createSchedule,
  E2E_API_BASE_URL,
  openSchedule,
  signupAndLogin,
  uniqueEmail,
  uniqueTitle,
} from './helpers';

test('creates, reads, updates with location KEEP/SET/REMOVE, changes display timezone, and deletes', async ({
  page,
}, testInfo) => {
  const email = uniqueEmail(testInfo);
  const originalTitle = uniqueTitle('crud');
  const renamedTitle = `${originalTitle}-renamed`;
  try {
    await signupAndLogin(page, email);
    const createResponse = await createSchedule(page, {
      title: originalTitle,
      startAt: '2027-03-10T09:00',
      endAt: '2027-03-10T10:00',
      location: 'Room A',
    });
    expect(createResponse.status()).toBe(201);
    const created = (await createResponse.json()) as { id: number; startAt: string; endAt: string };
    await expect(page.getByRole('button', { name: new RegExp(originalTitle, 'u') })).toBeVisible();

    await openSchedule(page, originalTitle);
    const keepDialog = page.getByRole('dialog');
    await keepDialog.getByRole('button', { name: '수정' }).click();
    await keepDialog.getByLabel('제목').fill(renamedTitle);
    const keepRequestPromise = page.waitForRequest(
      (request) =>
        request.url().endsWith(`/api/v1/schedules/${created.id}`) && request.method() === 'PATCH',
    );
    await keepDialog.getByRole('button', { name: '저장' }).click();
    const keepBody = keepRequestPromise.then(
      (request) => request.postDataJSON() as Record<string, unknown>,
    );
    expect(await keepBody).not.toHaveProperty('location');
    await expect(page.getByRole('dialog')).toContainText(renamedTitle);

    await page.getByRole('dialog').getByRole('button', { name: '수정' }).click();
    await page.getByRole('dialog').getByLabel(/장소/u).fill('Room B');
    const setRequestPromise = page.waitForRequest(
      (request) =>
        request.url().endsWith(`/api/v1/schedules/${created.id}`) && request.method() === 'PATCH',
    );
    await page.getByRole('dialog').getByRole('button', { name: '저장' }).click();
    expect((await setRequestPromise).postDataJSON()).toMatchObject({ location: 'Room B' });
    await expect(page.getByRole('dialog')).toContainText('Room B');

    await page.getByRole('dialog').getByRole('button', { name: '수정' }).click();
    await page.getByRole('dialog').getByLabel(/장소/u).fill('');
    const removeRequestPromise = page.waitForRequest(
      (request) =>
        request.url().endsWith(`/api/v1/schedules/${created.id}`) && request.method() === 'PATCH',
    );
    await page.getByRole('dialog').getByRole('button', { name: '저장' }).click();
    expect((await removeRequestPromise).postDataJSON()).toMatchObject({ location: null });
    await expect(page.getByRole('dialog')).toContainText('없음');
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toHaveCount(0);

    const item = page.getByRole('button', { name: new RegExp(renamedTitle, 'u') });
    const seoulDisplay = await item.innerText();
    const before = await page.evaluate(
      async ({ apiBaseUrl, id }) => {
        const response = await fetch(`${apiBaseUrl}/api/v1/schedules/${id}`, {
          credentials: 'include',
        });
        return (await response.json()) as { startAt: string; endAt: string };
      },
      { apiBaseUrl: E2E_API_BASE_URL, id: created.id },
    );
    await page.getByLabel('표시 시간대').fill('Asia/Tokyo');
    await page.getByRole('button', { name: '변경' }).click();
    await expect(page.getByRole('status')).toBeVisible();
    await expect(item).not.toHaveText(seoulDisplay);
    const after = await page.evaluate(
      async ({ apiBaseUrl, id }) => {
        const response = await fetch(`${apiBaseUrl}/api/v1/schedules/${id}`, {
          credentials: 'include',
        });
        return (await response.json()) as { startAt: string; endAt: string };
      },
      { apiBaseUrl: E2E_API_BASE_URL, id: created.id },
    );
    expect(after).toEqual(before);

    await openSchedule(page, renamedTitle);
    await page.getByRole('dialog').getByRole('button', { name: '삭제' }).click();
    await page.getByRole('dialog').getByRole('button', { name: '삭제' }).click();
    await expect(page.getByRole('button', { name: new RegExp(renamedTitle, 'u') })).toHaveCount(0);
  } finally {
    await cleanupAccount(page, email);
  }
});
