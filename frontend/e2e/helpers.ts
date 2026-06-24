import { Page } from '@playwright/test';

export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login');
  await page.fill('input[formControlName="username"], input[name="username"], input[type="text"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForTimeout(2500);
}
