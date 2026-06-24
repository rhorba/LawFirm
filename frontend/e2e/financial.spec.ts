import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Financial Module', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('navigate to financial page', async ({ page }) => {
    await page.goto('/financial');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/financial/);
  });

  test('financial page loads without crash', async ({ page }) => {
    await page.goto('/financial');
    await page.waitForTimeout(2000);
    const body = await page.content();
    expect(body).not.toContain('Cannot GET');
  });

  test('navigate to invoices', async ({ page }) => {
    await page.goto('/financial/invoices');
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/invoices/);
  });
});
