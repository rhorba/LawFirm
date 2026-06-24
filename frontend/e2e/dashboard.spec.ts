import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('dashboard loads after login', async ({ page }) => {
    await expect(page).not.toHaveURL(/login/);
    await page.waitForTimeout(1000);
    const body = await page.content();
    expect(body.length).toBeGreaterThan(100);
  });

  test('sidebar navigation is visible', async ({ page }) => {
    // Sidebar should contain navigation links
    const nav = page.locator('nav, aside, [class*="sidebar"], [class*="nav"]');
    await expect(nav.first()).toBeVisible();
  });

  test('header/topbar is visible', async ({ page }) => {
    const header = page.locator('header, [class*="header"], [class*="topbar"]');
    await expect(header.first()).toBeVisible();
  });

  test('dashboard contains KPI cards or summary widgets', async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForTimeout(2000);
    const cards = page.locator('[class*="card"], [class*="widget"], [class*="kpi"], [class*="stat"]');
    const count = await cards.count();
    expect(count).toBeGreaterThan(0);
  });
});
