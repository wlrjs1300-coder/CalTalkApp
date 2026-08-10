import { expect, test } from './fixtures';
import { cleanupAccount, signupAndLogin, uniqueEmail } from './helpers';

const viewports = [
  { width: 1920, height: 1080 },
  { width: 1440, height: 900 },
  { width: 1280, height: 800 },
  { width: 1024, height: 768 },
  { width: 768, height: 1024 },
  { width: 430, height: 932 },
  { width: 390, height: 844 },
  { width: 360, height: 800 },
];

async function expectNoHorizontalOverflow(page: import('@playwright/test').Page) {
  const dimensions = await page.evaluate(() => ({
    viewport: document.documentElement.clientWidth,
    document: document.documentElement.scrollWidth,
  }));
  expect(dimensions.document).toBeLessThanOrEqual(dimensions.viewport);
}

test('keeps authentication, home, and schedule form inside all target viewports', async ({
  page,
}, testInfo) => {
  const email = uniqueEmail(testInfo);
  try {
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await page.goto('/welcome');
      await expect(page.getByRole('heading', { name: /카톡으로 간편하게/ })).toBeVisible();
      await expectNoHorizontalOverflow(page);
      await page.goto('/login');
      await expect(page.getByRole('heading', { name: /복잡한 일정을/ })).toBeVisible();
      await expectNoHorizontalOverflow(page);
    }

    await page.setViewportSize({ width: 1280, height: 800 });
    await signupAndLogin(page, email);
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await expect(page.getByRole('heading', { name: '오늘의 일정' })).toBeVisible();
      await expectNoHorizontalOverflow(page);
      await page.getByRole('button', { name: '새 일정' }).click();
      const dialog = page.getByRole('dialog');
      const box = await dialog.boundingBox();
      expect(box).not.toBeNull();
      expect(box!.x).toBeGreaterThanOrEqual(0);
      expect(box!.x + box!.width).toBeLessThanOrEqual(viewport.width + 1);
      expect(box!.y).toBeGreaterThanOrEqual(0);
      expect(box!.y + box!.height).toBeLessThanOrEqual(viewport.height + 1);
      await page.keyboard.press('Escape');
    }
  } finally {
    await cleanupAccount(page, email);
  }
});
