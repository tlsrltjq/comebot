import { useMemo, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { NavLink, Outlet, useSearchParams } from 'react-router-dom';
import {
  Activity, BarChart3, BookOpen, BrainCircuit, Clock3, DollarSign,
  LineChart, MonitorCog, PieChart, Radar, Radio,
  ShieldCheck, ShieldX, TrendingUp, TriangleAlert, Wallet, Database,
} from 'lucide-react';
import { api, queryKeys } from '../shared/api/client';
import { POLLING_INTERVALS } from '../shared/api/polling';
import type { ExchangeMode } from '../shared/api/types';
import { ExchangeModeContext, exchangeParam } from '../shared/exchange/ExchangeModeContext';
import { cn } from '@/lib/utils';

// ── nav ──────────────────────────────────────────────────────────────────────

const NAV_GROUPS = [
  {
    label: '시장',
    items: [
      { to: '/', label: '대시보드', icon: Activity },
      { to: '/market', label: '시장 차트', icon: TrendingUp },
      { to: '/fund-flow', label: '자금 흐름', icon: DollarSign },
      { to: '/sentiment', label: '시장 심리', icon: BrainCircuit },
    ],
  },
  {
    label: '전략',
    items: [
      { to: '/candidates', label: '매수 후보', icon: Radar },
      { to: '/portfolio', label: '포트폴리오', icon: PieChart },
      { to: '/history', label: '거래 이력', icon: Clock3 },
      { to: '/trade-journal', label: '매매 일지', icon: BookOpen },
    ],
  },
  {
    label: '운영',
    items: [
      { to: '/risk', label: '리스크', icon: TriangleAlert },
      { to: '/system', label: '시스템', icon: MonitorCog },
      { to: '/trade', label: '자동 실행', icon: LineChart },
    ],
  },
];

// ── App ───────────────────────────────────────────────────────────────────────

export function App() {
  const [searchParams, setSearchParams] = useSearchParams();
  const exchange = parseExchange(searchParams.get('exchange'));

  const exchangeContext = useMemo(
    () => ({
      exchange,
      setExchange: (next: ExchangeMode) => {
        const p = new URLSearchParams(searchParams);
        p.set('exchange', exchangeParam(next));
        setSearchParams(p, { replace: true });
      },
    }),
    [exchange, searchParams, setSearchParams],
  );

  return (
    <ExchangeModeContext.Provider value={exchangeContext}>
      <div className="app-shell">
        {/* ── Sidebar ── */}
        <aside className="sidebar">
          {/* Brand */}
          <div className="sidebar-brand">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground shrink-0">
              <BarChart3 size={16} />
            </div>
            <div>
              <strong>Comebot</strong>
              <span>PAPER_TRADING</span>
            </div>
          </div>

          {/* Exchange toggle */}
          <div className="exchange-switch">
            {(['UPBIT', 'BINANCE'] as const).map((ex) => (
              <button
                key={ex}
                type="button"
                aria-pressed={exchange === ex}
                className={exchange === ex ? 'active' : ''}
                onClick={() => exchangeContext.setExchange(ex)}
              >
                {ex}
              </button>
            ))}
          </div>

          {/* Navigation */}
          <nav className="sidebar-nav">
            {NAV_GROUPS.map((group) => (
              <div key={group.label} className="mb-3">
                <div className="px-2 pb-1 text-[10px] font-semibold uppercase tracking-widest text-muted-foreground/60">
                  {group.label}
                </div>
                {group.items.map(({ to, label, icon: Icon }) => (
                  <NavLink
                    key={to}
                    to={{ pathname: to, search: `?exchange=${exchangeParam(exchange)}` }}
                    end={to === '/'}
                    className={({ isActive }) => cn('nav-item', isActive && 'active')}
                  >
                    <Icon size={15} aria-hidden />
                    {label}
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>

          {/* Footer */}
          <div className="sidebar-footer">
            v{new Date().getFullYear()} · PAPER only
          </div>
        </aside>

        {/* ── Main ── */}
        <div className="main-content">
          <StatusBar exchange={exchange} />
          <Outlet />
        </div>
      </div>
    </ExchangeModeContext.Provider>
  );
}

// ── Status bar ────────────────────────────────────────────────────────────────

function StatusBar({ exchange }: { exchange: ExchangeMode }) {
  const { data: system, isLoading: sysLoading } = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: POLLING_INTERVALS.status,
  });
  const { data: provider, isLoading: provLoading } = useQuery({
    queryKey: queryKeys.marketProviderStatus(),
    queryFn: api.marketProviderStatus,
    refetchInterval: POLLING_INTERVALS.status,
  });

  const loading = sysLoading || provLoading;
  const snapshotCount = exchange === 'BINANCE' ? provider?.binanceSnapshotCount : provider?.upbitSnapshotCount;
  const priceReady = Boolean(provider?.externalProvider && (snapshotCount ?? 0) > 0);
  const killOn = Boolean(system?.safety.killSwitchEnabled);
  const dbOk = Boolean(system?.database.connected);
  const cashWarn = Boolean(system?.portfolio.cashWarning);

  return (
    <header aria-label="운영 상태 바(Operation status bar)" className="flex items-center gap-3 border-b border-border bg-card px-5 py-2.5 text-xs">
      {/* exchange badge */}
      <span className="rounded bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
        {exchange}
      </span>

      <div className="flex items-center gap-4 ml-1">
        <Pill icon={<Database size={12} />} label="DB" ok={dbOk} text={loading ? '…' : dbOk ? '연결' : '오류'} />
        <Pill icon={<Radio size={12} />} label="시세" ok={priceReady} text={loading ? '…' : priceReady ? `${snapshotCount}` : '없음'} />
        <Pill icon={<Activity size={12} />} label="후보" ok={Boolean(system?.scheduler.candidateEnabled)}
          text={loading ? '…' : system?.scheduler.candidateEnabled ? `${(system.scheduler.candidateFixedDelayMs ?? 0) / 1000}s` : 'OFF'} />
        <Pill icon={<TrendingUp size={12} />} label="청산" ok={Boolean(system?.scheduler.exitEnabled)}
          text={loading ? '…' : system?.scheduler.exitEnabled ? `${(system.scheduler.exitFixedDelayMs ?? 0) / 1000}s` : 'OFF'} />
        <Pill icon={<Wallet size={12} />} label="현금" ok={!cashWarn && !sysLoading}
          text={loading ? '…' : `${system?.portfolio.remainingBuyCount ?? 0}회`} />
        <Pill
          icon={killOn ? <ShieldX size={12} /> : <ShieldCheck size={12} />}
          label="Kill" ok={!killOn} text={killOn ? 'ON' : 'OFF'}
        />
      </div>
    </header>
  );
}

function Pill({ icon, label, ok, text }: { icon: ReactNode; label: string; ok: boolean; text: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <span className={cn('text-xs', ok ? 'text-green-600' : 'text-destructive')}>{icon}</span>
      <span className="text-muted-foreground">{label}</span>
      <span className={cn('font-medium', ok ? 'text-foreground' : 'text-destructive')}>{text}</span>
    </div>
  );
}

function parseExchange(value: string | null): ExchangeMode {
  return value?.toUpperCase() === 'BINANCE' ? 'BINANCE' : 'UPBIT';
}
