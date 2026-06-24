import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env['CI'],
  retries: 0,
  workers: 1,
  timeout: 30000,

  reporter: [
    ['html', { outputFolder: '../docs/coverage/e2e', open: 'never' }],
    ['list'],
  ],

  use: {
    baseURL: 'http://localhost:4200',
    video: 'on',
    screenshot: 'on',
    trace: 'retain-on-failure',
    headless: true,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  outputDir: '../docs/test-videos',
});
