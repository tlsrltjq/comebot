import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
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
    const user = userEvent.setup();
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/candidates?exchange=upbit&limit=20') {
        return new Response(
          JSON.stringify([
            {
              market: 'KRW-BTC',
              decision: 'SELECTED',
              reason: 'Volatility long candidate selected',
              reasonType: 'SELECTED',
              riskReasonType: 'NONE',
              currentPrice: '90000000',
              priceChangeRate: '2.1',
              highLowRangeRate: '4.2',
              tradeAmountChangeRate: '8.3',
              trend: 'UP',
              scannedAt: '2026-04-30T00:00:00Z',
            },
            {
              market: 'KRW-ETH',
              decision: 'SKIPPED',
              reason: 'Trend is not UP',
              reasonType: 'TREND_NOT_UP',
              riskReasonType: 'NONE',
              currentPrice: '2500000',
              priceChangeRate: '-0.4',
              highLowRangeRate: '2.2',
              tradeAmountChangeRate: '4.1',
              trend: 'DOWN',
              scannedAt: '2026-04-30T00:00:01Z',
            },
            {
              market: 'KRW-XRP',
              decision: 'SKIPPED',
              reason: 'Market concentration exceeds block exposure rate: market=KRW-XRP exposure=10% limit=10%',
              reasonType: 'CONCENTRATION_RISK',
              riskReasonType: 'CONCENTRATION',
              currentPrice: '800',
              priceChangeRate: '0.1',
              highLowRangeRate: '1.2',
              tradeAmountChangeRate: '2.1',
              trend: 'SIDEWAYS',
              scannedAt: '2026-04-30T00:00:02Z',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }
      if (url === '/api/portfolio/positions?exchange=upbit') {
        return new Response(
          JSON.stringify([{ market: 'KRW-BTC', quantity: '0.001', averageBuyPrice: '89000000' }]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }

      return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } });
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('KRW-BTC')).toBeInTheDocument();
    expect(screen.getByText('총 후보')).toBeInTheDocument();
    expect(screen.getByText('선택됨')).toBeInTheDocument();
    expect(screen.getByText('제외됨')).toBeInTheDocument();
    expect(screen.getByText('보유 마켓')).toBeInTheDocument();
    expect(screen.getByText('리스크 경고')).toBeInTheDocument();
    expect(screen.getByText(/쏠림.*cooldown/)).toBeInTheDocument();
    expect(screen.getByText('제외 사유 TOP 5')).toBeInTheDocument();
    // audit strip removed in redesign — check page title instead
    expect(screen.getByText('매수 후보')).toBeInTheDocument();
    expect(screen.getAllByText('Trend is not UP').length).toBeGreaterThan(0);
    expect(screen.getByText('CONCENTRATION')).toBeInTheDocument();
    expect(screen.getAllByText('보유').length).toBeGreaterThan(0);
    // position badge shows '-' for non-held markets
    expect(screen.getAllByText('-').length).toBeGreaterThan(0);
    expect(screen.getAllByText('SELECTED').length).toBeGreaterThan(0);
    expect(screen.getAllByText('SKIPPED').length).toBeGreaterThan(0);

    await user.click(screen.getByRole('button', { name: /선택만/ }));
    expect(screen.queryByText('KRW-ETH')).not.toBeInTheDocument();
    expect(screen.queryByText('KRW-XRP')).not.toBeInTheDocument();
    expect(screen.getByText('KRW-BTC')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '50' }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/candidates?exchange=upbit&limit=50', expect.anything()));

    await user.type(screen.getByPlaceholderText(/KRW-BTC/), 'KRW-SOL');
    expect(fetchMock).not.toHaveBeenCalledWith('/api/candidates?exchange=upbit&market=KRW-SOL', expect.anything());
    await user.click(screen.getByRole('button', { name: /조회/ }));
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith('/api/candidates?exchange=upbit&market=KRW-SOL', expect.anything()));

    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
  });

  it('shows backend errors for Binance mode', async () => {
    const fetchMock = vi.fn(async () => new Response('Binance candidate scan failed', { status: 500 }));
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient('BINANCE');

    expect((await screen.findAllByRole('alert'))[0]).toHaveTextContent('Binance candidate scan failed');
    expect(fetchMock).toHaveBeenCalledWith('/api/candidates?exchange=binance&limit=20', expect.anything());
  });
});
