import type {
  AnalyticsLossResponse,
  AnalyticsPnlResponse,
  AnalyticsRange,
  AnalyticsSummaryResponse,
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
};
