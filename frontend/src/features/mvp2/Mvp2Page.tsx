import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart3, Clock3, FlaskConical, PlayCircle, ShieldCheck, Store, TrendingUp, Wallet } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import type { Mvp2Exchange, Mvp2ExchangeResponse, Mvp2PaperPositionResponse, Mvp2PaperPositionValuationResponse } from '../../shared/api/types';
import { formatDateTime, formatKrw, formatNumber } from '../../shared/format';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';

const profileCards = [
  { name: '안정형(Stable)', detail: '강한 추세 확인, 낮은 손실 허용', tone: 'good' as const },
  { name: '공격형(Aggressive)', detail: '빠른 변동성 포착, 높은 진입 빈도', tone: 'warn' as const },
  { name: '수비형(Defensive)', detail: '노출 제한 강화, 손실 방어 우선', tone: 'info' as const },
];

export function Mvp2Page() {
  const exchangesQuery = useQuery({ queryKey: queryKeys.mvp2Exchanges, queryFn: api.mvp2Exchanges, refetchInterval: 15_000 });
  const exchanges = useMemo(() => exchangesQuery.data ?? [], [exchangesQuery.data]);
  const [selectedExchange, setSelectedExchange] = useState<Mvp2Exchange>('UPBIT');
  const selected = useMemo(() => selectExchange(exchanges, selectedExchange), [exchanges, selectedExchange]);
  const statusQuery = useQuery({
    queryKey: queryKeys.mvp2ExchangeStatus(selected.exchange),
    queryFn: () => api.mvp2ExchangeStatus(selected.exchange),
    enabled: Boolean(selected.exchange),
    refetchInterval: 15_000,
  });
  const binanceSelected = selected.exchange === 'BINANCE';
  const upbitSelected = selected.exchange === 'UPBIT';
  const upbitSystemQuery = useQuery({
    queryKey: queryKeys.system,
    queryFn: api.systemStatus,
    enabled: upbitSelected,
    refetchInterval: 5_000,
  });
  const upbitValuationQuery = useQuery({
    queryKey: queryKeys.portfolioValuation,
    queryFn: api.portfolioValuation,
    enabled: upbitSelected,
    refetchInterval: 5_000,
  });
  const upbitCandidatesQuery = useQuery({
    queryKey: queryKeys.candidates(),
    queryFn: () => api.candidates(),
    enabled: upbitSelected,
    refetchInterval: 30_000,
  });
  const upbitHistoryQuery = useQuery({
    queryKey: queryKeys.history(undefined, 10),
    queryFn: () => api.history(undefined, 10),
    enabled: upbitSelected,
    refetchInterval: 15_000,
  });
  const binancePaperStatusQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperStatus,
    queryFn: api.mvp2BinancePaperStatus,
    enabled: binanceSelected,
    refetchInterval: 15_000,
  });
  const binancePaperPortfolioQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperPortfolio,
    queryFn: api.mvp2BinancePaperPortfolio,
    enabled: binanceSelected,
    refetchInterval: 15_000,
  });
  const binancePaperValuationQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperValuation,
    queryFn: api.mvp2BinancePaperValuation,
    enabled: binanceSelected,
    refetchInterval: 5_000,
  });
  const binancePaperCandidatesQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperCandidates,
    queryFn: api.mvp2BinancePaperCandidates,
    enabled: binanceSelected,
    refetchInterval: 30_000,
  });
  const binancePaperHistoryQuery = useQuery({
    queryKey: queryKeys.mvp2BinancePaperHistory(10),
    queryFn: () => api.mvp2BinancePaperHistory(10),
    enabled: binanceSelected,
    refetchInterval: 15_000,
  });
  const binancePortfolio = binancePaperPortfolioQuery.data;
  const binanceValuation = binancePaperValuationQuery.data;
  const binancePositions = binanceValuation?.positions ?? binancePortfolio?.positions ?? [];
  const binanceStatus = binancePaperStatusQuery.data;
  const binanceCandidates = binancePaperCandidatesQuery.data ?? [];
  const binanceHistory = binancePaperHistoryQuery.data ?? [];
  const upbitValuation = upbitValuationQuery.data;
  const upbitSystem = upbitSystemQuery.data;
  const upbitCandidates = upbitCandidatesQuery.data ?? [];
  const upbitHistory = upbitHistoryQuery.data ?? [];
  const modeName = selected.exchange === 'BINANCE' ? 'Binance' : 'Upbit';
  const modeCurrency = selected.exchange === 'BINANCE' ? 'USDT' : 'KRW';
  const modeEquity = binanceSelected ? binanceValuation?.totalEquity ?? binancePortfolio?.cash : upbitValuation?.totalEquity;
  const modeCash = binanceSelected ? binanceValuation?.cash ?? binancePortfolio?.cash : upbitValuation?.cash;
  const modePositionValue = binanceSelected ? binanceValuation?.totalPositionValue : upbitValuation?.totalPositionValue;
  const modeRealizedProfit = binanceSelected ? binanceValuation?.realizedProfit ?? binancePortfolio?.realizedProfit : upbitValuation?.realizedProfit;
  const modeUnrealizedProfit = binanceSelected ? binanceValuation?.unrealizedProfit : upbitValuation?.unrealizedProfit;
  const modeTotalProfit = binanceSelected ? binanceValuation?.totalProfit : upbitValuation?.totalProfit;
  const modePositions = binanceSelected
    ? binancePositions.map((position) => ({ symbol: position.symbol, summary: positionSummary(position) }))
    : (upbitValuation?.positions ?? []).map((position) => ({ symbol: position.market, summary: upbitPositionSummary(position) }));
  const modeCandidates = binanceSelected
    ? binanceCandidates.map((candidate) => ({ symbol: candidate.symbol, decision: candidate.decision, reason: candidate.reason }))
    : upbitCandidates.map((candidate) => ({ symbol: candidate.market, decision: candidate.decision, reason: candidate.reason }));
  const modeHistory = binanceSelected
    ? binanceHistory.map((history) => ({
      symbol: history.symbol,
      badge: history.side ?? 'HOLD',
      status: history.status,
      reason: history.reason,
      message: history.message,
      createdAt: history.createdAt,
    }))
    : upbitHistory.map((history) => ({
      symbol: history.market,
      badge: history.signalType ?? 'HOLD',
      status: history.orderStatus,
      reason: history.signalReason,
      message: history.message,
      createdAt: history.createdAt,
    }));
  const selectedCandidates = modeCandidates.filter((candidate) => candidate.decision === 'SELECTED');
  const schedulerEnabled = binanceSelected ? Boolean(binanceStatus?.schedulerEnabled) : Boolean(upbitSystem?.scheduler?.candidateEnabled);
  const schedulerDelayMs = binanceSelected ? binanceStatus?.schedulerFixedDelayMs : upbitSystem?.scheduler?.candidateFixedDelayMs;
  const orderAmount = binanceSelected ? binanceStatus?.orderAmount : upbitSystem?.strategy?.orderAmount;
  const targetCount = binanceSelected ? binanceStatus?.symbols.length : upbitSystem?.scheduler?.candidateMarkets.length;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>MVP2 실험 대시보드(Experiment Dashboard)</h1>
          <p>하나의 화면에서 Upbit/Binance 모드를 전환해 public market data와 PAPER 실험 상태를 확인합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      {exchangesQuery.error ? <ErrorPanel title="MVP2 거래소 조회 실패(Exchange list failed)" error={exchangesQuery.error} /> : null}
      {statusQuery.error ? <ErrorPanel title="MVP2 거래소 상태 조회 실패(Exchange status failed)" error={statusQuery.error} /> : null}
      {binancePaperStatusQuery.error ? <ErrorPanel title="Binance PAPER 상태 조회 실패(Binance paper status failed)" error={binancePaperStatusQuery.error} /> : null}
      {binancePaperPortfolioQuery.error ? <ErrorPanel title="Binance PAPER 포트폴리오 조회 실패(Binance paper portfolio failed)" error={binancePaperPortfolioQuery.error} /> : null}
      {binancePaperValuationQuery.error ? <ErrorPanel title="Binance PAPER 평가 조회 실패(Binance paper valuation failed)" error={binancePaperValuationQuery.error} /> : null}
      {binancePaperCandidatesQuery.error ? <ErrorPanel title="Binance PAPER 후보 조회 실패(Binance paper candidates failed)" error={binancePaperCandidatesQuery.error} /> : null}
      {upbitSystemQuery.error ? <ErrorPanel title="Upbit 상태 조회 실패(Upbit status failed)" error={upbitSystemQuery.error} /> : null}
      {upbitValuationQuery.error ? <ErrorPanel title="Upbit 평가 조회 실패(Upbit valuation failed)" error={upbitValuationQuery.error} /> : null}
      {upbitCandidatesQuery.error ? <ErrorPanel title="Upbit 후보 조회 실패(Upbit candidates failed)" error={upbitCandidatesQuery.error} /> : null}
      {upbitHistoryQuery.error ? <ErrorPanel title="Upbit 이력 조회 실패(Upbit history failed)" error={upbitHistoryQuery.error} /> : null}

      <div className="exchange-switch" aria-label="거래 모드 선택(Trading mode selector)">
        {exchanges.map((exchange) => (
          <button
            className={selected.exchange === exchange.exchange ? 'exchange-button active' : 'exchange-button'}
            key={exchange.exchange}
            type="button"
            onClick={() => setSelectedExchange(exchange.exchange)}
          >
            <Store size={18} />
            <span>{exchange.displayName} 모드(Mode)</span>
            <Badge tone={exchange.publicMarketDataOnly ? 'good' : 'bad'}>{exchange.publicMarketDataOnly ? '공개시세(Public)' : '점검(Review)'}</Badge>
          </button>
        ))}
      </div>

      <div className="metric-grid">
        <MetricCard label="선택 거래소(Exchange)" value={statusQuery.data?.displayName ?? selected.displayName} detail={selected.exchange} />
        <MetricCard label="시세 모드(Market Data)" value={statusQuery.data?.publicMarketDataOnly ? '공개 시세(Public)' : '점검 필요(Review)'} detail={statusQuery.data?.marketData ?? '-'} />
        <MetricCard label="실거래(Real Trading)" value={statusQuery.data?.realTradingSupported ? '지원(Supported)' : '미지원(Not supported)'} detail="PAPER/SIMULATION only" />
        <MetricCard label="PAPER 총자산(Paper Equity)" value={formatMoney(modeEquity, modeCurrency)} detail={`주문(Order) ${formatMoney(orderAmount, modeCurrency)}`} />
      </div>

      <div className="status-strip" aria-label={`${modeName} PAPER 모드 상태(${modeName} paper mode status)`}>
        <div className={`status-pill ${schedulerEnabled ? 'status-pill-good' : 'status-pill-bad'}`}>
          <PlayCircle size={16} />
          <span>스케줄러(Scheduler)</span>
          <strong>{schedulerEnabled ? `${formatNumber((schedulerDelayMs ?? 0) / 1000)}s` : '꺼짐(Off)'}</strong>
        </div>
        <div className="status-pill status-pill-good">
          <Wallet size={16} />
          <span>주문(Order)</span>
          <strong>PAPER only</strong>
        </div>
        <div className="status-pill status-pill-good">
          <BarChart3 size={16} />
          <span>보유(Positions)</span>
          <strong>{formatNumber(modePositions.length)}</strong>
        </div>
        <div className="status-pill status-pill-good">
          <TrendingUp size={16} />
          <span>선택 후보(Selected)</span>
          <strong>{formatNumber(selectedCandidates.length)}</strong>
        </div>
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>거래소 상태(Exchange Status)</h2>
            <ShieldCheck size={20} />
          </div>
          <dl className="definition-list">
            <dt>거래소(Exchange)</dt>
            <dd>{statusQuery.data?.displayName ?? selected.displayName}</dd>
            <dt>사용 가능(Enabled)</dt>
            <dd>{statusQuery.data?.enabled ? '예(Yes)' : '확인 중(Checking)'}</dd>
            <dt>데이터(Data)</dt>
            <dd>{statusQuery.data?.marketData ?? '상태 조회 중(Loading)'}</dd>
            <dt>주문(Order)</dt>
            <dd>실제 주문 없음(No real orders)</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>전략 비교(Strategy Profiles)</h2>
            <FlaskConical size={20} />
          </div>
          <div className="profile-list">
            {profileCards.map((profile) => (
              <div className="profile-item" key={profile.name}>
                <div>
                  <strong>{profile.name}</strong>
                  <small>{profile.detail}</small>
                </div>
                <Badge tone={profile.tone}>준비(Ready)</Badge>
              </div>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>{modeName} PAPER</h2>
            <Wallet size={20} />
          </div>
          <dl className="definition-list">
            <dt>스케줄러(Scheduler)</dt>
            <dd>{schedulerEnabled ? '켜짐(On)' : '꺼짐(Off)'}</dd>
            <dt>주기(Delay)</dt>
            <dd>{formatNumber(schedulerDelayMs)}ms</dd>
            <dt>대상(Symbols)</dt>
            <dd>{formatNumber(targetCount)}</dd>
            <dt>현금(Cash)</dt>
            <dd>{formatMoney(modeCash, modeCurrency)}</dd>
            <dt>실현손익(Realized)</dt>
            <dd>{formatMoney(modeRealizedProfit, modeCurrency, 4)}</dd>
            <dt>포지션 평가(Position Value)</dt>
            <dd>{formatMoney(modePositionValue, modeCurrency, 4)}</dd>
            <dt>미실현손익(Unrealized)</dt>
            <dd>{formatMoney(modeUnrealizedProfit, modeCurrency, 4)}</dd>
            <dt>총손익(Total PnL)</dt>
            <dd>{formatMoney(modeTotalProfit, modeCurrency, 4)}</dd>
          </dl>
        </article>
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>{modeName} 후보(Candidates)</h2>
            <TrendingUp size={20} />
          </div>
          <div className="mvp2-list">
            {modeCandidates.slice(0, 6).map((candidate) => (
              <div className="mvp2-row" key={candidate.symbol}>
                <div>
                  <strong>{candidate.symbol}</strong>
                  <small>{candidate.reason}</small>
                </div>
                <Badge tone={candidate.decision === 'SELECTED' ? 'good' : 'neutral'}>{candidate.decision}</Badge>
              </div>
            ))}
            {modeCandidates.length === 0 ? <p>후보 조회 대기 중(Waiting for candidates)</p> : null}
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>{modeName} 포지션(Positions)</h2>
            <BarChart3 size={20} />
          </div>
          <div className="mvp2-list">
            {modePositions.map((position) => (
              <div className="mvp2-row" key={position.symbol}>
                <div>
                  <strong>{position.symbol}</strong>
                  <small>{position.summary}</small>
                </div>
              </div>
            ))}
            {modePositions.length === 0 ? <p>보유 포지션 없음(No positions)</p> : null}
          </div>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>{modeName} 이력(History)</h2>
            <Clock3 size={20} />
          </div>
          <div className="mvp2-list">
            {modeHistory.slice(0, 6).map((history) => (
              <div className="mvp2-row" key={`${history.symbol}-${history.createdAt}-${history.message}`}>
                <div>
                  <strong>{history.symbol}</strong>
                  <small>{history.reason} / {history.message} / {formatDateTime(history.createdAt)}</small>
                </div>
                <Badge tone={history.status === 'FILLED' ? 'good' : history.status === 'REJECTED' ? 'warn' : 'neutral'}>
                  {history.badge}
                </Badge>
              </div>
            ))}
            {modeHistory.length === 0 ? <p>거래 이력 없음(No history)</p> : null}
          </div>
        </article>
      </div>
    </section>
  );
}

function isValuedPosition(position: Mvp2PaperPositionResponse): position is Mvp2PaperPositionValuationResponse {
  return 'currentPrice' in position;
}

function positionSummary(position: Mvp2PaperPositionResponse) {
  const base = `Avg ${formatNumber(position.averageBuyPrice, 6)} / Qty ${formatNumber(position.quantity, 8)}`;
  if (!isValuedPosition(position)) {
    return base;
  }
  return `${base} / Now ${formatNumber(position.currentPrice, 6)} / Value ${formatNumber(position.positionValue, 4)} / PnL ${formatNumber(position.unrealizedProfit, 4)} (${formatNumber(position.unrealizedProfitRate, 2)}%)`;
}

function upbitPositionSummary(position: {
  quantity: string;
  averageBuyPrice: string;
  currentPrice: string;
  positionValue: string;
  unrealizedProfit: string;
  unrealizedProfitRate: string;
}) {
  return `Avg ${formatKrw(position.averageBuyPrice)} / Qty ${formatNumber(position.quantity, 8)} / Now ${formatKrw(position.currentPrice)} / Value ${formatKrw(position.positionValue)} / PnL ${formatKrw(position.unrealizedProfit)} (${formatNumber(position.unrealizedProfitRate, 2)}%)`;
}

function formatMoney(value: string | number | null | undefined, currency: 'KRW' | 'USDT', fractionDigits = 2) {
  if (currency === 'KRW') {
    return formatKrw(value);
  }
  return `${formatNumber(value, fractionDigits)} USDT`;
}

function selectExchange(exchanges: Mvp2ExchangeResponse[], selectedExchange: Mvp2Exchange) {
  return exchanges.find((exchange) => exchange.exchange === selectedExchange)
    ?? exchanges[0]
    ?? {
      exchange: selectedExchange,
      displayName: selectedExchange === 'BINANCE' ? 'Binance' : 'Upbit',
      enabled: true,
      publicMarketDataOnly: true,
      statusPath: `/api/mvp2/exchanges/${selectedExchange}/status`,
    };
}
