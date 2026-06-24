import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('User Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to users page', async ({ page }) => {
    await page.goto('/users');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/users/);
  });

  test('users page shows user records', async ({ page }) => {
    await page.goto('/users');
    await page.waitForTimeout(2000);
    const content = await page.content();
    // Page should contain 'admin' user at minimum
    expect(content).toContain('admin');
  });
});
