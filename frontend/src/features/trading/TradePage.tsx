import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Power, ShieldCheck, TimerReset, TrendingDown, TrendingUp } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { LiveStatus } from '../../shared/ui/LiveStatus';
import { formatDateTime } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

const AUTO_RUN_REFRESH_MS = 2_000;

export function TradePage() {
  const { exchange } = useExchangeMode();
  const queryClient = useQueryClient();
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: AUTO_RUN_REFRESH_MS,
  });
  const historyQuery = useQuery({
    queryKey: queryKeys.history(exchange, undefined, 10),
    queryFn: () => api.history(exchange, undefined, 10),
    refetchInterval: AUTO_RUN_REFRESH_MS,
  });
  const schedulerMutation = useMutation({
    mutationFn: api.schedulerControl,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.system(exchange) });
    },
  });
  const autoTradingEnabled = Boolean(systemQuery.data?.scheduler.candidateEnabled && systemQuery.data.scheduler.exitEnabled);
  const candidateDelay = systemQuery.data?.scheduler.candidateFixedDelayMs === 30000 ? 30000 : 60000;
  const recentRows = historyQuery.data ?? [];
  const filledRuns = recentRows.filter((row) => row.orderStatus === 'FILLED').length;
  const rejectedRuns = recentRows.filter((row) => row.orderStatus === 'REJECTED').length;
  const latestRun = recentRows[0];

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>자동매매 운영(Auto Trading Operations)</h1>
          <p>후보 스케줄러와 청산 스케줄러를 제어하고 자동 PAPER 실행 이력을 확인합니다.</p>
        </div>
        <LiveStatus updatedAt={historyQuery.dataUpdatedAt || systemQuery.dataUpdatedAt} isFetching={systemQuery.isFetching || historyQuery.isFetching} intervalMs={AUTO_RUN_REFRESH_MS} />
      </header>

      {systemQuery.error ? <ErrorPanel title="자동매매 상태 조회 실패(Auto trading status failed)" error={systemQuery.error} /> : null}
      {historyQuery.error ? <ErrorPanel title="실행 이력 조회 실패(History failed)" error={historyQuery.error} /> : null}
      {schedulerMutation.error ? <ErrorPanel title="자동매매 설정 실패(Auto control failed)" error={schedulerMutation.error} /> : null}
      {systemQuery.data ? (
        <>
        <article className="panel auto-control-panel">
          <div className="panel-title-row">
            <h2>자동매매 제어(Auto Trading Control)</h2>
            <Badge tone={autoTradingEnabled ? 'good' : 'warn'}>
              {autoTradingEnabled ? '실행 중(Running)' : '정지됨(Stopped)'}
            </Badge>
          </div>
          <div className="auto-control-layout">
            <div className="auto-control-action">
              <div>
                <span>제어 범위(Control scope)</span>
                <strong>{autoTradingEnabled ? '후보 BUY와 보유 PAPER SELL 평가 중' : '후보 BUY와 보유 PAPER SELL 평가 정지'}</strong>
                <p>이 버튼은 후보 스케줄러와 청산 스케줄러를 함께 제어합니다. 수동 BUY와 실제 주문은 제공하지 않습니다.</p>
              </div>
              <div className="auto-control-buttons">
                <button
                  className={autoTradingEnabled ? 'button button-secondary' : 'button button-primary'}
                  type="button"
                  disabled={schedulerMutation.isPending}
                  onClick={() => schedulerMutation.mutate({ autoTradingEnabled: !autoTradingEnabled })}
                >
                  <Power size={16} />
                  {autoTradingEnabled ? '끄기(Turn off)' : '켜기(Turn on)'}
                </button>
              </div>
            </div>
            <div className="scheduler-flow" aria-label="자동매매 실행 흐름(Auto trading flow)">
              <SchedulerStep
                icon={<TrendingUp size={18} />}
                title="후보 스케줄러(Candidate)"
                status={systemQuery.data.scheduler.candidateEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}
                detail={`${systemQuery.data.scheduler.candidateFixedDelayMs / 1000}s / ${exchangeList(systemQuery.data.scheduler.candidateExchanges, systemQuery.data.scheduler.candidateExchange)}`}
                good={systemQuery.data.scheduler.candidateEnabled}
              />
              <SchedulerStep
                icon={<TrendingDown size={18} />}
                title="청산 스케줄러(Exit)"
                status={systemQuery.data.scheduler.exitEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}
                detail={`${systemQuery.data.scheduler.exitFixedDelayMs / 1000}s / 보유 ${systemQuery.data.scheduler.exitPositionMarketCount}개`}
                good={systemQuery.data.scheduler.exitEnabled}
              />
              <SchedulerStep
                icon={<ShieldCheck size={18} />}
                title="실행 방식(Mode)"
                status="PAPER 전용(Paper only)"
                detail="실제 주문 API 없음"
                good
              />
            </div>
          </div>
          <div className="control-grid">
            <div className="control-group">
              <span className="control-label">후보 조회 주기(Candidate interval)</span>
              <div className="segmented-row compact-segmented" aria-label="후보 조회 주기(Candidate interval)">
                {[30000, 60000].map((delay) => (
                  <button
                    key={delay}
                    className={candidateDelay === delay ? 'button button-primary' : 'button button-secondary'}
                    type="button"
                    disabled={schedulerMutation.isPending}
                    onClick={() => schedulerMutation.mutate({ candidateFixedDelayMs: delay as 30000 | 60000 })}
                  >
                    <TimerReset size={16} />
                    {delay / 1000}초({delay / 1000}s)
                  </button>
                ))}
              </div>
            </div>
          </div>
        </article>
        <div className="section-grid">
          <article className="panel">
            <div className="panel-title-row">
              <h2>전략 스케줄러(Legacy Trading Scheduler)</h2>
              <Badge tone={systemQuery.data.scheduler.enabled ? 'good' : 'warn'}>
                {systemQuery.data.scheduler.enabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}
              </Badge>
            </div>
            <dl className="definition-list">
              <dt>주기(Delay)</dt>
              <dd>{systemQuery.data.scheduler.fixedDelayMs} ms</dd>
              <dt>마켓(Markets)</dt>
              <dd>{systemQuery.data.scheduler.markets.join(', ')}</dd>
            </dl>
          </article>
          <article className="panel">
            <div className="panel-title-row">
              <h2>후보 스케줄러(Candidate Scheduler)</h2>
              <Badge tone={systemQuery.data.scheduler.candidateEnabled ? 'good' : 'warn'}>
                {systemQuery.data.scheduler.candidateEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}
              </Badge>
            </div>
            <dl className="definition-list">
              <dt>거래소(Exchange)</dt>
              <dd>{exchangeList(systemQuery.data.scheduler.candidateExchanges, systemQuery.data.scheduler.candidateExchange)}</dd>
              <dt>주기(Delay)</dt>
              <dd>{systemQuery.data.scheduler.candidateFixedDelayMs} ms</dd>
              <dt>마켓(Markets)</dt>
              <dd>{systemQuery.data.scheduler.candidateMarkets.join(', ')}</dd>
            </dl>
          </article>
          <article className="panel">
            <div className="panel-title-row">
              <h2>청산 스케줄러(Exit Scheduler)</h2>
              <Badge tone={systemQuery.data.scheduler.exitEnabled ? 'good' : 'warn'}>
                {systemQuery.data.scheduler.exitEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}
              </Badge>
            </div>
            <dl className="definition-list">
              <dt>주기(Delay)</dt>
              <dd>{systemQuery.data.scheduler.exitFixedDelayMs} ms</dd>
              <dt>대상 거래소(Exchange)</dt>
              <dd>{exchangeList(systemQuery.data.scheduler.exitExchanges, systemQuery.data.scheduler.exitExchange)}</dd>
              <dt>보유 마켓(Position markets)</dt>
              <dd>{systemQuery.data.scheduler.exitPositionMarketCount}</dd>
              <dt>HOLD 기록(HOLD history)</dt>
              <dd>{systemQuery.data.scheduler.exitSaveHoldHistory ? '저장(Save)' : '저장 안 함(Skip)'}</dd>
            </dl>
          </article>
        </div>
        </>
      ) : null}

      <article className="panel">
        <div className="panel-title-row">
          <h2>최근 자동 실행(Recent auto runs)</h2>
          <Badge tone={rejectedRuns > 0 ? 'warn' : 'info'}>{recentRows.length}건(Runs)</Badge>
        </div>
        <div className="run-summary-grid">
          <div>
            <span>최근 결과(Latest)</span>
            <strong>{latestRun ? `${latestRun.market} / ${latestRun.orderStatus ?? 'NO_ORDER'}` : '-'}</strong>
          </div>
          <div>
            <span>체결(Filled)</span>
            <strong>{filledRuns}</strong>
          </div>
          <div>
            <span>거절(Rejected)</span>
            <strong>{rejectedRuns}</strong>
          </div>
        </div>
        <div className="table-wrap table-in-panel">
          <table>
            <thead>
              <tr>
                <th>시각(Time)</th>
                <th>마켓(Market)</th>
                <th>신호(Signal)</th>
                <th>주문(Order)</th>
                <th>메시지(Message)</th>
              </tr>
            </thead>
            <tbody>
              {(historyQuery.data ?? []).map((row) => (
                <tr key={row.id}>
                  <td>{formatDateTime(row.createdAt)}</td>
                  <td>{row.market}</td>
                  <td>{row.signalType ?? '-'}</td>
                  <td>
                    <Badge tone={row.orderStatus === 'FILLED' ? 'good' : row.orderStatus === 'REJECTED' ? 'warn' : 'neutral'}>
                      {row.orderStatus ?? 'NO_ORDER'}
                    </Badge>
                  </td>
                  <td>{row.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </article>
    </section>
  );
}

function SchedulerStep({
  icon,
  title,
  status,
  detail,
  good,
}: {
  icon: ReactNode;
  title: string;
  status: string;
  detail: string;
  good: boolean;
}) {
  return (
    <div className={`scheduler-step ${good ? 'scheduler-step-good' : 'scheduler-step-warn'}`}>
      {icon}
      <div>
        <span>{title}</span>
        <strong>{status}</strong>
        <small>{detail}</small>
      </div>
    </div>
  );
}

function exchangeList(exchanges: string[] | undefined, fallback: string) {
  return exchanges && exchanges.length > 0 ? exchanges.join(', ') : fallback;
}
