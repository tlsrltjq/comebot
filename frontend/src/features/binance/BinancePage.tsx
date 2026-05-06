import { useMemo } from 'react';
import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart3, CandlestickChart, Clock3, ListChecks, PlayCircle, Wallet } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api, queryKeys } from '../../shared/api/client';
import type { Mvp2PaperCandidateResponse, Mvp2PaperPositionValuationResponse } from '../../shared/api/types';
import { formatDateTime, formatNumber } from '../../shared/format';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';

export function BinancePage() {
  const statusQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperStatus,
    queryFn: api.mvp2BinancePaperStatus,
    refetchInterval: 5_000,
  });
  const valuationQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperValuation,
    queryFn: api.mvp2BinancePaperValuation,
    refetchInterval: 5_000,
  });
  const candidatesQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperCandidates,
    queryFn: api.mvp2BinancePaperCandidates,
    refetchInterval: 30_000,
  });
  const historyQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperHistory(12),
    queryFn: () => api.mvp2BinancePaperHistory(12),
    refetchInterval: 5_000,
  });

  const valuation = valuationQuery.data;
  const status = statusQuery.data;
  const candidates = candidatesQuery.data ?? [];
  const history = historyQuery.data ?? [];
  const selectedCandidates = candidates.filter((candidate) => candidate.decision === 'SELECTED');
  const skippedCandidates = candidates.length - selectedCandidates.length;
  const pnl = Number(valuation?.totalProfit ?? 0);
  const pnlTone = pnl >= 0 ? 'tone-positive' : 'tone-negative';
  const allocationData = useMemo(() => positionChartData(valuation?.positions ?? []), [valuation?.positions]);

  return (
    <section className="page binance-page">
      <header className="page-header">
        <div>
          <h1>Binance PAPER 대시보드(Binance Paper Dashboard)</h1>
          <p>Binance public 시세 기반 자동 PAPER 거래 상태와 손익을 확인합니다.</p>
        </div>
        <Badge tone={status?.schedulerEnabled ? 'good' : 'warn'}>
          {status?.schedulerEnabled ? '자동 실행 중(Auto Running)' : '자동 실행 꺼짐(Auto Off)'}
        </Badge>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      {statusQuery.error ? <ErrorPanel title="Binance PAPER 상태 조회 실패(Status failed)" error={statusQuery.error} /> : null}
      {valuationQuery.error ? <ErrorPanel title="Binance PAPER 평가 조회 실패(Valuation failed)" error={valuationQuery.error} /> : null}
      {candidatesQuery.error ? <ErrorPanel title="Binance 후보 조회 실패(Candidates failed)" error={candidatesQuery.error} /> : null}
      {historyQuery.error ? <ErrorPanel title="Binance 이력 조회 실패(History failed)" error={historyQuery.error} /> : null}

      <div className="status-strip" aria-label="Binance PAPER 운영 상태(Binance paper status)">
        <StatusTile icon={<PlayCircle size={16} />} label="스케줄러(Scheduler)" value={status?.schedulerEnabled ? `${formatNumber(status.schedulerFixedDelayMs / 1000)}s` : '꺼짐(Off)'} good={Boolean(status?.schedulerEnabled)} />
        <StatusTile icon={<CandlestickChart size={16} />} label="시세(Market Data)" value="공개 API(Public)" good />
        <StatusTile icon={<Wallet size={16} />} label="주문(Order)" value="PAPER only" good />
        <StatusTile icon={<ListChecks size={16} />} label="대상(Symbols)" value={formatNumber(status?.symbols.length)} good={Boolean(status?.symbols.length)} />
      </div>

      <div className="metric-grid">
        <MetricCard label="총자산(Total Equity)" value={`${formatNumber(valuation?.totalEquity, 4)} USDT`} detail={`현금(Cash) ${formatNumber(valuation?.cash, 4)} USDT`} />
        <MetricCard label="총손익(Total PnL)" value={`${formatNumber(valuation?.totalProfit, 4)} USDT`} detail={`미실현(Unrealized) ${formatNumber(valuation?.unrealizedProfit, 4)} USDT`} />
        <MetricCard label="포지션 평가(Position Value)" value={`${formatNumber(valuation?.totalPositionValue, 4)} USDT`} detail={`보유(Positions) ${formatNumber(valuation?.positions.length)}`} />
        <MetricCard label="1회 주문(Order Size)" value={`${formatNumber(status?.orderAmount, 2)} USDT`} detail={`TP/SL ${formatNumber(status?.takeProfitRate, 2)}% / ${formatNumber(status?.stopLossRate, 2)}%`} />
      </div>

      <div className="section-grid">
        <article className="panel chart-panel">
          <div className="panel-title-row">
            <h2>포지션 비중(Position Allocation)</h2>
            <BarChart3 size={20} />
          </div>
          {allocationData.length ? (
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={allocationData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="symbol" tickLine={false} axisLine={false} />
                  <YAxis tickLine={false} axisLine={false} width={38} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#176b87" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <p>보유 포지션 없음(No positions)</p>
          )}
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>손익 상태(PnL Status)</h2>
            <Badge tone={pnl >= 0 ? 'good' : 'bad'}>{pnl >= 0 ? '수익(Profit)' : '손실(Loss)'}</Badge>
          </div>
          <dl className="definition-list">
            <dt>실현손익(Realized)</dt>
            <dd>{formatNumber(valuation?.realizedProfit, 4)} USDT</dd>
            <dt>미실현손익(Unrealized)</dt>
            <dd className={pnlTone}>{formatNumber(valuation?.unrealizedProfit, 4)} USDT</dd>
            <dt>총손익(Total)</dt>
            <dd className={pnlTone}>{formatNumber(valuation?.totalProfit, 4)} USDT</dd>
            <dt>대상(Symbols)</dt>
            <dd>{status?.symbols.join(', ') ?? '-'}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>후보 요약(Candidate Summary)</h2>
            <Badge tone={selectedCandidates.length ? 'good' : 'neutral'}>{formatNumber(selectedCandidates.length)} selected</Badge>
          </div>
          <dl className="definition-list">
            <dt>선택(Selected)</dt>
            <dd>{formatNumber(selectedCandidates.length)}</dd>
            <dt>제외(Skipped)</dt>
            <dd>{formatNumber(skippedCandidates)}</dd>
            <dt>최근 후보(Recent)</dt>
            <dd>{latestCandidate(candidates)}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>자동 실행 원칙(Auto Rules)</h2>
            <Wallet size={20} />
          </div>
          <dl className="definition-list">
            <dt>거래 모드(Mode)</dt>
            <dd>PAPER_TRADING</dd>
            <dt>실제 주문(Real order)</dt>
            <dd>없음(None)</dd>
            <dt>실행 방식(Run)</dt>
            <dd>스케줄러 자동 실행(Scheduler only)</dd>
            <dt>중복 실행(Overlap)</dt>
            <dd>방지(Guarded)</dd>
          </dl>
        </article>
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>보유 포지션(Positions)</h2>
            <Wallet size={20} />
          </div>
          <div className="mvp2-list">
            {(valuation?.positions ?? []).map((position) => (
              <div className="mvp2-row" key={position.symbol}>
                <div>
                  <strong>{position.symbol}</strong>
                  <small>{positionDetail(position)}</small>
                </div>
                <Badge tone={Number(position.unrealizedProfit) >= 0 ? 'good' : 'bad'}>
                  {formatNumber(position.unrealizedProfitRate, 2)}%
                </Badge>
              </div>
            ))}
            {valuation && valuation.positions.length === 0 ? <p>보유 포지션 없음(No positions)</p> : null}
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>최근 후보(Recent Candidates)</h2>
            <ListChecks size={20} />
          </div>
          <div className="mvp2-list">
            {candidates.slice(0, 8).map((candidate) => (
              <div className="mvp2-row" key={candidate.symbol}>
                <div>
                  <strong>{candidate.symbol}</strong>
                  <small>{candidate.reason}</small>
                </div>
                <Badge tone={candidate.decision === 'SELECTED' ? 'good' : 'neutral'}>{candidate.decision}</Badge>
              </div>
            ))}
            {candidates.length === 0 ? <p>후보 조회 대기 중(Waiting for candidates)</p> : null}
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>최근 이력(Recent History)</h2>
            <Clock3 size={20} />
          </div>
          <div className="mvp2-list">
            {history.slice(0, 8).map((item) => (
              <div className="mvp2-row" key={`${item.symbol}-${item.createdAt}-${item.message}`}>
                <div>
                  <strong>{item.symbol}</strong>
                  <small>{item.reason} / {item.message} / {formatDateTime(item.createdAt)}</small>
                </div>
                <Badge tone={item.status === 'FILLED' ? 'good' : item.status === 'REJECTED' ? 'warn' : 'neutral'}>
                  {item.side ?? 'HOLD'}
                </Badge>
              </div>
            ))}
            {history.length === 0 ? <p>거래 이력 없음(No history)</p> : null}
          </div>
        </article>
      </div>
    </section>
  );
}

function StatusTile({
  icon,
  label,
  value,
  good,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  good: boolean;
}) {
  return (
    <div className={`status-pill ${good ? 'status-pill-good' : 'status-pill-bad'}`}>
      {icon}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function positionChartData(positions: Mvp2PaperPositionValuationResponse[]) {
  return positions.map((position) => ({
    symbol: position.symbol,
    value: Number(position.positionValue),
  }));
}

function latestCandidate(candidates: Mvp2PaperCandidateResponse[]) {
  return candidates[0] ? `${candidates[0].symbol} / ${candidates[0].decision}` : '-';
}

function positionDetail(position: Mvp2PaperPositionValuationResponse) {
  return `Avg ${formatNumber(position.averageBuyPrice, 6)} / Now ${formatNumber(position.currentPrice, 6)} / Value ${formatNumber(position.positionValue, 4)} USDT / PnL ${formatNumber(position.unrealizedProfit, 4)} USDT`;
}
