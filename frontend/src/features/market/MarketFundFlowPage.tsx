import { useQuery } from '@tanstack/react-query';
import { TrendingDown, TrendingUp, Minus } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { formatNumber } from '../../shared/format';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { MetricCard } from '../../shared/ui/MetricCard';
import type { MarketFlowEntry } from '../../shared/api/types';

function fmt24h(val: string | number) {
  const n = Number(val);
  if (n >= 1_000_000_000_000) return `${(n / 1_000_000_000_000).toFixed(1)}조`;
  if (n >= 100_000_000) return `${(n / 100_000_000).toFixed(0)}억`;
  if (n >= 10_000) return `${(n / 10_000).toFixed(0)}만`;
  return formatNumber(n);
}

function RankBadge({ change }: { change: number }) {
  if (change > 0)
    return (
      <span style={{ color: '#d9534f', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 2 }}>
        <TrendingUp size={12} /> +{change}
      </span>
    );
  if (change < 0)
    return (
      <span style={{ color: '#5bc0de', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 2 }}>
        <TrendingDown size={12} /> {change}
      </span>
    );
  return <Minus size={12} style={{ color: '#999' }} />;
}

function CandidateDot({ count }: { count: number }) {
  if (count === 0) return <span style={{ color: '#ccc' }}>—</span>;
  const color = count >= 5 ? '#d9534f' : count >= 2 ? '#f0ad4e' : '#5cb85c';
  return (
    <span style={{ color, fontWeight: 700 }} title={`최근 24h SELECTED ${count}회`}>
      {count}회
    </span>
  );
}

function ShareBar({ pct }: { pct: number }) {
  const width = Math.min(pct * 4, 100);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div style={{
        width: 80, height: 6, background: '#eee', borderRadius: 3, overflow: 'hidden',
      }}>
        <div style={{ width: `${width}%`, height: '100%', background: '#337ab7', borderRadius: 3 }} />
      </div>
      <span style={{ fontSize: '0.75rem', color: '#555' }}>{pct.toFixed(1)}%</span>
    </div>
  );
}

function MarketTable({ markets }: { markets: MarketFlowEntry[] }) {
  const currency = markets[0]?.market?.startsWith('KRW-') ? 'KRW' : 'USDT';

  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
            <th style={{ padding: '8px 6px', width: 50 }}>순위</th>
            <th style={{ padding: '8px 6px', width: 40 }}>변동</th>
            <th style={{ padding: '8px 6px' }}>마켓</th>
            <th style={{ padding: '8px 6px', textAlign: 'right' }}>현재가</th>
            <th style={{ padding: '8px 6px', textAlign: 'right' }}>24h 거래대금</th>
            <th style={{ padding: '8px 6px' }}>점유율</th>
            <th style={{ padding: '8px 6px', textAlign: 'center' }}>봇 진입</th>
          </tr>
        </thead>
        <tbody>
          {markets.map((m) => (
            <tr
              key={m.market}
              style={{ borderBottom: '1px solid #f0f0f0' }}
            >
              <td style={{ padding: '7px 6px', fontWeight: 700, color: '#444' }}>#{m.rank}</td>
              <td style={{ padding: '7px 6px' }}>
                <RankBadge change={m.rankChange} />
              </td>
              <td style={{ padding: '7px 6px', fontWeight: 600 }}>
                {m.market.replace('KRW-', '').replace('USDT', '')}
                <span style={{ fontSize: '0.7rem', color: '#999', marginLeft: 4 }}>/{currency}</span>
              </td>
              <td style={{ padding: '7px 6px', textAlign: 'right', fontFamily: 'monospace' }}>
                {formatNumber(m.tradePrice)}
              </td>
              <td style={{ padding: '7px 6px', textAlign: 'right', fontFamily: 'monospace' }}>
                {fmt24h(m.accTradePrice24h)}
              </td>
              <td style={{ padding: '7px 6px' }}>
                <ShareBar pct={m.volumeSharePct} />
              </td>
              <td style={{ padding: '7px 6px', textAlign: 'center' }}>
                <CandidateDot count={m.selectedCount24h} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function MarketFundFlowPage() {
  const { exchange } = useExchangeMode();
  const { data, error, isLoading, isFetching, dataUpdatedAt } = useQuery({
    queryKey: queryKeys.fundFlow(exchange),
    queryFn: () => api.fundFlow(exchange),
    refetchInterval: POLLING_INTERVALS.fundFlow,
  });

  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';

  return (
    <main className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">시장 자금 흐름</h1>
          <p className="page-subtitle">24h 거래대금 기준 마켓 순위 · 자금 집중도 · 봇 진입 빈도</p>
        </div>
        <LiveStatus isFetching={isFetching} updatedAt={dataUpdatedAt} />
      </div>

      {error && <ErrorPanel error={error} />}

      {data && (
        <>
          <div className="metric-grid">
            <MetricCard
              label={`BTC 도미넌스 (${currency})`}
              value={`${data.btcDominancePct.toFixed(1)}%`}
              detail="BTC 24h 거래대금 / 전체 합계"
            />
            <MetricCard
              label="TOP 10 집중도"
              value={`${data.top10VolumePct.toFixed(1)}%`}
              detail="상위 10개 마켓이 차지하는 거래대금 비중"
            />
            <MetricCard
              label="모니터링 마켓 수"
              value={`${data.markets.length}개`}
              detail={`${exchange} 24h 거래대금 보유 마켓`}
            />
            <MetricCard
              label="봇 진입 활동 (24h)"
              value={`${data.markets.filter((m) => m.selectedCount24h > 0).length}개`}
              detail="최근 24시간 내 SELECTED된 마켓 수"
            />
          </div>

          <section className="section">
            <h2 className="section-title">마켓별 거래대금 순위</h2>
            {data.markets.length === 0 ? (
              <EmptyState title="시세 데이터를 수집 중입니다." description="WebSocket이 연결되면 표시됩니다." />
            ) : (
              <MarketTable markets={data.markets} />
            )}
          </section>
        </>
      )}

      {isLoading && !data && (
        <EmptyState title="자금 흐름 데이터를 불러오는 중..." />
      )}
    </main>
  );
}
