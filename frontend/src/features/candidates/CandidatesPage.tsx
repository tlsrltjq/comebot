import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CircleSlash, Filter, ListFilter, Search } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import type { TradingCandidateResponse } from '../../shared/api/types';
import { Badge } from '../../shared/ui/Badge';
import { EmptyState } from '../../shared/ui/EmptyState';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatCurrency, formatDateTime, formatNumber } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

type CandidateLimit = 20 | 50;

const candidateLimits: CandidateLimit[] = [20, 50];

export function CandidatesPage() {
  const [marketInput, setMarketInput] = useState('');
  const [market, setMarket] = useState('');
  const [limit, setLimit] = useState<CandidateLimit>(20);
  const [selectedOnly, setSelectedOnly] = useState(false);
  const normalizedMarket = market.trim().toUpperCase();
  const normalizedMarketInput = marketInput.trim().toUpperCase();
  const { exchange } = useExchangeMode();

  const candidatesQuery = useQuery({
    queryKey: queryKeys.candidates(exchange, normalizedMarket || undefined, limit),
    queryFn: () => api.candidates(exchange, normalizedMarket || undefined, limit),
    refetchInterval: POLLING_INTERVALS.candidates,
  });
  const positionsQuery = useQuery({
    queryKey: queryKeys.positions(exchange),
    queryFn: () => api.positions(exchange),
    refetchInterval: POLLING_INTERVALS.candidates,
  });

  const candidates = useMemo(() => candidatesQuery.data ?? [], [candidatesQuery.data]);
  const visibleCandidates = useMemo(
    () => (selectedOnly ? candidates.filter((candidate) => candidate.decision === 'SELECTED') : candidates),
    [candidates, selectedOnly],
  );
  const topSkippedReasons = useMemo(() => summarizeSkippedReasons(candidates), [candidates]);
  const positionMarkets = useMemo(
    () => new Set((positionsQuery.data ?? []).map((position) => position.market)),
    [positionsQuery.data],
  );
  const candidateSummary = useMemo(() => summarizeCandidates(candidates, positionMarkets), [candidates, positionMarkets]);
  const riskSummary = useMemo(() => summarizeRiskReasons(candidates), [candidates]);
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>후보 모니터링(Candidate Monitoring)</h1>
          <p>자동 실행 스케줄러(Auto scheduler)가 판단하는 롱 후보를 확인합니다.</p>
        </div>
        <LiveStatus updatedAt={candidatesQuery.dataUpdatedAt} isFetching={candidatesQuery.isFetching} intervalMs={POLLING_INTERVALS.candidates} />
      </header>

      <div className="metric-grid">
        <MetricCard label="조회 후보(Scanned)" value={formatNumber(candidateSummary.total)} detail={`${normalizedMarket || `상위 ${limit}`} / ${POLLING_INTERVALS.candidates / 1000}s`} />
        <MetricCard label="선택됨(Selected)" value={formatNumber(candidateSummary.selected)} detail={`${formatNumber(candidateSummary.selectedRate, 1)}%`} />
        <MetricCard label="제외됨(Skipped)" value={formatNumber(candidateSummary.skipped)} detail={`${formatNumber(candidateSummary.skippedRate, 1)}%`} />
        <MetricCard label="보유 포지션(Held)" value={formatNumber(candidateSummary.held)} detail="후보 market 기준" />
        <MetricCard label="리스크 경고(Risk)" value={formatNumber(riskSummary.total)} detail={`쏠림 ${formatNumber(riskSummary.concentration)} / cooldown ${formatNumber(riskSummary.cooldown)}`} />
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>후보 요약(Candidate Summary)</h2>
            <ListFilter size={20} />
          </div>
          <div className="candidate-summary">
            <Badge tone="good">SELECTED {candidateSummary.selected}</Badge>
            <Badge tone="neutral">SKIPPED {candidateSummary.skipped}</Badge>
            <Badge tone={candidateSummary.held > 0 ? 'info' : 'neutral'}>HELD {candidateSummary.held}</Badge>
            <Badge tone={riskSummary.total > 0 ? 'warn' : 'good'}>RISK {riskSummary.total}</Badge>
          </div>
        </article>
        <article className="panel">
          <div className="panel-title-row">
            <h2>제외 사유 TOP 5(Skipped Reasons)</h2>
            <CircleSlash size={20} />
          </div>
          <div className="reason-list">
            {topSkippedReasons.map((row) => (
              <div className="reason-item" key={row.reason}>
                <span>{row.reason}</span>
                <Badge tone="neutral">{row.count}</Badge>
              </div>
            ))}
            {topSkippedReasons.length === 0 ? (
              <EmptyState title="제외 사유 없음(No skipped reasons)" description="후보가 모두 선택됐거나 아직 조회된 후보가 없습니다." />
            ) : null}
          </div>
        </article>
      </div>

      <div className="audit-strip" aria-label="후보 감사 기준(Candidate audit rules)">
        <Badge tone="info">조회 전용(Read-only)</Badge>
        <span>후보 화면은 자동 스케줄러 판단 결과만 보여주며 수동 BUY나 후보 실행 버튼을 제공하지 않습니다.</span>
      </div>

      <div className="toolbar">
        <form
          className="toolbar-form"
          onSubmit={(event) => {
            event.preventDefault();
            setMarket(normalizedMarketInput);
          }}
        >
          <label>
            마켓(Market)
            <input value={marketInput} onChange={(event) => setMarketInput(event.target.value)} placeholder={exchange === 'BINANCE' ? '전체 또는 BTCUSDT' : '전체 또는 KRW-BTC'} />
          </label>
          <button className="button button-secondary" type="submit">
            <Search size={16} />
            조회(Search)
          </button>
          {normalizedMarket ? (
            <button
              className="button button-secondary"
              type="button"
              onClick={() => {
                setMarket('');
                setMarketInput('');
              }}
            >
              전체(All)
            </button>
          ) : null}
        </form>
        <div className="filter-stack">
          <span className="control-label">개수(Limit)</span>
          <div className="segmented-row compact-segmented">
            {candidateLimits.map((item) => (
              <button
                key={item}
                className={limit === item ? 'button button-primary' : 'button button-secondary'}
                type="button"
                onClick={() => setLimit(item)}
              >
                {item}
              </button>
            ))}
          </div>
        </div>
        <div className="filter-stack">
          <span className="control-label">보기(View)</span>
          <button
            className={selectedOnly ? 'button button-primary' : 'button button-secondary'}
            type="button"
            onClick={() => setSelectedOnly((current) => !current)}
          >
            <Filter size={16} />
            선택 후보만(Selected only)
          </button>
        </div>
      </div>

      {candidatesQuery.error ? <ErrorPanel error={candidatesQuery.error} /> : null}
      {positionsQuery.error ? <ErrorPanel title="보유 포지션 조회 실패(Position status failed)" error={positionsQuery.error} /> : null}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>마켓(Market)</th>
              <th>판단(Decision)</th>
              <th>추세(Trend)</th>
              <th>가격(Price)</th>
              <th>변화율(Change)</th>
              <th>범위(Range)</th>
              <th>거래대금(Trade amount)</th>
              <th>보유(Position)</th>
              <th>사유 타입(Reason Type)</th>
              <th>리스크 플래그(Risk Flag)</th>
              <th>이유(Reason)</th>
              <th>스캔 시각(Scanned)</th>
            </tr>
          </thead>
          <tbody>
            {visibleCandidates.map((candidate) => (
              <tr key={`${candidate.market}-${candidate.scannedAt}`}>
                <td>{candidate.market}</td>
                <td>
                  <Badge tone={candidate.decision === 'SELECTED' ? 'good' : 'neutral'}>{candidate.decision}</Badge>
                </td>
                <td>{candidate.trend ?? '-'}</td>
                <td>{formatCurrency(candidate.currentPrice, currency)}</td>
                <td>{formatNumber(candidate.priceChangeRate, 2)}%</td>
                <td>{formatNumber(candidate.highLowRangeRate, 2)}%</td>
                <td>{formatNumber(candidate.tradeAmountChangeRate, 2)}%</td>
                <td>
                  <div className="candidate-badge-stack">
                    <Badge tone={positionMarkets.has(candidate.market) ? 'info' : 'neutral'}>
                      {positionMarkets.has(candidate.market) ? '보유(Held)' : '없음(None)'}
                    </Badge>
                  </div>
                </td>
                <td><ReasonTypeBadge reasonType={candidate.reasonType} /></td>
                <td><RiskReasonBadge reasonType={candidate.riskReasonType} /></td>
                <td>{candidate.reason}</td>
                <td>{formatDateTime(candidate.scannedAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {!candidatesQuery.isLoading && visibleCandidates.length === 0 ? <EmptyState title="후보 없음(No candidates)" /> : null}
      </div>
    </section>
  );
}

function summarizeCandidates(candidates: TradingCandidateResponse[], positionMarkets: Set<string>) {
  const selected = candidates.filter((candidate) => candidate.decision === 'SELECTED').length;
  const skipped = candidates.filter((candidate) => candidate.decision === 'SKIPPED').length;
  const held = candidates.filter((candidate) => positionMarkets.has(candidate.market)).length;
  const total = candidates.length;
  return {
    total,
    selected,
    skipped,
    held,
    selectedRate: total > 0 ? (selected / total) * 100 : 0,
    skippedRate: total > 0 ? (skipped / total) * 100 : 0,
  };
}

function summarizeSkippedReasons(candidates: TradingCandidateResponse[]) {
  const counts = new Map<string, number>();
  candidates
    .filter((candidate) => candidate.decision === 'SKIPPED')
    .forEach((candidate) => counts.set(candidate.reason, (counts.get(candidate.reason) ?? 0) + 1));
  return [...counts.entries()]
    .map(([reason, count]) => ({ reason, count }))
    .sort((left, right) => right.count - left.count || left.reason.localeCompare(right.reason))
    .slice(0, 5);
}

function summarizeRiskReasons(candidates: TradingCandidateResponse[]) {
  return candidates.reduce(
    (summary, candidate) => {
      const type = candidate.riskReasonType ?? 'NONE';
      if (type === 'CONCENTRATION') {
        return { ...summary, total: summary.total + 1, concentration: summary.concentration + 1 };
      }
      if (type === 'STOP_LOSS_COOLDOWN') {
        return { ...summary, total: summary.total + 1, cooldown: summary.cooldown + 1 };
      }
      return summary;
    },
    { total: 0, concentration: 0, cooldown: 0 },
  );
}

function RiskReasonBadge({ reasonType }: { reasonType?: TradingCandidateResponse['riskReasonType'] }) {
  if (reasonType === 'CONCENTRATION') {
    return <Badge tone="warn">쏠림(Concentration)</Badge>;
  }
  if (reasonType === 'STOP_LOSS_COOLDOWN') {
    return <Badge tone="warn">Cooldown</Badge>;
  }
  return <Badge tone="neutral">NONE</Badge>;
}

function ReasonTypeBadge({ reasonType }: { reasonType?: TradingCandidateResponse['reasonType'] }) {
  if (reasonType === 'SELECTED') {
    return <Badge tone="good">SELECTED</Badge>;
  }
  if (reasonType === 'CONCENTRATION_RISK' || reasonType === 'STOP_LOSS_COOLDOWN') {
    return <Badge tone="warn">{reasonType}</Badge>;
  }
  if (!reasonType) {
    return <Badge tone="neutral">UNKNOWN</Badge>;
  }
  return <Badge tone="neutral">{reasonType}</Badge>;
}
