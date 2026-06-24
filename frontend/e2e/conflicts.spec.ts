import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Conflict Checking Module', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to conflicts page', async ({ page }) => {
    await page.goto('/conflicts');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/conflicts/);
  });

  test('conflicts page has search functionality', async ({ page }) => {
    await page.goto('/conflicts');
    await page.waitForTimeout(2000);
    const searchInput = page.locator('input[type="search"], input[type="text"], input[placeholder*="search" i], input[placeholder*="conflict" i]');
    await expect(searchInput.first()).toBeVisible();
  });
});
