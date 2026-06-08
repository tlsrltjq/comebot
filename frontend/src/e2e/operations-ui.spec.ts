import { expect, test, type Page } from '@playwright/test';

const fixedNow = '2026-05-14T02:00:00Z';

test.beforeEach(async ({ page }) => {
  await mockApi(page);
});

test.describe('operations UI regression', () => {
  const routes = [
    { path: '/', heading: '운영 대시보드' },
    { path: '/market?exchange=upbit', heading: '시장 차트' },
    { path: '/candidates?exchange=upbit', heading: '매수 후보' },
    { path: '/portfolio?exchange=upbit', heading: '포트폴리오' },
    { path: '/history?exchange=upbit', heading: '거래 이력' },
    { path: '/risk?exchange=upbit', heading: '리스크' },
    { path: '/system?exchange=upbit', heading: '시스템' },
  ];

  for (const route of routes) {
    test(`${route.heading} renders without layout overflow`, async ({ page }) => {
      await page.goto(route.path);
      await expect(page.getByRole('heading', { name: route.heading, exact: true })).toBeVisible();
      await expect(page.getByLabel('운영 상태 바(Operation status bar)')).toBeVisible();
      await expect(page.getByText('PAPER_TRADING').first()).toBeVisible();
      await expect(page.getByRole('button', { name: /매수 실행|BUY 실행|수동.*BUY|실거래|REAL_TRADING/ })).toHaveCount(0);
      await expect(page.getByRole('button', { name: /^실행$/ })).toHaveCount(0);
      await assertNoHorizontalOverflow(page);
    });
  }

  test('portfolio selected PAPER SELL confirmation stays scoped to selected positions', async ({ page }) => {
    await page.goto('/portfolio?exchange=upbit');
    await page.getByLabel('KRW-BTC 선택').filter({ visible: true }).check();
    await page.getByRole('button', { name: /선택 매도/ }).click();

    const dialog = page.getByRole('dialog', { name: '선택 PAPER SELL 확인' });
    await expect(dialog).toBeVisible();
    await expect(dialog.getByText('실제 거래소 주문이 아닌 선택 보유 포지션의 PAPER SELL만 실행합니다.')).toBeVisible();
    await expect(dialog.getByText('KRW-BTC')).toBeVisible();
    await expect(dialog.getByText('선택한 보유 PAPER 포지션 전량')).toBeVisible();
    await expect(page.getByRole('button', { name: /매수 실행|BUY 실행|수동.*BUY/ })).toHaveCount(0);
  });
});

async function assertNoHorizontalOverflow(page: Page) {
  const overflow = await page.evaluate(() => {
    const root = document.documentElement;
    return Math.ceil(root.scrollWidth) - Math.ceil(root.clientWidth);
  });
  expect(overflow).toBeLessThanOrEqual(2);
}

async function mockApi(page: Page) {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;

    if (!path.startsWith('/api/')) {
      return route.fallback();
    }

    if (path === '/api/system/status') {
      return route.fulfill({ json: systemStatus() });
    }
    if (path === '/api/market-provider/status') {
      return route.fulfill({ json: marketProviderStatus() });
    }
    if (path === '/api/analytics/summary') {
      return route.fulfill({ json: analyticsSummary() });
    }
    if (path === '/api/analytics/pnl') {
      return route.fulfill({ json: analyticsPnl() });
    }
    if (path === '/api/analytics/losses') {
      return route.fulfill({ json: analyticsLosses() });
    }
    if (path === '/api/risk/status') {
      return route.fulfill({ json: riskStatus() });
    }
    if (path === '/api/portfolio/status') {
      return route.fulfill({ json: portfolioStatus() });
    }
    if (path === '/api/portfolio/positions') {
      return route.fulfill({ json: positions() });
    }
    if (path === '/api/portfolio/valuation') {
      return route.fulfill({ json: portfolioValuation() });
    }
    if (path === '/api/candidates') {
      return route.fulfill({ json: candidates() });
    }
    if (path === '/api/trading-flow/history') {
      return route.fulfill({ json: historyRows() });
    }
    if (path === '/api/market/btc-change') {
      return route.fulfill({ json: btcChange() });
    }
    if (path === '/api/portfolio/positions/sell-selected') {
      return route.fulfill({ json: selectedSellResult() });
    }

    return route.fulfill({ json: {} });
  });
}

function systemStatus() {
  return {
    database: { connected: true },
    marketProvider: { provider: 'UPBIT', externalProvider: true },
    strategy: { strategyName: 'VolatilityBreakoutLongStrategy', buyPrice: '90000000', sellPrice: '110000000', orderQuantity: '0.001', orderAmount: '10000' },
    risk: { maxOrderAmount: '100000', allowedMarkets: ['ALL_KRW'] },
    scheduler: {
      enabled: false,
      fixedDelayMs: 60000,
      markets: ['ALL_KRW'],
      candidateEnabled: true,
      candidateFixedDelayMs: 60000,
      candidateMarkets: ['ALL_KRW'],
      candidateNotifySummary: false,
      candidateExchange: 'UPBIT',
      candidateExchanges: ['UPBIT'],
      exitEnabled: true,
      exitFixedDelayMs: 5000,
      exitSaveHoldHistory: false,
      exitExchange: 'UPBIT',
      exitExchanges: ['UPBIT'],
      exitPositionMarketCount: 2,
    },
    portfolio: portfolioCash(),
    safety: { killSwitchEnabled: false },
    notification: { enabled: false, sendHold: false, sendFilled: true, sendRejected: true },
    telegram: { enabled: false, configured: false, inboundEnabled: false, manualPaperExecutionEnabled: false },
  };
}

function portfolioCash() {
  return {
    exchange: 'UPBIT',
    currency: 'KRW',
    cash: '900000',
    initialCash: '1000000',
    orderAmount: '10000',
    cashRate: '90.00',
    remainingBuyCount: 90,
    cashWarning: false,
    cashWarningMessage: 'PAPER cash is available',
  };
}

function marketProviderStatus() {
  return {
    provider: 'UPBIT',
    externalProvider: true,
    message: 'ok',
    webSocketEnabled: true,
    snapshotCount: 120,
    upbitSnapshotCount: 120,
    binanceSnapshotCount: 0,
    freshSnapshotCount: 118,
    staleSnapshotCount: 2,
    orderStaleMs: 3000,
  };
}

function analyticsSummary() {
  return {
    range: '24h',
    from: '2026-05-13T02:00:00Z',
    to: fixedNow,
    total: 12,
    buyCount: 3,
    sellCount: 2,
    holdCount: 7,
    filledCount: 5,
    rejectedCount: 1,
    failedCount: 0,
    stopLossCount: 1,
    takeProfitCount: 1,
    averageStopLossRate: '-0.7',
    averageTakeProfitRate: '1.5',
    winRate: '33.33333333',
    averageHoldingSeconds: 5400,
    profitLossRatio: '2.14285714',
    topHoldReasons: [{ reason: 'Trend is not UP', count: 4 }],
    topMarkets: [{ market: 'KRW-BTC', count: 3 }],
  };
}

function analyticsPnl() {
  return {
    range: '24h',
    cash: '985000',
    totalPositionValue: '15050',
    totalEquity: '1000050',
    realizedProfit: '1200',
    unrealizedProfit: '50',
    totalProfit: '1250',
    positionCount: 2,
  };
}

function analyticsLosses() {
  return {
    range: '24h',
    worstTrades: [
      {
        market: 'KRW-ETH',
        currentPrice: '2480000',
        rate: '-0.8',
        reason: 'Stop loss rate reached',
        createdAt: fixedNow,
      },
    ],
    repeatedStopLossMarkets: [{ market: 'KRW-ETH', count: 1 }],
  };
}

function riskStatus() {
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
      exchange: 'UPBIT',
      enabled: true,
      warningExposureRate: '7',
      blockExposureRate: '10',
    },
    stopLossCooldown: {
      enabled: true,
      window: 'PT168H',
      triggerCount: 2,
      duration: 'PT24H',
    },
  };
}

function portfolioStatus() {
  return { exchange: 'UPBIT', currency: 'KRW', cash: '985000', realizedProfit: '1200' };
}

function positions() {
  return [
    { market: 'KRW-BTC', quantity: '0.0001', averageBuyPrice: '100000000' },
    { market: 'KRW-ETH', quantity: '0.002', averageBuyPrice: '2500000' },
  ];
}

function portfolioValuation() {
  return {
    exchange: 'UPBIT',
    currency: 'KRW',
    cash: '985000',
    totalPositionValue: '15050',
    totalEquity: '1000050',
    realizedProfit: '1200',
    unrealizedProfit: '50',
    totalProfit: '1250',
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
  };
}

function candidates() {
  return [
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
      scannedAt: fixedNow,
    },
    {
      market: 'KRW-XRP',
      decision: 'SKIPPED',
      reason: 'Market concentration exceeds block exposure rate',
      reasonType: 'CONCENTRATION_RISK',
      riskReasonType: 'CONCENTRATION',
      currentPrice: '800',
      priceChangeRate: '0.1',
      highLowRangeRate: '1.2',
      tradeAmountChangeRate: '2.1',
      trend: 'SIDEWAYS',
      scannedAt: fixedNow,
    },
  ];
}

function historyRows() {
  return [
    {
      id: 'history-1',
      market: 'KRW-BTC',
      currentPrice: '100000000',
      signalType: 'SELL',
      signalReason: 'Take profit rate reached',
      orderCreated: true,
      orderStatus: 'FILLED',
      message: 'Paper sell filled',
      createdAt: fixedNow,
    },
    {
      id: 'history-2',
      market: 'KRW-ETH',
      currentPrice: '2480000',
      signalType: 'HOLD',
      signalReason: 'Trend is not UP',
      orderCreated: false,
      orderStatus: null,
      message: 'No order',
      createdAt: '2026-05-14T01:55:00Z',
    },
  ];
}

function btcChange() {
  return {
    exchange: 'UPBIT',
    market: 'KRW-BTC',
    range: '24h',
    basePrice: '98000000',
    latestPrice: '100000000',
    highPrice: '101000000',
    lowPrice: '97000000',
    changeRate: '2.0408',
    points: [
      { time: '2026-05-13T02:00:00Z', price: '98000000', changeRate: '0' },
      { time: '2026-05-13T14:00:00Z', price: '99000000', changeRate: '1.0204' },
      { time: fixedNow, price: '100000000', changeRate: '2.0408' },
    ],
  };
}

function selectedSellResult() {
  return {
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
        executedAt: fixedNow,
      },
    ],
  };
}
