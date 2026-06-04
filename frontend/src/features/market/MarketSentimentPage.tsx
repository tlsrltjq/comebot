import { useQuery } from '@tanstack/react-query';
import { AlertTriangle, TrendingDown, TrendingUp, Minus } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import type { SentimentSignal } from '../../shared/api/types';

// ── helpers ──────────────────────────────────────────────────────────────────

function fmtB(b: number) {
  if (b >= 1000) return `$${(b / 1000).toFixed(2)}T`;
  return `$${b.toFixed(0)}B`;
}

function fmtChange(v: number) {
  return `${v >= 0 ? '+' : ''}${v.toFixed(2)}%`;
}

function fmtPct(v: number, digits = 4) {
  return `${v >= 0 ? '+' : ''}${v.toFixed(digits)}%`;
}

// ── Risk Label ────────────────────────────────────────────────────────────────

type RiskLabel = 'RISK_ON' | 'NEUTRAL' | 'RISK_OFF';

const RISK_META: Record<RiskLabel, { label: string; color: string; bg: string; desc: string }> = {
  RISK_ON:  { label: '🟢 Risk-on',  color: '#1a7c4a', bg: '#d4f5e2', desc: '시장이 위험 자산 선호 — 적극적 매수 심리' },
  NEUTRAL:  { label: '🟡 Neutral',  color: '#7c5e1a', bg: '#fef6d4', desc: '방향성 불명확 — 관망 또는 혼조 심리' },
  RISK_OFF: { label: '🔴 Risk-off', color: '#7c1a1a', bg: '#fde8e8', desc: '시장이 안전 자산 선호 — 방어적 심리' },
};

function RiskBadge({ label, score }: { label: RiskLabel; score: number }) {
  const meta = RISK_META[label];
  const max = 8;
  const pct = Math.min(Math.abs(score) / max * 100, 100);
  const barColor = label === 'RISK_ON' ? '#2ca05a' : label === 'RISK_OFF' ? '#d9534f' : '#f0ad4e';
  return (
    <div style={{
      background: meta.bg, border: `1px solid ${meta.color}30`,
      borderRadius: 12, padding: '20px 24px', marginBottom: 24,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <span style={{ fontSize: '1.5rem', fontWeight: 800, color: meta.color }}>{meta.label}</span>
        <span style={{
          background: meta.color, color: '#fff', borderRadius: 8,
          padding: '2px 10px', fontSize: '1.1rem', fontWeight: 700,
        }}>
          {score > 0 ? '+' : ''}{score} / {max}
        </span>
        <span style={{ color: '#666', fontSize: '0.9rem' }}>{meta.desc}</span>
      </div>
      <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: '0.75rem', color: '#888', width: 60 }}>Risk-off</span>
        <div style={{ flex: 1, height: 8, background: '#e0e0e0', borderRadius: 4, overflow: 'hidden' }}>
          <div style={{
            marginLeft: '50%', transform: 'translateX(-50%)',
            width: `${pct}%`,
            height: '100%', background: barColor, borderRadius: 4,
            transition: 'width 0.4s ease',
          }} />
        </div>
        <span style={{ fontSize: '0.75rem', color: '#888', width: 60, textAlign: 'right' }}>Risk-on</span>
      </div>
    </div>
  );
}

// ── Signal Row ────────────────────────────────────────────────────────────────

function SignalRow({ s }: { s: SentimentSignal }) {
  const color = s.score > 0 ? '#2ca05a' : s.score < 0 ? '#d9534f' : '#888';
  const Icon = s.score > 0 ? TrendingUp : s.score < 0 ? TrendingDown : Minus;
  const unavailable = s.value === 'N/A';
  return (
    <tr style={{ borderBottom: '1px solid #f0f0f0' }}>
      <td style={{ padding: '9px 8px', fontWeight: 600, fontSize: '0.875rem' }}>{s.name}</td>
      <td style={{ padding: '9px 8px', fontFamily: 'monospace', fontWeight: 700, color: unavailable ? '#bbb' : color }}>
        {s.value}
      </td>
      <td style={{ padding: '9px 8px' }}>
        {unavailable
          ? <span style={{ color: '#bbb', fontSize: '0.8rem' }}>—</span>
          : (
            <span style={{ display: 'flex', alignItems: 'center', gap: 4, color }}>
              <Icon size={14} />
              <strong style={{ fontSize: '0.85rem' }}>
                {s.score > 0 ? `+${s.score}` : s.score}
              </strong>
            </span>
          )
        }
      </td>
      <td style={{ padding: '9px 8px', color: '#777', fontSize: '0.8rem' }}>{s.note}</td>
    </tr>
  );
}

// ── Metric Card ───────────────────────────────────────────────────────────────

function Card({ label, value, sub, change }: {
  label: string; value: string; sub?: string; change?: number;
}) {
  const changeColor = change !== undefined ? (change >= 0 ? '#2ca05a' : '#d9534f') : undefined;
  return (
    <article style={{
      background: '#fff', border: '1px solid #e8e8e8', borderRadius: 10,
      padding: '16px 20px', display: 'flex', flexDirection: 'column', gap: 4,
    }}>
      <span style={{ color: '#888', fontSize: '0.78rem' }}>{label}</span>
      <strong style={{ fontSize: '1.35rem', lineHeight: 1.2 }}>{value}</strong>
      {change !== undefined && (
        <span style={{ color: changeColor, fontSize: '0.82rem', fontWeight: 600 }}>
          {change >= 0 ? '▲' : '▼'} {Math.abs(change).toFixed(2)}%
        </span>
      )}
      {sub && <span style={{ color: '#aaa', fontSize: '0.75rem' }}>{sub}</span>}
    </article>
  );
}

// ── Unavailable Notice ────────────────────────────────────────────────────────

function UnavailableNote({ items }: { items: { label: string; note: string }[] }) {
  return (
    <div style={{
      background: '#fafafa', border: '1px dashed #ddd', borderRadius: 8,
      padding: '14px 16px', marginTop: 16,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
        <AlertTriangle size={14} color="#f0ad4e" />
        <span style={{ fontSize: '0.82rem', color: '#888', fontWeight: 600 }}>표시 불가 지표</span>
      </div>
      {items.map(({ label, note }) => (
        <div key={label} style={{ fontSize: '0.8rem', color: '#aaa', marginTop: 4 }}>
          <strong style={{ color: '#bbb' }}>{label}</strong> — {note}
        </div>
      ))}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function MarketSentimentPage() {
  const { data, error, isLoading, isFetching, dataUpdatedAt } = useQuery({
    queryKey: queryKeys.sentiment(),
    queryFn: () => api.sentiment(),
    refetchInterval: POLLING_INTERVALS.sentiment,
  });

  const availableSignals = data?.signals.filter(s => s.value !== 'N/A') ?? [];
  const unavailableSignals = data?.signals.filter(s => s.value === 'N/A') ?? [];

  return (
    <main className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">시장 심리 · 자금 흐름</h1>
          <p className="page-subtitle">
            시장 전체 자금 흐름 지표 기반 Risk-on / Neutral / Risk-off 판단 &nbsp;·&nbsp;
            CoinGecko + Binance Futures (무료 공개 API, 5분 갱신)
          </p>
        </div>
        <LiveStatus isFetching={isFetching} updatedAt={dataUpdatedAt} />
      </div>

      {error && <ErrorPanel error={error} />}

      {isLoading && !data && <EmptyState title="시장 심리 데이터를 불러오는 중..." />}

      {data && (
        <>
          {/* Risk 판정 */}
          <RiskBadge label={data.riskLabel} score={data.riskScore} />

          {/* 요약 카드 */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
            gap: 12, marginBottom: 24,
          }}>
            <Card
              label="전체 시가총액"
              value={fmtB(data.totalMarketCapBillionUsd)}
              change={data.totalMarketCapChange24hPct}
              sub="24h 변화 포함"
            />
            <Card
              label="BTC 도미넌스"
              value={`${data.btcDominancePct.toFixed(1)}%`}
              sub="시총 대비 BTC 비중"
            />
            <Card
              label="스테이블코인 시총"
              value={fmtB(data.stablecoinMarketCapBillionUsd)}
              change={data.stablecoinChange24hPct}
              sub="USDT·USDC 등 상위 5종"
            />
            <Card
              label="BTC 펀딩레이트"
              value={fmtPct(data.btcFundingRatePct)}
              sub="양수=롱 우세 · 음수=숏 우세"
            />
            <Card
              label="ETH 펀딩레이트"
              value={fmtPct(data.ethFundingRatePct)}
              sub="Binance Futures"
            />
            <Card
              label="BTC OI (천 BTC)"
              value={data.btcOpenInterestBillionUsd.toFixed(1) + 'K'}
              change={data.btcOiChange4hPct}
              sub="4h 변화율 포함"
            />
            <Card
              label="상위 트레이더 L/S"
              value={data.btcLongShortRatio.toFixed(3)}
              sub={data.btcLongShortRatio >= 1 ? '롱 우세' : '숏 우세'}
            />
          </div>

          {/* 시그널 상세 테이블 */}
          <section className="section">
            <h2 className="section-title">시그널 상세 · 점수 기여</h2>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
                    <th style={{ padding: '8px' }}>지표</th>
                    <th style={{ padding: '8px' }}>현재값</th>
                    <th style={{ padding: '8px' }}>점수</th>
                    <th style={{ padding: '8px' }}>설명</th>
                  </tr>
                </thead>
                <tbody>
                  {availableSignals.map((s) => <SignalRow key={s.name} s={s} />)}
                </tbody>
              </table>
            </div>

            {/* 채점 기준 */}
            <details style={{ marginTop: 16, color: '#888', fontSize: '0.8rem' }}>
              <summary style={{ cursor: 'pointer', fontWeight: 600 }}>채점 기준 보기</summary>
              <div style={{ marginTop: 8, lineHeight: 1.8 }}>
                <p><strong>시가총액 24h:</strong> +5%↑=+2, +1~5%=+1, -1~+1%=0, -5~-1%=-1, -5%↓=-2</p>
                <p><strong>스테이블 흐름:</strong> -1%↓=+2(매수), 0~-1%=+1, 0~+1%=-1, +1%↑=-2(도피)</p>
                <p><strong>BTC 펀딩레이트:</strong> 0.03%↑=+2, 0.01~0.03%=+1, -0.01~0.01%=0, -0.03~-0.01%=-1, -0.03%↓=-2</p>
                <p><strong>OI 4h 변화:</strong> +3%↑=+1, -3~+3%=0, -3%↓=-1</p>
                <p><strong>L/S 비율:</strong> 1.5↑=+1, 0.8~1.5=0, 0.8↓=-1</p>
                <p><strong>판정:</strong> ≥+4 = Risk-on | -3~+3 = Neutral | ≤-4 = Risk-off</p>
              </div>
            </details>

            {/* 미지원 항목 */}
            {unavailableSignals.length > 0 && (
              <UnavailableNote items={unavailableSignals.map(s => ({ label: s.name, note: s.note }))} />
            )}
          </section>
        </>
      )}
    </main>
  );
}
