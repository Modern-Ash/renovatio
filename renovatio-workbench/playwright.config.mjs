import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.RENOVATIO_E2E_BASE_URL ?? 'http://127.0.0.1:3000/';
const chromiumExecutablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH;

export default defineConfig({
    testDir: './e2e',
    timeout: 60_000,
    expect: { timeout: 15_000 },
    fullyParallel: false,
    reporter: process.env.CI ? [['github'], ['list']] : 'list',
    use: {
        ...devices['Desktop Chrome'],
        baseURL,
        launchOptions: chromiumExecutablePath ? { executablePath: chromiumExecutablePath } : undefined,
        trace: 'on-first-retry'
    },
    webServer: process.env.RENOVATIO_E2E_BASE_URL ? undefined : {
        command: 'npm run start',
        url: baseURL,
        timeout: 90_000,
        reuseExistingServer: !process.env.CI,
        env: {
            RENOVATIO_BACKEND_URL: 'http://127.0.0.1:8080',
            RENOVATIO_DASHBOARD_URL: 'http://127.0.0.1:5173/',
            RENOVATIO_TELEMETRY_ENABLED: 'false'
        }
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        }
    ]
});
