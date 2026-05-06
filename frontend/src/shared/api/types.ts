export type SignalType = 'BUY' | 'SELL' | 'HOLD';
export type OrderStatus = 'FILLED' | 'REJECTED' | 'FAILED';
export type CandidateDecision = 'SELECTED' | 'SKIPPED';
export type MarketTrend = 'UP' | 'DOWN' | 'SIDEWAYS';
export type AnalyticsRange = '1h' | '24h' | '3d' | '7d';
export type Mvp2Exchange = 'UPBIT' | 'BINANCE';
export type OrderSide = 'BUY' | 'SELL';

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
  cash: string;
  totalPositionValue: string;
  totalEquity: string;
  realizedProfit: string;
  unrealizedProfit: string;
  totalProfit: string;
  positions: PositionValuationResponse[];
}

export interface TradingFlowHistoryResponse {
  id: string;
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

export interface Mvp2ExchangeResponse {
  exchange: Mvp2Exchange;
  displayName: string;
  enabled: boolean;
  publicMarketDataOnly: boolean;
  statusPath: string;
}

export interface Mvp2ExchangeStatusResponse {
  exchange: Mvp2Exchange;
  displayName: string;
  enabled: boolean;
  publicMarketDataOnly: boolean;
  realTradingSupported: boolean;
  marketData: string;
  message: string;
}

export interface Mvp2PaperStatusResponse {
  schedulerEnabled: boolean;
  schedulerFixedDelayMs: number;
  symbols: string[];
  initialCash: string;
  orderAmount: string;
  takeProfitRate: string;
  stopLossRate: string;
}

export interface Mvp2PaperCandidateResponse {
  exchange: Mvp2Exchange;
  symbol: string;
  decision: CandidateDecision;
  reason: string;
  currentPrice: string | null;
  priceChangeRate: string | null;
  highLowRangeRate: string | null;
  tradeAmountChangeRate: string | null;
  trend: MarketTrend | null;
  scannedAt: string;
}

export interface Mvp2PaperPositionResponse {
  symbol: string;
  quantity: string;
  averageBuyPrice: string;
}

export interface Mvp2PaperPortfolioResponse {
  exchange: Mvp2Exchange;
  cash: string;
  realizedProfit: string;
  positions: Mvp2PaperPositionResponse[];
}

export interface Mvp2PaperTradeHistoryResponse {
  exchange: Mvp2Exchange;
  symbol: string;
  side: OrderSide | null;
  quantity: string | null;
  price: string | null;
  status: OrderStatus | null;
  reason: string;
  message: string;
  createdAt: string;
}
