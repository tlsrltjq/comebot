import { useQuery } from '@tanstack/react-query';
import { Activity, CheckCircle2, MonitorCog, ShieldAlert, ShieldCheck, ShieldX, TrendingDown, TrendingUp } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { detectOperatingSystem, operatingSystemGuide } from '../../shared/os/operatingSystem';

const DASHBOARD_RANGE = '24h' as const;
export function DashboardPage() {
  const { exchange } = useExchangeMode();
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const providerQuery = useQuery({
    queryKey: queryKeys.marketProviderStatus(),
    queryFn: api.marketProviderStatus,
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const { data: summary } = useQuery({
    queryKey: queryKeys.analyticsSummary(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsSummary(DASHBOARD_RANGE, exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const { data: pnl } = useQuery({
    queryKey: queryKeys.analyticsPnl(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsPnl(DASHBOARD_RANGE, exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const { data: losses } = useQuery({
    queryKey: queryKeys.analyticsLosses(DASHBOARD_RANGE, exchange),
    queryFn: () => api.analyticsLosses(DASHBOARD_RANGE, exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const riskQuery = useQuery({
    queryKey: queryKeys.riskStatus(exchange),
    queryFn: () => api.riskStatus(exchange),
    refetchInterval: POLLING_INTERVALS.risk,
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
  const dataReadiness = dataReadinessStatus({
    totalRuns: summary?.total ?? 0,
    buyCount: summary?.buyCount ?? 0,
    sellCount: summary?.sellCount ?? 0,
    filledCount: summary?.filledCount ?? 0,
    positionCount: pnl?.positionCount ?? 0,
    exitPositionMarketCount: data.scheduler.exitPositionMarketCount,
  });
  const osGuide = operatingSystemGuide(detectOperatingSystem());
  const concentration = riskQuery.data?.concentration;
  const stopLossCooldown = riskQuery.data?.stopLossCooldown;
  const portfolioCash = data.portfolio;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>운영 대시보드(Dashboard)</h1>
          <p>자동 PAPER 운영 가능 여부, 데이터 준비도, 리스크 상태를 한 화면에서 판단합니다.</p>
        </div>
        <Badge tone={readinessTone}>
          {readinessTitle}
        </Badge>
        <LiveStatus updatedAt={dataUpdatedAt} isFetching={isFetching || providerQuery.isFetching} intervalMs={POLLING_INTERVALS.dashboard} />
      </header>

      <div className="control-room-grid" aria-label="운영 관제(Control room)">
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
            <OperationCheck label="PAPER 현금(Cash)" value={`${portfolioCash.remainingBuyCount}회 가능 / ${formatNumber(portfolioCash.cashRate, 1)}%`} good={!portfolioCash.cashWarning} />
            <OperationCheck label="긴급 정지(Kill switch)" value={data.safety.killSwitchEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'} good={!data.safety.killSwitchEnabled} />
          </div>
        </article>

        <article className={`data-readiness-panel data-readiness-${dataReadiness.tone}`}>
          <div className="data-readiness-head">
            <div>
              <span>데이터 준비 상태(Data Readiness)</span>
              <strong>{dataReadiness.title}</strong>
              <p>{dataReadiness.detail}</p>
            </div>
            <Badge tone={dataReadiness.tone}>{dataReadiness.badge}</Badge>
          </div>
          <div className="data-readiness-grid" aria-label="데이터 준비 지표(Data readiness metrics)">
            <ReadinessMetric label="24h 실행(Runs)" value={formatNumber(summary?.total)} />
            <ReadinessMetric label="체결(Filled)" value={formatNumber(summary?.filledCount)} />
            <ReadinessMetric label="BUY / SELL" value={`${formatNumber(summary?.buyCount)} / ${formatNumber(summary?.sellCount)}`} />
            <ReadinessMetric label="보유(Position)" value={formatNumber(pnl?.positionCount)} />
            <ReadinessMetric label="청산 대상(Exit)" value={formatNumber(data.scheduler.exitPositionMarketCount)} />
          </div>
        </article>

        <RiskSummaryPanel
          concentration={concentration}
          stopLossCooldown={stopLossCooldown}
          lossCount={losses?.worstTrades.length ?? 0}
          killSwitchEnabled={data.safety.killSwitchEnabled}
        />
      </div>

      <div className="metric-grid">
        <MetricCard label="총 평가금(Total Equity)" value={money(pnl?.totalEquity)} detail={`현금(Cash) ${money(pnl?.cash)}`} />
        <MetricCard label="총 손익(Total PnL)" value={money(pnl?.totalProfit)} detail={`실현(Realized) ${money(pnl?.realizedProfit)}`} />
        <MetricCard label="보유 평가(Position Value)" value={money(pnl?.totalPositionValue)} detail={`미실현(Unrealized) ${money(pnl?.unrealizedProfit)}`} />
        <MetricCard
          label="PAPER 현금(Cash)"
          value={money(portfolioCash.cash)}
          detail={portfolioCash.cashWarning ? portfolioCash.cashWarningMessage : `주문 가능(Available) ${portfolioCash.remainingBuyCount}회`}
        />
      </div>

      <div className="metric-grid">
        <MetricCard label="24시간 실행(Runs)" value={formatNumber(summary?.total)} detail={`체결(Filled) ${formatNumber(summary?.filledCount)}`} />
        <MetricCard label="매수 신호(Buy Signals)" value={formatNumber(summary?.buyCount)} detail={`매도(Sell) ${formatNumber(summary?.sellCount)}`} />
        <MetricCard label="익절/손절(TP/SL)" value={`${formatNumber(summary?.takeProfitCount)} / ${formatNumber(summary?.stopLossCount)}`} detail={`손실 매도(Loss sells) ${formatNumber(losses?.worstTrades.length)}`} />
        <MetricCard label="승률(Win Rate)" value={`${formatNumber(summary?.winRate, 2)}%`} detail={`손익비(P/L) ${formatNumber(summary?.profitLossRatio, 2)}`} />
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
            <dt>평균 보유 시간(Avg Holding)</dt>
            <dd>{formatDuration(summary?.averageHoldingSeconds)}</dd>
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
          <div className="panel-title-row">
            <h2>운영 환경(OS Guide)</h2>
            <MonitorCog size={20} />
          </div>
          <dl className="definition-list">
            <dt>감지(OS)</dt>
            <dd>{osGuide.label}</dd>
            <dt>Shell</dt>
            <dd>{osGuide.shell}</dd>
            <dt>실행(Run)</dt>
            <dd><code>{osGuide.runScript}</code></dd>
            <dt>스키마(Schema)</dt>
            <dd><code>{osGuide.schemaScript}</code></dd>
            <dt>작업 경로(Path)</dt>
            <dd><code>{osGuide.workspacePath}</code></dd>
            <dt>단축키(Shortcut)</dt>
            <dd>{osGuide.shortcutModifier}</dd>
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

function ReadinessMetric({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="data-readiness-item">
      <Activity size={16} />
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function RiskSummaryPanel({
  concentration,
  stopLossCooldown,
  lossCount,
  killSwitchEnabled,
}: {
  concentration: {
    exchange: string;
    enabled: boolean;
    warningExposureRate: string;
    blockExposureRate: string;
  } | undefined;
  stopLossCooldown: {
    enabled: boolean;
    window: string;
    triggerCount: number;
    duration: string;
  } | undefined;
  lossCount: number;
  killSwitchEnabled: boolean;
}) {
  const hasWarning = killSwitchEnabled || lossCount > 0 || Boolean(concentration?.enabled) || Boolean(stopLossCooldown?.enabled);
  return (
    <article className={`risk-summary-panel risk-summary-${hasWarning ? 'warn' : 'good'}`}>
      <div className="risk-summary-head">
        <div>
          <span>리스크 요약(Risk Summary)</span>
          <strong>{hasWarning ? '점검 항목 있음(Review)' : '차단 없음(Clear)'}</strong>
          <p>쏠림, 반복 손절, 손실 매도, kill switch 상태를 함께 봅니다.</p>
        </div>
        {hasWarning ? <ShieldAlert size={30} /> : <ShieldCheck size={30} />}
      </div>
      <dl className="risk-summary-list">
        <div>
          <dt>쏠림(Concentration)</dt>
          <dd>{concentration ? `${concentration.exchange} ${formatNumber(concentration.warningExposureRate, 0)}% / ${formatNumber(concentration.blockExposureRate, 0)}%` : '-'}</dd>
        </div>
        <div>
          <dt>Cooldown</dt>
          <dd>{stopLossCooldown?.enabled ? `${stopLossCooldown.triggerCount}회 / ${stopLossCooldown.duration}` : '꺼짐(Disabled)'}</dd>
        </div>
        <div>
          <dt>손실 매도(Loss sells)</dt>
          <dd>{formatNumber(lossCount)}</dd>
        </div>
        <div>
          <dt>Kill switch</dt>
          <dd>{killSwitchEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
        </div>
      </dl>
    </article>
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

type DataReadinessTone = 'good' | 'warn' | 'info';

function dataReadinessStatus({
  totalRuns,
  buyCount,
  sellCount,
  filledCount,
  positionCount,
  exitPositionMarketCount,
}: {
  totalRuns: number;
  buyCount: number;
  sellCount: number;
  filledCount: number;
  positionCount: number;
  exitPositionMarketCount: number;
}): {
  tone: DataReadinessTone;
  title: string;
  badge: string;
  detail: string;
} {
  if (totalRuns === 0 && positionCount === 0 && exitPositionMarketCount === 0) {
    return {
      tone: 'warn',
      title: '데이터 부족(Insufficient)',
      badge: '보류(Hold)',
      detail: '아직 24시간 실행 기록과 보유 PAPER 포지션이 없어 손익 판단은 보류합니다.',
    };
  }

  if (filledCount === 0 && positionCount === 0) {
    return {
      tone: 'warn',
      title: '수집 중(Collecting)',
      badge: '수집(Collect)',
      detail: '후보/HOLD 기록은 있으나 체결 또는 보유 포지션 표본이 부족합니다.',
    };
  }

  if (buyCount > 0 && sellCount === 0) {
    return {
      tone: 'info',
      title: '진입 검증 중(Entry Ready)',
      badge: '진입(Entry)',
      detail: 'BUY/보유 데이터는 있으나 SELL/청산 표본이 아직 부족합니다.',
    };
  }

  return {
    tone: 'good',
    title: '검증 가능(Reviewable)',
    badge: '검토 가능',
    detail: 'BUY/SELL/FILLED 데이터가 있어 PAPER 흐름 검토가 가능합니다.',
  };
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

function formatDuration(seconds: number | null | undefined) {
  if (!seconds) {
    return '-';
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  return `${minutes}m`;
}
