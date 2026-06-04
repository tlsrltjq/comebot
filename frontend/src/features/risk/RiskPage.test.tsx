import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ExchangeModeContext } from '../../shared/exchange/ExchangeModeContext';
import { RiskPage } from './RiskPage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <ExchangeModeContext.Provider value={{ exchange: 'UPBIT', setExchange: vi.fn() }}>
        <RiskPage />
      </ExchangeModeContext.Provider>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('RiskPage', () => {
  it('shows read-only risk policy without real trading or manual buy controls', async () => {
    vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL) => {
      if (String(input) === '/api/risk/status?exchange=upbit') {
        return json({
          maxOrderAmount: '100000', allowedMarkets: ['ALL_KRW'],
          takeProfitRate: '1.5', stopLossRate: '-0.7',
          positionExitEnabled: true, dailyRiskEnabled: false,
          dailyOrderLimit: 50, dailyLossLimit: '50000',
          concentration: { exchange: 'UPBIT', enabled: true, warningExposureRate: '7', blockExposureRate: '10' },
          stopLossCooldown: { enabled: true, window: 'PT168H', triggerCount: 2, duration: 'PT24H' },
        });
      }
      return json({});
    }));

    renderWithClient();

    expect(await screen.findByText('리스크')).toBeInTheDocument();
    // read-only notice present
    expect(screen.getAllByText(/읽기 전용/).length).toBeGreaterThan(0);
    // panels visible
    expect(screen.getByText('쏠림 리스크')).toBeInTheDocument();
    expect(screen.getByText('반복 손절 Cooldown')).toBeInTheDocument();
    expect(screen.getByText('미구현')).toBeInTheDocument();
    // no manual buy/real-trading buttons
    expect(screen.queryByRole('button', { name: /매수|BUY|실거래|REAL/ })).not.toBeInTheDocument();
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
