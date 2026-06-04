import { useQuery } from '@tanstack/react-query';
import { ShieldAlert, ShieldCheck, ShieldX } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { formatCurrency, formatNumber } from '../../shared/format';
import { cn } from '@/lib/utils';

export function RiskPage() {
  const { exchange } = useExchangeMode();
  const { data: risk, isLoading, error } = useQuery({
    queryKey: queryKeys.riskStatus(exchange),
    queryFn: () => api.riskStatus(exchange),
    refetchInterval: POLLING_INTERVALS.risk,
  });
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';

  if (isLoading) return <div className="page"><p className="text-muted-foreground">불러오는 중...</p></div>;
  if (error || !risk) return <div className="page"><div className="error-panel">리스크 상태 조회 실패</div></div>;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">리스크</h1>
          <p className="page-subtitle">PAPER 리스크 정책 — 읽기 전용</p>
        </div>
        <span className="badge badge-outline">{exchange}</span>
      </div>

      <div className="mb-4 flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
        <ShieldX size={14} />
        <span><strong>읽기 전용</strong> — REAL_TRADING, 실제 주문, 수동 BUY 없음</span>
      </div>

      <div className="metric-grid mb-5">
        {[
          { label: '최대 주문', val: formatCurrency(risk.maxOrderAmount, currency), sub: '자동 PAPER BUY 상한' },
          { label: '익절 기준', val: `${formatNumber(risk.takeProfitRate, 3)}%`, sub: 'PAPER SELL 기준' },
          { label: '손절 기준', val: `${formatNumber(risk.stopLossRate, 3)}%`, sub: '손실 제한' },
          { label: '일일 주문', val: formatNumber(risk.dailyOrderLimit), sub: risk.dailyRiskEnabled ? '켜짐' : '꺼짐' },
        ].map(({ label, val, sub }) => (
          <div key={label} className="metric-card">
            <span>{label}</span><strong className="num">{val}</strong><small>{sub}</small>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <RiskPanel title="쏠림 리스크" active={risk.concentration.enabled}>
          <Dl rows={[
            ['거래소', risk.concentration.exchange],
            ['적용', risk.concentration.enabled ? '켜짐' : '꺼짐'],
            ['경고 기준', `${formatNumber(risk.concentration.warningExposureRate, 0)}%`],
            ['차단 기준', `${formatNumber(risk.concentration.blockExposureRate, 0)}%`],
          ]} />
        </RiskPanel>

        <RiskPanel title="반복 손절 Cooldown" active={risk.stopLossCooldown.enabled}>
          <Dl rows={[
            ['적용', risk.stopLossCooldown.enabled ? '켜짐' : '꺼짐'],
            ['관찰 범위', risk.stopLossCooldown.window],
            ['트리거', `${risk.stopLossCooldown.triggerCount}회`],
            ['차단 시간', risk.stopLossCooldown.duration],
          ]} />
        </RiskPanel>

        <RiskPanel title="거래 제한">
          <Dl rows={[
            ['허용 마켓', risk.allowedMarkets.join(', ') || '-'],
            ['포지션 청산', risk.positionExitEnabled ? '켜짐' : '꺼짐'],
            ['일일 손실 한도', formatCurrency(risk.dailyLossLimit, currency)],
            ['실거래', '미구현'],
          ]} />
        </RiskPanel>
      </div>
    </div>
  );
}

function RiskPanel({ title, active, children }: { title: string; active?: boolean; children: React.ReactNode }) {
  return (
    <div className="section">
      <div className="flex items-center justify-between mb-3">
        <h2 className="section-title mb-0">{title}</h2>
        {active === true
          ? <ShieldAlert size={16} className="text-amber-500" />
          : active === false
            ? <ShieldCheck size={16} className="text-green-600" />
            : <ShieldX size={16} className="text-muted-foreground" />}
      </div>
      {children}
    </div>
  );
}

function Dl({ rows }: { rows: [string, string][] }) {
  return (
    <dl className="space-y-1.5">
      {rows.map(([label, value]) => (
        <div key={label} className="flex items-start gap-2 text-sm">
          <dt className="w-28 shrink-0 text-muted-foreground">{label}</dt>
          <dd className="font-medium">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

// React import for JSX
import React from 'react';
// cn is imported but used via className string - keeping for consistency
const _cn = cn;
void _cn;
