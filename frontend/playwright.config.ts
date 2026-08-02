import { defineConfig, devices } from '@playwright/test';
import { fileURLToPath } from 'node:url';

const frontendDirectory = fileURLToPath(new URL('.', import.meta.url));
const backendDirectory = fileURLToPath(new URL('../backend/', import.meta.url));
const backendCommand =
  process.platform === 'win32'
    ? '.\\gradlew.bat bootRun --console=plain'
    : './gradlew bootRun --console=plain';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    locale: 'ko-KR',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: backendCommand,
      cwd: backendDirectory,
      url: 'http://localhost:8080/api/v1/health',
      reuseExistingServer: false,
      timeout: 120_000,
    },
    {
      command: 'npm run dev -- --host localhost --port 5173 --strictPort',
      cwd: frontendDirectory,
      url: 'http://localhost:5173/login',
      reuseExistingServer: false,
      timeout: 60_000,
    },
  ],
});
