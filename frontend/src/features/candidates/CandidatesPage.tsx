import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CircleSlash, Filter, ListFilter, Search } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import type { TradingCandidateResponse } from '../../shared/api/types';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { cn } from '@/lib/utils';

export function CandidatesPage() {
  const [marketInput, setMarketInput] = useState('');
  const [market, setMarket] = useState('');
  const [limit, setLimit] = useState<20 | 50>(20);
  const [selectedOnly, setSelectedOnly] = useState(false);
  const { exchange } = useExchangeMode();
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const normalizedMarket = market.trim().toUpperCase();

  const candidatesQuery = useQuery({
    queryKey: queryKeys.candidates(exchange, normalizedMarket || undefined, limit),
    queryFn: () => api.candidates(exchange, normalizedMarket || undefined, limit),
    refetchInterval: POLLING_INTERVALS.candidates,
  });
  const positionsQuery = useQuery({
    queryKey: queryKeys.positions(exchange),
    queryFn: () => api.positions(exchange),
    refetchInterval: POLLING_INTERVALS.candidates,
  });

  const candidates = useMemo(() => candidatesQuery.data ?? [], [candidatesQuery.data]);
  const visible = useMemo(() =>
    selectedOnly ? candidates.filter((c) => c.decision === 'SELECTED') : candidates,
    [candidates, selectedOnly],
  );
  const positionMarkets = useMemo(() => new Set((positionsQuery.data ?? []).map((p) => p.market)), [positionsQuery.data]);
  const summary = useMemo(() => summarize(candidates, positionMarkets), [candidates, positionMarkets]);
  const skippedReasons = useMemo(() => topSkipped(candidates), [candidates]);
  const riskSummary = useMemo(() => riskCount(candidates), [candidates]);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">매수 후보</h1>
          <p className="page-subtitle">자동 스케줄러가 판단한 롱 후보 — 수동 BUY 없음</p>
        </div>
        <div className="live-status">
          <span className={cn('status-dot', candidatesQuery.isFetching ? 'warn' : 'live')} />
          {candidatesQuery.dataUpdatedAt ? new Date(candidatesQuery.dataUpdatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''}
        </div>
      </div>

      {/* Summary metrics */}
      <div className="metric-grid mb-4">
        {[
          { label: '총 후보', val: formatNumber(summary.total), sub: `${normalizedMarket || `상위 ${limit}`}` },
          { label: '선택됨', val: formatNumber(summary.selected), sub: `${formatNumber(summary.selectedRate, 1)}%` },
          { label: '제외됨', val: formatNumber(summary.skipped), sub: `${formatNumber(summary.skippedRate, 1)}%` },
          { label: '보유 마켓', val: formatNumber(summary.held), sub: '후보 마켓 기준' },
          { label: '리스크 경고', val: formatNumber(riskSummary.total), sub: `쏠림 ${riskSummary.concentration} / cooldown ${riskSummary.cooldown}` },
        ].map(({ label, val, sub }) => (
          <div key={label} className="metric-card">
            <span>{label}</span>
            <strong>{val}</strong>
            <small>{sub}</small>
          </div>
        ))}
      </div>

      {/* Summary + Skipped reasons */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 mb-4">
        <div className="section">
          <div className="flex items-center justify-between mb-3">
            <h2 className="section-title mb-0">후보 요약</h2>
            <ListFilter size={16} className="text-muted-foreground" />
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="badge badge-success">SELECTED {summary.selected}</span>
            <span className="badge badge-outline">SKIPPED {summary.skipped}</span>
            <span className={cn('badge', summary.held > 0 ? 'badge-default' : 'badge-outline')}>HELD {summary.held}</span>
            <span className={cn('badge', riskSummary.total > 0 ? 'badge-warning' : 'badge-outline')}>RISK {riskSummary.total}</span>
          </div>
        </div>
        <div className="section">
          <div className="flex items-center justify-between mb-3">
            <h2 className="section-title mb-0">제외 사유 TOP 5</h2>
            <CircleSlash size={16} className="text-muted-foreground" />
          </div>
          {skippedReasons.length === 0
            ? <p className="text-sm text-muted-foreground">제외 사유 없음</p>
            : (
              <div className="space-y-1.5">
                {skippedReasons.map(({ reason, count }) => (
                  <div key={reason} className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground truncate">{reason}</span>
                    <span className="badge badge-outline ml-2 shrink-0">{count}</span>
                  </div>
                ))}
              </div>
            )}
        </div>
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 mb-4">
        <form className="flex items-center gap-2" onSubmit={(e) => { e.preventDefault(); setMarket(marketInput.trim().toUpperCase()); }}>
          <input
            value={marketInput}
            onChange={(e) => setMarketInput(e.target.value)}
            placeholder={exchange === 'BINANCE' ? 'BTCUSDT...' : 'KRW-BTC...'}
            className="rounded-md border border-border bg-background px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-ring w-32"
          />
          <button type="submit" className="flex items-center gap-1.5 rounded-md border border-border bg-background px-3 py-1.5 text-sm hover:bg-muted transition-colors">
            <Search size={14} />조회
          </button>
          {normalizedMarket && (
            <button type="button" onClick={() => { setMarket(''); setMarketInput(''); }} className="rounded-md border border-border bg-background px-3 py-1.5 text-sm hover:bg-muted transition-colors">
              전체
            </button>
          )}
        </form>
        <div className="flex items-center gap-1">
          {([20, 50] as const).map((n) => (
            <button key={n} type="button" onClick={() => setLimit(n)}
              className={cn('rounded-md border px-3 py-1.5 text-sm transition-colors', limit === n ? 'bg-primary text-primary-foreground border-primary' : 'border-border bg-background hover:bg-muted')}>{n}</button>
          ))}
        </div>
        <button type="button" onClick={() => setSelectedOnly((p) => !p)}
          className={cn('flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition-colors', selectedOnly ? 'bg-primary text-primary-foreground border-primary' : 'border-border bg-background hover:bg-muted')}>
          <Filter size={14} />선택만
        </button>
      </div>

      {candidatesQuery.error && <div className="error-panel mb-3" role="alert">후보 조회 실패: {String(candidatesQuery.error)}</div>}
      {positionsQuery.error && <div className="error-panel mb-3" role="alert">포지션 조회 실패</div>}

      {/* Table */}
      <div className="section overflow-x-auto">
        <table className="data-table w-full">
          <thead>
            <tr>
              <th>마켓</th><th>판단</th><th>추세</th><th>가격</th>
              <th>변화율</th><th>범위</th><th>거래대금</th>
              <th>보유</th><th>리스크</th><th>사유</th><th>스캔 시각</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((c) => (
              <tr key={`${c.market}-${c.scannedAt}`}>
                <td className="font-semibold">{c.market}</td>
                <td>
                  <span className={cn('badge', c.decision === 'SELECTED' ? 'badge-success' : 'badge-outline')}>
                    {c.decision}
                  </span>
                </td>
                <td className="text-muted-foreground">{c.trend ?? '-'}</td>
                <td className="num">{formatCurrency(c.currentPrice, currency)}</td>
                <td className={cn('num', Number(c.priceChangeRate) >= 0 ? 'pos' : 'neg')}>{formatNumber(c.priceChangeRate, 2)}%</td>
                <td className="num">{formatNumber(c.highLowRangeRate, 2)}%</td>
                <td className="num">{formatNumber(c.tradeAmountChangeRate, 2)}%</td>
                <td>
                  <span className={cn('badge', positionMarkets.has(c.market) ? 'badge-default' : 'badge-outline')}>
                    {positionMarkets.has(c.market) ? '보유' : '-'}
                  </span>
                </td>
                <td>{c.riskReasonType && c.riskReasonType !== 'NONE'
                  ? <span className="badge badge-warning">{c.riskReasonType}</span>
                  : <span className="text-muted-foreground text-xs">-</span>}</td>
                <td className="text-muted-foreground text-xs max-w-[200px] truncate">{c.reason}</td>
                <td className="text-muted-foreground text-xs whitespace-nowrap">{formatDateTime(c.scannedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!candidatesQuery.isLoading && visible.length === 0 && (
          <div className="empty-state"><strong>후보 없음</strong></div>
        )}
      </div>
    </div>
  );
}

function summarize(candidates: TradingCandidateResponse[], held: Set<string>) {
  const selected = candidates.filter((c) => c.decision === 'SELECTED').length;
  const skipped = candidates.length - selected;
  const heldCount = candidates.filter((c) => held.has(c.market)).length;
  return {
    total: candidates.length, selected, skipped,
    held: heldCount,
    selectedRate: candidates.length > 0 ? selected / candidates.length * 100 : 0,
    skippedRate: candidates.length > 0 ? skipped / candidates.length * 100 : 0,
  };
}

function topSkipped(candidates: TradingCandidateResponse[]) {
  const counts = new Map<string, number>();
  candidates.filter((c) => c.decision === 'SKIPPED').forEach((c) => counts.set(c.reason, (counts.get(c.reason) ?? 0) + 1));
  return [...counts.entries()].map(([reason, count]) => ({ reason, count })).sort((a, b) => b.count - a.count).slice(0, 5);
}

function riskCount(candidates: TradingCandidateResponse[]) {
  return candidates.reduce((acc, c) => {
    if (c.riskReasonType === 'CONCENTRATION') return { ...acc, total: acc.total + 1, concentration: acc.concentration + 1 };
    if (c.riskReasonType === 'STOP_LOSS_COOLDOWN') return { ...acc, total: acc.total + 1, cooldown: acc.cooldown + 1 };
    return acc;
  }, { total: 0, concentration: 0, cooldown: 0 });
}
