import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Bot, CheckCircle2, Database, Radio, ShieldCheck, ShieldX, TrendingDown, TrendingUp } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

const DASHBOARD_RANGE = '24h' as const;
const LIVE_REFRESH_MS = 2_000;

export function DashboardPage() {
  const { exchange } = useExchangeMode();
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: LIVE_REFRESH_MS,
  });
  const providerQuery = useQuery({
    queryKey: queryKeys.marketProviderStatus(),
    queryFn: api.marketProviderStatus,
    refetchInterval: LIVE_REFRESH_MS,
  });
  const { data: summary } = useQuery({
    queryKey: queryKeys.analyticsSummary(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsSummary(DASHBOARD_RANGE, exchange),
    refetchInterval: LIVE_REFRESH_MS,
  });
  const { data: pnl } = useQuery({
    queryKey: queryKeys.analyticsPnl(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsPnl(DASHBOARD_RANGE, exchange),
    refetchInterval: LIVE_REFRESH_MS,
  });
  const { data: losses } = useQuery({
    queryKey: queryKeys.analyticsLosses(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsLosses(DASHBOARD_RANGE, exchange),
    refetchInterval: LIVE_REFRESH_MS,
  });
  const { data, error, isLoading, isFetching, dataUpdatedAt } = systemQuery;

  if (isLoading) {
    return <div className="page-state">상태를 불러오는 중</div>;
  }

  if (error || !data) {
    return <ErrorPanel error={error} />;
  }

  const signalChartData = [
    { name: '매수(BUY)', count: summary?.buyCount ?? 0 },
    { name: '매도(SELL)', count: summary?.sellCount ?? 0 },
    { name: '대기(HOLD)', count: summary?.holdCount ?? 0 },
  ];
  const totalProfit = Number(pnl?.totalProfit ?? 0);
  const totalProfitTone = totalProfit >= 0 ? 'tone-positive' : 'tone-negative';
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const money = (value: string | number | null | undefined) => formatCurrency(value, currency);
  const snapshotCount = exchange === 'BINANCE' ? providerQuery.data?.binanceSnapshotCount : providerQuery.data?.upbitSnapshotCount;
  const marketCoverage = data.scheduler.candidateMarkets.some((market) => market === 'ALL_KRW' || market === 'ALL_USDT')
    ? marketSummary(data.scheduler.candidateMarkets, exchange)
    : `${snapshotCount ?? 0}`;
  const priceReady = Boolean(providerQuery.data?.externalProvider && ((snapshotCount ?? 0) > 0 || marketCoverage.startsWith('전체')));
  const schedulerReady = data.scheduler.candidateEnabled && data.scheduler.exitEnabled;
  const autoReady = data.database.connected && priceReady && schedulerReady && !data.safety.killSwitchEnabled;
  const readinessTone = data.safety.killSwitchEnabled ? 'bad' : autoReady ? 'good' : 'warn';
  const readinessTitle = data.safety.killSwitchEnabled
    ? '거래 흐름 차단(Blocked)'
    : autoReady
      ? '자동 PAPER 운영 가능(Ready)'
      : '운영 조건 점검 필요(Review)';
  const readinessDetail = autoReady
    ? `${exchange} 기준 후보 BUY와 보유 PAPER SELL 평가가 실행 가능한 상태입니다.`
    : readinessMessage({
        databaseConnected: data.database.connected,
        priceReady,
        candidateEnabled: data.scheduler.candidateEnabled,
        exitEnabled: data.scheduler.exitEnabled,
        killSwitchEnabled: data.safety.killSwitchEnabled,
      });

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>운영 대시보드(Dashboard)</h1>
          <p>자동 PAPER 운영 상태와 손익 리스크를 확인합니다.</p>
        </div>
        <Badge tone={readinessTone}>
          {readinessTitle}
        </Badge>
        <LiveStatus updatedAt={dataUpdatedAt} isFetching={isFetching || providerQuery.isFetching} intervalMs={LIVE_REFRESH_MS} />
      </header>

      <article className={`operations-overview operations-overview-${readinessTone}`}>
        <div className="operations-summary">
          <div>
            <span>운영 준비 상태(Operational Readiness)</span>
            <strong>{readinessTitle}</strong>
            <p>{readinessDetail}</p>
          </div>
          {readinessTone === 'bad' ? <ShieldX size={34} /> : <ShieldCheck size={34} />}
        </div>
        <div className="readiness-list" aria-label="운영 준비 조건(Readiness checks)">
          <OperationCheck label="DB" value={data.database.connected ? '연결됨(Connected)' : '끊김(Disconnected)'} good={data.database.connected} />
          <OperationCheck label="시세(Price)" value={`${providerQuery.data?.provider ?? data.marketProvider.provider} / ${marketCoverage}`} good={priceReady} />
          <OperationCheck label="후보 스케줄러(Candidate)" value={data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐(Disabled)'} good={data.scheduler.candidateEnabled} />
          <OperationCheck label="청산 스케줄러(Exit)" value={data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐(Disabled)'} good={data.scheduler.exitEnabled} />
          <OperationCheck label="긴급 정지(Kill switch)" value={data.safety.killSwitchEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'} good={!data.safety.killSwitchEnabled} />
        </div>
      </article>

      <div className="status-strip dashboard-status-strip" aria-label="운영 상태(Operation status)">
        <StatusPill icon={<Database size={16} />} label="DB" value={data.database.connected ? '연결됨(Connected)' : '끊김(Disconnected)'} good={data.database.connected} />
        <StatusPill icon={<Radio size={16} />} label="시세(Price)" value={`${providerQuery.data?.provider ?? data.marketProvider.provider} / ${marketCoverage}`} good={priceReady} />
        <StatusPill icon={<Bot size={16} />} label="후보(Candidate)" value={data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐(Off)'} good={data.scheduler.candidateEnabled} />
        <StatusPill icon={<TrendingDown size={16} />} label="청산(Exit)" value={data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐(Off)'} good={data.scheduler.exitEnabled} />
        <StatusPill icon={data.safety.killSwitchEnabled ? <ShieldX size={16} /> : <ShieldCheck size={16} />} label="긴급 정지(Kill)" value={data.safety.killSwitchEnabled ? '켜짐(On)' : '꺼짐(Off)'} good={!data.safety.killSwitchEnabled} />
      </div>

      <div className="metric-grid">
        <MetricCard label="총 평가금(Total Equity)" value={money(pnl?.totalEquity)} detail={`현금(Cash) ${money(pnl?.cash)}`} />
        <MetricCard label="총 손익(Total PnL)" value={money(pnl?.totalProfit)} detail={`실현(Realized) ${money(pnl?.realizedProfit)}`} />
        <MetricCard label="보유 평가(Position Value)" value={money(pnl?.totalPositionValue)} detail={`미실현(Unrealized) ${money(pnl?.unrealizedProfit)}`} />
        <MetricCard label="전략(Strategy)" value={data.strategy.strategyName} detail={`1회 거래(Order) ${formatNumber(data.strategy.orderAmount, currency === 'USDT' ? 2 : 0)} ${currency}`} />
      </div>

      <div className="metric-grid">
        <MetricCard label="24시간 실행(Runs)" value={formatNumber(summary?.total)} detail={`체결(Filled) ${formatNumber(summary?.filledCount)}`} />
        <MetricCard label="매수 신호(Buy Signals)" value={formatNumber(summary?.buyCount)} detail={`매도(Sell) ${formatNumber(summary?.sellCount)}`} />
        <MetricCard label="익절/손절(TP/SL)" value={`${formatNumber(summary?.takeProfitCount)} / ${formatNumber(summary?.stopLossCount)}`} detail={`손실 매도(Loss sells) ${formatNumber(losses?.worstTrades.length)}`} />
        <MetricCard label="평균 익절률(Avg TP)" value={`${formatNumber(summary?.averageTakeProfitRate, 3)}%`} detail={`보유 종목(Positions) ${formatNumber(pnl?.positionCount)}`} />
      </div>

      <div className="section-grid">
        <article className="panel">
          <h2>스케줄러(Schedulers)</h2>
          <dl className="definition-list">
            <dt>전략 스케줄러(Trading Scheduler)</dt>
            <dd>{data.scheduler.enabled ? `${data.scheduler.fixedDelayMs / 1000}s` : '꺼짐(Disabled)'}</dd>
            <dt>후보 스케줄러(Candidate Scheduler)</dt>
            <dd>{data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐(Disabled)'}</dd>
            <dt>후보 거래소(Candidate exchange)</dt>
            <dd>{exchangeList(data.scheduler.candidateExchanges, data.scheduler.candidateExchange)}</dd>
            <dt>후보 마켓(Candidate markets)</dt>
            <dd>{marketSummary(data.scheduler.candidateMarkets, exchange)}</dd>
            <dt>청산 스케줄러(Exit Scheduler)</dt>
            <dd>{data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐(Disabled)'}</dd>
            <dt>청산 대상(Exit positions)</dt>
            <dd>{data.scheduler.exitPositionMarketCount}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>손익 요약(PnL Summary)</h2>
            {totalProfit >= 0 ? <TrendingUp className="tone-positive" size={22} /> : <TrendingDown className="tone-negative" size={22} />}
          </div>
          <dl className="definition-list">
            <dt>총 손익(Total)</dt>
            <dd className={totalProfitTone}>{money(pnl?.totalProfit)}</dd>
            <dt>평균 익절률(Avg TP)</dt>
            <dd>{`${formatNumber(summary?.averageTakeProfitRate, 3)}%`}</dd>
            <dt>평균 손절률(Avg SL)</dt>
            <dd className="tone-negative">{`${formatNumber(summary?.averageStopLossRate, 3)}%`}</dd>
            <dt>최근 범위(Range)</dt>
            <dd>{summary ? `${formatDateTime(summary.from)} - ${formatDateTime(summary.to)}` : '-'}</dd>
          </dl>
        </article>

        <article className="panel chart-panel">
          <div className="panel-title-row">
            <h2>24시간 신호 분포(24h Signals)</h2>
            <Badge tone="info">자동 갱신(Live)</Badge>
          </div>
          <div className="chart-wrap">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={signalChartData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="name" tickLine={false} axisLine={false} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} width={36} />
                <Tooltip />
                <Bar dataKey="count" fill="#176b87" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>손실 점검(Loss Review)</h2>
            <Badge tone={losses?.worstTrades.length ? 'warn' : 'good'}>
              {losses?.worstTrades.length ? '점검 필요(Review)' : '손실 없음(No losses)'}
            </Badge>
          </div>
          {losses?.worstTrades.length ? (
            <div className="loss-list">
              {losses.worstTrades.slice(0, 4).map((trade) => (
                <div key={`${trade.market}-${trade.createdAt}`} className="loss-item">
                  <strong>{trade.market}</strong>
                  <span className="tone-negative">{formatNumber(trade.rate, 3)}%</span>
                  <small>{formatDateTime(trade.createdAt)}</small>
                </div>
              ))}
            </div>
          ) : (
            <p>최근 24시간 손실 매도 기록이 없습니다(No recent loss sells).</p>
          )}
        </article>

        <article className="panel">
          <h2>안전장치(Safety)</h2>
          <div className="status-row">
            {data.safety.killSwitchEnabled ? <ShieldX size={22} /> : <ShieldCheck size={22} />}
            <span>{data.safety.killSwitchEnabled ? '거래 흐름 차단 중(Blocked)' : '자동 PAPER 거래 허용(Auto paper allowed)'}</span>
          </div>
        </article>

        <article className="panel">
          <h2>운영 제약(Controls)</h2>
          <dl className="definition-list">
            <dt>전략(Strategy)</dt>
            <dd>{data.strategy.strategyName}</dd>
            <dt>1회 거래(Order)</dt>
            <dd>{`${formatNumber(data.strategy.orderAmount, currency === 'USDT' ? 2 : 0)} ${currency}`}</dd>
            <dt>최대 주문(Max order)</dt>
            <dd>{money(data.risk.maxOrderAmount)}</dd>
            <dt>허용 마켓(Allowed)</dt>
            <dd>{marketSummary(data.risk.allowedMarkets, exchange)}</dd>
            <dt>수동 PAPER(Manual)</dt>
            <dd>{data.telegram.manualPaperExecutionEnabled ? '허용(Allowed)' : '차단(Blocked)'}</dd>
          </dl>
        </article>

        <article className="panel">
          <h2>알림(Notifications)</h2>
          <dl className="definition-list">
            <dt>사용(Enabled)</dt>
            <dd>{data.notification.enabled ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>체결(Filled)</dt>
            <dd>{data.notification.sendFilled ? '전송(Send)' : '건너뜀(Skip)'}</dd>
            <dt>거절(Rejected)</dt>
            <dd>{data.notification.sendRejected ? '전송(Send)' : '건너뜀(Skip)'}</dd>
          </dl>
        </article>

        <article className="panel">
          <h2>텔레그램(Telegram)</h2>
          <dl className="definition-list">
            <dt>사용(Enabled)</dt>
            <dd>{data.telegram.enabled ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>설정(Configured)</dt>
            <dd>{data.telegram.configured ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>수신(Inbound)</dt>
            <dd>{data.telegram.inboundEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>수동 PAPER 실행(Manual paper run)</dt>
            <dd>{data.telegram.manualPaperExecutionEnabled ? '허용(Allowed)' : '차단(Blocked)'}</dd>
          </dl>
        </article>

      </div>
    </section>
  );
}

function OperationCheck({
  label,
  value,
  good,
}: {
  label: string;
  value: string;
  good: boolean;
}) {
  return (
    <div className={`operation-check ${good ? 'operation-check-good' : 'operation-check-bad'}`}>
      {good ? <CheckCircle2 size={17} /> : <ShieldX size={17} />}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusPill({
  icon,
  label,
  value,
  good,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  good: boolean;
}) {
  return (
    <div className={`status-pill ${good ? 'status-pill-good' : 'status-pill-bad'}`}>
      {icon}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function marketSummary(markets: string[], exchange: string) {
  if (markets.length === 0) {
    return '-';
  }
  if (markets.includes('ALL_KRW')) {
    return '전체 KRW(ALL_KRW)';
  }
  if (markets.includes('ALL_USDT')) {
    return '전체 USDT(ALL_USDT)';
  }
  const preview = markets.slice(0, 3).join(', ');
  return markets.length > 3 ? `${preview} 외 ${markets.length - 3}개` : preview || exchange;
}

function exchangeList(exchanges: string[] | undefined, fallback: string) {
  return exchanges && exchanges.length > 0 ? exchanges.join(', ') : fallback;
}

function readinessMessage({
  databaseConnected,
  priceReady,
  candidateEnabled,
  exitEnabled,
  killSwitchEnabled,
}: {
  databaseConnected: boolean;
  priceReady: boolean;
  candidateEnabled: boolean;
  exitEnabled: boolean;
  killSwitchEnabled: boolean;
}) {
  if (killSwitchEnabled) {
    return 'Kill switch가 켜져 있어 자동 PAPER 흐름이 차단됩니다.';
  }
  const missing = [
    !databaseConnected ? 'DB' : null,
    !priceReady ? '시세' : null,
    !candidateEnabled ? '후보 스케줄러' : null,
    !exitEnabled ? '청산 스케줄러' : null,
  ].filter(Boolean);
  return `${missing.join(', ')} 상태를 확인해야 합니다.`;
}
