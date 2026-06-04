import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Power, ShieldCheck, TimerReset, TrendingDown, TrendingUp } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { POLLING_INTERVALS } from '../../shared/api/polling';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { formatDateTime } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { cn } from '@/lib/utils';

export function TradePage() {
  const { exchange } = useExchangeMode();
  const queryClient = useQueryClient();
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: POLLING_INTERVALS.autoRun,
  });
  const historyQuery = useQuery({
    queryKey: queryKeys.history(exchange, undefined, 10),
    queryFn: () => api.history(exchange, undefined, 10),
    refetchInterval: POLLING_INTERVALS.autoRun,
  });
  const schedulerMutation = useMutation({
    mutationFn: api.schedulerControl,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.system(exchange) });
    },
  });

  const sys = systemQuery.data;
  const autoOn = Boolean(sys?.scheduler.candidateEnabled && sys?.scheduler.exitEnabled);
  const candidateDelay = sys?.scheduler.candidateFixedDelayMs === 30000 ? 30000 : 60000;
  const rows = historyQuery.data ?? [];
  const filledCount = rows.filter((r) => r.orderStatus === 'FILLED').length;
  const rejectedCount = rows.filter((r) => r.orderStatus === 'REJECTED').length;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">자동 실행</h1>
          <p className="page-subtitle">후보·청산 스케줄러 제어 + 최근 PAPER 실행 이력</p>
        </div>
        <div className="live-status">
          <span className={cn('status-dot', (systemQuery.isFetching || historyQuery.isFetching) ? 'warn' : 'live')} />
          {historyQuery.dataUpdatedAt
            ? new Date(historyQuery.dataUpdatedAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
            : ''}
        </div>
      </div>

      {systemQuery.error && <ErrorPanel title="상태 조회 실패" error={systemQuery.error} />}
      {historyQuery.error && <ErrorPanel title="이력 조회 실패" error={historyQuery.error} />}
      {schedulerMutation.error && <ErrorPanel title="스케줄러 제어 실패" error={schedulerMutation.error} />}

      {sys && (
        <>
          {/* ── Control panel ── */}
          <div className="section mb-4">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="section-title mb-0">자동매매 제어</h2>
                <p className="text-sm text-muted-foreground mt-1">
                  {autoOn ? '후보 BUY·청산 SELL 평가 실행 중' : '후보 BUY·청산 SELL 평가 정지됨'}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className={cn('badge', autoOn ? 'badge-success' : 'badge-warning')}>
                  {autoOn ? '🟢 실행 중' : '🟡 정지됨'}
                </span>
                <button
                  type="button"
                  disabled={schedulerMutation.isPending}
                  onClick={() => schedulerMutation.mutate({ autoTradingEnabled: !autoOn })}
                  className={cn(
                    'flex items-center gap-1.5 rounded-md px-4 py-2 text-sm font-semibold transition-colors disabled:opacity-50',
                    autoOn
                      ? 'border border-border bg-background hover:bg-muted'
                      : 'bg-primary text-primary-foreground hover:bg-primary/90',
                  )}
                >
                  <Power size={15} />
                  {autoOn ? '끄기' : '켜기'}
                </button>
              </div>
            </div>

            {/* Scheduler steps */}
            <div className="grid grid-cols-1 gap-2 sm:grid-cols-3 mb-4">
              <Step icon={<TrendingUp size={16} />} title="후보 스캔"
                ok={sys.scheduler.candidateEnabled}
                status={sys.scheduler.candidateEnabled ? '켜짐' : '꺼짐'}
                detail={`${sys.scheduler.candidateFixedDelayMs / 1000}s · ${exchangeList(sys.scheduler.candidateExchanges, sys.scheduler.candidateExchange)}`} />
              <Step icon={<TrendingDown size={16} />} title="청산 스캔"
                ok={sys.scheduler.exitEnabled}
                status={sys.scheduler.exitEnabled ? '켜짐' : '꺼짐'}
                detail={`${sys.scheduler.exitFixedDelayMs / 1000}s · 보유 ${sys.scheduler.exitPositionMarketCount}개`} />
              <Step icon={<ShieldCheck size={16} />} title="실행 방식"
                ok
                status="PAPER 전용"
                detail="실제 주문 API 없음" />
            </div>

            {/* Interval control */}
            <div className="flex items-center gap-3">
              <span className="text-sm text-muted-foreground">후보 조회 주기</span>
              {([30000, 60000] as const).map((d) => (
                <button
                  key={d}
                  type="button"
                  disabled={schedulerMutation.isPending}
                  onClick={() => schedulerMutation.mutate({ candidateFixedDelayMs: d })}
                  className={cn(
                    'flex items-center gap-1 rounded-md border px-3 py-1.5 text-sm transition-colors disabled:opacity-50',
                    candidateDelay === d
                      ? 'bg-primary text-primary-foreground border-primary'
                      : 'border-border bg-background hover:bg-muted',
                  )}
                >
                  <TimerReset size={13} />{d / 1000}초
                </button>
              ))}
            </div>
          </div>

          {/* ── Scheduler detail cards ── */}
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3 mb-4">
            <InfoPanel title="후보 스케줄러" ok={sys.scheduler.candidateEnabled}>
              <Dl rows={[
                ['거래소', exchangeList(sys.scheduler.candidateExchanges, sys.scheduler.candidateExchange)],
                ['주기', `${sys.scheduler.candidateFixedDelayMs} ms`],
                ['마켓', sys.scheduler.candidateMarkets.join(', ')],
              ]} />
            </InfoPanel>
            <InfoPanel title="청산 스케줄러" ok={sys.scheduler.exitEnabled}>
              <Dl rows={[
                ['주기', `${sys.scheduler.exitFixedDelayMs} ms`],
                ['거래소', exchangeList(sys.scheduler.exitExchanges, sys.scheduler.exitExchange)],
                ['보유 마켓', `${sys.scheduler.exitPositionMarketCount}개`],
                ['HOLD 저장', sys.scheduler.exitSaveHoldHistory ? '저장' : '건너뜀'],
              ]} />
            </InfoPanel>
            <InfoPanel title="전략 스케줄러 (구버전)" ok={sys.scheduler.enabled}>
              <Dl rows={[
                ['주기', `${sys.scheduler.fixedDelayMs} ms`],
                ['마켓', sys.scheduler.markets.join(', ')],
              ]} />
            </InfoPanel>
          </div>
        </>
      )}

      {/* ── Recent runs ── */}
      <div className="section">
        <div className="flex items-center justify-between mb-3">
          <h2 className="section-title mb-0">최근 자동 실행 (10건)</h2>
          <div className="flex gap-2">
            <span className="badge badge-success">체결 {filledCount}</span>
            {rejectedCount > 0 && <span className="badge badge-warning">거절 {rejectedCount}</span>}
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>시각</th><th>마켓</th><th>신호</th><th>주문 상태</th><th>메시지</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td className="text-muted-foreground text-xs whitespace-nowrap">{formatDateTime(row.createdAt)}</td>
                  <td className="font-semibold">{row.market}</td>
                  <td>{row.signalType ?? '-'}</td>
                  <td>
                    <span className={cn('badge',
                      row.orderStatus === 'FILLED' ? 'badge-success'
                        : row.orderStatus === 'REJECTED' ? 'badge-warning'
                          : row.orderStatus === 'FAILED' ? 'badge-destructive'
                            : 'badge-outline',
                    )}>
                      {row.orderStatus ?? 'NO_ORDER'}
                    </span>
                  </td>
                  <td className="text-muted-foreground text-xs">{row.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!historyQuery.isLoading && rows.length === 0 && (
            <div className="empty-state"><strong>실행 이력 없음</strong></div>
          )}
        </div>
      </div>
    </div>
  );
}

function Step({ icon, title, ok, status, detail }: { icon: ReactNode; title: string; ok: boolean; status: string; detail: string }) {
  return (
    <div className={cn('rounded-lg border p-3', ok ? 'border-green-200 bg-green-50' : 'border-amber-200 bg-amber-50')}>
      <div className="flex items-center gap-2 mb-1">
        <span className={cn(ok ? 'text-green-600' : 'text-amber-600')}>{icon}</span>
        <span className="text-sm font-semibold">{title}</span>
      </div>
      <p className={cn('text-sm font-medium', ok ? 'text-green-700' : 'text-amber-700')}>{status}</p>
      <p className="text-xs text-muted-foreground mt-0.5">{detail}</p>
    </div>
  );
}

function InfoPanel({ title, ok, children }: { title: string; ok: boolean; children: ReactNode }) {
  return (
    <div className="section">
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-sm font-semibold">{title}</h3>
        <span className={cn('badge', ok ? 'badge-success' : 'badge-warning')}>{ok ? '켜짐' : '꺼짐'}</span>
      </div>
      {children}
    </div>
  );
}

function Dl({ rows }: { rows: [string, string][] }) {
  return (
    <dl className="space-y-1">
      {rows.map(([label, value]) => (
        <div key={label} className="flex items-start gap-2 text-sm">
          <dt className="w-20 shrink-0 text-muted-foreground">{label}</dt>
          <dd className="font-medium">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

function exchangeList(exchanges: string[] | undefined, fallback: string) {
  return exchanges && exchanges.length > 0 ? exchanges.join(', ') : fallback;
}
