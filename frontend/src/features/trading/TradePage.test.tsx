import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TradePage } from './TradePage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <TradePage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('TradePage', () => {
  it('shows scheduler state without manual execution controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/system/status?exchange=upbit') {
        return new Response(
          JSON.stringify({
            database: { connected: true },
            marketProvider: { provider: 'UPBIT', externalProvider: true },
            strategy: { strategyName: 'VolatilityBreakoutLongStrategy', buyPrice: '90000000', sellPrice: '110000000', orderQuantity: '0.001', orderAmount: '10000' },
            risk: { maxOrderAmount: '100000', allowedMarkets: ['KRW-BTC', 'KRW-ETH'] },
            scheduler: {
              enabled: true,
              fixedDelayMs: 60000,
              markets: ['KRW-BTC', 'KRW-ETH'],
              candidateEnabled: true,
              candidateFixedDelayMs: 60000,
              candidateMarkets: ['KRW-BTC', 'KRW-ETH'],
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
            safety: { killSwitchEnabled: false },
            notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
            telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }

      if (url === '/api/trading-flow/history?exchange=upbit&limit=10') {
        return new Response(
          JSON.stringify([
            {
              id: 'history-1',
              exchange: 'UPBIT',
              market: 'KRW-BTC',
              currentPrice: '100000000',
              signalType: 'BUY',
              signalReason: 'Long candidate selected',
              orderCreated: true,
              orderStatus: 'FILLED',
              message: 'Paper buy filled',
              createdAt: '2026-05-13T00:00:00Z',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }

      return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('제어 범위(Control scope)')).toBeInTheDocument();
    expect(screen.getByText('후보 BUY와 보유 PAPER SELL 평가 중')).toBeInTheDocument();
    expect(screen.getByText('PAPER 전용(Paper only)')).toBeInTheDocument();
    expect(screen.getByText('전략 스케줄러(Legacy Trading Scheduler)')).toBeInTheDocument();
    expect(screen.getByText('청산 스케줄러(Exit Scheduler)')).toBeInTheDocument();
    expect(screen.getByText('최근 결과(Latest)')).toBeInTheDocument();
    expect(screen.getByText('KRW-BTC / FILLED')).toBeInTheDocument();
    expect(screen.getAllByText('켜짐(Enabled)').length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
  });
});
