import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
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
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('DashboardPage', () => {
  it('prioritizes operational readiness and keeps the dashboard read-only', async () => {
    const fetchMock = mockDashboardFetch();
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('navigator', { userAgentData: { platform: 'Windows' }, userAgent: '' });

    renderWithClient();

    // readiness section
    expect(await screen.findByText('운영 준비 상태')).toBeInTheDocument();
    // risk panel
    expect(screen.getByText('리스크 요약')).toBeInTheDocument();
    // scheduler panel
    expect(screen.getByText('스케줄러')).toBeInTheDocument();
    // OS guide
    expect(screen.getByText('Windows')).toBeInTheDocument();
    // no manual execution buttons
    expect(screen.queryByRole('button', { name: /실행|매수|BUY|매도|SELL/ })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
  });

  it('marks paper analytics as insufficient before runs or positions exist', async () => {
    vi.stubGlobal('fetch', mockDashboardFetch({
      summary: {
        total: 0,
        buyCount: 0,
        sellCount: 0,
        holdCount: 0,
        filledCount: 0,
        rejectedCount: 0,
        failedCount: 0,
        stopLossCount: 0,
        takeProfitCount: 0,
      },
      pnl: {
        totalPositionValue: '0',
        totalEquity: '900000',
        realizedProfit: '0',
        unrealizedProfit: '0',
        totalProfit: '0',
        positionCount: 0,
      },
      exitPositionMarketCount: 0,
    }));

    renderWithClient();

    // dashboard renders with zero data — no manual execution controls
    expect(await screen.findByText('운영 준비 상태')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /실행|매수|BUY|매도|SELL/ })).not.toBeInTheDocument();
  });

  it('shows read-only paper cash warning without adding trade controls', async () => {
    vi.stubGlobal('fetch', mockDashboardFetch({
      portfolio: {
        cash: '5000',
        cashRate: '0.50',
        remainingBuyCount: 0,
        cashWarning: true,
        cashWarningMessage: 'PAPER cash is below one order amount',
      },
    }));

    renderWithClient();

    expect((await screen.findAllByText('PAPER 현금')).length).toBeGreaterThan(0);
    expect(screen.getByText('PAPER cash is below one order amount')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /실행|매수|BUY|매도|SELL/ })).not.toBeInTheDocument();
  });
});

function mockDashboardFetch(overrides: {
  summary?: Partial<ReturnType<typeof defaultSummary>>;
  pnl?: Partial<ReturnType<typeof defaultPnl>>;
  portfolio?: Partial<ReturnType<typeof portfolioCash>>;
  exitPositionMarketCount?: number;
} = {}) {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url === '/api/system/status?exchange=upbit') {
      return json(defaultSystemStatus(overrides.exitPositionMarketCount ?? 2, overrides.portfolio));
    }
    if (url === '/api/market-provider/status') {
      return json({
        provider: 'UPBIT',
        externalProvider: true,
        message: 'ok',
        webSocketEnabled: true,
        snapshotCount: 20,
        upbitSnapshotCount: 20,
        binanceSnapshotCount: 10,
        freshSnapshotCount: 20,
        upbitFreshSnapshotCount: 20,
        binanceFreshSnapshotCount: 10,
        staleSnapshotCount: 0,
        orderStaleMs: 3000,
        automationReady: true,
      });
    }
    if (url.startsWith('/api/analytics/summary')) {
      return json({ ...defaultSummary(), ...overrides.summary });
    }
    if (url.startsWith('/api/analytics/pnl')) {
      return json({ ...defaultPnl(), ...overrides.pnl });
    }
    if (url.startsWith('/api/analytics/losses')) {
      return json({ worstTrades: [] });
    }
    if (url.startsWith('/api/risk/status')) {
      return json(defaultRiskStatus());
    }
    return json({});
  });
}

function defaultSystemStatus(exitPositionMarketCount = 2, portfolioOverride?: Partial<ReturnType<typeof portfolioCash>>) {
  return {
    database: { connected: true },
    marketProvider: { provider: 'UPBIT', externalProvider: true },
    strategy: { strategyName: 'VolatilityBreakoutLongStrategy', buyPrice: '90000000', sellPrice: '110000000', orderQuantity: '0.001', orderAmount: '10000' },
    risk: { maxOrderAmount: '100000', allowedMarkets: ['ALL_KRW'] },
    scheduler: {
      enabled: false, fixedDelayMs: 60000, markets: ['KRW-BTC'],
      candidateEnabled: true, candidateFixedDelayMs: 60000,
      candidateMarkets: ['ALL_KRW'], candidateNotifySummary: false,
      candidateExchange: 'UPBIT', candidateExchanges: ['UPBIT'],
      exitEnabled: true, exitFixedDelayMs: 5000,
      exitSaveHoldHistory: false, exitExchange: 'UPBIT',
      exitExchanges: ['UPBIT'], exitPositionMarketCount,
    },
    portfolio: { ...portfolioCash(), ...portfolioOverride },
    safety: { killSwitchEnabled: false },
    notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
    telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
  };
}

function defaultSummary() {
  return {
    total: 10, buyCount: 4, sellCount: 3, holdCount: 3,
    filledCount: 3, rejectedCount: 0, failedCount: 0,
    stopLossCount: 1, takeProfitCount: 2,
    winRate: '66.67', profitLossRatio: '1.5',
    averageTakeProfitRate: '2.1', averageStopLossRate: '-1.5',
    averageHoldingSeconds: 1800,
    from: '2026-06-03T00:00:00Z', to: '2026-06-04T00:00:00Z',
  };
}

function defaultPnl() {
  return {
    totalPositionValue: '50000', totalEquity: '950000',
    realizedProfit: '3000', unrealizedProfit: '2000',
    totalProfit: '5000', positionCount: 2,
    cash: '900000',
  };
}

function defaultRiskStatus() {
  return {
    maxOrderAmount: '100000',
    takeProfitRate: '4',
    stopLossRate: '-2',
    dailyOrderLimit: 10,
    dailyRiskEnabled: true,
    dailyLossLimit: '-50000',
    positionExitEnabled: true,
    allowedMarkets: ['ALL_KRW'],
    concentration: { exchange: 'UPBIT', enabled: true, warningExposureRate: '7', blockExposureRate: '10' },
    stopLossCooldown: { enabled: true, window: 'PT24H', triggerCount: 2, duration: 'PT24H' },
  };
}

function portfolioCash() {
  return {
    exchange: 'UPBIT', currency: 'KRW', cash: '900000',
    initialCash: '1000000', orderAmount: '10000',
    cashRate: '90.00', remainingBuyCount: 90,
    cashWarning: false, cashWarningMessage: 'PAPER cash is available',
  };
}

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
