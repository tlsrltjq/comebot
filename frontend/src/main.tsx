import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { App } from './app/App';
import { BinancePage } from './features/binance/BinancePage';
import { CandidatesPage } from './features/candidates/CandidatesPage';
import { HistoryPage } from './features/history/HistoryPage';
import { Mvp2Page } from './features/mvp2/Mvp2Page';
import { PortfolioPage } from './features/portfolio/PortfolioPage';
import { DashboardPage } from './features/system/DashboardPage';
import { TradePage } from './features/trading/TradePage';
import './styles.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 10_000,
      retry: 1,
    },
  },
});

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'candidates', element: <CandidatesPage /> },
      { path: 'trade', element: <TradePage /> },
      { path: 'portfolio', element: <PortfolioPage /> },
      { path: 'history', element: <HistoryPage /> },
      { path: 'mvp2', element: <Mvp2Page /> },
      { path: 'binance', element: <BinancePage /> },
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
