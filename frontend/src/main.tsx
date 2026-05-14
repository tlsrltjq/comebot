import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { App } from './app/App';
import { CandidatesPage } from './features/candidates/CandidatesPage';
import { HistoryPage } from './features/history/HistoryPage';
import { MarketOverviewPage } from './features/market/MarketOverviewPage';
import { PortfolioPage } from './features/portfolio/PortfolioPage';
import { RiskPage } from './features/risk/RiskPage';
import { DashboardPage } from './features/system/DashboardPage';
import { SystemPage } from './features/system/SystemPage';
import { TradePage } from './features/trading/TradePage';
import './styles.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 2_000,
      retry: 1,
      refetchIntervalInBackground: true,
      refetchOnReconnect: 'always',
      refetchOnWindowFocus: 'always',
    },
  },
});

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'market', element: <MarketOverviewPage /> },
      { path: 'candidates', element: <CandidatesPage /> },
      { path: 'trade', element: <TradePage /> },
      { path: 'portfolio', element: <PortfolioPage /> },
      { path: 'history', element: <HistoryPage /> },
      { path: 'risk', element: <RiskPage /> },
      { path: 'system', element: <SystemPage /> },
    ],
  },
]);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
);
