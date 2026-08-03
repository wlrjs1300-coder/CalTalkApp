import { expect, test } from './fixtures';
import {
  cleanupAccount,
  createSchedule,
  openSchedule,
  signupAndLogin,
  uniqueEmail,
  uniqueTitle,
} from './helpers';

test('approves a CREATE_EVENT conflict through the confirmation API', async ({
  page,
}, testInfo) => {
  const email = uniqueEmail(testInfo);
  const existingTitle = uniqueTitle('existing-create');
  const conflictingTitle = uniqueTitle('conflicting-create');
  try {
    await signupAndLogin(page, email);
    expect(
      (
        await createSchedule(page, {
          title: existingTitle,
          startAt: '2027-04-10T09:00',
          endAt: '2027-04-10T10:00',
        })
      ).status(),
    ).toBe(201);

    const conflictResponse = await createSchedule(page, {
      title: conflictingTitle,
      startAt: '2027-04-10T09:30',
      endAt: '2027-04-10T10:30',
    });
    expect(conflictResponse.status()).toBe(409);
    const dialog = page.getByRole('dialog');
    await expect(dialog).toContainText(existingTitle);
    const approvalPromise = page.waitForResponse((response) =>
      /\/api\/v1\/confirmations\/\d+\/approve$/u.test(response.url()),
    );
    await dialog.getByRole('button', { name: '그래도 저장' }).click();
    expect((await approvalPromise).status()).toBe(200);
    await expect(page.getByRole('dialog')).toContainText(conflictingTitle);
  } finally {
    await cleanupAccount(page, email);
  }
});

test('approves an UPDATE_EVENT conflict and keeps the edited schedule', async ({
  page,
}, testInfo) => {
  const email = uniqueEmail(testInfo);
  const existingTitle = uniqueTitle('existing-update');
  const editedTitle = uniqueTitle('edited-update');
  try {
    await signupAndLogin(page, email);
    expect(
      (
        await createSchedule(page, {
          title: existingTitle,
          startAt: '2027-05-10T09:00',
          endAt: '2027-05-10T10:00',
        })
      ).status(),
    ).toBe(201);
    expect(
      (
        await createSchedule(page, {
          title: editedTitle,
          startAt: '2027-05-10T11:00',
          endAt: '2027-05-10T12:00',
        })
      ).status(),
    ).toBe(201);

    await openSchedule(page, editedTitle);
    await page.getByRole('dialog').getByRole('button', { name: '일정 수정' }).click();
    const form = page.getByRole('dialog');
    await form.getByLabel('시작').fill('2027-05-10T09:30');
    await form.getByLabel('종료').fill('2027-05-10T10:30');
    const conflictResponsePromise = page.waitForResponse(
      (response) =>
        /\/api\/v1\/schedules\/\d+$/u.test(response.url()) &&
        response.request().method() === 'PATCH',
    );
    await form.getByRole('button', { name: '변경사항 저장' }).click();
    expect((await conflictResponsePromise).status()).toBe(409);
    const conflictDialog = page.getByRole('dialog');
    await expect(conflictDialog).toContainText(existingTitle);
    const approvalPromise = page.waitForResponse((response) =>
      /\/api\/v1\/confirmations\/\d+\/approve$/u.test(response.url()),
    );
    await conflictDialog.getByRole('button', { name: '그래도 저장' }).click();
    expect((await approvalPromise).status()).toBe(200);
    await expect(page.getByRole('dialog')).toContainText(editedTitle);
  } finally {
    await cleanupAccount(page, email);
  }
});
