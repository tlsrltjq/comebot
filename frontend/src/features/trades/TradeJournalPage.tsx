import { useQuery } from '@tanstack/react-query';
import { TrendingDown, TrendingUp } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { formatCurrency, formatNumber } from '../../shared/format';
import type { MatchedTrade } from '../../shared/api/types';
import { cn } from '@/lib/utils';

// ── formatting ────────────────────────────────────────────────────────────────

function fmtTime(iso: string) {
  const d = new Date(iso);
  return d.toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', hour12: false,
  }).replace(/\. /g, '/').replace('.', '');
}

function fmtDuration(seconds: number) {
  if (seconds < 60) return `${seconds}초`;
  const m = Math.floor(seconds / 60);
  if (m < 60) return `${m}분`;
  const h = Math.floor(m / 60);
  const rem = m % 60;
  return rem > 0 ? `${h}시간 ${rem}분` : `${h}시간`;
}

const EXIT_LABELS: Record<MatchedTrade['exitReason'], string> = {
  TAKE_PROFIT: '익절',
  STOP_LOSS: '손절',
  TRAILING_STOP: '트레일링',
  MANUAL: '수동',
};

const EXIT_CLASS: Record<MatchedTrade['exitReason'], string> = {
  TAKE_PROFIT: 'badge-success',
  STOP_LOSS: 'badge-destructive',
  TRAILING_STOP: 'badge-default',
  MANUAL: 'badge-outline',
};

// ── summary stats ─────────────────────────────────────────────────────────────

function calcStats(trades: MatchedTrade[]) {
  if (trades.length === 0) return null;
  const wins = trades.filter((t) => Number(t.profitRatePct) > 0);
  const losses = trades.filter((t) => Number(t.profitRatePct) <= 0);
  const avgWin = wins.length > 0 ? wins.reduce((s, t) => s + Number(t.profitRatePct), 0) / wins.length : 0;
  const avgLoss = losses.length > 0 ? losses.reduce((s, t) => s + Number(t.profitRatePct), 0) / losses.length : 0;
  const avgHold = trades.reduce((s, t) => s + t.holdingSeconds, 0) / trades.length;
  return {
    total: trades.length,
    wins: wins.length,
    losses: losses.length,
    winRate: wins.length / trades.length * 100,
    avgWin,
    avgLoss,
    avgHold,
  };
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function TradeJournalPage() {
  const { exchange } = useExchangeMode();
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const money = (v: string | number | null | undefined) => formatCurrency(v, currency);

  const { data, isLoading, isFetching, dataUpdatedAt } = useQuery({
    queryKey: queryKeys.matchedTrades(exchange, 100),
    queryFn: () => api.matchedTrades(exchange, 100),
    refetchInterval: POLLING_INTERVALS.history,
  });

  const trades = data ?? [];
  const stats = calcStats(trades);

  return (
    <div className="page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">매매 일지</h1>
          <p className="page-subtitle">체결된 BUY → SELL 매칭 기록 — 최근 30일 · 최대 100건</p>
        </div>
        <div className="live-status">
          <span className={cn('status-dot', isFetching ? 'warn' : 'live')} />
          {dataUpdatedAt ? new Date(dataUpdatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''}
        </div>
      </div>

      {/* Stats */}
      {stats && (
        <div className="metric-grid mb-5">
          <div className="metric-card">
            <span>완성된 거래</span>
            <strong>{stats.total}건</strong>
            <small>BUY→SELL 매칭 완료</small>
          </div>
          <div className="metric-card">
            <span>승률</span>
            <strong className={cn('num', stats.winRate >= 50 ? 'pos' : 'neg')}>
              {formatNumber(stats.winRate, 1)}%
            </strong>
            <small>{stats.wins}승 / {stats.losses}패</small>
          </div>
          <div className="metric-card">
            <span>평균 익절률</span>
            <strong className="num pos">{stats.wins > 0 ? `+${formatNumber(stats.avgWin, 3)}%` : '-'}</strong>
            <small>수익 거래 평균</small>
          </div>
          <div className="metric-card">
            <span>평균 손절률</span>
            <strong className="num neg">{stats.losses > 0 ? `${formatNumber(stats.avgLoss, 3)}%` : '-'}</strong>
            <small>손실 거래 평균</small>
          </div>
          <div className="metric-card">
            <span>평균 보유 시간</span>
            <strong>{fmtDuration(Math.round(stats.avgHold))}</strong>
            <small>매수→매도 경과</small>
          </div>
        </div>
      )}

      {/* Table */}
      <div className="section overflow-x-auto">
        {isLoading && !data && (
          <div className="empty-state"><strong>불러오는 중...</strong></div>
        )}
        {!isLoading && trades.length === 0 && (
          <div className="empty-state">
            <strong>완성된 거래 없음</strong>
            <span>BUY → SELL이 모두 체결된 거래가 있어야 표시됩니다.</span>
          </div>
        )}
        {trades.length > 0 && (
          <table className="data-table">
            <thead>
              <tr>
                <th>마켓</th>
                <th>매수 시각</th>
                <th>매수가</th>
                <th>매도 시각</th>
                <th>매도가</th>
                <th>보유 시간</th>
                <th>수익률</th>
                <th>청산</th>
              </tr>
            </thead>
            <tbody>
              {trades.map((t, i) => {
                const rate = Number(t.profitRatePct);
                const positive = rate > 0;
                return (
                  <tr key={`${t.market}-${t.buyAt}-${i}`}>
                    <td className="font-semibold">{t.market}</td>
                    <td className="text-xs text-muted-foreground whitespace-nowrap">{fmtTime(t.buyAt)}</td>
                    <td className="num text-sm">{money(t.buyPrice)}</td>
                    <td className="text-xs text-muted-foreground whitespace-nowrap">{fmtTime(t.sellAt)}</td>
                    <td className="num text-sm">{money(t.sellPrice)}</td>
                    <td className="text-xs text-muted-foreground">{fmtDuration(t.holdingSeconds)}</td>
                    <td>
                      <span className={cn(
                        'flex items-center gap-0.5 num font-semibold text-sm',
                        positive ? 'pos' : 'neg',
                      )}>
                        {positive ? <TrendingUp size={13} /> : <TrendingDown size={13} />}
                        {positive ? '+' : ''}{formatNumber(rate, 3)}%
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${EXIT_CLASS[t.exitReason]}`}>
                        {EXIT_LABELS[t.exitReason]}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
