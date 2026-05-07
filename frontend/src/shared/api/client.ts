import type {
  AnalyticsLossResponse,
  AnalyticsPnlResponse,
  AnalyticsRange,
  AnalyticsSummaryResponse,
  ExchangeMode,
  PortfolioStatusResponse,
  PortfolioValuationResponse,
  PositionResponse,
  SystemStatusResponse,
  TradingCandidateResponse,
  TradingFlowHistoryResponse,
} from './types';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: { Accept: 'application/json', ...init?.headers },
    ...init,
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

const exchangeParam = (exchange: ExchangeMode) => exchange.toLowerCase();

const query = (params: Record<string, string | number | undefined>) => {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      searchParams.set(key, String(value));
    }
  });
  const value = searchParams.toString();
  return value ? `?${value}` : '';
};

export const api = {
  systemStatus: (exchange: ExchangeMode = 'UPBIT') =>
    request<SystemStatusResponse>(`/api/system/status${query({ exchange: exchangeParam(exchange) })}`),
  candidates: (exchange: ExchangeMode = 'UPBIT', market?: string) =>
    request<TradingCandidateResponse[]>(`/api/candidates${query({ exchange: exchangeParam(exchange), market })}`),
  portfolioStatus: (exchange: ExchangeMode = 'UPBIT') =>
    request<PortfolioStatusResponse>(`/api/portfolio/status${query({ exchange: exchangeParam(exchange) })}`),
  positions: (exchange: ExchangeMode = 'UPBIT') =>
    request<PositionResponse[]>(`/api/portfolio/positions${query({ exchange: exchangeParam(exchange) })}`),
  portfolioValuation: (exchange: ExchangeMode = 'UPBIT') =>
    request<PortfolioValuationResponse>(`/api/portfolio/valuation${query({ exchange: exchangeParam(exchange) })}`),
  history: (exchange: ExchangeMode = 'UPBIT', market?: string, limit = 20) =>
    request<TradingFlowHistoryResponse[]>(`/api/trading-flow/history${query({ exchange: exchangeParam(exchange), market, limit })}`),
  analyticsSummary: (range: AnalyticsRange, exchange: ExchangeMode = 'UPBIT') =>
    request<AnalyticsSummaryResponse>(`/api/analytics/summary${query({ exchange: exchangeParam(exchange), range })}`),
  analyticsPnl: (range: AnalyticsRange, exchange: ExchangeMode = 'UPBIT') =>
    request<AnalyticsPnlResponse>(`/api/analytics/pnl${query({ exchange: exchangeParam(exchange), range })}`),
  analyticsLosses: (range: AnalyticsRange, exchange: ExchangeMode = 'UPBIT') =>
    request<AnalyticsLossResponse>(`/api/analytics/losses${query({ exchange: exchangeParam(exchange), range })}`),
};

export const queryKeys = {
  system: (exchange: ExchangeMode = 'UPBIT') => ['system', exchange] as const,
  candidates: (exchange: ExchangeMode = 'UPBIT', market?: string) => ['candidates', exchange, market ?? 'all'] as const,
  portfolioStatus: (exchange: ExchangeMode = 'UPBIT') => ['portfolioStatus', exchange] as const,
  positions: (exchange: ExchangeMode = 'UPBIT') => ['positions', exchange] as const,
  portfolioValuation: (exchange: ExchangeMode = 'UPBIT') => ['portfolioValuation', exchange] as const,
  history: (exchange: ExchangeMode = 'UPBIT', market?: string, limit = 20) => ['history', exchange, market ?? 'all', limit] as const,
  analyticsSummary: (range: AnalyticsRange, exchange: ExchangeMode = 'UPBIT') => ['analyticsSummary', exchange, range] as const,
  analyticsPnl: (range: AnalyticsRange, exchange: ExchangeMode = 'UPBIT') => ['analyticsPnl', exchange, range] as const,
  analyticsLosses: (range: AnalyticsRange, exchange: ExchangeMode = 'UPBIT') => ['analyticsLosses', exchange, range] as const,
};
