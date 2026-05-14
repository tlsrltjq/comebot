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

    expect(await screen.findByText('운영 준비 상태(Operational Readiness)')).toBeInTheDocument();
    expect(screen.getAllByText('자동 PAPER 운영 가능(Ready)').length).toBeGreaterThan(0);
    expect(screen.getByText('데이터 준비 상태(Data Readiness)')).toBeInTheDocument();
    expect(screen.getByText('검증 가능(Reviewable)')).toBeInTheDocument();
    expect(screen.getByText('BUY/SELL/FILLED 데이터가 있어 PAPER 흐름 검토가 가능합니다.')).toBeInTheDocument();
    expect(screen.getByText('후보 스케줄러(Candidate)')).toBeInTheDocument();
    expect(screen.getByText('청산 스케줄러(Exit)')).toBeInTheDocument();
    expect(screen.getByText('운영 제약(Controls)')).toBeInTheDocument();
    expect(screen.getByText('리스크 요약(Risk Summary)')).toBeInTheDocument();
    expect(screen.getByText('UPBIT 7% / 10%')).toBeInTheDocument();
    expect(screen.getByText('2회 / PT24H')).toBeInTheDocument();
    expect(screen.getByText('운영 환경(OS Guide)')).toBeInTheDocument();
    expect(screen.getByText('Windows')).toBeInTheDocument();
    expect(screen.getByText('scripts\\run-upbit-paper.bat')).toBeInTheDocument();
    expect(screen.getByText('%USERPROFILE%\\workspace\\comebot')).toBeInTheDocument();
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

    expect(await screen.findByText('데이터 준비 상태(Data Readiness)')).toBeInTheDocument();
    expect(screen.getByText('데이터 부족(Insufficient)')).toBeInTheDocument();
    expect(screen.getByText('아직 24시간 실행 기록과 보유 PAPER 포지션이 없어 손익 판단은 보류합니다.')).toBeInTheDocument();
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

    expect((await screen.findAllByText('PAPER 현금(Cash)')).length).toBeGreaterThan(0);
    expect(screen.getByText('0회 가능 / 0.5%')).toBeInTheDocument();
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
        snapshotCount: 120,
        upbitSnapshotCount: 120,
        binanceSnapshotCount: 0,
        freshSnapshotCount: 118,
        staleSnapshotCount: 2,
        orderStaleMs: 3000,
      });
    }
    if (url.startsWith('/api/analytics/summary')) {
      return json({ ...defaultSummary(), ...overrides.summary });
    }
    if (url.startsWith('/api/analytics/pnl')) {
      return json({ ...defaultPnl(), ...overrides.pnl });
    }
    if (url.startsWith('/api/analytics/losses')) {
      return json({
        range: '24h',
        worstTrades: [],
        repeatedStopLossMarkets: [],
      });
    }
    if (url === '/api/risk/status?exchange=upbit') {
      return json({
        maxOrderAmount: '100000',
        allowedMarkets: ['ALL_KRW'],
        takeProfitRate: '1.5',
        stopLossRate: '-0.7',
        positionExitEnabled: true,
        dailyRiskEnabled: false,
        dailyOrderLimit: 10,
        dailyLossLimit: '50000',
        concentration: {
          exchange: 'UPBIT',
          enabled: false,
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
}

function defaultSystemStatus(exitPositionMarketCount: number, portfolio?: Partial<ReturnType<typeof portfolioCash>>) {
  return {
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
      exitPositionMarketCount,
    },
    portfolio: { ...portfolioCash(), ...portfolio },
    safety: { killSwitchEnabled: false },
    notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
    telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
  };
}

function portfolioCash() {
  return {
    exchange: 'UPBIT',
    currency: 'KRW',
    cash: '900000',
    initialCash: '1000000',
    orderAmount: '10000',
    cashRate: '90.00',
    remainingBuyCount: 90,
    cashWarning: false,
    cashWarningMessage: 'PAPER cash is available',
  };
}

function defaultSummary() {
  return {
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
  };
}

function defaultPnl() {
  return {
    range: '24h',
    cash: '900000',
    totalPositionValue: '120000',
    totalEquity: '1020000',
    realizedProfit: '10000',
    unrealizedProfit: '10000',
    totalProfit: '20000',
    positionCount: 2,
  };
}

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
