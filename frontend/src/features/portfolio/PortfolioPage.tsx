import { useMemo, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowDownAZ, CircleDollarSign, PieChart as PieIcon,
  Radar, ShieldCheck, TrendingDown, TrendingUp, Wallet,
} from 'lucide-react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import type { PositionValuationResponse, SelectedPaperSellResponse } from '../../shared/api/types';
import { ConfirmDialog } from '../../shared/ui/ConfirmDialog';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { formatCurrency, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { cn } from '@/lib/utils';

type SortKey = 'value' | 'profitRate' | 'market';
type AllocationSlice = { id: string; label: string; value: number; rate: number; color: string; };

const TP = 1.5; const SL = -0.7;
const COLORS = ['#2563eb', '#16a34a', '#d97706', '#9333ea', '#dc2626', '#0891b2'];

export function PortfolioPage() {
  const [sortKey, setSortKey] = useState<SortKey>('profitRate');
  const [selectedMarkets, setSelectedMarkets] = useState<Set<string>>(() => new Set());
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [sellSummary, setSellSummary] = useState<SelectedPaperSellResponse | null>(null);
  const { exchange } = useExchangeMode();
  const queryClient = useQueryClient();

  const statusQ = useQuery({ queryKey: queryKeys.portfolioStatus(exchange), queryFn: () => api.portfolioStatus(exchange), refetchInterval: POLLING_INTERVALS.portfolio });
  const valQ = useQuery({ queryKey: queryKeys.portfolioValuation(exchange), queryFn: () => api.portfolioValuation(exchange), refetchInterval: POLLING_INTERVALS.portfolio });
  const sysQ = useQuery({ queryKey: queryKeys.system(exchange), queryFn: () => api.systemStatus(exchange), refetchInterval: POLLING_INTERVALS.system });
  const riskQ = useQuery({ queryKey: queryKeys.riskStatus(exchange), queryFn: () => api.riskStatus(exchange), refetchInterval: POLLING_INTERVALS.risk });

  const positions = useMemo(() => sortPositions(valQ.data?.positions ?? [], sortKey), [sortKey, valQ.data?.positions]);
  const totalEquity = Number(valQ.data?.totalEquity ?? 0);
  const cash = Number(valQ.data?.cash ?? statusQ.data?.cash ?? 0);
  const currency = valQ.data?.currency ?? statusQ.data?.currency ?? (exchange === 'BINANCE' ? 'USDT' : 'KRW');
  const money = (v: string | number | null | undefined) => formatCurrency(v, currency);
  const positionValue = Number(valQ.data?.totalPositionValue ?? 0);
  const orderAmount = Number(sysQ.data?.strategy.orderAmount ?? 0);
  const cashRate = totalEquity > 0 ? (cash / totalEquity) * 100 : 0;
  const positionRate = totalEquity > 0 ? (positionValue / totalEquity) * 100 : 0;
  const remainingBuys = orderAmount > 0 ? Math.floor(cash / orderAmount) : 0;
  const exposureRows = useMemo(() => buildExposureRows(valQ.data?.positions ?? [], totalEquity), [valQ.data?.positions, totalEquity]);
  const concentration = riskQ.data?.concentration;
  const warnRate = Number(concentration?.warningExposureRate ?? (exchange === 'BINANCE' ? 25 : 7));
  const blockRate = Number(concentration?.blockExposureRate ?? (exchange === 'BINANCE' ? 40 : 10));
  const assetSlices = useMemo(() => buildAssetSlices(cash, positionValue, totalEquity), [cash, positionValue, totalEquity]);
  const marketSlices = useMemo(() => buildMarketSlices(valQ.data?.positions ?? [], totalEquity), [valQ.data?.positions, totalEquity]);
  const largestExposure = exposureRows[0]?.exposureRate ?? 0;
  const best = positions.reduce<PositionValuationResponse | null>((b, p) => !b || Number(p.unrealizedProfitRate) > Number(b.unrealizedProfitRate) ? p : b, null);
  const worst = positions.reduce<PositionValuationResponse | null>((w, p) => !w || Number(p.unrealizedProfitRate) < Number(w.unrealizedProfitRate) ? p : w, null);
  const selectedVisible = positions.map((p) => p.market).filter((m) => selectedMarkets.has(m));
  const selectedPositions = positions.filter((p) => selectedMarkets.has(p.market));
  const selectedValue = selectedPositions.reduce((s, p) => s + Number(p.positionValue), 0);
  const selectedPnl = selectedPositions.reduce((s, p) => s + Number(p.unrealizedProfit), 0);
  const displaySell = sellSummary?.exchange === exchange ? sellSummary : null;

  const sellMutation = useMutation({
    mutationFn: (markets: string[]) => api.sellSelectedPositions(exchange, { markets }),
    onSuccess: (res) => {
      setSellSummary(res);
      setSelectedMarkets(new Set());
      [queryKeys.portfolioStatus(exchange), queryKeys.positions(exchange), queryKeys.portfolioValuation(exchange), queryKeys.history(exchange)]
        .forEach((k) => void queryClient.invalidateQueries({ queryKey: k }));
    },
  });

  const toggle = (m: string) => setSelectedMarkets((cur) => {
    const next = new Set(cur);
    if (next.has(m)) {
      next.delete(m);
    } else {
      next.add(m);
    }
    return next;
  });

  return (
    <div className="page">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title">포트폴리오</h1>
          <p className="page-subtitle">PAPER 현금 · 포지션 · 평가 손익</p>
        </div>
        <div className="live-status">
          <span className={cn('status-dot', (statusQ.isFetching || valQ.isFetching) ? 'warn' : 'live')} />
          {valQ.dataUpdatedAt ? new Date(valQ.dataUpdatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''}
        </div>
      </div>

      {statusQ.error && <ErrorPanel title="포트폴리오 조회 실패" error={statusQ.error} />}
      {valQ.error && <ErrorPanel title="평가 조회 실패" error={valQ.error} />}
      {sellMutation.error && <ErrorPanel title="선택 매도 실패" error={sellMutation.error} />}

      {/* KPI */}
      <div className="metric-grid mb-4">
        <div className="metric-card"><span>현금</span><strong className="num">{money(valQ.data?.cash ?? statusQ.data?.cash)}</strong><small>{currency} {formatNumber(cashRate, 1)}%</small></div>
        <div className="metric-card"><span>포지션 가치</span><strong className="num">{money(valQ.data?.totalPositionValue)}</strong><small>{formatNumber(positionRate, 1)}%</small></div>
        <div className="metric-card"><span>매수 가능</span><strong>{remainingBuys}회</strong><small>1회 {money(orderAmount)}</small></div>
        <div className="metric-card">
          <span>총 손익</span>
          <strong className={cn('num', Number(valQ.data?.totalProfit ?? 0) >= 0 ? 'pos' : 'neg')}>{money(valQ.data?.totalProfit)}</strong>
          <small>실현 {money(valQ.data?.realizedProfit)}</small>
        </div>
      </div>

      {/* Pie charts + allocation */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4 mb-4">
        <PiePanel title="자산 비중" currency={currency} slices={assetSlices} />
        <PiePanel title="마켓 비중" currency={currency} slices={marketSlices} />

        {/* Allocation bars */}
        <div className="section">
          <div className="flex items-center gap-2 mb-3"><PieIcon size={15} className="text-muted-foreground" /><h2 className="section-title mb-0">자산 배분</h2></div>
          <Bar icon={<Wallet size={14} />} label="현금" value={cashRate} color="#16a34a" />
          <Bar icon={<CircleDollarSign size={14} />} label="포지션" value={positionRate} color="#2563eb" />
        </div>

        {/* Leaders */}
        <div className="section">
          <div className="flex items-center gap-2 mb-3"><TrendingUp size={15} className="text-muted-foreground" /><h2 className="section-title mb-0">손익 리더</h2></div>
          <LeaderItem title="최고 수익" pos={best} currency={currency} positive />
          <LeaderItem title="최대 손실" pos={worst} currency={currency} />
        </div>
      </div>

      {/* Exposure */}
      <div className="section mb-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2"><Radar size={15} className="text-muted-foreground" /><h2 className="section-title mb-0">마켓별 비중</h2></div>
          <div className="flex items-center gap-2">
            <span className={cn('badge', largestExposure >= blockRate ? 'badge-destructive' : largestExposure >= warnRate ? 'badge-warning' : 'badge-success')}>
              {largestExposure >= blockRate ? 'BLOCK' : largestExposure >= warnRate ? 'WARN' : 'OK'}
            </span>
            <span className="text-xs text-muted-foreground">{exchange} {formatNumber(warnRate, 0)}% / {formatNumber(blockRate, 0)}%</span>
          </div>
        </div>
        {exposureRows.length === 0
          ? <p className="text-sm text-muted-foreground">보유 포지션 없음</p>
          : exposureRows.slice(0, 5).map((row) => (
            <div key={row.market} className="mb-2">
              <div className="flex items-center justify-between text-sm mb-0.5">
                <span className="font-medium">{row.market}</span>
                <div className="flex items-center gap-2">
                  <span className="num text-xs">{money(row.positionValue)}</span>
                  <span className={cn(row.unrealizedProfitRate >= 0 ? 'pos' : 'neg', 'num text-xs')}>{formatNumber(row.unrealizedProfitRate, 2)}%</span>
                  <span className="num text-xs text-muted-foreground">{formatNumber(row.exposureRate, 1)}%</span>
                </div>
              </div>
              <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
                <div className={cn('h-full rounded-full transition-all',
                  row.exposureRate >= blockRate ? 'bg-destructive' : row.exposureRate >= warnRate ? 'bg-amber-500' : 'bg-primary')}
                  style={{ width: `${Math.min(100, row.exposureRate / blockRate * 100)}%` }} />
              </div>
            </div>
          ))}
      </div>

      {/* Sort toolbar */}
      <div className="flex items-center gap-2 mb-3">
        <span className="text-sm text-muted-foreground">정렬</span>
        {([['profitRate', <TrendingDown size={13} />, '손익률'], ['value', <CircleDollarSign size={13} />, '평가액'], ['market', <ArrowDownAZ size={13} />, '마켓']] as const).map(([key, icon, label]) => (
          <button key={key} type="button" onClick={() => setSortKey(key as SortKey)}
            className={cn('flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition-colors',
              sortKey === key ? 'bg-primary text-primary-foreground border-primary' : 'border-border bg-background hover:bg-muted')}>
            {icon}{label}
          </button>
        ))}
      </div>

      {/* Selected sell bar */}
      {selectedVisible.length > 0 && (
        <div className="mb-3 flex items-center justify-between rounded-lg border border-amber-300 bg-amber-50 px-4 py-3">
          <div>
            <p className="text-sm font-semibold">선택 PAPER SELL — {selectedVisible.length}개 포지션</p>
            <p className="text-sm text-muted-foreground">{money(selectedValue)} · 손익 <span className={cn(selectedPnl >= 0 ? 'pos' : 'neg')}>{money(selectedPnl)}</span></p>
          </div>
          <button type="button" disabled={sellMutation.isPending}
            onClick={() => setConfirmOpen(true)}
            className="flex items-center gap-1.5 rounded-md bg-destructive px-4 py-2 text-sm font-semibold text-white hover:bg-destructive/90 disabled:opacity-50">
            <ShieldCheck size={14} />선택 매도
          </button>
        </div>
      )}

      {/* Sell result */}
      {displaySell && (
        <div className="section mb-4">
          <div className="flex items-center justify-between mb-2">
            <h2 className="section-title mb-0">매도 결과</h2>
            <span className={cn('badge', displaySell.failedCount > 0 ? 'badge-warning' : 'badge-success')}>
              {displaySell.succeededCount}/{displaySell.requestedCount}
            </span>
          </div>
          <div className="space-y-1">
            {displaySell.results.map((r) => (
              <div key={r.market} className="flex items-center gap-2 text-sm">
                <span className="font-medium">{r.market}</span>
                <span className="badge badge-outline">{r.orderStatus}</span>
                <span className="text-muted-foreground text-xs">{r.message}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Position table */}
      <div className="section overflow-x-auto">
        <table className="data-table">
          <thead>
            <tr>
              <th>선택</th><th>마켓</th><th>수량</th><th>평균매수가</th>
              <th>현재가</th><th>평가액</th><th>미실현 손익</th><th>수익률</th><th>비중</th><th>상태</th>
            </tr>
          </thead>
          <tbody>
            {positions.map((p) => {
              const rate = Number(p.unrealizedProfitRate);
              const expRate = totalEquity > 0 ? Number(p.positionValue) / totalEquity * 100 : 0;
              return (
                <tr key={p.market}>
                  <td>
                    <input type="checkbox" aria-label={`${p.market} 선택`}
                      checked={selectedMarkets.has(p.market)} disabled={sellMutation.isPending}
                      onChange={() => toggle(p.market)} className="rounded" />
                  </td>
                  <td className="font-semibold">{p.market}</td>
                  <td className="num text-sm">{formatNumber(p.quantity, 8)}</td>
                  <td className="num text-sm">{money(p.averageBuyPrice)}</td>
                  <td className="num text-sm">{money(p.currentPrice)}</td>
                  <td className="num text-sm font-medium">{money(p.positionValue)}</td>
                  <td className={cn('num text-sm', rate >= 0 ? 'pos' : 'neg')}>{money(p.unrealizedProfit)}</td>
                  <td>
                    <span className={cn('flex items-center gap-0.5 num text-sm font-semibold', rate >= 0 ? 'pos' : 'neg')}>
                      {rate >= 0 ? <TrendingUp size={13} /> : <TrendingDown size={13} />}
                      {formatNumber(rate, 2)}%
                    </span>
                  </td>
                  <td className="num text-sm">{formatNumber(expRate, 2)}%</td>
                  <td><ExitBadge rate={rate} /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {!valQ.isLoading && positions.length === 0 && (
          <div className="empty-state"><strong>보유 포지션 없음</strong><span>자동 PAPER 매수가 체결되면 여기에 표시됩니다.</span></div>
        )}
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="선택 PAPER SELL 확인"
        confirmLabel="PAPER SELL 실행"
        busy={sellMutation.isPending}
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => { setConfirmOpen(false); sellMutation.mutate(selectedVisible); }}
        description={(
          <div>
            <p className="text-sm mb-2">실제 거래소 주문이 아닌 선택 보유 포지션의 PAPER SELL만 실행합니다.</p>
            <p className="text-sm mb-2 font-medium">선택한 보유 PAPER 포지션 전량</p>
            <dl className="space-y-1 text-sm">
              <div className="flex gap-2"><dt className="text-muted-foreground w-24">선택 포지션</dt><dd>{selectedVisible.join(', ')}</dd></div>
              <div className="flex gap-2"><dt className="text-muted-foreground w-24">예상 평가액</dt><dd>{money(selectedValue)}</dd></div>
              <div className="flex gap-2"><dt className="text-muted-foreground w-24">현재 손익</dt><dd className={selectedPnl >= 0 ? 'pos' : 'neg'}>{money(selectedPnl)}</dd></div>
            </dl>
          </div>
        )}
      />
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function PiePanel({ title, currency, slices }: { title: string; currency: string; slices: AllocationSlice[] }) {
  return (
    <div className="section">
      <div className="flex items-center gap-2 mb-2"><PieIcon size={15} className="text-muted-foreground" /><h2 className="section-title mb-0">{title}</h2></div>
      {slices.length === 0
        ? <p className="text-sm text-muted-foreground py-4 text-center">데이터 없음</p>
        : (
          <div className="flex gap-3">
            <div className="h-28 w-28 shrink-0">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={slices} dataKey="value" innerRadius="50%" outerRadius="80%" paddingAngle={2}>
                    {slices.map((s) => <Cell key={s.id} fill={s.color} />)}
                  </Pie>
                  <Tooltip formatter={(v) => formatCurrency(v as number, currency)} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="space-y-1 flex-1 min-w-0">
              {slices.map((s) => (
                <div key={s.id} className="flex items-center gap-1.5 text-xs">
                  <span className="h-2.5 w-2.5 rounded-full shrink-0" style={{ background: s.color }} />
                  <span className="truncate text-muted-foreground">{s.label}</span>
                  <span className="ml-auto shrink-0 font-medium">{formatNumber(s.rate, 1)}%</span>
                </div>
              ))}
            </div>
          </div>
        )}
    </div>
  );
}

function Bar({ icon, label, value, color }: { icon: ReactNode; label: string; value: number; color: string }) {
  return (
    <div className="mb-3">
      <div className="flex items-center justify-between text-sm mb-1">
        <div className="flex items-center gap-1.5 text-muted-foreground">{icon}{label}</div>
        <strong>{formatNumber(value, 1)}%</strong>
      </div>
      <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
        <div className="h-full rounded-full" style={{ width: `${Math.min(100, value)}%`, background: color }} />
      </div>
    </div>
  );
}

function LeaderItem({ title, pos, currency, positive = false }: { title: string; pos: PositionValuationResponse | null; currency: string; positive?: boolean }) {
  return (
    <div className="mb-3">
      <p className="text-xs text-muted-foreground mb-0.5">{title}</p>
      {pos ? (
        <div className="flex items-center justify-between text-sm">
          <span className="font-semibold">{pos.market}</span>
          <span className={cn(positive || Number(pos.unrealizedProfitRate) >= 0 ? 'pos' : 'neg', 'num text-xs')}>
            {formatNumber(pos.unrealizedProfitRate, 2)}% / {formatCurrency(pos.unrealizedProfit, currency)}
          </span>
        </div>
      ) : <p className="text-sm text-muted-foreground">-</p>}
    </div>
  );
}

function ExitBadge({ rate }: { rate: number }) {
  if (rate >= TP) return <span className="badge badge-success">익절권</span>;
  if (rate <= SL) return <span className="badge badge-destructive">손절권</span>;
  if (TP - rate <= 0.3) return <span className="badge badge-default">익절 근접</span>;
  if (rate - SL <= 0.3) return <span className="badge badge-warning">손절 근접</span>;
  return <span className="badge badge-outline">보유</span>;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function sortPositions(positions: PositionValuationResponse[], key: SortKey) {
  return [...positions].sort((a, b) =>
    key === 'market' ? a.market.localeCompare(b.market)
      : key === 'value' ? Number(b.positionValue) - Number(a.positionValue)
        : Number(a.unrealizedProfitRate) - Number(b.unrealizedProfitRate));
}

function buildExposureRows(positions: PositionValuationResponse[], totalEquity: number) {
  return [...positions].map((p) => ({
    market: p.market,
    positionValue: Number(p.positionValue),
    unrealizedProfitRate: Number(p.unrealizedProfitRate),
    exposureRate: totalEquity > 0 ? Number(p.positionValue) / totalEquity * 100 : 0,
  })).sort((a, b) => b.positionValue - a.positionValue);
}

function buildAssetSlices(cash: number, posVal: number, total: number): AllocationSlice[] {
  if (total <= 0) return [];
  return [
    { id: 'cash', label: '현금', value: Math.max(0, cash), rate: Math.max(0, cash) / total * 100, color: '#16a34a' },
    { id: 'pos', label: '포지션', value: Math.max(0, posVal), rate: Math.max(0, posVal) / total * 100, color: '#2563eb' },
  ].filter((s) => s.value > 0);
}

function buildMarketSlices(positions: PositionValuationResponse[], total: number): AllocationSlice[] {
  if (total <= 0 || positions.length === 0) return [];
  const sorted = positions.map((p) => ({ id: p.market, label: p.market, value: Math.max(0, Number(p.positionValue)) }))
    .filter((s) => s.value > 0).sort((a, b) => b.value - a.value);
  const top = sorted.slice(0, 5);
  const other = sorted.slice(5).reduce((s, x) => s + x.value, 0);
  const slices = other > 0 ? [...top, { id: 'other', label: '기타', value: other }] : top;
  return slices.map((s, i) => ({ ...s, rate: s.value / total * 100, color: s.id === 'other' ? '#94a3b8' : COLORS[i % COLORS.length] }));
}
