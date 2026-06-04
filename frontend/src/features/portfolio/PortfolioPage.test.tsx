import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ExchangeModeContext } from '../../shared/exchange/ExchangeModeContext';
import type { ExchangeMode } from '../../shared/api/types';
import { PortfolioPage } from './PortfolioPage';

function renderWithClient(exchange: ExchangeMode = 'UPBIT') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ExchangeModeContext.Provider value={{ exchange, setExchange: () => {} }}>
        <PortfolioPage />
      </ExchangeModeContext.Provider>
    </QueryClientProvider>,
  );
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe('PortfolioPage', () => {
  it('shows allocation and position exit status without buy or trading-flow controls', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/portfolio/status?exchange=upbit') {
        return json({ exchange: 'UPBIT', currency: 'KRW', cash: '985000', realizedProfit: '1200' });
      }
      if (url === '/api/system/status?exchange=upbit') {
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
            candidateExchange: 'UPBIT',
            exitEnabled: true,
            exitFixedDelayMs: 5000,
            exitSaveHoldHistory: false,
            exitExchange: 'UPBIT',
            exitPositionMarketCount: 0,
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/portfolio/positions?exchange=upbit') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation?exchange=upbit') {
        return json({
          exchange: 'UPBIT',
          currency: 'KRW',
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
      if (url === '/api/risk/status?exchange=upbit') {
        return json(riskStatus('UPBIT', '7', '10'));
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    // portfolio panels visible
    expect(await screen.findByText('자산 비중')).toBeInTheDocument();
    expect(await screen.findByText('마켓 비중')).toBeInTheDocument();
    expect(screen.getByText('손익 리더')).toBeInTheDocument();
    expect(screen.getByText('마켓별 비중')).toBeInTheDocument();
    // remaining buy count
    expect(screen.getByText('98회')).toBeInTheDocument();
    // exposure badge + threshold label
    expect(screen.getAllByText('OK').length).toBeGreaterThan(0);
    expect(screen.getByText('UPBIT 7% / 10%')).toBeInTheDocument();
    // position table rows
    expect((await screen.findAllByText('KRW-ETH')).length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText('KRW-BTC 선택').length).toBeGreaterThan(0);
    // exit badges
    expect(screen.getAllByText('손절권').length).toBeGreaterThan(0);
    expect(screen.getAllByText('보유').length).toBeGreaterThan(0);
    // no manual buy/execution controls
    expect(screen.queryByRole('button', { name: '실행' })).not.toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/candidates/execute'), expect.anything());
    expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/api/trading-flow/run'), expect.anything());
  });

  it('marks concentrated markets with exchange-specific warning thresholds', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/portfolio/status?exchange=upbit') {
        return json({ exchange: 'UPBIT', currency: 'KRW', cash: '890000', realizedProfit: '0' });
      }
      if (url === '/api/system/status?exchange=upbit') {
        return json(systemStatus());
      }
      if (url === '/api/portfolio/positions?exchange=upbit') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation?exchange=upbit') {
        return json({
          exchange: 'UPBIT',
          currency: 'KRW',
          cash: '890000',
          totalPositionValue: '110000',
          totalEquity: '1000000',
          realizedProfit: '0',
          unrealizedProfit: '0',
          totalProfit: '0',
          positions: [
            {
              market: 'KRW-BTC',
              quantity: '0.0011',
              averageBuyPrice: '100000000',
              currentPrice: '100000000',
              positionValue: '110000',
              unrealizedProfit: '0',
              unrealizedProfitRate: '0',
            },
          ],
        });
      }
      if (url === '/api/risk/status?exchange=upbit') {
        return json(riskStatus('UPBIT', '7', '10'));
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('BLOCK')).toBeInTheDocument();
    expect(screen.getByText('BLOCK')).toBeInTheDocument();
    expect(screen.getByText('UPBIT 7% / 10%')).toBeInTheDocument();
  });

  it('uses binance concentration thresholds on the portfolio exposure panel', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/portfolio/status?exchange=binance') {
        return json({ exchange: 'BINANCE', currency: 'USDT', cash: '700', realizedProfit: '0' });
      }
      if (url === '/api/system/status?exchange=binance') {
        return json(systemStatus());
      }
      if (url === '/api/portfolio/positions?exchange=binance') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation?exchange=binance') {
        return json({
          exchange: 'BINANCE',
          currency: 'USDT',
          cash: '700',
          totalPositionValue: '300',
          totalEquity: '1000',
          realizedProfit: '0',
          unrealizedProfit: '0',
          totalProfit: '0',
          positions: [
            {
              market: 'BTCUSDT',
              quantity: '0.003',
              averageBuyPrice: '100000',
              currentPrice: '100000',
              positionValue: '300',
              unrealizedProfit: '0',
              unrealizedProfitRate: '0',
            },
          ],
        });
      }
      if (url === '/api/risk/status?exchange=binance') {
        return json(riskStatus('BINANCE', '25', '40'));
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient('BINANCE');

    expect(await screen.findByText('WARN')).toBeInTheDocument();
    expect(screen.getByText('WARN')).toBeInTheDocument();
    expect(screen.getByText('BINANCE 25% / 40%')).toBeInTheDocument();
  });

  it('groups small market allocation slices into other', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/portfolio/status?exchange=upbit') {
        return json({ exchange: 'UPBIT', currency: 'KRW', cash: '0', realizedProfit: '0' });
      }
      if (url === '/api/system/status?exchange=upbit') {
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
            candidateExchange: 'UPBIT',
            exitEnabled: true,
            exitFixedDelayMs: 5000,
            exitSaveHoldHistory: false,
            exitExchange: 'UPBIT',
            exitPositionMarketCount: 0,
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/portfolio/positions?exchange=upbit') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation?exchange=upbit') {
        return json({
          exchange: 'UPBIT',
          currency: 'KRW',
          cash: '0',
          totalPositionValue: '21000',
          totalEquity: '21000',
          realizedProfit: '0',
          unrealizedProfit: '0',
          totalProfit: '0',
          positions: ['BTC', 'ETH', 'XRP', 'SOL', 'ADA', 'DOT'].map((symbol, index) => ({
            market: `KRW-${symbol}`,
            quantity: '1',
            averageBuyPrice: String(6000 - index * 1000),
            currentPrice: String(6000 - index * 1000),
            positionValue: String(6000 - index * 1000),
            unrealizedProfit: '0',
            unrealizedProfitRate: '0',
          })),
        });
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect(await screen.findByText('기타')).toBeInTheDocument();
  });

  it('shows empty chart state when total equity is zero', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url === '/api/portfolio/status?exchange=upbit') {
        return json({ exchange: 'UPBIT', currency: 'KRW', cash: '0', realizedProfit: '0' });
      }
      if (url === '/api/system/status?exchange=upbit') {
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
            candidateExchange: 'UPBIT',
            exitEnabled: true,
            exitFixedDelayMs: 5000,
            exitSaveHoldHistory: false,
            exitExchange: 'UPBIT',
            exitPositionMarketCount: 0,
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/portfolio/positions?exchange=upbit') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation?exchange=upbit') {
        return json({
          exchange: 'UPBIT',
          currency: 'KRW',
          cash: '0',
          totalPositionValue: '0',
          totalEquity: '0',
          realizedProfit: '0',
          unrealizedProfit: '0',
          totalProfit: '0',
          positions: [],
        });
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    expect((await screen.findAllByText('데이터 없음')).length).toBeGreaterThan(0);
  });

  it('sells only selected paper positions after confirmation', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url === '/api/portfolio/status?exchange=upbit') {
        return json({ exchange: 'UPBIT', currency: 'KRW', cash: '985000', realizedProfit: '1200' });
      }
      if (url === '/api/system/status?exchange=upbit') {
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
            candidateExchange: 'UPBIT',
            exitEnabled: true,
            exitFixedDelayMs: 5000,
            exitSaveHoldHistory: false,
            exitExchange: 'UPBIT',
            exitPositionMarketCount: 0,
          },
          safety: { killSwitchEnabled: false },
          notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
          telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
        });
      }
      if (url === '/api/portfolio/positions?exchange=upbit') {
        return json([]);
      }
      if (url === '/api/portfolio/valuation?exchange=upbit') {
        return json({
          exchange: 'UPBIT',
          currency: 'KRW',
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
          ],
        });
      }
      if (url === '/api/portfolio/positions/sell-selected?exchange=upbit') {
        expect(init?.method).toBe('POST');
        expect(init?.body).toBe(JSON.stringify({ markets: ['KRW-BTC'] }));
        return json({
          exchange: 'UPBIT',
          requestedCount: 1,
          succeededCount: 1,
          failedCount: 0,
          results: [
            {
              market: 'KRW-BTC',
              signalType: 'SELL',
              orderCreated: true,
              orderStatus: 'FILLED',
              message: 'Selected PAPER position sold',
              executedAt: '2026-05-07T00:00:00Z',
            },
          ],
        });
      }
      if (url === '/api/trading-flow/history?exchange=upbit&limit=20') {
        return json([]);
      }

      return json([]);
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithClient();

    await user.click((await screen.findAllByLabelText('KRW-BTC 선택'))[0]);
    await user.click(screen.getByRole('button', { name: /선택 매도/ }));
    const dialog = screen.getByRole('dialog', { name: '선택 PAPER SELL 확인' });
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText('실제 거래소 주문이 아닌 선택 보유 포지션의 PAPER SELL만 실행합니다.')).toBeInTheDocument();
    expect(within(dialog).getByText('KRW-BTC')).toBeInTheDocument();
    expect(within(dialog).getByText('실제 거래소 주문이 아닌 선택 보유 포지션의 PAPER SELL만 실행합니다.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'PAPER SELL 실행' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/portfolio/positions/sell-selected?exchange=upbit',
      expect.objectContaining({ method: 'POST' }),
    ));
    expect(await screen.findByText('매도 결과')).toBeInTheDocument();
    // sell result shows status and message in separate elements
    expect(screen.getAllByText('FILLED').length).toBeGreaterThan(0);
  });
});

function json(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } });
}

function systemStatus() {
  return {
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
      candidateExchange: 'UPBIT',
      exitEnabled: true,
      exitFixedDelayMs: 5000,
      exitSaveHoldHistory: false,
      exitExchange: 'UPBIT',
      exitPositionMarketCount: 0,
    },
    safety: { killSwitchEnabled: false },
    notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
    telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
  };
}

function riskStatus(exchange: ExchangeMode, warningExposureRate: string, blockExposureRate: string) {
  return {
    maxOrderAmount: '100000',
    allowedMarkets: ['ALL_KRW'],
    takeProfitRate: '1.5',
    stopLossRate: '-0.7',
    positionExitEnabled: true,
    dailyRiskEnabled: false,
    dailyOrderLimit: 50,
    dailyLossLimit: '50000',
    concentration: {
      exchange,
      enabled: false,
      warningExposureRate,
      blockExposureRate,
    },
    stopLossCooldown: {
      enabled: false,
      window: 'PT168H',
      triggerCount: 2,
      duration: 'PT24H',
    },
  };
}
