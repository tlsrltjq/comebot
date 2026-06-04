import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Filter, RotateCcw, Search } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import type { AnalyticsRange, OrderStatus, SignalType, TradingFlowHistoryResponse } from '../../shared/api/types';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { cn } from '@/lib/utils';

type SignalFilter = 'ALL' | SignalType;
type OrderFilter = 'ALL' | OrderStatus | 'NO_ORDER';
type ReasonFilter = 'ALL' | 'TAKE_PROFIT' | 'STOP_LOSS' | 'HOLD';
type HistoryLimit = 20 | 50 | 100 | 200;

const RANGES: AnalyticsRange[] = ['1h', '24h', '3d', '7d'];
const SIGNAL_FILTERS: SignalFilter[] = ['ALL', 'BUY', 'SELL', 'HOLD'];
const ORDER_FILTERS: OrderFilter[] = ['ALL', 'FILLED', 'REJECTED', 'FAILED', 'NO_ORDER'];
const REASON_FILTERS: ReasonFilter[] = ['ALL', 'TAKE_PROFIT', 'STOP_LOSS', 'HOLD'];
const LIMITS: HistoryLimit[] = [20, 50, 100, 200];

export function HistoryPage() {
  const [marketInput, setMarketInput] = useState('');
  const [market, setMarket] = useState('');
  const [limit, setLimit] = useState<HistoryLimit>(50);
  const [range, setRange] = useState<AnalyticsRange>('24h');
  const [signalFilter, setSignalFilter] = useState<SignalFilter>('ALL');
  const [orderFilter, setOrderFilter] = useState<OrderFilter>('ALL');
  const [reasonFilter, setReasonFilter] = useState<ReasonFilter>('ALL');
  const { exchange } = useExchangeMode();
  const normalizedMarket = market.trim().toUpperCase();

  const historyQuery = useQuery({
    queryKey: queryKeys.history(exchange, normalizedMarket || undefined, limit),
    queryFn: () => api.history(exchange, normalizedMarket || undefined, limit),
    refetchInterval: POLLING_INTERVALS.history,
  });
  const summaryQuery = useQuery({
    queryKey: queryKeys.analyticsSummary(range, exchange),
    queryFn: () => api.analyticsSummary(range, exchange),
    refetchInterval: POLLING_INTERVALS.analytics,
  });
  const lossesQuery = useQuery({
    queryKey: queryKeys.analyticsLosses(range, exchange),
    queryFn: () => api.analyticsLosses(range, exchange),
    refetchInterval: POLLING_INTERVALS.analytics,
  });

  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const money = (v: string | number | null | undefined) => formatCurrency(v, currency);
  const summary = summaryQuery.data;
  const losses = lossesQuery.data;
  const rows = filterRows(historyQuery.data ?? [], signalFilter, orderFilter, reasonFilter);

  const reset = () => {
    setMarket(''); setMarketInput(''); setLimit(50);
    setSignalFilter('ALL'); setOrderFilter('ALL'); setReasonFilter('ALL');
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">거래 이력</h1>
          <p className="page-subtitle">자동 실행 결과 — HOLD · REJECTED · FILLED · FAILED</p>
        </div>
        <div className="live-status">
          <span className={cn('status-dot', (historyQuery.isFetching || summaryQuery.isFetching) ? 'warn' : 'live')} />
          {historyQuery.dataUpdatedAt
            ? new Date(historyQuery.dataUpdatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
            : ''}
        </div>
      </div>

      {/* Range */}
      <div className="mb-4 flex gap-1.5">
        {RANGES.map((r) => (
          <button key={r} type="button" onClick={() => setRange(r)}
            className={cn('rounded-md border px-3 py-1.5 text-sm font-medium transition-colors',
              range === r ? 'bg-primary text-primary-foreground border-primary' : 'border-border bg-background text-muted-foreground hover:bg-muted')}>
            {r}
          </button>
        ))}
      </div>

      {/* Metrics */}
      <div className="metric-grid mb-4">
        {[
          { label: '실행 수', val: formatNumber(summary?.total), sub: `체결 ${formatNumber(summary?.filledCount)}` },
          { label: 'BUY / SELL', val: `${formatNumber(summary?.buyCount)} / ${formatNumber(summary?.sellCount)}`, sub: `HOLD ${formatNumber(summary?.holdCount)}` },
          { label: '거절 / 실패', val: `${formatNumber(summary?.rejectedCount)} / ${formatNumber(summary?.failedCount)}`, sub: '' },
          { label: '익절 / 손절', val: `${formatNumber(summary?.takeProfitCount)} / ${formatNumber(summary?.stopLossCount)}`, sub: `평균 SL ${formatNumber(summary?.averageStopLossRate, 3)}%` },
          { label: '승률', val: `${formatNumber(summary?.winRate, 2)}%`, sub: `평균 보유 ${formatDuration(summary?.averageHoldingSeconds)}` },
          { label: '손익비', val: formatNumber(summary?.profitLossRatio, 2), sub: `평균 TP ${formatNumber(summary?.averageTakeProfitRate, 3)}%` },
          { label: '손실 매도', val: formatNumber(losses?.worstTrades.length), sub: `반복 손절 ${formatNumber(losses?.repeatedStopLossMarkets.length)}개` },
        ].map(({ label, val, sub }) => (
          <div key={label} className="metric-card">
            <span>{label}</span><strong className="num">{val}</strong><small>{sub}</small>
          </div>
        ))}
      </div>

      {/* Analytics panels */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3 mb-4">
        <div className="section">
          <h2 className="section-title">주문 상태 분포</h2>
          <div className="space-y-2">
            {[
              { label: 'FILLED', v: summary?.filledCount ?? 0, cls: 'badge-success' },
              { label: 'REJECTED', v: summary?.rejectedCount ?? 0, cls: 'badge-warning' },
              { label: 'FAILED', v: summary?.failedCount ?? 0, cls: 'badge-destructive' },
              { label: 'NO ORDER', v: summary?.holdCount ?? 0, cls: 'badge-outline' },
            ].map(({ label, v, cls }) => (
              <div key={label} className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">{label}</span>
                <span className={`badge ${cls}`}>{formatNumber(v)}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="section">
          <h2 className="section-title">반복 HOLD 사유 TOP 5</h2>
          {(summary?.topHoldReasons ?? []).length === 0
            ? <p className="text-sm text-muted-foreground">없음</p>
            : (summary?.topHoldReasons ?? []).slice(0, 5).map((item) => (
              <div key={item.reason} className="flex items-center justify-between text-sm py-0.5">
                <span className="text-muted-foreground truncate">{item.reason}</span>
                <strong className="ml-2 shrink-0">{formatNumber(item.count)}</strong>
              </div>
            ))}
        </div>
        <div className="section">
          <div className="flex items-center justify-between mb-2">
            <h2 className="section-title mb-0">손실 원인</h2>
            <span className={cn('badge', (losses?.worstTrades.length ?? 0) > 0 ? 'badge-warning' : 'badge-success')}>
              {(losses?.worstTrades.length ?? 0) > 0 ? '점검 필요' : '없음'}
            </span>
          </div>
          {(losses?.worstTrades ?? []).length === 0
            ? <p className="text-sm text-muted-foreground">손절 기록 없음</p>
            : losses!.worstTrades.slice(0, 5).map((t) => (
              <div key={`${t.market}-${t.createdAt}`} className="flex items-center justify-between text-sm py-0.5">
                <span className="font-medium">{t.market}</span>
                <span className="neg num text-xs">{formatNumber(t.rate, 3)}%</span>
              </div>
            ))}
        </div>
      </div>

      {(losses?.repeatedStopLossMarkets ?? []).length > 0 && (
        <div className="section mb-4">
          <div className="flex items-center justify-between mb-2">
            <h2 className="section-title mb-0">반복 손절 마켓</h2>
            <span className="badge badge-warning">{losses!.repeatedStopLossMarkets.length}개</span>
          </div>
          <div className="flex flex-wrap gap-2">
            {losses!.repeatedStopLossMarkets.map((item) => (
              <span key={item.market} className="rounded-md bg-red-50 border border-red-200 px-2 py-1 text-xs font-medium text-red-700">
                {item.market} <strong>{item.count}</strong>
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3 mb-3">
        <form className="flex items-center gap-2" onSubmit={(e) => { e.preventDefault(); setMarket(marketInput.trim().toUpperCase()); }}>
          <input value={marketInput} onChange={(e) => setMarketInput(e.target.value)}
            placeholder={exchange === 'BINANCE' ? 'BTCUSDT...' : 'KRW-BTC...'}
            className="rounded-md border border-border bg-background px-3 py-1.5 text-sm w-32 focus:outline-none focus:ring-2 focus:ring-ring" />
          <button type="submit" className="flex items-center gap-1.5 rounded-md border border-border bg-background px-3 py-1.5 text-sm hover:bg-muted transition-colors">
            <Search size={13} />조회</button>
          {normalizedMarket && (
            <button type="button" onClick={() => { setMarket(''); setMarketInput(''); }}
              className="rounded-md border border-border bg-background px-3 py-1.5 text-sm hover:bg-muted transition-colors">전체</button>
          )}
        </form>
        <div className="flex gap-1">
          {LIMITS.map((n) => (
            <button key={n} type="button" onClick={() => setLimit(n)}
              className={cn('rounded-md border px-2.5 py-1.5 text-xs transition-colors',
                limit === n ? 'bg-primary text-primary-foreground border-primary' : 'border-border bg-background hover:bg-muted')}>
              {n}
            </button>
          ))}
        </div>
        <button type="button" onClick={reset}
          className="flex items-center gap-1.5 rounded-md border border-border bg-background px-3 py-1.5 text-sm hover:bg-muted transition-colors">
          <RotateCcw size={13} />초기화</button>
      </div>

      {/* Filters */}
      <div className="mb-3 flex flex-wrap items-start gap-4 rounded-md border border-border bg-muted/40 px-3 py-2">
        <Filter size={14} className="mt-1 text-muted-foreground shrink-0" />
        <FG label="신호" values={SIGNAL_FILTERS} value={signalFilter} onChange={setSignalFilter}
          labelOf={(v) => v === 'ALL' ? '전체' : v} />
        <FG label="주문" values={ORDER_FILTERS} value={orderFilter} onChange={setOrderFilter}
          labelOf={(v) => v === 'ALL' ? '전체' : v === 'NO_ORDER' ? '없음' : v} />
        <FG label="사유" values={REASON_FILTERS} value={reasonFilter} onChange={setReasonFilter}
          labelOf={(v) => ({ ALL: '전체', TAKE_PROFIT: 'TP', STOP_LOSS: 'SL', HOLD: 'HOLD' }[v])} />
      </div>

      {historyQuery.error && <ErrorPanel error={historyQuery.error} />}

      {/* Table */}
      <div className="section overflow-x-auto">
        <table className="data-table">
          <thead>
            <tr><th>시각</th><th>마켓</th><th>가격</th><th>신호</th><th>주문</th><th>사유</th><th>메시지</th></tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td className="text-muted-foreground text-xs whitespace-nowrap">{formatDateTime(row.createdAt)}</td>
                <td className="font-semibold">{row.market}</td>
                <td className="num text-sm">{money(row.currentPrice)}</td>
                <td>
                  <span className={cn('badge', row.signalType === 'BUY' ? 'badge-success' : row.signalType === 'SELL' ? 'badge-destructive' : 'badge-outline')}>
                    {row.signalType ?? '-'}
                  </span>
                </td>
                <td>
                  <span className={cn('badge', row.orderStatus === 'FILLED' ? 'badge-success' : row.orderStatus === 'REJECTED' ? 'badge-warning' : row.orderStatus === 'FAILED' ? 'badge-destructive' : 'badge-outline')}>
                    {row.orderStatus ?? 'NO_ORDER'}
                  </span>
                </td>
                <td>
                  {containsReason(row, 'Take profit')
                    ? <span className="badge badge-success">익절</span>
                    : containsReason(row, 'Stop loss')
                      ? <span className="badge badge-destructive">손절</span>
                      : <span className="text-xs text-muted-foreground">{row.signalReason}</span>}
                </td>
                <td className="text-xs text-muted-foreground">{row.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!historyQuery.isLoading && rows.length === 0 && (
          <div className="empty-state"><strong>이력 없음</strong></div>
        )}
      </div>
    </div>
  );
}

function FG<T extends string>({ label, values, value, onChange, labelOf }: {
  label: string; values: T[]; value: T; onChange: (v: T) => void; labelOf: (v: T) => string;
}) {
  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      <span className="text-xs text-muted-foreground shrink-0">{label}</span>
      {values.map((v) => (
        <button key={v} type="button" onClick={() => onChange(v)}
          className={cn('rounded border px-2 py-0.5 text-xs transition-colors',
            value === v ? 'bg-primary text-primary-foreground border-primary' : 'border-border bg-background hover:bg-muted')}>
          {labelOf(v)}
        </button>
      ))}
    </div>
  );
}

function filterRows(rows: TradingFlowHistoryResponse[], sig: SignalFilter, ord: OrderFilter, rsn: ReasonFilter) {
  return rows.filter((row) => {
    if (sig !== 'ALL' && row.signalType !== sig) return false;
    if (ord === 'NO_ORDER' && row.orderStatus !== null) return false;
    if (ord !== 'ALL' && ord !== 'NO_ORDER' && row.orderStatus !== ord) return false;
    if (rsn === 'TAKE_PROFIT' && !containsReason(row, 'Take profit')) return false;
    if (rsn === 'STOP_LOSS' && !containsReason(row, 'Stop loss')) return false;
    if (rsn === 'HOLD' && row.signalType !== 'HOLD') return false;
    return true;
  });
}

function containsReason(row: TradingFlowHistoryResponse, text: string) {
  return row.signalReason.includes(text) || row.message.includes(text);
}

function formatDuration(seconds: number | null | undefined) {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}
