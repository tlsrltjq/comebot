import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PortfolioPage } from './PortfolioPage';

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <PortfolioPage />
    </QueryClientProvider>,
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('PortfolioPage', () => {
  it('shows allocation and position exit status without manual trading controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/portfolio/status') {
        return json({ cash: '985000', realizedProfit: '1200' });
      }
      if (url === '/api/system/status') {
        return json({
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
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/portfolio/positions') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation') {
        return json({
          cash: '985000',
          totalPositionValue: '15000',
          totalEquity: '1000000',
          realizedProfit: '1200',
          unrealizedProfit: '-100',
          totalProfit: '1100',
          positions: [
            {
              market: 'KRW-BTC',
              quantity: '0.0001',
              averageBuyPrice: '100000000',
              currentPrice: '100900000',
              positionValue: '10090',
              unrealizedProfit: '90',
              unrealizedProfitRate: '0.9',
            },
            {
              market: 'KRW-ETH',
              quantity: '0.002',
              averageBuyPrice: '2500000',
              currentPrice: '2480000',
              positionValue: '4960',
              unrealizedProfit: '-40',
              unrealizedProfitRate: '-0.8',
            },
          ],
        });
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('자산 배분(Allocation)')).toBeInTheDocument();
    expect(await screen.findByText('자금 활용(Capital Usage)')).toBeInTheDocument();
    expect(screen.getByText('매수 가능(Buys left) 98')).toBeInTheDocument();
    expect(screen.getByText('98회')).toBeInTheDocument();
    expect((await screen.findAllByText('KRW-ETH')).length).toBeGreaterThan(0);
    expect(screen.getByText('손익 리더(Profit Leaders)')).toBeInTheDocument();
    expect(screen.getByText('손절권(Stop loss)')).toBeInTheDocument();
    expect(screen.getByText('보유(Hold)')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}
