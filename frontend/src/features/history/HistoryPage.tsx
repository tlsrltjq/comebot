import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { formatDateTime, formatKrw } from '../../shared/format';

export function HistoryPage() {
  const [market, setMarket] = useState('');
  const [limit, setLimit] = useState(20);
  const normalizedMarket = market.trim().toUpperCase();
  const historyQuery = useQuery({
    queryKey: queryKeys.history(normalizedMarket || undefined, limit),
    queryFn: () => api.history(normalizedMarket || undefined, limit),
    refetchInterval: 3_000,
  });

  const rows = historyQuery.data ?? [];

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>실행 이력(History)</h1>
          <p>자동 실행 결과를 HOLD, REJECTED, FILLED, FAILED 상태로 확인합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      <div className="toolbar">
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
      </div>

      {historyQuery.error ? <ErrorPanel error={historyQuery.error} /> : null}

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
                <td>{row.market}</td>
                <td>{formatKrw(row.currentPrice)}</td>
                <td>{row.signalType ?? '-'}</td>
                <td>
                  <Badge tone={row.orderStatus === 'FILLED' ? 'good' : row.orderStatus === 'REJECTED' ? 'warn' : 'neutral'}>
                    {row.orderStatus ?? (row.orderCreated ? 'CREATED' : 'NO_ORDER')}
                  </Badge>
                </td>
                <td>{row.signalReason}</td>
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
