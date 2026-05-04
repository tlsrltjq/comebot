import { useQuery } from '@tanstack/react-query';
import { ShieldCheck, ShieldX } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';
import { formatKrw, formatNumber } from '../../shared/format';

export function DashboardPage() {
  const { data, error, isLoading } = useQuery({
    queryKey: queryKeys.system,
    queryFn: api.systemStatus,
    refetchInterval: 5_000,
  });

  if (isLoading) {
    return <div className="page-state">상태를 불러오는 중</div>;
  }

  if (error || !data) {
    return <ErrorPanel error={error} />;
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>운영 대시보드(Dashboard)</h1>
          <p>자동 PAPER 거래 상태(Auto paper trading status)를 확인합니다.</p>
        </div>
        <Badge tone={data.safety.killSwitchEnabled ? 'bad' : 'good'}>
          {data.safety.killSwitchEnabled ? '중지됨(Kill switch ON)' : '자동 실행 가능(Auto ready)'}
        </Badge>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      <div className="metric-grid">
        <MetricCard label="데이터베이스(Database)" value={data.database.connected ? '연결됨(Connected)' : '끊김(Disconnected)'} />
        <MetricCard label="시세 제공자(Market Provider)" value={data.marketProvider.provider} detail={data.marketProvider.externalProvider ? '외부(External)' : '메모리(In memory)'} />
        <MetricCard label="전략(Strategy)" value={data.strategy.strategyName} detail={`거래금액(Order) ${formatNumber(data.strategy.orderAmount, 0)} KRW`} />
        <MetricCard label="최대 주문(Max Order)" value={formatKrw(data.risk.maxOrderAmount)} detail={data.risk.allowedMarkets.join(', ')} />
      </div>

      <div className="section-grid">
        <article className="panel">
          <h2>안전장치(Safety)</h2>
          <div className="status-row">
            {data.safety.killSwitchEnabled ? <ShieldX size={22} /> : <ShieldCheck size={22} />}
            <span>{data.safety.killSwitchEnabled ? '거래 흐름 차단 중(Blocked)' : '자동 PAPER 거래 허용(Auto paper allowed)'}</span>
          </div>
        </article>

        <article className="panel">
          <h2>자동 실행(Schedulers)</h2>
          <dl className="definition-list">
            <dt>전략 실행(Trading)</dt>
            <dd>{data.scheduler.enabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>후보 실행(Candidate)</dt>
            <dd>{data.scheduler.candidateEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>후보 마켓(Candidate markets)</dt>
            <dd>{data.scheduler.candidateMarkets.join(', ') || '-'}</dd>
          </dl>
        </article>

        <article className="panel">
          <h2>알림(Notifications)</h2>
          <dl className="definition-list">
            <dt>사용(Enabled)</dt>
            <dd>{data.notification.enabled ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>체결(Filled)</dt>
            <dd>{data.notification.sendFilled ? '전송(Send)' : '건너뜀(Skip)'}</dd>
            <dt>거절(Rejected)</dt>
            <dd>{data.notification.sendRejected ? '전송(Send)' : '건너뜀(Skip)'}</dd>
          </dl>
        </article>

        <article className="panel">
          <h2>텔레그램(Telegram)</h2>
          <dl className="definition-list">
            <dt>사용(Enabled)</dt>
            <dd>{data.telegram.enabled ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>설정(Configured)</dt>
            <dd>{data.telegram.configured ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>수신(Inbound)</dt>
            <dd>{data.telegram.inboundEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>수동 PAPER 실행(Manual paper run)</dt>
            <dd>{data.telegram.manualPaperExecutionEnabled ? '허용(Allowed)' : '차단(Blocked)'}</dd>
          </dl>
        </article>
      </div>
    </section>
  );
}
