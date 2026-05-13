import { useMemo, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowDownAZ, CircleDollarSign, Gauge, PieChart as PieIcon, Radar, ShieldCheck, TrendingDown, TrendingUp, Wallet } from 'lucide-react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import type { PositionValuationResponse, SelectedPaperSellResponse } from '../../shared/api/types';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatCurrency, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

type SortKey = 'value' | 'profitRate' | 'market';
type AllocationSlice = {
  id: string;
  label: string;
  value: number;
  rate: number;
  color: string;
};

const TAKE_PROFIT_RATE = 1.5;
const STOP_LOSS_RATE = -0.7;
const MARKET_COLORS = ['#176b87', '#1f8a70', '#b7791f', '#8c5a2b', '#5d6d7e', '#bd3d2f'];
const CASH_COLOR = '#1f8a70';
const POSITION_COLOR = '#176b87';
const OTHER_COLOR = '#7b8794';
const PORTFOLIO_REFRESH_MS = 2_000;

export function PortfolioPage() {
  const [sortKey, setSortKey] = useState<SortKey>('profitRate');
  const [selectedMarkets, setSelectedMarkets] = useState<Set<string>>(() => new Set());
  const [sellSummary, setSellSummary] = useState<SelectedPaperSellResponse | null>(null);
  const { exchange } = useExchangeMode();
  const queryClient = useQueryClient();
  const statusQuery = useQuery({ queryKey: queryKeys.portfolioStatus(exchange), queryFn: () => api.portfolioStatus(exchange), refetchInterval: PORTFOLIO_REFRESH_MS });
  const positionsQuery = useQuery({ queryKey: queryKeys.positions(exchange), queryFn: () => api.positions(exchange), refetchInterval: PORTFOLIO_REFRESH_MS });
  const valuationQuery = useQuery({ queryKey: queryKeys.portfolioValuation(exchange), queryFn: () => api.portfolioValuation(exchange), refetchInterval: PORTFOLIO_REFRESH_MS });
  const systemQuery = useQuery({ queryKey: queryKeys.system(exchange), queryFn: () => api.systemStatus(exchange), refetchInterval: 5_000 });

  const positions = useMemo(
    () => sortPositions(valuationQuery.data?.positions ?? [], sortKey),
    [sortKey, valuationQuery.data?.positions],
  );
  const totalEquity = Number(valuationQuery.data?.totalEquity ?? 0);
  const cash = Number(valuationQuery.data?.cash ?? statusQuery.data?.cash ?? 0);
  const currency = valuationQuery.data?.currency ?? statusQuery.data?.currency ?? (exchange === 'BINANCE' ? 'USDT' : 'KRW');
  const money = (value: string | number | null | undefined) => formatCurrency(value, currency);
  const positionValue = Number(valuationQuery.data?.totalPositionValue ?? 0);
  const orderAmount = Number(systemQuery.data?.strategy.orderAmount ?? 0);
  const cashRate = totalEquity > 0 ? (cash / totalEquity) * 100 : 0;
  const positionRate = totalEquity > 0 ? (positionValue / totalEquity) * 100 : 0;
  const capitalUseRate = totalEquity > 0 ? (positionValue / totalEquity) * 100 : 0;
  const remainingBuyCount = orderAmount > 0 ? Math.floor(cash / orderAmount) : 0;
  const reservedCashAfterBuys = orderAmount > 0 ? cash - remainingBuyCount * orderAmount : cash;
  const exposureRows = useMemo(() => buildExposureRows(valuationQuery.data?.positions ?? [], totalEquity), [valuationQuery.data?.positions, totalEquity]);
  const assetMixSlices = useMemo(() => buildAssetMixSlices(cash, positionValue, totalEquity), [cash, positionValue, totalEquity]);
  const marketAllocationSlices = useMemo(() => buildMarketAllocationSlices(valuationQuery.data?.positions ?? [], totalEquity), [valuationQuery.data?.positions, totalEquity]);
  const exchangeAllocationSlices = useMemo(() => buildExchangeAllocationSlices(exchange, totalEquity), [exchange, totalEquity]);
  const largestExposureRate = exposureRows[0]?.exposureRate ?? 0;
  const bestPosition = positions.reduce<PositionValuationResponse | null>(
    (best, position) => (best === null || Number(position.unrealizedProfitRate) > Number(best.unrealizedProfitRate) ? position : best),
    null,
  );
  const worstPosition = positions.reduce<PositionValuationResponse | null>(
    (worst, position) => (worst === null || Number(position.unrealizedProfitRate) < Number(worst.unrealizedProfitRate) ? position : worst),
    null,
  );
  const selectedVisibleMarkets = positions.map((position) => position.market).filter((market) => selectedMarkets.has(market));
  const selectedCount = selectedVisibleMarkets.length;
  const displayedSellSummary = sellSummary?.exchange === exchange ? sellSummary : null;
  const sellSelectedMutation = useMutation({
    mutationFn: (markets: string[]) => api.sellSelectedPositions(exchange, { markets }),
    onSuccess: (response) => {
      setSellSummary(response);
      setSelectedMarkets(new Set());
      void queryClient.invalidateQueries({ queryKey: queryKeys.portfolioStatus(exchange) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.positions(exchange) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.portfolioValuation(exchange) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.history(exchange) });
    },
  });

  const toggleMarket = (market: string) => {
    setSelectedMarkets((current) => {
      const next = new Set(current);
      if (next.has(market)) {
        next.delete(market);
      } else {
        next.add(market);
      }
      return next;
    });
  };

  const sellSelected = () => {
    const markets = selectedVisibleMarkets;
    if (markets.length === 0) {
      return;
    }
    if (!window.confirm('선택한 PAPER 포지션을 전량 매도할까요?')) {
      return;
    }
    sellSelectedMutation.mutate(markets);
  };

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>포트폴리오(Portfolio)</h1>
          <p>자동 PAPER 거래의 현금, 포지션, 평가 손익을 확인합니다.</p>
        </div>
        <LiveStatus updatedAt={valuationQuery.dataUpdatedAt} isFetching={statusQuery.isFetching || positionsQuery.isFetching || valuationQuery.isFetching} intervalMs={PORTFOLIO_REFRESH_MS} />
      </header>

      {statusQuery.error ? <ErrorPanel title="포트폴리오 상태 조회 실패(Portfolio status failed)" error={statusQuery.error} /> : null}
      {valuationQuery.error ? <ErrorPanel title="포트폴리오 평가 조회 실패(Portfolio valuation failed)" error={valuationQuery.error} /> : null}
      {systemQuery.error ? <ErrorPanel title="시스템 상태 조회 실패(System status failed)" error={systemQuery.error} /> : null}
      {sellSelectedMutation.error ? <ErrorPanel title="선택 매도 실패(Selected sell failed)" error={sellSelectedMutation.error} /> : null}

      <div className="metric-grid">
        <MetricCard label="현금(Cash)" value={money(valuationQuery.data?.cash ?? statusQuery.data?.cash)} detail={`${currency} ${formatNumber(cashRate, 1)}%`} />
        <MetricCard label="포지션 가치(Position Value)" value={money(valuationQuery.data?.totalPositionValue)} detail={`${formatNumber(positionRate, 1)}%`} />
        <MetricCard label="자금 사용률(Capital Used)" value={`${formatNumber(capitalUseRate, 1)}%`} detail={`매수 가능(Buys left) ${formatNumber(remainingBuyCount)}`} />
        <MetricCard label="총손익(Total Profit)" value={money(valuationQuery.data?.totalProfit)} detail={`실현(Realized) ${money(valuationQuery.data?.realizedProfit)}`} />
      </div>

      <div className="portfolio-chart-grid">
        <AllocationPiePanel
          currency={currency}
          emptyDescription="평가 가능한 자산이 생기면 현금과 포지션 비중이 표시됩니다."
          emptyTitle="자산 비중 없음(No asset mix)"
          slices={assetMixSlices}
          title="자산 비중(Asset Mix)"
        />
        <AllocationPiePanel
          currency={currency}
          emptyDescription="보유 포지션이 생기면 market별 비중이 표시됩니다."
          emptyTitle="마켓 비중 없음(No market allocation)"
          slices={marketAllocationSlices}
          title="마켓 비중(Market Allocation)"
        />
        <AllocationPiePanel
          currency={currency}
          emptyDescription="선택 거래소의 평가 자산이 생기면 거래소 비중이 표시됩니다."
          emptyTitle="거래소 비중 없음(No exchange allocation)"
          slices={exchangeAllocationSlices}
          title="거래소 비중(Exchange Allocation)"
        />
      </div>

      <div className="portfolio-overview">
        <article className="panel">
          <div className="panel-title-row">
            <h2>자산 배분(Allocation)</h2>
            <PieIcon size={20} />
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
            <dd>{money(orderAmount)}</dd>
            <dt>매수 가능(Buys left)</dt>
            <dd>{formatNumber(remainingBuyCount)}회</dd>
            <dt>단위 미만 현금(Residual cash)</dt>
            <dd>{money(reservedCashAfterBuys)}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>손익 리더(Profit Leaders)</h2>
            <Badge tone={positions.length ? 'info' : 'neutral'}>{positions.length} positions</Badge>
          </div>
          <div className="leader-grid">
            <LeaderItem title="최고 수익(Best)" position={bestPosition} currency={currency} positive />
            <LeaderItem title="최대 손실(Worst)" position={worstPosition} currency={currency} />
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
                  <span>{money(row.positionValue)}</span>
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

      {selectedCount > 0 ? (
        <div className="toolbar portfolio-toolbar" aria-label="선택 PAPER 매도(Selected PAPER sell)">
          <span>선택됨(Selected) {selectedCount}</span>
          <button className="button button-primary" type="button" onClick={sellSelected} disabled={sellSelectedMutation.isPending}>
            <ShieldCheck size={16} />
            선택 매도(Sell selected)
          </button>
        </div>
      ) : null}

      {displayedSellSummary ? (
        <article className="panel">
          <div className="panel-title-row">
            <h2>선택 매도 결과(Selected Sell Result)</h2>
            <Badge tone={displayedSellSummary.failedCount > 0 ? 'warn' : 'good'}>
              {displayedSellSummary.succeededCount}/{displayedSellSummary.requestedCount}
            </Badge>
          </div>
          <div className="definition-list">
            {displayedSellSummary.results.map((result) => (
              <div key={result.market}>
                <dt>{result.market}</dt>
                <dd>{result.orderStatus} · {result.message}</dd>
              </div>
            ))}
          </div>
        </article>
      ) : null}

      <div className="position-card-list" aria-label="모바일 포지션 카드(Mobile position cards)">
        {positions.map((position) => (
          <article className="position-card" key={position.market}>
            <div className="position-card-head">
              <label className="position-select">
                <input
                  aria-label={`${position.market} 선택`}
                  checked={selectedMarkets.has(position.market)}
                  disabled={sellSelectedMutation.isPending}
                  type="checkbox"
                  onChange={() => toggleMarket(position.market)}
                />
                <span>{position.market}</span>
              </label>
              <ExitBadge rate={Number(position.unrealizedProfitRate)} />
            </div>
            <div className="position-card-value">
              <span>평가액(Value)</span>
              <strong>{money(position.positionValue)}</strong>
            </div>
            <dl className="position-card-metrics">
              <div>
                <dt>손익률(PnL %)</dt>
                <dd className={profitClass(position.unrealizedProfitRate)}>{formatNumber(position.unrealizedProfitRate, 2)}%</dd>
              </div>
              <div>
                <dt>미실현(Unrealized)</dt>
                <dd className={profitClass(position.unrealizedProfit)}>{money(position.unrealizedProfit)}</dd>
              </div>
              <div>
                <dt>수량(Quantity)</dt>
                <dd>{formatNumber(position.quantity, 8)}</dd>
              </div>
              <div>
                <dt>현재가(Current)</dt>
                <dd>{money(position.currentPrice)}</dd>
              </div>
              <div>
                <dt>평균매수가(Avg Buy)</dt>
                <dd>{money(position.averageBuyPrice)}</dd>
              </div>
            </dl>
          </article>
        ))}
        {!valuationQuery.isLoading && positions.length === 0 ? (
          <EmptyState title="보유 포지션 없음(No positions)" description="자동 PAPER 매수가 체결되면 여기에 표시됩니다." />
        ) : null}
      </div>

      <div className="table-wrap portfolio-position-table">
        <table>
          <thead>
            <tr>
              <th>선택(Select)</th>
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
                <td>
                  <input
                    aria-label={`${position.market} 선택`}
                    checked={selectedMarkets.has(position.market)}
                    disabled={sellSelectedMutation.isPending}
                    type="checkbox"
                    onChange={() => toggleMarket(position.market)}
                  />
                </td>
                <td><strong>{position.market}</strong></td>
                <td>{formatNumber(position.quantity, 8)}</td>
                <td>{money(position.averageBuyPrice)}</td>
                <td>{money(position.currentPrice)}</td>
                <td>{money(position.positionValue)}</td>
                <td className={profitClass(position.unrealizedProfit)}>{money(position.unrealizedProfit)}</td>
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

function buildAssetMixSlices(cash: number, positionValue: number, totalEquity: number): AllocationSlice[] {
  if (totalEquity <= 0) {
    return [];
  }
  return [
    { id: 'cash', label: '현금(Cash)', value: Math.max(0, cash), rate: (Math.max(0, cash) / totalEquity) * 100, color: CASH_COLOR },
    { id: 'positions', label: '포지션(Positions)', value: Math.max(0, positionValue), rate: (Math.max(0, positionValue) / totalEquity) * 100, color: POSITION_COLOR },
  ].filter((slice) => slice.value > 0);
}

function buildMarketAllocationSlices(positions: PositionValuationResponse[], totalEquity: number): AllocationSlice[] {
  if (totalEquity <= 0 || positions.length === 0) {
    return [];
  }
  const sorted = positions
    .map((position) => ({
      id: position.market,
      label: position.market,
      value: Math.max(0, Number(position.positionValue)),
    }))
    .filter((slice) => slice.value > 0)
    .sort((left, right) => right.value - left.value);
  const top = sorted.slice(0, 5);
  const otherValue = sorted.slice(5).reduce((sum, slice) => sum + slice.value, 0);
  const slices = otherValue > 0
    ? [...top, { id: 'other', label: '기타(Other)', value: otherValue }]
    : top;

  return slices.map((slice, index) => ({
    ...slice,
    rate: (slice.value / totalEquity) * 100,
    color: slice.id === 'other' ? OTHER_COLOR : MARKET_COLORS[index % MARKET_COLORS.length],
  }));
}

function buildExchangeAllocationSlices(exchange: string, totalEquity: number): AllocationSlice[] {
  if (totalEquity <= 0) {
    return [];
  }
  return [{
    id: exchange,
    label: `${exchange} 선택 거래소(Selected exchange)`,
    value: totalEquity,
    rate: 100,
    color: exchange === 'BINANCE' ? '#b7791f' : '#176b87',
  }];
}

function AllocationPiePanel({
  currency,
  emptyDescription,
  emptyTitle,
  slices,
  title,
}: {
  currency: string;
  emptyDescription: string;
  emptyTitle: string;
  slices: AllocationSlice[];
  title: string;
}) {
  return (
    <article className="panel portfolio-chart-panel">
      <div className="panel-title-row">
        <h2>{title}</h2>
        <PieIcon size={20} />
      </div>
      {slices.length === 0 ? (
        <EmptyState title={emptyTitle} description={emptyDescription} />
      ) : (
        <div className="portfolio-chart-layout">
          <div className="portfolio-pie-wrap" aria-label={title}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={slices} dataKey="value" nameKey="label" innerRadius="54%" outerRadius="82%" paddingAngle={2}>
                  {slices.map((slice) => (
                    <Cell key={slice.id} fill={slice.color} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatCurrency(typeof value === 'number' || typeof value === 'string' ? value : null, currency)} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="pie-legend">
            {slices.map((slice) => (
              <div className="pie-legend-item" key={slice.id}>
                <span style={{ background: slice.color }} />
                <strong>{slice.label}</strong>
                <small>{formatCurrency(slice.value, currency)} · {formatNumber(slice.rate, 1)}%</small>
              </div>
            ))}
          </div>
        </div>
      )}
    </article>
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
  currency,
  positive = false,
}: {
  title: string;
  position: PositionValuationResponse | null;
  currency: string;
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
        {formatNumber(position.unrealizedProfitRate, 2)}% / {formatCurrency(position.unrealizedProfit, currency)}
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
