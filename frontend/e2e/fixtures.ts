import { expect, test as base } from '@playwright/test';

export const test = base.extend({
  page: async ({ page }, fixtureUse) => {
    const pageErrors: string[] = [];
    page.on('pageerror', (error) => pageErrors.push(error.message));
    await fixtureUse(page);
    expect(pageErrors, 'uncaught browser page errors').toEqual([]);
  },
});

export { expect } from '@playwright/test';
