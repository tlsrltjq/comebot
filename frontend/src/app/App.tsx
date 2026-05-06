import { NavLink, Outlet } from 'react-router-dom';
import { Activity, BarChart3, Clock3, LineChart, PieChart, Radar } from 'lucide-react';

const navItems = [
  { to: '/', label: '대시보드(Dashboard)', icon: Activity },
  { to: '/candidates', label: '후보(Candidates)', icon: Radar },
  { to: '/trade', label: '자동 실행(Auto Run)', icon: LineChart },
  { to: '/portfolio', label: '포트폴리오(Portfolio)', icon: PieChart },
  { to: '/history', label: '이력(History)', icon: Clock3 },
];

export function App() {
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <BarChart3 size={26} aria-hidden />
          <div>
            <strong>Comebot</strong>
            <span>PAPER_TRADING</span>
          </div>
        </div>
        <nav className="nav" aria-label="주요 메뉴">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>
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
  );
}
