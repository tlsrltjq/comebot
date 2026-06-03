import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import type { BtcChangeRange } from '../../shared/api/types';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { MetricCard } from '../../shared/ui/MetricCard';

const ranges: BtcChangeRange[] = ['1h', '24h', '3d', '7d'];
export function MarketOverviewPage() {
  const { exchange } = useExchangeMode();
  const [range, setRange] = useState<BtcChangeRange>('24h');
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const { data, error, isLoading, isFetching, dataUpdatedAt } = useQuery({
    queryKey: queryKeys.btcChange(range, exchange),
    queryFn: () => api.btcChange(range, exchange),
    refetchInterval: POLLING_INTERVALS.marketChart,
  });
  const chartData = (data?.points ?? []).map((point) => ({
    time: point.time,
    label: formatDateTime(point.time),
    price: Number(point.price),
    changeRate: Number(point.changeRate),
  }));
  const changeRate = Number(data?.changeRate ?? 0);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>시장 개요(Market Overview)</h1>
          <p>선택 거래소 기준 비트코인 등락률을 조회 전용 그래프로 확인합니다.</p>
        </div>
        <Badge tone="info">{exchange}</Badge>
        <LiveStatus updatedAt={dataUpdatedAt} isFetching={isFetching} intervalMs={POLLING_INTERVALS.marketChart} />
      </header>

      <div className="segmented-row" aria-label="BTC 등락률 범위(BTC change range)">
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

      {error ? <ErrorPanel title="BTC 등락률 조회 실패(BTC chart failed)" error={error} /> : null}

      <div className="metric-grid">
        <MetricCard label="현재가(Current)" value={formatCurrency(data?.latestPrice, currency)} detail={data?.market ?? btcMarket(exchange)} />
        <MetricCard label="기간 등락률(Change)" value={`${formatNumber(data?.changeRate, 4)}%`} detail={`범위(Range) ${rangeLabel(range)}`} />
        <MetricCard label="고가(High)" value={formatCurrency(data?.highPrice, currency)} detail={`시작가(Base) ${formatCurrency(data?.basePrice, currency)}`} />
        <MetricCard label="저가(Low)" value={formatCurrency(data?.lowPrice, currency)} detail={changeRate >= 0 ? '상승 구간(Up)' : '하락 구간(Down)'} />
      </div>

      <article className="panel chart-panel">
        <div className="panel-title-row">
          <h2>{exchange === 'BINANCE' ? 'BTC 등락률(BTCUSDT Change)' : 'BTC 등락률(KRW-BTC Change)'}</h2>
          <Badge tone={changeRate >= 0 ? 'good' : 'bad'}>{`${formatNumber(data?.changeRate, 4)}%`}</Badge>
        </div>
        {isLoading ? <EmptyState title="차트 불러오는 중(Loading chart)" description="BTC candle 데이터를 조회하고 있습니다." /> : null}
        {!isLoading && chartData.length < 2 ? (
          <EmptyState title="차트 데이터 없음(No chart data)" description="표시할 candle 데이터가 충분하지 않습니다." />
        ) : null}
        {chartData.length >= 2 ? (
          <div className="chart-wrap market-chart-wrap">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#21262d" />
                <XAxis dataKey="label" tickLine={false} axisLine={false} minTickGap={26} tick={{ fill: '#8b949e', fontSize: 12 }} />
                <YAxis tickLine={false} axisLine={false} width={54} unit="%" tick={{ fill: '#8b949e', fontSize: 12 }} />
                <Tooltip
                  contentStyle={{ background: '#161b22', border: '1px solid #30363d', borderRadius: 5, color: '#c9d1d9', fontSize: 12 }}
                  formatter={(value, name) => (name === 'changeRate' ? [`${formatNumber(Number(value), 4)}%`, '등락률(Change)'] : [formatCurrency(value as number, currency), '가격(Price)'])}
                  labelFormatter={(_, payload) => (payload?.[0]?.payload?.time ? formatDateTime(payload[0].payload.time) : '')}
                />
                <Line type="monotone" dataKey="changeRate" stroke="#58a6ff" strokeWidth={2} dot={false} activeDot={{ r: 4, fill: '#58a6ff' }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : null}
      </article>
    </section>
  );
}

function rangeLabel(range: BtcChangeRange) {
  return range;
}

function btcMarket(exchange: string) {
  return exchange === 'BINANCE' ? 'BTCUSDT' : 'KRW-BTC';
}
