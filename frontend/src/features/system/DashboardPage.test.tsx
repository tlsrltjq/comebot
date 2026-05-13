import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DashboardPage } from './DashboardPage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <DashboardPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('DashboardPage', () => {
  it('prioritizes operational readiness and keeps the dashboard read-only', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/system/status?exchange=upbit') {
        return json({
          database: { connected: true },
          marketProvider: { provider: 'UPBIT', externalProvider: true },
          strategy: { strategyName: 'VolatilityBreakoutLongStrategy', buyPrice: '90000000', sellPrice: '110000000', orderQuantity: '0.001', orderAmount: '10000' },
          risk: { maxOrderAmount: '100000', allowedMarkets: ['ALL_KRW'] },
          scheduler: {
            enabled: false,
            fixedDelayMs: 60000,
            markets: ['KRW-BTC', 'KRW-ETH'],
            candidateEnabled: true,
            candidateFixedDelayMs: 60000,
            candidateMarkets: ['ALL_KRW'],
            candidateNotifySummary: false,
            candidateExchange: 'UPBIT',
            candidateExchanges: ['UPBIT'],
            exitEnabled: true,
            exitFixedDelayMs: 5000,
            exitSaveHoldHistory: false,
            exitExchange: 'UPBIT',
            exitExchanges: ['UPBIT'],
            exitPositionMarketCount: 2,
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/market-provider/status') {
        return json({
          provider: 'UPBIT',
          externalProvider: true,
          message: 'ok',
          webSocketEnabled: true,
          snapshotCount: 120,
          upbitSnapshotCount: 120,
          binanceSnapshotCount: 0,
        });
      }
      if (url.startsWith('/api/analytics/summary')) {
        return json({
          range: '24h',
          from: '2026-05-12T00:00:00Z',
          to: '2026-05-13T00:00:00Z',
          total: 8,
          buyCount: 2,
          sellCount: 1,
          holdCount: 5,
          filledCount: 3,
          rejectedCount: 0,
          failedCount: 0,
          stopLossCount: 1,
          takeProfitCount: 1,
          averageStopLossRate: '-0.7',
          averageTakeProfitRate: '1.5',
          topHoldReasons: [],
          topMarkets: [],
        });
      }
      if (url.startsWith('/api/analytics/pnl')) {
        return json({
          range: '24h',
          cash: '900000',
          totalPositionValue: '120000',
          totalEquity: '1020000',
          realizedProfit: '10000',
          unrealizedProfit: '10000',
          totalProfit: '20000',
          positionCount: 2,
        });
      }
      if (url.startsWith('/api/analytics/losses')) {
        return json({
          range: '24h',
          worstTrades: [],
          repeatedStopLossMarkets: [],
        });
      }
      return json({});
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('운영 준비 상태(Operational Readiness)')).toBeInTheDocument();
    expect(screen.getAllByText('자동 PAPER 운영 가능(Ready)').length).toBeGreaterThan(0);
    expect(screen.getByText('후보 스케줄러(Candidate)')).toBeInTheDocument();
    expect(screen.getByText('청산 스케줄러(Exit)')).toBeInTheDocument();
    expect(screen.getByText('운영 제약(Controls)')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /실행|매수|BUY|매도|SELL/ })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
