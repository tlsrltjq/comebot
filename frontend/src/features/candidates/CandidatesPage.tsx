import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { formatDateTime, formatKrw, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

export function CandidatesPage() {
  const [market, setMarket] = useState('');
  const normalizedMarket = market.trim().toUpperCase();
  const { exchange } = useExchangeMode();

  const candidatesQuery = useQuery({
    queryKey: queryKeys.candidates(exchange, normalizedMarket || undefined),
    queryFn: () => api.candidates(exchange, normalizedMarket || undefined),
    refetchInterval: 5_000,
  });

  const candidates = useMemo(() => candidatesQuery.data ?? [], [candidatesQuery.data]);

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>후보 모니터링(Candidate Monitoring)</h1>
          <p>자동 실행 스케줄러(Auto scheduler)가 판단하는 롱 후보를 확인합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      <div className="toolbar">
        <label>
          마켓(Market)
          <input value={market} onChange={(event) => setMarket(event.target.value)} placeholder="전체 또는 KRW-BTC" />
        </label>
      </div>

      {candidatesQuery.error ? <ErrorPanel error={candidatesQuery.error} /> : null}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>마켓(Market)</th>
              <th>판단(Decision)</th>
              <th>추세(Trend)</th>
              <th>가격(Price)</th>
              <th>변화율(Change)</th>
              <th>범위(Range)</th>
              <th>거래대금(Trade amount)</th>
              <th>이유(Reason)</th>
              <th>스캔 시각(Scanned)</th>
            </tr>
          </thead>
          <tbody>
            {candidates.map((candidate) => (
              <tr key={`${candidate.market}-${candidate.scannedAt}`}>
                <td>{candidate.market}</td>
                <td>
                  <Badge tone={candidate.decision === 'SELECTED' ? 'good' : 'neutral'}>{candidate.decision}</Badge>
                </td>
                <td>{candidate.trend ?? '-'}</td>
                <td>{formatKrw(candidate.currentPrice)}</td>
                <td>{formatNumber(candidate.priceChangeRate, 2)}%</td>
                <td>{formatNumber(candidate.highLowRangeRate, 2)}%</td>
                <td>{formatNumber(candidate.tradeAmountChangeRate, 2)}%</td>
                <td>{candidate.reason}</td>
                <td>{formatDateTime(candidate.scannedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!candidatesQuery.isLoading && candidates.length === 0 ? <EmptyState title="후보 없음(No candidates)" /> : null}
      </div>
    </section>
  );
}
