import { useQuery } from '@tanstack/react-query';
import { Bell, Bot, MonitorCog, Radio } from 'lucide-react';
import React from 'react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { detectOperatingSystem, operatingSystemGuide } from '../../shared/os/operatingSystem';

export function SystemPage() {
  const { exchange } = useExchangeMode();
  const { data, isLoading, error } = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: POLLING_INTERVALS.system,
  });
  const { data: provider } = useQuery({
    queryKey: queryKeys.marketProviderStatus(),
    queryFn: api.marketProviderStatus,
    refetchInterval: POLLING_INTERVALS.system,
  });

  if (isLoading) return <div className="page"><p className="text-muted-foreground">불러오는 중...</p></div>;
  if (error || !data) return <div className="page"><div className="error-panel">시스템 조회 실패</div></div>;

  const osGuide = operatingSystemGuide(detectOperatingSystem());
  const snapshotCount = exchange === 'BINANCE' ? provider?.binanceSnapshotCount : provider?.upbitSnapshotCount;
  const freshCount = exchange === 'BINANCE' ? provider?.binanceFreshSnapshotCount : provider?.upbitFreshSnapshotCount;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">시스템</h1>
          <p className="page-subtitle">운영 환경 · 스케줄러 · 시세 · 알림 상태</p>
        </div>
        <span className={`badge ${data.safety.killSwitchEnabled ? 'badge-destructive' : 'badge-success'}`}>
          {data.safety.killSwitchEnabled ? '🔴 거래 차단' : '🟢 PAPER 운영'}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <SPanel title="운영 환경 (OS)" icon={<MonitorCog size={16} />}>
          <Dl rows={[
            ['OS', osGuide.label],
            ['Shell', osGuide.shell],
            ['실행 스크립트', <Code>{osGuide.runScript}</Code>],
            ['스키마 스크립트', <Code>{osGuide.schemaScript}</Code>],
            ['작업 경로', <Code>{osGuide.workspacePath}</Code>],
            ['단축키 modifier', osGuide.shortcutModifier],
          ]} />
        </SPanel>

        <SPanel title="스케줄러" icon={<Bot size={16} />}>
          <Dl rows={[
            ['후보 스캔', data.scheduler.candidateEnabled ? `${data.scheduler.candidateFixedDelayMs / 1000}s` : '꺼짐'],
            ['후보 거래소', (data.scheduler.candidateExchanges?.join(', ')) || data.scheduler.candidateExchange],
            ['청산 스캔', data.scheduler.exitEnabled ? `${data.scheduler.exitFixedDelayMs / 1000}s` : '꺼짐'],
            ['청산 대상', `${data.scheduler.exitPositionMarketCount}개`],
          ]} />
        </SPanel>

        <SPanel title="시세 Provider" icon={<Radio size={16} />}>
          <Dl rows={[
            ['Provider', provider?.provider ?? data.marketProvider.provider],
            ['WebSocket', provider?.webSocketEnabled ? '켜짐' : '꺼짐'],
            ['Snapshot', `${snapshotCount ?? '-'}`],
            ['Fresh / Stale', provider ? `${provider.freshSnapshotCount} / ${provider.staleSnapshotCount}` : '-'],
            ['선택 거래소 Fresh', `${freshCount ?? '-'}`],
            ['자동 실행', provider ? (provider.automationReady ? '정상' : `차단: ${provider.automationBlockReason}`) : '-'],
          ]} />
        </SPanel>

        <SPanel title="알림 · Telegram" icon={<Bell size={16} />}>
          <Dl rows={[
            ['알림', data.notification.enabled ? '켜짐' : '꺼짐'],
            ['체결 알림', data.notification.sendFilled ? '전송' : '건너뜀'],
            ['거절 알림', data.notification.sendRejected ? '전송' : '건너뜀'],
            ['Telegram', data.telegram.configured ? '설정됨' : '미설정'],
            ['수동 PAPER', data.telegram.manualPaperExecutionEnabled ? '허용' : '차단'],
          ]} />
        </SPanel>
      </div>
    </div>
  );
}

function SPanel({ title, icon, children }: { title: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="section">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-muted-foreground">{icon}</span>
        <h2 className="section-title mb-0">{title}</h2>
      </div>
      {children}
    </div>
  );
}

function Code({ children }: { children: React.ReactNode }) {
  return <code className="rounded bg-muted px-1 py-0.5 text-xs font-mono">{children}</code>;
}

function Dl({ rows }: { rows: [string, React.ReactNode][] }) {
  return (
    <dl className="space-y-1.5">
      {rows.map(([label, value]) => (
        <div key={label} className="flex items-start gap-2 text-sm">
          <dt className="w-32 shrink-0 text-muted-foreground">{label}</dt>
          <dd className="font-medium break-all">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
