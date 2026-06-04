import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ExchangeModeContext } from '../../shared/exchange/ExchangeModeContext';
import { SystemPage } from './SystemPage';

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
        <SystemPage />
      </ExchangeModeContext.Provider>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('SystemPage', () => {
  it('shows OS-specific operations guide and system status without trading controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/system/status?exchange=upbit') {
        return json(systemStatus());
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
      return json({});
    });
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('navigator', { userAgentData: { platform: 'Windows' }, userAgent: '' });

    renderWithClient();

    expect(await screen.findByText('시스템')).toBeInTheDocument();
    expect(screen.getAllByText('Windows').length).toBeGreaterThan(0);
    expect(screen.getByText('scripts\\run-upbit-paper.bat')).toBeInTheDocument();
    expect(screen.getByText('%USERPROFILE%\\workspace\\comebot')).toBeInTheDocument();
    expect(screen.getByText('운영 환경 (OS)')).toBeInTheDocument();
    expect(screen.getByText('스케줄러')).toBeInTheDocument();
    expect(screen.getByText('시세 Provider')).toBeInTheDocument();
    expect(screen.getByText('시세 Provider')).toBeInTheDocument();
    expect(screen.getByText('알림 · Telegram')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /매수|BUY|실거래|REAL/ })).not.toBeInTheDocument();
  });
});

function systemStatus() {
  return {
    database: { connected: true },
    marketProvider: { provider: 'UPBIT', externalProvider: true },
    strategy: { strategyName: 'VolatilityBreakoutLongStrategy', buyPrice: '90000000', sellPrice: '110000000', orderQuantity: '0.01', orderAmount: '10000' },
    risk: { maxOrderAmount: '100000', allowedMarkets: ['ALL_KRW'] },
    scheduler: {
      enabled: true,
      fixedDelayMs: 30000,
      markets: ['ALL_KRW'],
      candidateEnabled: true,
      candidateFixedDelayMs: 30000,
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
    portfolio: portfolioCash(),
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

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
