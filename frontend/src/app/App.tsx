import { useMemo, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { NavLink, Outlet, useSearchParams } from 'react-router-dom';
import { Activity, BarChart3, BrainCircuit, Clock3, Database, DollarSign, LineChart, MonitorCog, PieChart, Radar, Radio, ShieldCheck, ShieldX, TrendingUp, TriangleAlert, Wallet } from 'lucide-react';
import { api, queryKeys } from '../shared/api/client';
import { POLLING_INTERVALS } from '../shared/api/polling';
import type { ExchangeMode } from '../shared/api/types';
import { ExchangeModeContext, exchangeParam } from '../shared/exchange/ExchangeModeContext';
import { LiveStatus } from '../shared/ui/LiveStatus';

const navItems = [
  { to: '/', label: '대시보드(Dashboard)', icon: Activity },
  { to: '/market', label: '시장(Markets)', icon: TrendingUp },
  { to: '/fund-flow', label: '자금흐름(Flow)', icon: DollarSign },
  { to: '/sentiment', label: '시장심리(Risk)', icon: BrainCircuit },
  { to: '/candidates', label: '후보(Candidates)', icon: Radar },
  { to: '/portfolio', label: '포트폴리오(Portfolio)', icon: PieChart },
  { to: '/history', label: '이력(History)', icon: Clock3 },
  { to: '/risk', label: '리스크(Risk)', icon: TriangleAlert },
  { to: '/system', label: '시스템(System)', icon: MonitorCog },
  { to: '/trade', label: '자동 실행(Auto Run)', icon: LineChart },
];

export function App() {
  const [searchParams, setSearchParams] = useSearchParams();
  const exchange = parseExchange(searchParams.get('exchange'));
  const exchangeContext = useMemo(
    () => ({
      exchange,
      setExchange: (nextExchange: ExchangeMode) => {
        const nextParams = new URLSearchParams(searchParams);
        nextParams.set('exchange', exchangeParam(nextExchange));
        setSearchParams(nextParams, { replace: true });
      },
    }),
    [exchange, searchParams, setSearchParams],
  );

  return (
    <ExchangeModeContext.Provider value={exchangeContext}>
      <div className="shell">
        <aside className="sidebar">
          <div className="brand">
            <BarChart3 size={26} aria-hidden />
            <div>
              <strong>Comebot</strong>
              <span>PAPER_TRADING</span>
            </div>
          </div>
          <div className="exchange-switch" aria-label="거래소 모드(Exchange mode)">
            {(['UPBIT', 'BINANCE'] as const).map((item) => (
              <button
                key={item}
                type="button"
                className={exchange === item ? 'active' : ''}
                onClick={() => exchangeContext.setExchange(item)}
                aria-pressed={exchange === item}
              >
                {item}
              </button>
            ))}
          </div>
          <nav className="nav" aria-label="주요 메뉴">
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={{ pathname: to, search: `?exchange=${exchangeParam(exchange)}` }}
                end={to === '/'}
                className={({ isActive }) => (isActive ? 'active' : '')}
              >
                <Icon size={18} aria-hidden />
                {label}
              </NavLink>
            ))}
          </nav>
        </aside>
        <div className="workspace">
          <TopStatusBar exchange={exchange} />
          <main className="content">
            <Outlet />
          </main>
        </div>
      </div>
    </ExchangeModeContext.Provider>
  );
}

function TopStatusBar({ exchange }: { exchange: ExchangeMode }) {
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: POLLING_INTERVALS.status,
  });
  const providerQuery = useQuery({
    queryKey: queryKeys.marketProviderStatus(),
    queryFn: api.marketProviderStatus,
    refetchInterval: POLLING_INTERVALS.status,
  });
  const system = systemQuery.data;
  const provider = providerQuery.data;
  const snapshotCount = exchange === 'BINANCE' ? provider?.binanceSnapshotCount : provider?.upbitSnapshotCount;
  const priceReady = Boolean(provider?.externalProvider && (snapshotCount ?? 0) > 0);
  const candidateReady = Boolean(system?.scheduler.candidateEnabled);
  const exitReady = Boolean(system?.scheduler.exitEnabled);
  const killSwitchEnabled = Boolean(system?.safety.killSwitchEnabled);
  const databaseReady = Boolean(system?.database.connected);
  const cashWarning = Boolean(system?.portfolio.cashWarning);
  const updatedAt = Math.max(systemQuery.dataUpdatedAt, providerQuery.dataUpdatedAt);

  return (
    <header className="top-status-bar" aria-label="운영 상태 바(Operation status bar)">
      <div className="top-status-main">
        <strong>{exchange}</strong>
        <span>PAPER_TRADING</span>
      </div>
      <div className="top-status-grid">
        <TopStatusItem icon={<Database size={15} />} label="DB" value={databaseReady ? '연결' : statusFallback(systemQuery.isLoading)} good={databaseReady} />
        <TopStatusItem icon={<Radio size={15} />} label="시세" value={priceReady ? `${provider?.provider ?? '-'} ${snapshotCount ?? 0}` : statusFallback(providerQuery.isLoading)} good={priceReady} />
        <TopStatusItem icon={<Activity size={15} />} label="후보" value={candidateReady ? `${(system?.scheduler.candidateFixedDelayMs ?? 0) / 1000}s` : statusFallback(systemQuery.isLoading)} good={candidateReady} />
        <TopStatusItem icon={<TrendingUp size={15} />} label="청산" value={exitReady ? `${(system?.scheduler.exitFixedDelayMs ?? 0) / 1000}s` : statusFallback(systemQuery.isLoading)} good={exitReady} />
        <TopStatusItem icon={<Wallet size={15} />} label="현금" value={system ? `${system.portfolio.remainingBuyCount}회` : statusFallback(systemQuery.isLoading)} good={!cashWarning && !systemQuery.isError} />
        <TopStatusItem icon={killSwitchEnabled ? <ShieldX size={15} /> : <ShieldCheck size={15} />} label="Kill" value={killSwitchEnabled ? 'ON' : 'OFF'} good={!killSwitchEnabled && !systemQuery.isError} />
      </div>
      <LiveStatus updatedAt={updatedAt} isFetching={systemQuery.isFetching || providerQuery.isFetching} intervalMs={POLLING_INTERVALS.status} />
    </header>
  );
}

function TopStatusItem({
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
    <div className={`top-status-item ${good ? 'top-status-item-good' : 'top-status-item-warn'}`}>
      {icon}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function statusFallback(isLoading: boolean) {
  return isLoading ? '확인 중' : '점검';
}

function parseExchange(value: string | null): ExchangeMode {
  return value?.toUpperCase() === 'BINANCE' ? 'BINANCE' : 'UPBIT';
}
