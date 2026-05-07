import { useMemo } from 'react';
import { NavLink, Outlet, useSearchParams } from 'react-router-dom';
import { Activity, BarChart3, Clock3, LineChart, PieChart, Radar } from 'lucide-react';
import type { ExchangeMode } from '../shared/api/types';
import { ExchangeModeContext, exchangeParam } from '../shared/exchange/ExchangeModeContext';

const navItems = [
  { to: '/', label: '대시보드(Dashboard)', icon: Activity },
  { to: '/candidates', label: '후보(Candidates)', icon: Radar },
  { to: '/trade', label: '자동 실행(Auto Run)', icon: LineChart },
  { to: '/portfolio', label: '포트폴리오(Portfolio)', icon: PieChart },
  { to: '/history', label: '이력(History)', icon: Clock3 },
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
        <main className="content">
          <Outlet />
        </main>
      </div>
    </ExchangeModeContext.Provider>
  );
}

function parseExchange(value: string | null): ExchangeMode {
  return value?.toUpperCase() === 'BINANCE' ? 'BINANCE' : 'UPBIT';
}
