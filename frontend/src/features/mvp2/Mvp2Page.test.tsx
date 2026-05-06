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
      return json({});
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('MVP2 실험 대시보드(Experiment Dashboard)')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /Upbit/ })).toBeInTheDocument();
    fireEvent.click(await screen.findByRole('button', { name: /Binance/ }));

    expect((await screen.findAllByText('Binance public ticker/kline provider is available.')).length).toBeGreaterThan(0);
    expect(screen.getByText('안정형(Stable)')).toBeInTheDocument();
    expect(screen.getByText('공격형(Aggressive)')).toBeInTheDocument();
    expect(screen.getByText('수비형(Defensive)')).toBeInTheDocument();
    expect(screen.getByText('실제 주문 없음(No real orders)')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
