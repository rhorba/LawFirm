import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Calendar Module', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to calendar page', async ({ page }) => {
    await page.goto('/calendar');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/calendar/);
  });

  test('calendar grid is visible', async ({ page }) => {
    await page.goto('/calendar');
    await page.waitForTimeout(2000);
    const grid = page.locator('[class*="calendar"], [class*="grid"], table');
    const count = await grid.count();
    expect(count).toBeGreaterThan(0);
  });
});
