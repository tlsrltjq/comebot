import { useQuery } from '@tanstack/react-query';
import {
  CheckCircle2, MonitorCog, ShieldAlert,
  TrendingDown, TrendingUp, AlertCircle,
} from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { detectOperatingSystem, operatingSystemGuide } from '../../shared/os/operatingSystem';
import { cn } from '@/lib/utils';

const RANGE = '24h' as const;

export function DashboardPage() {
  const { exchange } = useExchangeMode();
  const { data, error, isLoading, isFetching, dataUpdatedAt } = useQuery({
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
    queryKey: queryKeys.analyticsSummary(RANGE, exchange),
    queryFn: () => api.analyticsSummary(RANGE, exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const { data: pnl } = useQuery({
    queryKey: queryKeys.analyticsPnl(RANGE, exchange),
    queryFn: () => api.analyticsPnl(RANGE, exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const { data: losses } = useQuery({
    queryKey: queryKeys.analyticsLosses(RANGE, exchange),
    queryFn: () => api.analyticsLosses(RANGE, exchange),
    refetchInterval: POLLING_INTERVALS.dashboard,
  });
  const { data: risk } = useQuery({
    queryKey: queryKeys.riskStatus(exchange),
    queryFn: () => api.riskStatus(exchange),
    refetchInterval: POLLING_INTERVALS.risk,
  });

  if (isLoading) return <div className="page"><p className="text-muted-foreground">상태를 불러오는 중...</p></div>;
  if (error || !data) return (
    <div className="page">
      <div className="error-panel">연결 실패 — 백엔드 서버를 확인해주세요.</div>
    </div>
  );

  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const money = (v: string | number | null | undefined) => formatCurrency(v, currency);
  const freshCount = exchange === 'BINANCE' ? providerQuery.data?.binanceFreshSnapshotCount : providerQuery.data?.upbitFreshSnapshotCount;
  const priceReady = providerQuery.data?.automationReady ?? Boolean(providerQuery.data?.externalProvider && (freshCount ?? 0) > 0);
  const schedulerReady = data.scheduler.candidateEnabled && data.scheduler.exitEnabled;
  const autoReady = data.database.connected && priceReady && schedulerReady && !data.safety.killSwitchEnabled;
  const killOn = data.safety.killSwitchEnabled;
  const totalProfit = Number(pnl?.totalProfit ?? 0);
  const portfolio = data.portfolio;

  const signalData = [
    { name: 'BUY', count: summary?.buyCount ?? 0 },
    { name: 'SELL', count: summary?.sellCount ?? 0 },
    { name: 'HOLD', count: summary?.holdCount ?? 0 },
  ];
  const osGuide = operatingSystemGuide(detectOperatingSystem());

  return (
    <div className="page">
      {/* ── Header ── */}
      <div className="page-header">
        <div>
          <h1 className="page-title">운영 대시보드</h1>
          <p className="page-subtitle">PAPER 자동 운영 상태 · 데이터 준비도 · 리스크 요약</p>
        </div>
        <div className="flex items-center gap-3">
          <ReadinessBadge ready={autoReady} kill={killOn} />
          <LiveStatusDot fetching={isFetching || providerQuery.isFetching} updatedAt={dataUpdatedAt} />
        </div>
      </div>

      {/* ── Top status cards ── */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6 mb-5">
        <StatusCard label="DB" ok={data.database.connected} value={data.database.connected ? '연결됨' : '오류'} />
        <StatusCard label="시세" ok={priceReady} value={priceReady ? `fresh ${freshCount ?? 0}` : '없음'} />
        <StatusCard label="후보 스캔" ok={data.scheduler.candidateEnabled} value={data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : 'OFF'} />
        <StatusCard label="청산 스캔" ok={data.scheduler.exitEnabled} value={data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : 'OFF'} />
        <StatusCard label="현금" ok={!portfolio.cashWarning} value={`${portfolio.remainingBuyCount}회`} />
        <StatusCard label="Kill Switch" ok={!killOn} value={killOn ? '🔴 ON' : '🟢 OFF'} />
      </div>

      {/* ── PnL metric grid ── */}
      <div className="metric-grid mb-2">
        <KpiCard label="총 평가금" value={money(pnl?.totalEquity)} sub={`현금 ${money(pnl?.cash)}`} />
        <KpiCard label="총 손익" value={money(pnl?.totalProfit)} sub={`실현 ${money(pnl?.realizedProfit)}`} tone={totalProfit >= 0 ? 'pos' : 'neg'} />
        <KpiCard label="포지션 평가" value={money(pnl?.totalPositionValue)} sub={`미실현 ${money(pnl?.unrealizedProfit)}`} />
        <KpiCard label="PAPER 현금" value={money(portfolio.cash)} sub={portfolio.cashWarning ? portfolio.cashWarningMessage : `주문 가능 ${portfolio.remainingBuyCount}회`} tone={portfolio.cashWarning ? 'neg' : undefined} />
      </div>

      {/* ── Analytics metric grid ── */}
      <div className="metric-grid mb-5">
        <KpiCard label="24h 실행" value={formatNumber(summary?.total)} sub={`체결 ${formatNumber(summary?.filledCount)}`} />
        <KpiCard label="매수 신호" value={formatNumber(summary?.buyCount)} sub={`매도 ${formatNumber(summary?.sellCount)}`} />
        <KpiCard label="익절/손절" value={`${formatNumber(summary?.takeProfitCount)} / ${formatNumber(summary?.stopLossCount)}`} sub={`손실매도 ${formatNumber(losses?.worstTrades.length)}`} />
        <KpiCard label="승률" value={`${formatNumber(summary?.winRate, 2)}%`} sub={`손익비 ${formatNumber(summary?.profitLossRatio, 2)}`} />
      </div>

      {/* ── Main panels ── */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3">

        {/* 운영 준비 */}
        <Panel title="운영 준비 상태">
          <div className="space-y-1.5">
            <CheckRow label="DB 연결" value={data.database.connected ? '연결됨' : '끊김'} ok={data.database.connected} />
            <CheckRow label="시세" value={`${providerQuery.data?.provider ?? '-'} / fresh ${freshCount ?? 0}`} ok={priceReady} />
            <CheckRow label="후보 스케줄러" value={data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐'} ok={data.scheduler.candidateEnabled} />
            <CheckRow label="청산 스케줄러" value={data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐'} ok={data.scheduler.exitEnabled} />
            <CheckRow label="PAPER 현금" value={`${portfolio.remainingBuyCount}회 / ${formatNumber(portfolio.cashRate, 1)}%`} ok={!portfolio.cashWarning} />
            <CheckRow label="Kill Switch" value={killOn ? '켜짐 ⚠' : '꺼짐'} ok={!killOn} />
          </div>
        </Panel>

        {/* 손익 요약 */}
        <Panel title="손익 요약" right={totalProfit >= 0 ? <TrendingUp size={18} className="text-green-600" /> : <TrendingDown size={18} className="text-red-500" />}>
          <Dl rows={[
            ['총 손익', <span className={cn('num font-semibold', totalProfit >= 0 ? 'pos' : 'neg')}>{money(pnl?.totalProfit)}</span>],
            ['평균 익절률', `${formatNumber(summary?.averageTakeProfitRate, 3)}%`],
            ['평균 손절률', `${formatNumber(summary?.averageStopLossRate, 3)}%`],
            ['평균 보유', formatDuration(summary?.averageHoldingSeconds)],
            ['기간', summary ? `${formatDateTime(summary.from)} ~` : '-'],
          ]} />
        </Panel>

        {/* 리스크 */}
        <Panel title="리스크 요약" right={<ShieldAlert size={18} className="text-amber-500" />}>
          <Dl rows={[
            ['쏠림 차단', risk?.concentration ? `${formatNumber(risk.concentration.warningExposureRate, 0)}% / ${formatNumber(risk.concentration.blockExposureRate, 0)}%` : '-'],
            ['SL Cooldown', risk?.stopLossCooldown?.enabled ? `${risk.stopLossCooldown.triggerCount}회 / ${risk.stopLossCooldown.duration}` : '꺼짐'],
            ['손실 매도 (24h)', formatNumber(losses?.worstTrades.length)],
            ['Kill Switch', killOn ? '켜짐 🔴' : '꺼짐 🟢'],
          ]} />
        </Panel>

        {/* 24h 신호 차트 */}
        <Panel title="24h 신호 분포">
          <div className="h-44">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={signalData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(220 13% 91%)" />
                <XAxis dataKey="name" tickLine={false} axisLine={false} tick={{ fontSize: 12 }} />
                <YAxis allowDecimals={false} tickLine={false} axisLine={false} tick={{ fontSize: 11 }} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 6 }} />
                <Bar dataKey="count" fill="hsl(221 83% 53%)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Panel>

        {/* 손실 점검 */}
        <Panel title="손실 점검" right={
          losses?.worstTrades.length
            ? <span className="text-xs text-amber-600 font-semibold">점검 필요</span>
            : <span className="text-xs text-green-600 font-semibold">없음</span>
        }>
          {losses?.worstTrades.length ? (
            <div className="space-y-2">
              {losses.worstTrades.slice(0, 4).map((t) => (
                <div key={`${t.market}-${t.createdAt}`} className="flex items-center justify-between text-sm">
                  <span className="font-medium">{t.market}</span>
                  <span className="neg num text-xs">{formatNumber(t.rate, 3)}%</span>
                  <span className="text-xs text-muted-foreground">{formatDateTime(t.createdAt)}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">최근 24시간 손실 매도 없음.</p>
          )}
        </Panel>

        {/* 스케줄러 */}
        <Panel title="스케줄러">
          <Dl rows={[
            ['후보 스캔', data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐'],
            ['후보 거래소', exchangeList(data.scheduler.candidateExchanges, data.scheduler.candidateExchange)],
            ['후보 마켓', marketSummary(data.scheduler.candidateMarkets, exchange)],
            ['청산 스캔', data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐'],
            ['청산 대상', `${data.scheduler.exitPositionMarketCount}개`],
          ]} />
        </Panel>

        {/* 운영 제약 */}
        <Panel title="운영 제약">
          <Dl rows={[
            ['전략', data.strategy.strategyName.replace('Strategy', '')],
            ['1회 주문', `${formatNumber(data.strategy.orderAmount, currency === 'USDT' ? 2 : 0)} ${currency}`],
            ['최대 주문', money(data.risk.maxOrderAmount)],
            ['허용 마켓', marketSummary(data.risk.allowedMarkets, exchange)],
            ['수동 실행', data.telegram.manualPaperExecutionEnabled ? '허용' : '차단'],
          ]} />
        </Panel>

        {/* 안전/알림/텔레그램 */}
        <Panel title="안전 · 알림 · 텔레그램">
          <Dl rows={[
            ['Kill Switch', killOn ? '켜짐 🔴' : '꺼짐 🟢'],
            ['알림 사용', data.notification.enabled ? '예' : '아니오'],
            ['체결 알림', data.notification.sendFilled ? '전송' : '건너뜀'],
            ['Telegram', data.telegram.enabled ? '켜짐' : '꺼짐'],
            ['Telegram 수신', data.telegram.inboundEnabled ? '켜짐' : '꺼짐'],
          ]} />
        </Panel>

        {/* OS 가이드 */}
        <Panel title="운영 환경" right={<MonitorCog size={16} className="text-muted-foreground" />}>
          <Dl rows={[
            ['OS', osGuide.label],
            ['Shell', osGuide.shell],
            ['실행', <code className="text-xs bg-muted px-1 py-0.5 rounded">{osGuide.runScript}</code>],
          ]} />
        </Panel>
      </div>
    </div>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function ReadinessBadge({ ready, kill }: { ready: boolean; kill: boolean }) {
  if (kill) return <span className="badge badge-destructive">🔴 거래 차단</span>;
  if (ready) return <span className="badge badge-success">🟢 운영 가능</span>;
  return <span className="badge badge-warning">🟡 점검 필요</span>;
}

function LiveStatusDot({ fetching, updatedAt }: { fetching: boolean; updatedAt: number }) {
  return (
    <div className="live-status">
      <span className={cn('status-dot', fetching ? 'warn' : 'live')} />
      {updatedAt ? new Date(updatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''}
    </div>
  );
}

function StatusCard({ label, ok, value }: { label: string; ok: boolean; value: string }) {
  return (
    <div className={cn(
      'rounded-lg border px-3 py-2.5 text-center',
      ok ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50',
    )}>
      <div className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-0.5">{label}</div>
      <div className={cn('text-sm font-semibold', ok ? 'text-green-700' : 'text-red-600')}>{value}</div>
    </div>
  );
}

function KpiCard({ label, value, sub, tone }: { label: string; value: string; sub?: string; tone?: 'pos' | 'neg' }) {
  return (
    <div className="metric-card">
      <span>{label}</span>
      <strong className={cn('num', tone === 'pos' && 'pos', tone === 'neg' && 'neg')}>{value}</strong>
      {sub && <small>{sub}</small>}
    </div>
  );
}

function Panel({ title, right, children }: { title: string; right?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="section">
      <div className="flex items-center justify-between mb-3">
        <h2 className="section-title mb-0">{title}</h2>
        {right}
      </div>
      {children}
    </div>
  );
}

function CheckRow({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  return (
    <div className="flex items-center gap-2 text-sm py-0.5">
      {ok
        ? <CheckCircle2 size={14} className="text-green-600 shrink-0" />
        : <AlertCircle size={14} className="text-red-500 shrink-0" />}
      <span className="text-muted-foreground w-28 shrink-0">{label}</span>
      <span className={cn('font-medium', ok ? 'text-foreground' : 'text-red-500')}>{value}</span>
    </div>
  );
}

function Dl({ rows }: { rows: [string, React.ReactNode][] }) {
  return (
    <dl className="space-y-1.5">
      {rows.map(([label, value]) => (
        <div key={label} className="flex items-start gap-2 text-sm">
          <dt className="w-28 shrink-0 text-muted-foreground">{label}</dt>
          <dd className="font-medium break-all">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function marketSummary(markets: string[], exchange: string) {
  if (markets.length === 0) return '-';
  if (markets.includes('ALL_KRW')) return '전체 KRW';
  if (markets.includes('ALL_USDT')) return '전체 USDT';
  const preview = markets.slice(0, 3).join(', ');
  return markets.length > 3 ? `${preview} 외 ${markets.length - 3}개` : preview || exchange;
}

function exchangeList(exchanges: string[] | undefined, fallback: string) {
  return exchanges && exchanges.length > 0 ? exchanges.join(', ') : fallback;
}

function formatDuration(seconds: number | null | undefined) {
  if (!seconds) return '-';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

// React import for JSX
import React from 'react';
