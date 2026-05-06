import { useMemo, useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart3, CheckCircle2, FlaskConical, ShieldCheck, Store, TrendingUp } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import type { Mvp2Exchange, Mvp2ExchangeResponse } from '../../shared/api/types';
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

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>MVP2 실험 대시보드(Experiment Dashboard)</h1>
          <p>거래소별 public market data와 전략 profile 실험 준비 상태를 확인합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      {exchangesQuery.error ? <ErrorPanel title="MVP2 거래소 조회 실패(Exchange list failed)" error={exchangesQuery.error} /> : null}
      {statusQuery.error ? <ErrorPanel title="MVP2 거래소 상태 조회 실패(Exchange status failed)" error={statusQuery.error} /> : null}

      <div className="exchange-switch" aria-label="거래소 선택(Exchange selector)">
        {exchanges.map((exchange) => (
          <button
            className={selected.exchange === exchange.exchange ? 'exchange-button active' : 'exchange-button'}
            key={exchange.exchange}
            type="button"
            onClick={() => setSelectedExchange(exchange.exchange)}
          >
            <Store size={18} />
            <span>{exchange.displayName}</span>
            <Badge tone={exchange.publicMarketDataOnly ? 'good' : 'bad'}>{exchange.publicMarketDataOnly ? '공개시세(Public)' : '점검(Review)'}</Badge>
          </button>
        ))}
      </div>

      <div className="metric-grid">
        <MetricCard label="선택 거래소(Exchange)" value={statusQuery.data?.displayName ?? selected.displayName} detail={selected.exchange} />
        <MetricCard label="시세 모드(Market Data)" value={statusQuery.data?.publicMarketDataOnly ? '공개 시세(Public)' : '점검 필요(Review)'} detail={statusQuery.data?.marketData ?? '-'} />
        <MetricCard label="실거래(Real Trading)" value={statusQuery.data?.realTradingSupported ? '지원(Supported)' : '미지원(Not supported)'} detail="PAPER/SIMULATION only" />
        <MetricCard label="전략 Profile" value="3개(3 profiles)" detail="Stable / Aggressive / Defensive" />
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
            <h2>다음 연결(Next)</h2>
            <BarChart3 size={20} />
          </div>
          <div className="mvp2-next-list">
            <NextItem icon={<CheckCircle2 size={17} />} text="거래소별 상태 API 연결" />
            <NextItem icon={<TrendingUp size={17} />} text="profile별 PAPER/SIMULATION 성과 분리" />
            <NextItem icon={<FlaskConical size={17} />} text="Leaderboard 수익률/승률/낙폭 비교" />
          </div>
        </article>
      </div>
    </section>
  );
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

function NextItem({ icon, text }: { icon: ReactNode; text: string }) {
  return (
    <div className="next-item">
      {icon}
      <span>{text}</span>
    </div>
  );
}
