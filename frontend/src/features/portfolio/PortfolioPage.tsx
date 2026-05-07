import { useMemo, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ArrowDownAZ, CircleDollarSign, Gauge, PieChart, Radar, TrendingDown, TrendingUp, Wallet } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import type { PositionValuationResponse } from '../../shared/api/types';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatKrw, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

type SortKey = 'value' | 'profitRate' | 'market';

const TAKE_PROFIT_RATE = 1.5;
const STOP_LOSS_RATE = -0.7;

export function PortfolioPage() {
  const [sortKey, setSortKey] = useState<SortKey>('profitRate');
  const { exchange } = useExchangeMode();
  const statusQuery = useQuery({ queryKey: queryKeys.portfolioStatus(exchange), queryFn: () => api.portfolioStatus(exchange), refetchInterval: 5_000 });
  const positionsQuery = useQuery({ queryKey: queryKeys.positions(exchange), queryFn: () => api.positions(exchange), refetchInterval: 5_000 });
  const valuationQuery = useQuery({ queryKey: queryKeys.portfolioValuation(exchange), queryFn: () => api.portfolioValuation(exchange), refetchInterval: 5_000 });
  const systemQuery = useQuery({ queryKey: queryKeys.system(exchange), queryFn: () => api.systemStatus(exchange), refetchInterval: 10_000 });

  const positions = useMemo(
    () => sortPositions(valuationQuery.data?.positions ?? [], sortKey),
    [sortKey, valuationQuery.data?.positions],
  );
  const totalEquity = Number(valuationQuery.data?.totalEquity ?? 0);
  const cash = Number(valuationQuery.data?.cash ?? statusQuery.data?.cash ?? 0);
  const positionValue = Number(valuationQuery.data?.totalPositionValue ?? 0);
  const orderAmount = Number(systemQuery.data?.strategy.orderAmount ?? 0);
  const cashRate = totalEquity > 0 ? (cash / totalEquity) * 100 : 0;
  const positionRate = totalEquity > 0 ? (positionValue / totalEquity) * 100 : 0;
  const capitalUseRate = totalEquity > 0 ? (positionValue / totalEquity) * 100 : 0;
  const remainingBuyCount = orderAmount > 0 ? Math.floor(cash / orderAmount) : 0;
  const reservedCashAfterBuys = orderAmount > 0 ? cash - remainingBuyCount * orderAmount : cash;
  const exposureRows = useMemo(() => buildExposureRows(valuationQuery.data?.positions ?? [], totalEquity), [valuationQuery.data?.positions, totalEquity]);
  const largestExposureRate = exposureRows[0]?.exposureRate ?? 0;
  const bestPosition = positions.reduce<PositionValuationResponse | null>(
    (best, position) => (best === null || Number(position.unrealizedProfitRate) > Number(best.unrealizedProfitRate) ? position : best),
    null,
  );
  const worstPosition = positions.reduce<PositionValuationResponse | null>(
    (worst, position) => (worst === null || Number(position.unrealizedProfitRate) < Number(worst.unrealizedProfitRate) ? position : worst),
    null,
  );

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
      {systemQuery.error ? <ErrorPanel title="시스템 상태 조회 실패(System status failed)" error={systemQuery.error} /> : null}

      <div className="metric-grid">
        <MetricCard label="현금(Cash)" value={formatKrw(valuationQuery.data?.cash ?? statusQuery.data?.cash)} detail={`${formatNumber(cashRate, 1)}%`} />
        <MetricCard label="포지션 가치(Position Value)" value={formatKrw(valuationQuery.data?.totalPositionValue)} detail={`${formatNumber(positionRate, 1)}%`} />
        <MetricCard label="자금 사용률(Capital Used)" value={`${formatNumber(capitalUseRate, 1)}%`} detail={`매수 가능(Buys left) ${formatNumber(remainingBuyCount)}`} />
        <MetricCard label="총손익(Total Profit)" value={formatKrw(valuationQuery.data?.totalProfit)} detail={`실현(Realized) ${formatKrw(valuationQuery.data?.realizedProfit)}`} />
      </div>

      <div className="portfolio-overview">
        <article className="panel">
          <div className="panel-title-row">
            <h2>자산 배분(Allocation)</h2>
            <PieChart size={20} />
          </div>
          <div className="allocation-bars">
            <AllocationBar icon={<Wallet size={17} />} label="현금(Cash)" value={cashRate} />
            <AllocationBar icon={<CircleDollarSign size={17} />} label="포지션(Positions)" value={positionRate} />
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>자금 활용(Capital Usage)</h2>
            <Gauge size={20} />
          </div>
          <div className="allocation-bars">
            <AllocationBar icon={<CircleDollarSign size={17} />} label="사용 중(Used)" value={capitalUseRate} />
            <AllocationBar icon={<Wallet size={17} />} label="대기 현금(Idle cash)" value={cashRate} />
          </div>
          <dl className="definition-list capital-list">
            <dt>1회 매수(Order)</dt>
            <dd>{formatKrw(orderAmount)}</dd>
            <dt>매수 가능(Buys left)</dt>
            <dd>{formatNumber(remainingBuyCount)}회</dd>
            <dt>단위 미만 현금(Residual cash)</dt>
            <dd>{formatKrw(reservedCashAfterBuys)}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>손익 리더(Profit Leaders)</h2>
            <Badge tone={positions.length ? 'info' : 'neutral'}>{positions.length} positions</Badge>
          </div>
          <div className="leader-grid">
            <LeaderItem title="최고 수익(Best)" position={bestPosition} positive />
            <LeaderItem title="최대 손실(Worst)" position={worstPosition} />
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>market별 비중(Market Exposure)</h2>
            <Radar size={20} />
          </div>
          <div className="exposure-summary">
            <Badge tone={largestExposureRate >= 10 ? 'warn' : 'good'}>
              {largestExposureRate >= 10 ? '쏠림 점검(Review)' : '분산 양호(Diversified)'}
            </Badge>
            <span>TOP {Math.min(5, exposureRows.length)}</span>
          </div>
          <div className="exposure-list">
            {exposureRows.slice(0, 5).map((row) => (
              <div className="exposure-item" key={row.market}>
                <div className="exposure-item-main">
                  <strong>{row.market}</strong>
                  <span>{formatKrw(row.positionValue)}</span>
                  <small className={profitClass(row.unrealizedProfitRate)}>{formatNumber(row.unrealizedProfitRate, 2)}%</small>
                </div>
                <div className="allocation-track">
                  <span style={{ width: `${Math.max(0, Math.min(100, row.exposureRate))}%` }} />
                </div>
                <small>{formatNumber(row.exposureRate, 2)}% of equity</small>
              </div>
            ))}
            {exposureRows.length === 0 ? <EmptyState title="비중 데이터 없음(No exposure)" description="보유 포지션이 생기면 market별 비중이 표시됩니다." /> : null}
          </div>
        </article>
      </div>

      <div className="toolbar portfolio-toolbar" aria-label="포트폴리오 정렬(Portfolio sort)">
        <span>정렬(Sort)</span>
        <button className={sortKey === 'profitRate' ? 'button button-primary' : 'button button-secondary'} type="button" onClick={() => setSortKey('profitRate')}>
          <TrendingDown size={16} />
          손익률(PnL %)
        </button>
        <button className={sortKey === 'value' ? 'button button-primary' : 'button button-secondary'} type="button" onClick={() => setSortKey('value')}>
          <CircleDollarSign size={16} />
          평가액(Value)
        </button>
        <button className={sortKey === 'market' ? 'button button-primary' : 'button button-secondary'} type="button" onClick={() => setSortKey('market')}>
          <ArrowDownAZ size={16} />
          마켓(Market)
        </button>
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
              <th>상태(Status)</th>
            </tr>
          </thead>
          <tbody>
            {positions.map((position) => (
              <tr key={position.market}>
                <td><strong>{position.market}</strong></td>
                <td>{formatNumber(position.quantity, 8)}</td>
                <td>{formatKrw(position.averageBuyPrice)}</td>
                <td>{formatKrw(position.currentPrice)}</td>
                <td>{formatKrw(position.positionValue)}</td>
                <td className={profitClass(position.unrealizedProfit)}>{formatKrw(position.unrealizedProfit)}</td>
                <td>
                  <span className={`profit-rate ${profitClass(position.unrealizedProfitRate)}`}>
                    {Number(position.unrealizedProfitRate) >= 0 ? <TrendingUp size={15} /> : <TrendingDown size={15} />}
                    {formatNumber(position.unrealizedProfitRate, 2)}%
                  </span>
                </td>
                <td>
                  <ExitBadge rate={Number(position.unrealizedProfitRate)} />
                </td>
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

function sortPositions(positions: PositionValuationResponse[], sortKey: SortKey) {
  return [...positions].sort((left, right) => {
    if (sortKey === 'market') {
      return left.market.localeCompare(right.market);
    }
    if (sortKey === 'value') {
      return Number(right.positionValue) - Number(left.positionValue);
    }
    return Number(left.unrealizedProfitRate) - Number(right.unrealizedProfitRate);
  });
}

function buildExposureRows(positions: PositionValuationResponse[], totalEquity: number) {
  return [...positions]
    .map((position) => ({
      market: position.market,
      positionValue: Number(position.positionValue),
      unrealizedProfitRate: Number(position.unrealizedProfitRate),
      exposureRate: totalEquity > 0 ? (Number(position.positionValue) / totalEquity) * 100 : 0,
    }))
    .sort((left, right) => right.positionValue - left.positionValue);
}

function profitClass(value: string | number) {
  return Number(value) >= 0 ? 'tone-positive' : 'tone-negative';
}

function AllocationBar({ icon, label, value }: { icon: ReactNode; label: string; value: number }) {
  return (
    <div className="allocation-row">
      <div className="allocation-label">
        {icon}
        <span>{label}</span>
        <strong>{formatNumber(value, 1)}%</strong>
      </div>
      <div className="allocation-track">
        <span style={{ width: `${Math.max(0, Math.min(100, value))}%` }} />
      </div>
    </div>
  );
}

function LeaderItem({
  title,
  position,
  positive = false,
}: {
  title: string;
  position: PositionValuationResponse | null;
  positive?: boolean;
}) {
  if (!position) {
    return (
      <div className="leader-item">
        <span>{title}</span>
        <strong>-</strong>
        <small>보유 없음(No position)</small>
      </div>
    );
  }

  return (
    <div className="leader-item">
      <span>{title}</span>
      <strong>{position.market}</strong>
      <small className={positive ? 'tone-positive' : profitClass(position.unrealizedProfitRate)}>
        {formatNumber(position.unrealizedProfitRate, 2)}% / {formatKrw(position.unrealizedProfit)}
      </small>
    </div>
  );
}

function ExitBadge({ rate }: { rate: number }) {
  if (rate >= TAKE_PROFIT_RATE) {
    return <Badge tone="good">익절권(Take profit)</Badge>;
  }
  if (rate <= STOP_LOSS_RATE) {
    return <Badge tone="bad">손절권(Stop loss)</Badge>;
  }
  if (TAKE_PROFIT_RATE - rate <= 0.3) {
    return <Badge tone="info">익절 근접(Near TP)</Badge>;
  }
  if (rate - STOP_LOSS_RATE <= 0.3) {
    return <Badge tone="warn">손절 근접(Near SL)</Badge>;
  }
  return <Badge>보유(Hold)</Badge>;
}
