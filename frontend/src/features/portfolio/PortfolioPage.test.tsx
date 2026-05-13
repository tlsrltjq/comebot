import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
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

    expect(await screen.findByText('자산 배분(Allocation)')).toBeInTheDocument();
    expect(await screen.findByText('자산 비중(Asset Mix)')).toBeInTheDocument();
    expect(await screen.findByText('마켓 비중(Market Allocation)')).toBeInTheDocument();
    expect(await screen.findByText('거래소 비중(Exchange Allocation)')).toBeInTheDocument();
    expect(await screen.findByText('자금 활용(Capital Usage)')).toBeInTheDocument();
    expect(screen.getByText('매수 가능(Buys left) 98')).toBeInTheDocument();
    expect(screen.getByText('98회')).toBeInTheDocument();
    expect((await screen.findAllByText('KRW-ETH')).length).toBeGreaterThan(0);
    expect(screen.getByText('손익 리더(Profit Leaders)')).toBeInTheDocument();
    expect(screen.getByText('market별 비중(Market Exposure)')).toBeInTheDocument();
    expect(screen.getByText('UPBIT 7% / 10%')).toBeInTheDocument();
    expect(screen.getAllByText('현금(Cash)').length).toBeGreaterThan(0);
    expect(screen.getAllByText('포지션(Positions)').length).toBeGreaterThan(0);
    expect(screen.getByText('UPBIT 선택 거래소(Selected exchange)')).toBeInTheDocument();
    expect(screen.getByText('분산 양호(Diversified)')).toBeInTheDocument();
    expect(screen.getAllByText('OK').length).toBeGreaterThan(0);
    expect(screen.getAllByText('손절권(Stop loss)').length).toBeGreaterThan(0);
    expect(screen.getAllByText('보유(Hold)').length).toBeGreaterThan(0);
    expect(screen.getByLabelText('모바일 포지션 카드(Mobile position cards)')).toBeInTheDocument();
    expect(screen.getAllByText('평가액(Value)').length).toBeGreaterThan(0);
    expect(screen.getAllByText('손익률(PnL %)').length).toBeGreaterThan(0);
    expect(screen.getAllByText('미실현(Unrealized)').length).toBeGreaterThan(0);
    expect(screen.getAllByLabelText('KRW-BTC 선택').length).toBeGreaterThan(1);
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

    expect(await screen.findByText('차단 기준(Block)')).toBeInTheDocument();
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

    expect(await screen.findByText('쏠림 경고(Warning)')).toBeInTheDocument();
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

    expect(await screen.findByText('기타(Other)')).toBeInTheDocument();
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

    expect(await screen.findByText('자산 비중 없음(No asset mix)')).toBeInTheDocument();
    expect(screen.getByText('마켓 비중 없음(No market allocation)')).toBeInTheDocument();
  });

  it('sells only selected paper positions after confirmation', async () => {
    const user = userEvent.setup();
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
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

    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(
      '/api/portfolio/positions/sell-selected?exchange=upbit',
      expect.objectContaining({ method: 'POST' }),
    ));
    expect(confirmSpy).toHaveBeenCalled();
    expect(await screen.findByText('선택 매도 결과(Selected Sell Result)')).toBeInTheDocument();
    expect(screen.getByText('FILLED · Selected PAPER position sold')).toBeInTheDocument();
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
