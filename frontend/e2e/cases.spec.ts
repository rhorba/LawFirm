import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Case Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to cases list', async ({ page }) => {
    await page.goto('/cases');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/cases/);
  });

  test('cases list renders a table or list', async ({ page }) => {
    await page.goto('/cases');
    await page.waitForTimeout(2000);
    const table = page.locator('table, [class*="table"], [class*="list"], ul, ol');
    const count = await table.count();
    expect(count).toBeGreaterThan(0);
  });

  test('search/filter controls visible on cases page', async ({ page }) => {
    await page.goto('/cases');
    await page.waitForTimeout(1500);
    const searchInput = page.locator('input[type="search"], input[placeholder*="search" i], input[placeholder*="cherch" i], input[placeholder*="filter" i], input[placeholder*="filtr" i]');
    await expect(searchInput.first()).toBeVisible();
  });

  test('create case button or link is present', async ({ page }) => {
    await page.goto('/cases');
    await page.waitForTimeout(1500);
    const createBtn = page.locator('button:has-text("New"), button:has-text("Create"), button:has-text("Add"), button:has-text("Nouveau"), a:has-text("New Case"), a[href*="new"], a[href*="create"]');
    await expect(createBtn.first()).toBeVisible();
  });
});
