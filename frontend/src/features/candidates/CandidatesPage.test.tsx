import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { CandidatesPage } from './CandidatesPage';
import { ExchangeModeContext } from '../../shared/exchange/ExchangeModeContext';
import type { ExchangeMode } from '../../shared/api/types';

function renderWithClient(exchange: ExchangeMode = 'UPBIT') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ExchangeModeContext.Provider value={{ exchange, setExchange: vi.fn() }}>
        <CandidatesPage />
      </ExchangeModeContext.Provider>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('CandidatesPage', () => {
  it('shows candidates without exposing manual execution controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/candidates?exchange=upbit') {
        return new Response(
          JSON.stringify([
            {
              market: 'KRW-BTC',
              decision: 'SELECTED',
              reason: 'Volatility long candidate selected',
              currentPrice: '90000000',
              priceChangeRate: '2.1',
              highLowRangeRate: '4.2',
              tradeAmountChangeRate: '8.3',
              trend: 'UP',
              scannedAt: '2026-04-30T00:00:00Z',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }

      return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('KRW-BTC')).toBeInTheDocument();
    expect(screen.getByText('SELECTED')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
  });

  it('shows the backend not implemented response for Binance mode', async () => {
    const fetchMock = vi.fn(async () => new Response('Binance exchange mode is not implemented yet', { status: 501 }));
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient('BINANCE');

    expect(await screen.findByRole('alert')).toHaveTextContent('Binance exchange mode is not implemented yet');
    expect(fetchMock).toHaveBeenCalledWith('/api/candidates?exchange=binance', expect.anything());
  });
});
