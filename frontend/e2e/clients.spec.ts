import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Client Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to clients list', async ({ page }) => {
    await page.goto('/clients');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/clients/);
  });

  test('clients page renders without error', async ({ page }) => {
    await page.goto('/clients');
    await page.waitForTimeout(2000);
    // Check page content doesn't contain raw HTTP error codes
    const content = await page.content();
    expect(content).not.toContain('Cannot GET /clients');
    expect(content.length).toBeGreaterThan(500);
  });
});
