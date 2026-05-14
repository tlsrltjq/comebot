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
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/risk/status?exchange=upbit') {
        return json({
          maxOrderAmount: '100000',
          allowedMarkets: ['ALL_KRW'],
          takeProfitRate: '1.5',
          stopLossRate: '-0.7',
          positionExitEnabled: true,
          dailyRiskEnabled: false,
          dailyOrderLimit: 50,
          dailyLossLimit: '50000',
          concentration: {
            exchange: 'UPBIT',
            enabled: true,
            warningExposureRate: '7',
            blockExposureRate: '10',
          },
          stopLossCooldown: {
            enabled: true,
            window: 'PT168H',
            triggerCount: 2,
            duration: 'PT24H',
          },
        });
      }
      return json({});
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('리스크(Risk)')).toBeInTheDocument();
    expect(screen.getByText('읽기 전용(Read-only)')).toBeInTheDocument();
    expect(screen.getByText('Risk 화면은 정책과 현재 적용 상태만 표시합니다. `REAL_TRADING`, 실제 주문, 수동 BUY 설정은 제공하지 않습니다.')).toBeInTheDocument();
    expect(screen.getByText('쏠림 리스크(Concentration)')).toBeInTheDocument();
    expect(screen.getByText('반복 손절 Cooldown')).toBeInTheDocument();
    expect(screen.getByText('구현 안 됨(Not implemented)')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /매수|BUY|실거래|REAL/ })).not.toBeInTheDocument();
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
