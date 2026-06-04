import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { App } from './app/App';
import './styles.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5_000,
      retry: 1,
      refetchIntervalInBackground: false,
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
      {
        index: true,
        lazy: async () => {
          const { DashboardPage } = await import('./features/system/DashboardPage');
          return { Component: DashboardPage };
        },
      },
      {
        path: 'market',
        lazy: async () => {
          const { MarketOverviewPage } = await import('./features/market/MarketOverviewPage');
          return { Component: MarketOverviewPage };
        },
      },
      {
        path: 'candidates',
        lazy: async () => {
          const { CandidatesPage } = await import('./features/candidates/CandidatesPage');
          return { Component: CandidatesPage };
        },
      },
      {
        path: 'trade',
        lazy: async () => {
          const { TradePage } = await import('./features/trading/TradePage');
          return { Component: TradePage };
        },
      },
      {
        path: 'portfolio',
        lazy: async () => {
          const { PortfolioPage } = await import('./features/portfolio/PortfolioPage');
          return { Component: PortfolioPage };
        },
      },
      {
        path: 'history',
        lazy: async () => {
          const { HistoryPage } = await import('./features/history/HistoryPage');
          return { Component: HistoryPage };
        },
      },
      {
        path: 'risk',
        lazy: async () => {
          const { RiskPage } = await import('./features/risk/RiskPage');
          return { Component: RiskPage };
        },
      },
      {
        path: 'system',
        lazy: async () => {
          const { SystemPage } = await import('./features/system/SystemPage');
          return { Component: SystemPage };
        },
      },
      {
        path: 'fund-flow',
        lazy: async () => {
          const { MarketFundFlowPage } = await import('./features/market/MarketFundFlowPage');
          return { Component: MarketFundFlowPage };
        },
      },
      {
        path: 'sentiment',
        lazy: async () => {
          const { MarketSentimentPage } = await import('./features/market/MarketSentimentPage');
          return { Component: MarketSentimentPage };
        },
      },
      {
        path: 'trade-journal',
        lazy: async () => {
          const { TradeJournalPage } = await import('./features/trades/TradeJournalPage');
          return { Component: TradeJournalPage };
        },
      },
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
