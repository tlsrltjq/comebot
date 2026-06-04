import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MarketOverviewPage } from './MarketOverviewPage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MarketOverviewPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('MarketOverviewPage', () => {
  it('shows BTC change chart metrics and range controls without trading buttons', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.startsWith('/api/market/btc-change')) {
        return new Response(
          JSON.stringify({
            exchange: 'UPBIT',
            market: 'KRW-BTC',
            range: '24h',
            basePrice: '100000000',
            latestPrice: '103000000',
            changeRate: '3.00000000',
            highPrice: '104000000',
            lowPrice: '99000000',
            points: [
              { time: '2026-05-08T00:00:00Z', price: '100000000', changeRate: '0.00000000' },
              { time: '2026-05-08T00:15:00Z', price: '103000000', changeRate: '3.00000000' },
            ],
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }
      return new Response('{}', { status: 404 });
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('시장 차트')).toBeInTheDocument();
    expect(await screen.findByText('KRW-BTC')).toBeInTheDocument();
    expect((await screen.findAllByText('3%')).length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: /매수|BUY|매도|SELL/ })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '7d' }));
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/market/btc-change?exchange=upbit&range=7d'), expect.anything());
  });
});
