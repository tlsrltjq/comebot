export type SignalType = 'BUY' | 'SELL' | 'HOLD';
export type OrderStatus = 'FILLED' | 'REJECTED' | 'FAILED';
export type CandidateDecision = 'SELECTED' | 'SKIPPED';
export type MarketTrend = 'UP' | 'DOWN' | 'SIDEWAYS';

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
