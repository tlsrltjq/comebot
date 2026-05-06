import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Mvp2Page } from './Mvp2Page';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <Mvp2Page />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('Mvp2Page', () => {
  it('shows exchange selector and status without manual trading controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/mvp2/exchanges') {
        return json([
          {
            exchange: 'UPBIT',
            displayName: 'Upbit',
            enabled: true,
            publicMarketDataOnly: true,
            statusPath: '/api/mvp2/exchanges/UPBIT/status',
          },
          {
            exchange: 'BINANCE',
            displayName: 'Binance',
            enabled: true,
            publicMarketDataOnly: true,
            statusPath: '/api/mvp2/exchanges/BINANCE/status',
          },
        ]);
      }
      if (url === '/api/mvp2/exchanges/UPBIT/status') {
        return json({
          exchange: 'UPBIT',
          displayName: 'Upbit',
          enabled: true,
          publicMarketDataOnly: true,
          realTradingSupported: false,
          marketData: 'Upbit public ticker/candle adapter is available.',
          message: 'MVP2 uses public market data only. Orders remain PAPER/SIMULATION only.',
        });
      }
      if (url === '/api/system/status') {
        return json({
          database: { connected: true },
          marketProvider: { provider: 'UPBIT', externalProvider: true },
          strategy: {
            strategyName: 'VolatilityBreakoutLongStrategy',
            buyPrice: '0',
            sellPrice: '0',
            orderQuantity: '0',
            orderAmount: '10000',
          },
          risk: { maxOrderAmount: '10000', allowedMarkets: ['ALL_KRW'] },
          scheduler: {
            enabled: false,
            fixedDelayMs: 30000,
            markets: [],
            candidateEnabled: true,
            candidateFixedDelayMs: 30000,
            candidateMarkets: ['ALL_KRW'],
            candidateNotifySummary: false,
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/portfolio/valuation') {
        return json({
          cash: '990000',
          totalPositionValue: '12000',
          totalEquity: '1002000',
          realizedProfit: '0',
          unrealizedProfit: '2000',
          totalProfit: '2000',
          positions: [
            {
              market: 'KRW-BTC',
              quantity: '0.0001',
              averageBuyPrice: '100000000',
              currentPrice: '120000000',
              positionValue: '12000',
              unrealizedProfit: '2000',
              unrealizedProfitRate: '20',
            },
          ],
        });
      }
      if (url === '/api/candidates') {
        return json([
          {
            market: 'KRW-BTC',
            decision: 'SELECTED',
            reason: 'Volatility long candidate selected',
            currentPrice: '120000000',
            priceChangeRate: '1',
            highLowRangeRate: '2',
            tradeAmountChangeRate: '30',
            trend: 'UP',
            scannedAt: '2026-05-06T00:00:00Z',
          },
        ]);
      }
      if (url === '/api/trading-flow/history?limit=10') {
        return json([
          {
            id: '1',
            market: 'KRW-BTC',
            currentPrice: '120000000',
            signalType: 'BUY',
            signalReason: 'Volatility long candidate selected',
            orderCreated: true,
            orderStatus: 'FILLED',
            message: 'PAPER order filled',
            createdAt: '2026-05-06T00:00:00Z',
          },
        ]);
      }
      if (url === '/api/mvp2/exchanges/BINANCE/status') {
        return json({
          exchange: 'BINANCE',
          displayName: 'Binance',
          enabled: true,
          publicMarketDataOnly: true,
          realTradingSupported: false,
          marketData: 'Binance public ticker/kline provider is available.',
          message: 'MVP2 uses public market data only. Orders remain PAPER/SIMULATION only.',
        });
      }
      if (url === '/api/mvp2/binance/paper/status') {
        return json({
          schedulerEnabled: false,
          schedulerFixedDelayMs: 30000,
          symbols: ['BTCUSDT', 'ETHUSDT'],
          initialCash: '1000',
          orderAmount: '10',
          takeProfitRate: '1.5',
          stopLossRate: '-0.7',
        });
      }
      if (url === '/api/mvp2/binance/paper/portfolio') {
        return json({
          exchange: 'BINANCE',
          cash: '990',
          realizedProfit: '0',
          positions: [{ symbol: 'BTCUSDT', quantity: '0.0001', averageBuyPrice: '100000' }],
        });
      }
      if (url === '/api/mvp2/binance/paper/valuation') {
        return json({
          exchange: 'BINANCE',
          cash: '990',
          totalPositionValue: '10.5',
          totalEquity: '1000.5',
          realizedProfit: '0',
          unrealizedProfit: '0.5',
          totalProfit: '0.5',
          positions: [
            {
              symbol: 'BTCUSDT',
              quantity: '0.0001',
              averageBuyPrice: '100000',
              currentPrice: '105000',
              positionValue: '10.5',
              unrealizedProfit: '0.5',
              unrealizedProfitRate: '5',
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
            currentPrice: '100000',
            priceChangeRate: '1',
            highLowRangeRate: '2',
            tradeAmountChangeRate: '30',
            trend: 'UP',
            scannedAt: '2026-05-06T00:00:00Z',
          },
        ]);
      }
      if (url === '/api/mvp2/binance/paper/history?limit=10') {
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

    expect(await screen.findByText('MVP2 실험 대시보드(Experiment Dashboard)')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /Upbit 모드/ })).toBeInTheDocument();
    expect(await screen.findByText('Upbit PAPER')).toBeInTheDocument();
    expect(await screen.findByText('₩1,002,000')).toBeInTheDocument();
    expect((await screen.findAllByText('KRW-BTC')).length).toBeGreaterThan(0);
    fireEvent.click(await screen.findByRole('button', { name: /Binance/ }));

    expect((await screen.findAllByText('Binance public ticker/kline provider is available.')).length).toBeGreaterThan(0);
    expect(await screen.findByText('Binance PAPER')).toBeInTheDocument();
    expect(await screen.findByText('총손익(Total PnL)')).toBeInTheDocument();
    expect(await screen.findByText('1,000.5 USDT')).toBeInTheDocument();
    expect((await screen.findAllByText('BTCUSDT')).length).toBeGreaterThan(0);
    expect((await screen.findAllByText((_, element) => element?.textContent?.includes('MVP2 paper order filled') ?? false)).length).toBeGreaterThan(0);
    expect(screen.getByText('안정형(Stable)')).toBeInTheDocument();
    expect(screen.getByText('공격형(Aggressive)')).toBeInTheDocument();
    expect(screen.getByText('수비형(Defensive)')).toBeInTheDocument();
    expect(screen.getByText('실제 주문 없음(No real orders)')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/mvp2/binance/paper/run'), expect.anything());
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
