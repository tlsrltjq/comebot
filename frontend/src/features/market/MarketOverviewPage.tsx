import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import type { BtcChangeRange } from '../../shared/api/types';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { cn } from '@/lib/utils';

const RANGES: BtcChangeRange[] = ['1h', '24h', '3d', '7d'];

export function MarketOverviewPage() {
  const { exchange } = useExchangeMode();
  const [range, setRange] = useState<BtcChangeRange>('24h');
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';

  const { data, error, isLoading, isFetching, dataUpdatedAt } = useQuery({
    queryKey: queryKeys.btcChange(range, exchange),
    queryFn: () => api.btcChange(range, exchange),
    refetchInterval: POLLING_INTERVALS.marketChart,
  });

  const chartData = (data?.points ?? []).map((p) => ({
    time: p.time,
    label: formatDateTime(p.time),
    price: Number(p.price),
    changeRate: Number(p.changeRate),
  }));
  const changeRate = Number(data?.changeRate ?? 0);
  const positive = changeRate >= 0;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">시장 차트</h1>
          <p className="page-subtitle">BTC 등락률 — {exchange === 'BINANCE' ? 'BTCUSDT' : 'KRW-BTC'}</p>
        </div>
        <div className="flex items-center gap-3">
          <span className={cn('badge', positive ? 'badge-success' : 'badge-destructive')}>
            {positive ? '▲' : '▼'} {formatNumber(changeRate, 4)}%
          </span>
          <div className="live-status">
            <span className={cn('status-dot', isFetching ? 'warn' : 'live')} />
            {dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''}
          </div>
        </div>
      </div>

      {/* Range selector */}
      <div className="mb-4 flex gap-1.5">
        {RANGES.map((r) => (
          <button
            key={r}
            type="button"
            onClick={() => setRange(r)}
            className={cn(
              'rounded-md px-3 py-1 text-sm font-medium border transition-colors',
              range === r
                ? 'bg-primary text-primary-foreground border-primary'
                : 'bg-background border-border text-muted-foreground hover:text-foreground hover:bg-muted',
            )}
          >
            {r}
          </button>
        ))}
      </div>

      {error && <div className="error-panel mb-4">BTC 차트 조회 실패</div>}

      {/* Metrics */}
      <div className="metric-grid mb-4">
        <div className="metric-card">
          <span>현재가</span>
          <strong className="num">{formatCurrency(data?.latestPrice, currency)}</strong>
          <small>{data?.market ?? (exchange === 'BINANCE' ? 'BTCUSDT' : 'KRW-BTC')}</small>
        </div>
        <div className="metric-card">
          <span>기간 등락률</span>
          <strong className={cn('num', positive ? 'pos' : 'neg')}>{formatNumber(changeRate, 4)}%</strong>
          <small>기간 {range}</small>
        </div>
        <div className="metric-card">
          <span>고가</span>
          <strong className="num">{formatCurrency(data?.highPrice, currency)}</strong>
          <small>시작가 {formatCurrency(data?.basePrice, currency)}</small>
        </div>
        <div className="metric-card">
          <span>저가</span>
          <strong className="num">{formatCurrency(data?.lowPrice, currency)}</strong>
          <small>{positive ? '상승 구간' : '하락 구간'}</small>
        </div>
      </div>

      {/* Chart */}
      <div className="section">
        <h2 className="section-title">등락률 차트</h2>
        {isLoading && <div className="empty-state"><strong>차트 불러오는 중...</strong></div>}
        {!isLoading && chartData.length < 2 && (
          <div className="empty-state"><strong>데이터 없음</strong><span>캔들 데이터가 부족합니다.</span></div>
        )}
        {chartData.length >= 2 && (
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(220 13% 91%)" />
                <XAxis dataKey="label" tickLine={false} axisLine={false} minTickGap={30} tick={{ fontSize: 11 }} />
                <YAxis tickLine={false} axisLine={false} width={52} unit="%" tick={{ fontSize: 11 }} />
                <Tooltip
                  contentStyle={{ fontSize: 12, borderRadius: 6 }}
                  formatter={(value, name) =>
                    name === 'changeRate'
                      ? [`${formatNumber(Number(value), 4)}%`, '등락률']
                      : [formatCurrency(value as number, currency), '가격']
                  }
                  labelFormatter={(_, payload) =>
                    payload?.[0]?.payload?.time ? formatDateTime(payload[0].payload.time) : ''
                  }
                />
                <Line
                  type="monotone"
                  dataKey="changeRate"
                  stroke={positive ? 'hsl(142 71% 45%)' : 'hsl(0 72% 51%)'}
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>
    </div>
  );
}
