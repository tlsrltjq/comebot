import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { HistoryPage } from './HistoryPage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <HistoryPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('HistoryPage', () => {
  it('shows analytics, loss review, and client-side filters without manual execution controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.startsWith('/api/trading-flow/history')) {
        return json([
          {
            id: 'history-1',
            market: 'KRW-BTC',
            currentPrice: '100000000',
            signalType: 'SELL',
            signalReason: 'Stop loss rate reached: -1.25000000',
            orderCreated: true,
            orderStatus: 'FILLED',
            message: 'Paper sell filled',
            createdAt: '2026-05-04T00:00:00Z',
          },
          {
            id: 'history-2',
            market: 'KRW-ETH',
            currentPrice: '2500000',
            signalType: 'HOLD',
            signalReason: 'Trend is not UP',
            orderCreated: false,
            orderStatus: null,
            message: 'No order',
            createdAt: '2026-05-04T00:01:00Z',
          },
        ]);
      }
      if (url.startsWith('/api/analytics/summary')) {
        return json({
          range: '24h',
          from: '2026-05-03T00:00:00Z',
          to: '2026-05-04T00:00:00Z',
          total: 12,
          buyCount: 2,
          sellCount: 1,
          holdCount: 9,
          filledCount: 3,
          rejectedCount: 0,
          failedCount: 0,
          stopLossCount: 1,
          takeProfitCount: 0,
          averageStopLossRate: '-1.25',
          averageTakeProfitRate: '0',
          winRate: '0',
          averageHoldingSeconds: 3600,
          profitLossRatio: '0',
          topHoldReasons: [{ reason: 'Trend is not UP', count: 5 }],
          topMarkets: [{ market: 'KRW-BTC', count: 2 }],
        });
      }
      if (url.startsWith('/api/analytics/losses')) {
        return json({
          range: '24h',
          worstTrades: [
            {
              market: 'KRW-BTC',
              currentPrice: '100000000',
              rate: '-1.25',
              reason: 'Stop loss rate reached: -1.25000000',
              createdAt: '2026-05-04T00:00:00Z',
            },
          ],
          repeatedStopLossMarkets: [{ market: 'KRW-BTC', count: 1 }],
        });
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('반복 HOLD 사유 TOP 5')).toBeInTheDocument();
    expect(screen.getByText('주문 상태 분포')).toBeInTheDocument();
    expect(screen.getByText('거절 / 실패')).toBeInTheDocument();
    expect(screen.getByText('손실 원인')).toBeInTheDocument();
    expect((await screen.findAllByText('Trend is not UP')).length).toBeGreaterThan(0);
    expect(screen.getAllByText('FILLED').length).toBeGreaterThan(0);
    expect(screen.getAllByText('NO_ORDER').length).toBeGreaterThan(0);
    expect(screen.getAllByText('손절').length).toBeGreaterThan(0);
    expect((await screen.findAllByText('KRW-ETH')).length).toBeGreaterThan(0);
    expect(fetchMock).toHaveBeenCalledWith('/api/trading-flow/history?exchange=upbit&limit=50', expect.anything());

    await userEvent.click(screen.getByRole('button', { name: 'SL' }));

    expect(screen.getAllByText('KRW-BTC').length).toBeGreaterThan(0);
    expect(screen.queryByText('KRW-ETH')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '200' }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/trading-flow/history?exchange=upbit&limit=200', expect.anything()));

    await userEvent.type(screen.getByPlaceholderText(/KRW-BTC/), 'KRW-SOL');
    expect(fetchMock).not.toHaveBeenCalledWith('/api/trading-flow/history?exchange=upbit&limit=200&market=KRW-SOL', expect.anything());
    await userEvent.click(screen.getByRole('button', { name: /조회/ }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/trading-flow/history?exchange=upbit&market=KRW-SOL&limit=200', expect.anything()));

    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
