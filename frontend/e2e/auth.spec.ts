import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('login page renders correctly', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL('**/login**');
    await expect(page.locator('input[formControlName="username"], input[name="username"], input[placeholder*="user" i], input[type="text"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
  });

  test('login with invalid credentials shows error', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[name="username"], input[type="text"]', 'wronguser');
    await page.fill('input[type="password"]', 'wrongpass');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000);
    // Must still be on login page — not redirected to dashboard
    await expect(page).toHaveURL(/login/);
  });

  test('login with valid credentials redirects to dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[name="username"], input[type="text"]', 'admin');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(3000);
    // Should be on dashboard or some authenticated page
    const url = page.url();
    expect(url).not.toContain('login');
  });

  test('logout clears session and redirects to login', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[formControlName="username"], input[name="username"], input[type="text"]', 'admin');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000);

    // Find and click logout
    const logoutBtn = page.locator('[data-testid="logout"], button:has-text("Logout"), button:has-text("Déconnexion"), a:has-text("Logout")');
    await expect(logoutBtn.first()).toBeVisible({ timeout: 5000 });
    await logoutBtn.first().click();
    await page.waitForTimeout(1000);
    await expect(page).toHaveURL(/login/);
  });
});
