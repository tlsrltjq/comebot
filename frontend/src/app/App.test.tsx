import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';

function renderApp(initialEntry = '/') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  const router = createMemoryRouter(
    [
      {
        path: '/',
        element: <App />,
        children: [
          { index: true, element: <div>content</div> },
          { path: 'risk', element: <div>risk</div> },
          { path: 'system', element: <div>system</div> },
        ],
      },
    ],
    { initialEntries: [initialEntry] },
  );

  return render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

describe('App', () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('defaults to Upbit exchange mode', () => {
    vi.stubGlobal('fetch', mockStatusFetch());
    renderApp();

    expect(screen.getByRole('button', { name: 'UPBIT' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'BINANCE' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByLabelText('운영 상태 바(Operation status bar)')).toBeInTheDocument();
    expect(screen.getAllByText('PAPER_TRADING').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: /Risk/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /System/ })).toBeInTheDocument();
  });

  it('reads and updates exchange mode from the URL query', async () => {
    vi.stubGlobal('fetch', mockStatusFetch());
    renderApp('/?exchange=binance');

    expect(screen.getByRole('button', { name: 'BINANCE' })).toHaveAttribute('aria-pressed', 'true');

    await userEvent.click(screen.getByRole('button', { name: 'UPBIT' }));

    expect(screen.getByRole('button', { name: 'UPBIT' })).toHaveAttribute('aria-pressed', 'true');
  });
});

function mockStatusFetch() {
  return vi.fn(async (input: RequestInfo | URL) => {
    const url = String(input);
    if (url.startsWith('/api/system/status')) {
      return json({
        database: { connected: true },
        marketProvider: { provider: 'UPBIT', externalProvider: true },
        strategy: { strategyName: 'VolatilityBreakoutLongStrategy', buyPrice: '90000000', sellPrice: '110000000', orderQuantity: '0.001', orderAmount: '10000' },
        risk: { maxOrderAmount: '100000', allowedMarkets: ['ALL_KRW'] },
        scheduler: {
          enabled: false,
          fixedDelayMs: 60000,
          markets: ['KRW-BTC'],
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
          exitPositionMarketCount: 1,
        },
        portfolio: portfolioCash(),
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
        snapshotCount: 20,
        upbitSnapshotCount: 20,
        binanceSnapshotCount: 10,
        freshSnapshotCount: 20,
        staleSnapshotCount: 0,
        orderStaleMs: 3000,
      });
    }
    return json({});
  });
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
