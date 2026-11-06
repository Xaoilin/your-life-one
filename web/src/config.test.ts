import { afterEach, describe, expect, it, vi } from 'vitest';

afterEach(() => { vi.unstubAllEnvs(); vi.resetModules(); });

describe('live update API bases', () => {
  it('keeps the product prefix and deduplicates service URLs', async () => {
    vi.stubEnv('VITE_PRAYER_API_BASE_URL', 'https://shared.test/your-life-one/');
    vi.stubEnv('VITE_CALENDAR_API_BASE_URL', 'https://shared.test/your-life-one');
    const { liveGatewayOrigins } = await import('./config');
    expect(liveGatewayOrigins()).toEqual(['https://shared.test/your-life-one']);
  });
  it('supports the existing root API base', async () => {
    vi.stubEnv('VITE_PRAYER_API_BASE_URL', 'https://shared.test/');
    const { liveGatewayOrigins } = await import('./config');
    expect(liveGatewayOrigins()).toEqual(['https://shared.test']);
  });
});
