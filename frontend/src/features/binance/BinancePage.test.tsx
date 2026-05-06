import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { BinancePage } from './BinancePage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BinancePage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('BinancePage', () => {
  it('shows Binance paper monitoring without manual order controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/mvp2/binance/paper/status') {
        return json({
          schedulerEnabled: true,
          schedulerFixedDelayMs: 30000,
          symbols: ['BTCUSDT', 'ETHUSDT'],
          initialCash: '1000',
          orderAmount: '10',
          takeProfitRate: '1.5',
          stopLossRate: '-0.7',
        });
      }
      if (url === '/api/mvp2/binance/paper/valuation') {
        return json({
          exchange: 'BINANCE',
          cash: '990',
          totalPositionValue: '12',
          totalEquity: '1002',
          realizedProfit: '0',
          unrealizedProfit: '2',
          totalProfit: '2',
          positions: [
            {
              symbol: 'BTCUSDT',
              quantity: '0.0001',
              averageBuyPrice: '100000',
              currentPrice: '120000',
              positionValue: '12',
              unrealizedProfit: '2',
              unrealizedProfitRate: '20',
            },
          ],
        });
      }
      if (url === '/api/mvp2/binance/paper/candidates') {
        return json([
          {
            exchange: 'BINANCE',
            symbol: 'BTCUSDT',
            decision: 'SELECTED',
            reason: 'Volatility long candidate selected',
            currentPrice: '120000',
            priceChangeRate: '1',
            highLowRangeRate: '2',
            tradeAmountChangeRate: '30',
            trend: 'UP',
            scannedAt: '2026-05-06T00:00:00Z',
          },
        ]);
      }
      if (url === '/api/mvp2/binance/paper/history?limit=12') {
        return json([
          {
            exchange: 'BINANCE',
            symbol: 'BTCUSDT',
            side: 'BUY',
            quantity: '0.0001',
            price: '100000',
            status: 'FILLED',
            reason: 'Volatility long candidate selected',
            message: 'MVP2 paper order filled',
            createdAt: '2026-05-06T00:00:00Z',
          },
        ]);
      }
      return json({});
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('Binance PAPER 대시보드(Binance Paper Dashboard)')).toBeInTheDocument();
    expect(await screen.findByText('자동 실행 중(Auto Running)')).toBeInTheDocument();
    expect(await screen.findByText('총자산(Total Equity)')).toBeInTheDocument();
    expect(await screen.findByText('1,002 USDT')).toBeInTheDocument();
    expect((await screen.findAllByText('BTCUSDT')).length).toBeGreaterThan(0);
    expect(await screen.findByText('자동 실행 원칙(Auto Rules)')).toBeInTheDocument();
    expect(screen.getByText('없음(None)')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /BUY|SELL|매수|매도|실행/ })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/mvp2/binance/paper/run'), expect.anything());
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
