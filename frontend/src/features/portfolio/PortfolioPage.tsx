import { useQuery } from '@tanstack/react-query';
import { api, queryKeys } from '../../shared/api/client';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatKrw, formatNumber } from '../../shared/format';

export function PortfolioPage() {
  const statusQuery = useQuery({ queryKey: queryKeys.portfolioStatus, queryFn: api.portfolioStatus, refetchInterval: 5_000 });
  const positionsQuery = useQuery({ queryKey: queryKeys.positions, queryFn: api.positions, refetchInterval: 5_000 });
  const valuationQuery = useQuery({ queryKey: queryKeys.portfolioValuation, queryFn: api.portfolioValuation, refetchInterval: 5_000 });

  const positions = valuationQuery.data?.positions ?? [];

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>포트폴리오(Portfolio)</h1>
          <p>자동 PAPER 거래의 현금, 포지션, 평가 손익을 확인합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      {statusQuery.error ? <ErrorPanel title="포트폴리오 상태 조회 실패(Portfolio status failed)" error={statusQuery.error} /> : null}
      {valuationQuery.error ? <ErrorPanel title="포트폴리오 평가 조회 실패(Portfolio valuation failed)" error={valuationQuery.error} /> : null}

      <div className="metric-grid">
        <MetricCard label="현금(Cash)" value={formatKrw(valuationQuery.data?.cash ?? statusQuery.data?.cash)} />
        <MetricCard label="포지션 가치(Position Value)" value={formatKrw(valuationQuery.data?.totalPositionValue)} />
        <MetricCard label="총자산(Total Equity)" value={formatKrw(valuationQuery.data?.totalEquity)} />
        <MetricCard label="총손익(Total Profit)" value={formatKrw(valuationQuery.data?.totalProfit)} />
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>마켓(Market)</th>
              <th>수량(Quantity)</th>
              <th>평균매수가(Avg Buy)</th>
              <th>현재가(Current)</th>
              <th>가치(Value)</th>
              <th>미실현손익(Unrealized)</th>
              <th>수익률(Rate)</th>
            </tr>
          </thead>
          <tbody>
            {positions.map((position) => (
              <tr key={position.market}>
                <td>{position.market}</td>
                <td>{formatNumber(position.quantity, 8)}</td>
                <td>{formatKrw(position.averageBuyPrice)}</td>
                <td>{formatKrw(position.currentPrice)}</td>
                <td>{formatKrw(position.positionValue)}</td>
                <td>{formatKrw(position.unrealizedProfit)}</td>
                <td>{formatNumber(position.unrealizedProfitRate, 2)}%</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!valuationQuery.isLoading && positions.length === 0 ? (
          <EmptyState title="보유 포지션 없음(No positions)" description="자동 PAPER 매수가 체결되면 여기에 표시됩니다." />
        ) : null}
      </div>

      {positionsQuery.data && positions.length === 0 && positionsQuery.data.length > 0 ? (
        <article className="panel">
          <h2>평가 불가 포지션(Unpriced positions)</h2>
          <p>현재가 평가 API가 실패했지만 보유 포지션은 존재합니다.</p>
        </article>
      ) : null}
    </section>
  );
}
