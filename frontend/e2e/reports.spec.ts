import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Reporting Module', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to reports page', async ({ page }) => {
    await page.goto('/reports');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/reports/);
  });

  test('reports page renders KPI cards', async ({ page }) => {
    await page.goto('/reports');
    await page.waitForTimeout(3000);
    const cards = page.locator('[class*="card"], [class*="kpi"], [class*="stat"], [class*="widget"]');
    await expect(cards.first()).toBeVisible();
  });
});
