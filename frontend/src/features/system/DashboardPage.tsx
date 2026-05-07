import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Activity, Bell, Bot, Database, ShieldCheck, ShieldX, TrendingDown, TrendingUp } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatDateTime, formatKrw, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

const DASHBOARD_RANGE = '24h' as const;

export function DashboardPage() {
  const { exchange } = useExchangeMode();
  const { data, error, isLoading } = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: 5_000,
  });
  const { data: summary } = useQuery({
    queryKey: queryKeys.analyticsSummary(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsSummary(DASHBOARD_RANGE, exchange),
    refetchInterval: 5_000,
  });
  const { data: pnl } = useQuery({
    queryKey: queryKeys.analyticsPnl(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsPnl(DASHBOARD_RANGE, exchange),
    refetchInterval: 5_000,
  });
  const { data: losses } = useQuery({
    queryKey: queryKeys.analyticsLosses(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsLosses(DASHBOARD_RANGE, exchange),
    refetchInterval: 5_000,
  });

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

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>운영 대시보드(Dashboard)</h1>
          <p>자동 PAPER 거래 상태(Auto paper trading status)를 확인합니다.</p>
        </div>
        <Badge tone={data.safety.killSwitchEnabled ? 'bad' : 'good'}>
          {data.safety.killSwitchEnabled ? '중지됨(Kill switch ON)' : '자동 실행 가능(Auto ready)'}
        </Badge>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      <div className="status-strip" aria-label="운영 상태(Operation status)">
        <StatusPill icon={<Database size={16} />} label="DB" value={data.database.connected ? '연결됨(Connected)' : '끊김(Disconnected)'} good={data.database.connected} />
        <StatusPill icon={<Activity size={16} />} label="자동매매(Auto)" value={data.scheduler.enabled ? `${data.scheduler.fixedDelayMs / 1000}s` : '꺼짐(Off)'} good={data.scheduler.enabled} />
        <StatusPill icon={<Bot size={16} />} label="후보(Candidate)" value={data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐(Off)'} good={data.scheduler.candidateEnabled} />
        <StatusPill icon={<Bell size={16} />} label="수동실행(Manual)" value={data.telegram.manualPaperExecutionEnabled ? '허용(Allowed)' : '차단(Blocked)'} good={!data.telegram.manualPaperExecutionEnabled} />
      </div>

      <div className="metric-grid">
        <MetricCard label="총 평가금(Total Equity)" value={formatKrw(pnl?.totalEquity)} detail={`현금(Cash) ${formatKrw(pnl?.cash)}`} />
        <MetricCard label="총 손익(Total PnL)" value={formatKrw(pnl?.totalProfit)} detail={`실현(Realized) ${formatKrw(pnl?.realizedProfit)}`} />
        <MetricCard label="보유 평가(Position Value)" value={formatKrw(pnl?.totalPositionValue)} detail={`미실현(Unrealized) ${formatKrw(pnl?.unrealizedProfit)}`} />
        <MetricCard label="전략(Strategy)" value={data.strategy.strategyName} detail={`1회 거래(Order) ${formatNumber(data.strategy.orderAmount, 0)} KRW`} />
      </div>

      <div className="metric-grid">
        <MetricCard label="24시간 실행(Runs)" value={formatNumber(summary?.total)} detail={`체결(Filled) ${formatNumber(summary?.filledCount)}`} />
        <MetricCard label="매수 신호(Buy Signals)" value={formatNumber(summary?.buyCount)} detail={`매도(Sell) ${formatNumber(summary?.sellCount)}`} />
        <MetricCard label="익절/손절(TP/SL)" value={`${formatNumber(summary?.takeProfitCount)} / ${formatNumber(summary?.stopLossCount)}`} detail={`손실 매도(Loss sells) ${formatNumber(losses?.worstTrades.length)}`} />
        <MetricCard label="평균 익절률(Avg TP)" value={`${formatNumber(summary?.averageTakeProfitRate, 3)}%`} detail={`보유 종목(Positions) ${formatNumber(pnl?.positionCount)}`} />
      </div>

      <div className="section-grid">
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
            <h2>손익 요약(PnL Summary)</h2>
            {totalProfit >= 0 ? <TrendingUp className="tone-positive" size={22} /> : <TrendingDown className="tone-negative" size={22} />}
          </div>
          <dl className="definition-list">
            <dt>총 손익(Total)</dt>
            <dd className={totalProfitTone}>{formatKrw(pnl?.totalProfit)}</dd>
            <dt>평균 익절률(Avg TP)</dt>
            <dd>{`${formatNumber(summary?.averageTakeProfitRate, 3)}%`}</dd>
            <dt>평균 손절률(Avg SL)</dt>
            <dd className="tone-negative">{`${formatNumber(summary?.averageStopLossRate, 3)}%`}</dd>
            <dt>최근 범위(Range)</dt>
            <dd>{summary ? `${formatDateTime(summary.from)} - ${formatDateTime(summary.to)}` : '-'}</dd>
          </dl>
        </article>

        <article className="panel">
          <h2>안전장치(Safety)</h2>
          <div className="status-row">
            {data.safety.killSwitchEnabled ? <ShieldX size={22} /> : <ShieldCheck size={22} />}
            <span>{data.safety.killSwitchEnabled ? '거래 흐름 차단 중(Blocked)' : '자동 PAPER 거래 허용(Auto paper allowed)'}</span>
          </div>
        </article>

        <article className="panel">
          <h2>자동 실행(Schedulers)</h2>
          <dl className="definition-list">
            <dt>전략 실행(Trading)</dt>
            <dd>{data.scheduler.enabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>후보 실행(Candidate)</dt>
            <dd>{data.scheduler.candidateEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>후보 마켓(Candidate markets)</dt>
            <dd>{data.scheduler.candidateMarkets.length ? `전체 KRW(${data.scheduler.candidateMarkets.length})` : '-'}</dd>
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
      </div>
    </section>
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
