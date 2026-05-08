export type SignalType = 'BUY' | 'SELL' | 'HOLD';
export type OrderStatus = 'FILLED' | 'REJECTED' | 'FAILED';
export type CandidateDecision = 'SELECTED' | 'SKIPPED';
export type MarketTrend = 'UP' | 'DOWN' | 'SIDEWAYS';
export type AnalyticsRange = '1h' | '24h' | '3d' | '7d';
export type ExchangeMode = 'UPBIT' | 'BINANCE';

export interface SystemStatusResponse {
  database: { connected: boolean };
  marketProvider: { provider: string; externalProvider: boolean };
  strategy: {
    strategyName: string;
    buyPrice: string;
    sellPrice: string;
    orderQuantity: string;
    orderAmount: string;
  };
  risk: { maxOrderAmount: string; allowedMarkets: string[] };
  scheduler: {
    enabled: boolean;
    fixedDelayMs: number;
    markets: string[];
    candidateEnabled: boolean;
    candidateFixedDelayMs: number;
    candidateMarkets: string[];
    candidateNotifySummary: boolean;
    exitEnabled: boolean;
    exitFixedDelayMs: number;
    exitSaveHoldHistory: boolean;
    exitExchange: ExchangeMode;
    exitPositionMarketCount: number;
  };
  safety: { killSwitchEnabled: boolean };
  notification: {
    enabled: boolean;
    sendHold: boolean;
    sendFilled: boolean;
    sendRejected: boolean;
  };
  telegram: { enabled: boolean; configured: boolean; inboundEnabled: boolean; manualPaperExecutionEnabled: boolean };
}

export interface TradingCandidateResponse {
  market: string;
  decision: CandidateDecision;
  reason: string;
  currentPrice: string | null;
  priceChangeRate: string | null;
  highLowRangeRate: string | null;
  tradeAmountChangeRate: string | null;
  trend: MarketTrend | null;
  scannedAt: string;
}

export interface TradingFlowRunResponse {
  market: string;
  signalType: SignalType | null;
  signalReason: string;
  orderCreated: boolean;
  orderStatus: OrderStatus | null;
  message: string;
  executedAt: string;
}

export interface PortfolioStatusResponse {
  exchange: ExchangeMode;
  currency: string;
  cash: string;
  realizedProfit: string;
}

export interface PositionResponse {
  market: string;
  quantity: string;
  averageBuyPrice: string;
}

export interface PositionValuationResponse extends PositionResponse {
  currentPrice: string;
  positionValue: string;
  unrealizedProfit: string;
  unrealizedProfitRate: string;
}

export interface PortfolioValuationResponse {
  exchange: ExchangeMode;
  currency: string;
  cash: string;
  totalPositionValue: string;
  totalEquity: string;
  realizedProfit: string;
  unrealizedProfit: string;
  totalProfit: string;
  positions: PositionValuationResponse[];
}

export interface SelectedPaperSellRequest {
  markets: string[];
}

export interface SelectedPaperSellResultResponse {
  market: string;
  signalType: SignalType;
  orderCreated: boolean;
  orderStatus: OrderStatus;
  message: string;
  executedAt: string;
}

export interface SelectedPaperSellResponse {
  exchange: ExchangeMode;
  requestedCount: number;
  succeededCount: number;
  failedCount: number;
  results: SelectedPaperSellResultResponse[];
}

export interface TradingFlowHistoryResponse {
  id: string;
  exchange: ExchangeMode;
  market: string;
  currentPrice: string | null;
  signalType: SignalType | null;
  signalReason: string;
  orderCreated: boolean;
  orderStatus: OrderStatus | null;
  message: string;
  createdAt: string;
}

export interface ReasonCountResponse {
  reason: string;
  count: number;
}

export interface MarketCountResponse {
  market: string;
  count: number;
}

export interface AnalyticsSummaryResponse {
  range: AnalyticsRange;
  from: string;
  to: string;
  total: number;
  buyCount: number;
  sellCount: number;
  holdCount: number;
  filledCount: number;
  rejectedCount: number;
  failedCount: number;
  stopLossCount: number;
  takeProfitCount: number;
  averageStopLossRate: string;
  averageTakeProfitRate: string;
  topHoldReasons: ReasonCountResponse[];
  topMarkets: MarketCountResponse[];
}

export interface AnalyticsPnlResponse {
  range: AnalyticsRange;
  from: string;
  to: string;
  cash: string;
  totalPositionValue: string;
  totalEquity: string;
  realizedProfit: string;
  unrealizedProfit: string;
  totalProfit: string;
  positionCount: number;
}

export interface LossTradeResponse {
  market: string;
  currentPrice: string | null;
  rate: string;
  reason: string;
  createdAt: string;
}

export interface AnalyticsLossResponse {
  range: AnalyticsRange;
  worstTrades: LossTradeResponse[];
  repeatedStopLossMarkets: MarketCountResponse[];
}
