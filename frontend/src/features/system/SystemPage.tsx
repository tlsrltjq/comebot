import { useQuery } from '@tanstack/react-query';
import { Bell, Bot, MonitorCog, Radio } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { detectOperatingSystem, operatingSystemGuide } from '../../shared/os/operatingSystem';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';

export function SystemPage() {
  const { exchange } = useExchangeMode();
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: POLLING_INTERVALS.system,
  });
  const providerQuery = useQuery({
    queryKey: queryKeys.marketProviderStatus(),
    queryFn: api.marketProviderStatus,
    refetchInterval: POLLING_INTERVALS.system,
  });

  if (systemQuery.isLoading) {
    return <div className="page-state">시스템 상태를 불러오는 중</div>;
  }

  if (systemQuery.error || !systemQuery.data) {
    return <ErrorPanel error={systemQuery.error} />;
  }

  const data = systemQuery.data;
  const osGuide = operatingSystemGuide(detectOperatingSystem());
  const snapshotCount = exchange === 'BINANCE' ? providerQuery.data?.binanceSnapshotCount : providerQuery.data?.upbitSnapshotCount;
  const freshSnapshotCount = exchange === 'BINANCE' ? providerQuery.data?.binanceFreshSnapshotCount : providerQuery.data?.upbitFreshSnapshotCount;

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>시스템(System)</h1>
          <p>운영 환경, 스케줄러, 알림 상태를 OS별 안내와 함께 확인합니다.</p>
        </div>
        <Badge tone={data.safety.killSwitchEnabled ? 'bad' : 'good'}>
          {data.safety.killSwitchEnabled ? '차단(Blocked)' : 'PAPER 운영'}
        </Badge>
      </header>

      <div className="audit-strip" aria-label="운영 환경 안내(System guide constraints)">
        <Badge tone="info">{osGuide.label}</Badge>
        <span>OS별 차이는 실행 스크립트, 경로, shell 안내만 바꾸며 거래 기능과 안전 제약은 동일합니다.</span>
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>운영 환경(OS Guide)</h2>
            <MonitorCog size={20} />
          </div>
          <dl className="definition-list">
            <dt>감지(OS)</dt>
            <dd>{osGuide.label}</dd>
            <dt>Shell</dt>
            <dd>{osGuide.shell}</dd>
            <dt>실행(Run)</dt>
            <dd><code>{osGuide.runScript}</code></dd>
            <dt>스키마(Schema)</dt>
            <dd><code>{osGuide.schemaScript}</code></dd>
            <dt>작업 경로(Path)</dt>
            <dd><code>{osGuide.workspacePath}</code></dd>
            <dt>단축키(Shortcut)</dt>
            <dd>{osGuide.shortcutModifier}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>스케줄러(Schedulers)</h2>
            <Bot size={20} />
          </div>
          <dl className="definition-list">
            <dt>전략(Trading)</dt>
            <dd>{data.scheduler.enabled ? `${data.scheduler.fixedDelayMs / 1000}s` : '꺼짐(Disabled)'}</dd>
            <dt>후보(Candidate)</dt>
            <dd>{data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐(Disabled)'}</dd>
            <dt>후보 거래소</dt>
            <dd>{exchangeList(data.scheduler.candidateExchanges, data.scheduler.candidateExchange)}</dd>
            <dt>청산(Exit)</dt>
            <dd>{data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐(Disabled)'}</dd>
            <dt>청산 대상</dt>
            <dd>{data.scheduler.exitPositionMarketCount}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>시세 Provider</h2>
            <Radio size={20} />
          </div>
          <dl className="definition-list">
            <dt>Provider</dt>
            <dd>{providerQuery.data?.provider ?? data.marketProvider.provider}</dd>
            <dt>외부 Provider</dt>
            <dd>{(providerQuery.data?.externalProvider ?? data.marketProvider.externalProvider) ? '예(Yes)' : '아니오(No)'}</dd>
            <dt>WebSocket</dt>
            <dd>{providerQuery.data?.webSocketEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>Snapshot</dt>
            <dd>{snapshotCount ?? '-'}</dd>
            <dt>Fresh/Stale</dt>
            <dd>
              {providerQuery.data
                ? `${providerQuery.data.freshSnapshotCount}/${providerQuery.data.staleSnapshotCount} (${providerQuery.data.orderStaleMs}ms)`
                : '-'}
            </dd>
            <dt>선택 거래소 Fresh</dt>
            <dd>{freshSnapshotCount ?? '-'}</dd>
            <dt>자동 실행 보호</dt>
            <dd>
              {providerQuery.data
                ? providerQuery.data.automationReady
                  ? '정상(Ready)'
                  : `차단(Blocked): ${providerQuery.data.automationBlockReason}`
                : '-'}
            </dd>
            <dt>Message</dt>
            <dd>{providerQuery.data?.message ?? '-'}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>알림과 Telegram</h2>
            <Bell size={20} />
          </div>
          <dl className="definition-list">
            <dt>알림(Enabled)</dt>
            <dd>{data.notification.enabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>체결 알림(Filled)</dt>
            <dd>{data.notification.sendFilled ? '전송(Send)' : '건너뜀(Skip)'}</dd>
            <dt>거절 알림(Rejected)</dt>
            <dd>{data.notification.sendRejected ? '전송(Send)' : '건너뜀(Skip)'}</dd>
            <dt>Telegram</dt>
            <dd>{data.telegram.configured ? '설정됨(Configured)' : '미설정(Not configured)'}</dd>
            <dt>수동 PAPER</dt>
            <dd>{data.telegram.manualPaperExecutionEnabled ? '허용(Allowed)' : '차단(Blocked)'}</dd>
          </dl>
        </article>
      </div>
    </section>
  );
}

function exchangeList(exchanges: string[] | undefined, fallback: string) {
  return exchanges && exchanges.length > 0 ? exchanges.join(', ') : fallback;
}
