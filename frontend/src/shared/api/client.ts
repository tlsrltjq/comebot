import type {
  AnalyticsLossResponse,
  AnalyticsPnlResponse,
  AnalyticsRange,
  AnalyticsSummaryResponse,
  Mvp2Exchange,
  Mvp2ExchangeResponse,
  Mvp2ExchangeStatusResponse,
  Mvp2PaperCandidateResponse,
  Mvp2PaperPortfolioResponse,
  Mvp2PaperStatusResponse,
  Mvp2PaperTradeHistoryResponse,
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
  systemStatus: () => request<SystemStatusResponse>('/api/system/status'),
  candidates: (market?: string) => request<TradingCandidateResponse[]>(`/api/candidates${query({ market })}`),
  portfolioStatus: () => request<PortfolioStatusResponse>('/api/portfolio/status'),
  positions: () => request<PositionResponse[]>('/api/portfolio/positions'),
  portfolioValuation: () => request<PortfolioValuationResponse>('/api/portfolio/valuation'),
  history: (market?: string, limit = 20) =>
    request<TradingFlowHistoryResponse[]>(`/api/trading-flow/history${query({ market, limit })}`),
  analyticsSummary: (range: AnalyticsRange) =>
    request<AnalyticsSummaryResponse>(`/api/analytics/summary${query({ range })}`),
  analyticsPnl: (range: AnalyticsRange) => request<AnalyticsPnlResponse>(`/api/analytics/pnl${query({ range })}`),
  analyticsLosses: (range: AnalyticsRange) =>
    request<AnalyticsLossResponse>(`/api/analytics/losses${query({ range })}`),
  mvp2Exchanges: () => request<Mvp2ExchangeResponse[]>('/api/mvp2/exchanges'),
  mvp2ExchangeStatus: (exchange: Mvp2Exchange) =>
    request<Mvp2ExchangeStatusResponse>(`/api/mvp2/exchanges/${exchange}/status`),
  mvp2BinancePaperStatus: () => request<Mvp2PaperStatusResponse>('/api/mvp2/binance/paper/status'),
  mvp2BinancePaperCandidates: () => request<Mvp2PaperCandidateResponse[]>('/api/mvp2/binance/paper/candidates'),
  mvp2BinancePaperPortfolio: () => request<Mvp2PaperPortfolioResponse>('/api/mvp2/binance/paper/portfolio'),
  mvp2BinancePaperHistory: (limit = 20) =>
    request<Mvp2PaperTradeHistoryResponse[]>(`/api/mvp2/binance/paper/history${query({ limit })}`),
};

export const queryKeys = {
  system: ['system'] as const,
  candidates: (market?: string) => ['candidates', market ?? 'all'] as const,
  portfolioStatus: ['portfolioStatus'] as const,
  positions: ['positions'] as const,
  portfolioValuation: ['portfolioValuation'] as const,
  history: (market?: string, limit = 20) => ['history', market ?? 'all', limit] as const,
  analyticsSummary: (range: AnalyticsRange) => ['analyticsSummary', range] as const,
  analyticsPnl: (range: AnalyticsRange) => ['analyticsPnl', range] as const,
  analyticsLosses: (range: AnalyticsRange) => ['analyticsLosses', range] as const,
  mvp2Exchanges: ['mvp2Exchanges'] as const,
  mvp2ExchangeStatus: (exchange: Mvp2Exchange) => ['mvp2ExchangeStatus', exchange] as const,
  mvp2BinancePaperStatus: ['mvp2BinancePaperStatus'] as const,
  mvp2BinancePaperCandidates: ['mvp2BinancePaperCandidates'] as const,
  mvp2BinancePaperPortfolio: ['mvp2BinancePaperPortfolio'] as const,
  mvp2BinancePaperHistory: (limit = 20) => ['mvp2BinancePaperHistory', limit] as const,
};
