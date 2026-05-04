import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Filter, RotateCcw } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import type { AnalyticsRange, OrderStatus, SignalType, TradingFlowHistoryResponse } from '../../shared/api/types';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatDateTime, formatKrw, formatNumber } from '../../shared/format';

type SignalFilter = 'ALL' | SignalType;
type OrderFilter = 'ALL' | OrderStatus | 'NO_ORDER';
type ReasonFilter = 'ALL' | 'TAKE_PROFIT' | 'STOP_LOSS' | 'HOLD';

const ranges: AnalyticsRange[] = ['1h', '24h', '3d', '7d'];
const signals: SignalFilter[] = ['ALL', 'BUY', 'SELL', 'HOLD'];
const orders: OrderFilter[] = ['ALL', 'FILLED', 'REJECTED', 'FAILED', 'NO_ORDER'];
const reasons: ReasonFilter[] = ['ALL', 'TAKE_PROFIT', 'STOP_LOSS', 'HOLD'];

export function HistoryPage() {
  const [market, setMarket] = useState('');
  const [limit, setLimit] = useState(100);
  const [range, setRange] = useState<AnalyticsRange>('24h');
  const [signalFilter, setSignalFilter] = useState<SignalFilter>('ALL');
  const [orderFilter, setOrderFilter] = useState<OrderFilter>('ALL');
  const [reasonFilter, setReasonFilter] = useState<ReasonFilter>('ALL');
  const normalizedMarket = market.trim().toUpperCase();
  const historyQuery = useQuery({
    queryKey: queryKeys.history(normalizedMarket || undefined, limit),
    queryFn: () => api.history(normalizedMarket || undefined, limit),
    refetchInterval: 3_000,
  });
  const summaryQuery = useQuery({
    queryKey: queryKeys.analyticsSummary(range),
    queryFn: () => api.analyticsSummary(range),
    refetchInterval: 5_000,
  });
  const lossesQuery = useQuery({
    queryKey: queryKeys.analyticsLosses(range),
    queryFn: () => api.analyticsLosses(range),
    refetchInterval: 5_000,
  });

  const rows = filterRows(historyQuery.data ?? [], signalFilter, orderFilter, reasonFilter);
  const summary = summaryQuery.data;
  const losses = lossesQuery.data;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>실행 이력(History)</h1>
          <p>자동 실행 결과를 HOLD, REJECTED, FILLED, FAILED 상태로 확인합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      <div className="segmented-row" aria-label="분석 범위(Analytics range)">
        {ranges.map((item) => (
          <button
            key={item}
            className={range === item ? 'button button-primary' : 'button button-secondary'}
            type="button"
            onClick={() => setRange(item)}
          >
            {rangeLabel(item)}
          </button>
        ))}
      </div>

      <div className="metric-grid">
        <MetricCard label="실행 수(Runs)" value={formatNumber(summary?.total)} detail={`체결(Filled) ${formatNumber(summary?.filledCount)}`} />
        <MetricCard label="BUY / SELL" value={`${formatNumber(summary?.buyCount)} / ${formatNumber(summary?.sellCount)}`} detail={`HOLD ${formatNumber(summary?.holdCount)}`} />
        <MetricCard label="익절/손절(TP/SL)" value={`${formatNumber(summary?.takeProfitCount)} / ${formatNumber(summary?.stopLossCount)}`} detail={`평균 SL ${formatNumber(summary?.averageStopLossRate, 3)}%`} />
        <MetricCard label="손실 SELL(Loss sells)" value={formatNumber(losses?.worstTrades.length)} detail={`반복 손절 market ${formatNumber(losses?.repeatedStopLossMarkets.length)}`} />
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>반복 HOLD 사유(Top Hold Reasons)</h2>
            <Badge tone="info">{rangeLabel(range)}</Badge>
          </div>
          <ReasonList items={summary?.topHoldReasons ?? []} empty="반복 HOLD 사유가 없습니다(No repeated HOLD reasons)." />
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>손실 원인(Loss Review)</h2>
            <Badge tone={losses?.worstTrades.length ? 'warn' : 'good'}>
              {losses?.worstTrades.length ? '점검 필요(Review)' : '손실 없음(No losses)'}
            </Badge>
          </div>
          {losses?.worstTrades.length ? (
            <div className="loss-list">
              {losses.worstTrades.slice(0, 5).map((trade) => (
                <div key={`${trade.market}-${trade.createdAt}`} className="loss-item">
                  <strong>{trade.market}</strong>
                  <span className="tone-negative">{formatNumber(trade.rate, 3)}%</span>
                  <small>{formatDateTime(trade.createdAt)} / {trade.reason}</small>
                </div>
              ))}
            </div>
          ) : (
            <p>선택한 범위에 손절 SELL 기록이 없습니다(No stop-loss sells in range).</p>
          )}
        </article>
      </div>

      <article className="panel">
        <div className="panel-title-row">
          <h2>반복 손절 Market(Repeated Stop-loss Markets)</h2>
          <Badge tone={losses?.repeatedStopLossMarkets.length ? 'warn' : 'neutral'}>
            {formatNumber(losses?.repeatedStopLossMarkets.length)} markets
          </Badge>
        </div>
        <MarketCountList items={losses?.repeatedStopLossMarkets ?? []} />
      </article>

      <div className="toolbar history-toolbar">
        <label>
          마켓(Market)
          <input value={market} onChange={(event) => setMarket(event.target.value)} placeholder="전체 또는 KRW-BTC" />
        </label>
        <label>
          개수(Limit)
          <input
            type="number"
            min="1"
            max="100"
            value={limit}
            onChange={(event) => setLimit(Math.max(1, Number(event.target.value) || 1))}
          />
        </label>
        <button
          className="button button-secondary"
          type="button"
          onClick={() => {
            setMarket('');
            setLimit(100);
            setSignalFilter('ALL');
            setOrderFilter('ALL');
            setReasonFilter('ALL');
          }}
        >
          <RotateCcw size={16} />
          초기화(Reset)
        </button>
      </div>

      <div className="filter-panel" aria-label="실행 이력 필터(History filters)">
        <Filter size={18} />
        <div className="filter-stack">
          <FilterGroup label="신호(Signal)" values={signals} value={signalFilter} onChange={setSignalFilter} labelOf={signalLabel} />
          <FilterGroup label="주문(Order)" values={orders} value={orderFilter} onChange={setOrderFilter} labelOf={orderLabel} />
          <FilterGroup label="사유(Reason)" values={reasons} value={reasonFilter} onChange={setReasonFilter} labelOf={reasonLabel} />
        </div>
      </div>

      {historyQuery.error ? <ErrorPanel error={historyQuery.error} /> : null}
      {summaryQuery.error ? <ErrorPanel title="집계 조회 실패(Analytics summary failed)" error={summaryQuery.error} /> : null}
      {lossesQuery.error ? <ErrorPanel title="손실 조회 실패(Loss analytics failed)" error={lossesQuery.error} /> : null}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>시각(Created)</th>
              <th>마켓(Market)</th>
              <th>가격(Price)</th>
              <th>신호(Signal)</th>
              <th>주문(Order)</th>
              <th>이유(Reason)</th>
              <th>메시지(Message)</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{formatDateTime(row.createdAt)}</td>
                <td><strong>{row.market}</strong></td>
                <td>{formatKrw(row.currentPrice)}</td>
                <td><SignalBadge signal={row.signalType} /></td>
                <td>
                  <Badge tone={orderTone(row.orderStatus)}>
                    {row.orderStatus ?? (row.orderCreated ? 'CREATED' : 'NO_ORDER')}
                  </Badge>
                </td>
                <td><ReasonBadge row={row} /></td>
                <td>{row.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!historyQuery.isLoading && rows.length === 0 ? <EmptyState title="실행 이력 없음(No history)" /> : null}
      </div>
    </section>
  );
}

function filterRows(
  rows: TradingFlowHistoryResponse[],
  signalFilter: SignalFilter,
  orderFilter: OrderFilter,
  reasonFilter: ReasonFilter,
) {
  return rows.filter((row) => {
    if (signalFilter !== 'ALL' && row.signalType !== signalFilter) {
      return false;
    }
    if (orderFilter === 'NO_ORDER' && row.orderStatus !== null) {
      return false;
    }
    if (orderFilter !== 'ALL' && orderFilter !== 'NO_ORDER' && row.orderStatus !== orderFilter) {
      return false;
    }
    if (reasonFilter === 'TAKE_PROFIT' && !containsReason(row, 'Take profit')) {
      return false;
    }
    if (reasonFilter === 'STOP_LOSS' && !containsReason(row, 'Stop loss')) {
      return false;
    }
    if (reasonFilter === 'HOLD' && row.signalType !== 'HOLD') {
      return false;
    }
    return true;
  });
}

function containsReason(row: TradingFlowHistoryResponse, text: string) {
  return row.signalReason.includes(text) || row.message.includes(text);
}

function orderTone(orderStatus: OrderStatus | null) {
  if (orderStatus === 'FILLED') {
    return 'good';
  }
  if (orderStatus === 'REJECTED') {
    return 'warn';
  }
  if (orderStatus === 'FAILED') {
    return 'bad';
  }
  return 'neutral';
}

function signalTone(signal: SignalType | null) {
  if (signal === 'BUY') {
    return 'good';
  }
  if (signal === 'SELL') {
    return 'bad';
  }
  if (signal === 'HOLD') {
    return 'neutral';
  }
  return 'info';
}

function SignalBadge({ signal }: { signal: SignalType | null }) {
  return <Badge tone={signalTone(signal)}>{signal ?? '-'}</Badge>;
}

function ReasonBadge({ row }: { row: TradingFlowHistoryResponse }) {
  if (containsReason(row, 'Take profit')) {
    return <Badge tone="good">익절(Take profit)</Badge>;
  }
  if (containsReason(row, 'Stop loss')) {
    return <Badge tone="bad">손절(Stop loss)</Badge>;
  }
  if (row.signalType === 'HOLD') {
    return <span>{row.signalReason}</span>;
  }
  return <span>{row.signalReason}</span>;
}

function FilterGroup<T extends string>({
  label,
  values,
  value,
  onChange,
  labelOf,
}: {
  label: string;
  values: T[];
  value: T;
  onChange: (value: T) => void;
  labelOf: (value: T) => string;
}) {
  return (
    <div className="filter-group">
      <span>{label}</span>
      <div className="segmented-row">
        {values.map((item) => (
          <button
            key={item}
            className={value === item ? 'button button-primary' : 'button button-secondary'}
            type="button"
            onClick={() => onChange(item)}
          >
            {labelOf(item)}
          </button>
        ))}
      </div>
    </div>
  );
}

function ReasonList({ items, empty }: { items: { reason: string; count: number }[]; empty: string }) {
  if (items.length === 0) {
    return <p>{empty}</p>;
  }
  return (
    <div className="reason-list">
      {items.slice(0, 5).map((item) => (
        <div key={item.reason} className="reason-item">
          <span>{item.reason}</span>
          <strong>{formatNumber(item.count)}</strong>
        </div>
      ))}
    </div>
  );
}

function MarketCountList({ items }: { items: { market: string; count: number }[] }) {
  if (items.length === 0) {
    return <p>반복 손절 market이 없습니다(No repeated stop-loss market).</p>;
  }
  return (
    <div className="market-chip-list">
      {items.map((item) => (
        <span key={item.market} className="market-chip">
          {item.market} <strong>{formatNumber(item.count)}</strong>
        </span>
      ))}
    </div>
  );
}

function rangeLabel(range: AnalyticsRange) {
  const labels: Record<AnalyticsRange, string> = {
    '1h': '1시간(1h)',
    '24h': '24시간(24h)',
    '3d': '3일(3d)',
    '7d': '7일(7d)',
  };
  return labels[range];
}

function signalLabel(value: SignalFilter) {
  return value === 'ALL' ? '전체(All)' : value;
}

function orderLabel(value: OrderFilter) {
  return value === 'ALL' ? '전체(All)' : value === 'NO_ORDER' ? '주문 없음(No order)' : value;
}

function reasonLabel(value: ReasonFilter) {
  const labels: Record<ReasonFilter, string> = {
    ALL: '전체(All)',
    TAKE_PROFIT: '익절(TP)',
    STOP_LOSS: '손절(SL)',
    HOLD: 'HOLD',
  };
  return labels[value];
}
