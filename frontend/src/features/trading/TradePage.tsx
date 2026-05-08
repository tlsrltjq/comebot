import { useQuery } from '@tanstack/react-query';
import { api, queryKeys } from '../../shared/api/client';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { formatDateTime } from '../../shared/format';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';

export function TradePage() {
  const { exchange } = useExchangeMode();
  const systemQuery = useQuery({
    queryKey: queryKeys.system(exchange),
    queryFn: () => api.systemStatus(exchange),
    refetchInterval: 10_000,
  });
  const historyQuery = useQuery({
    queryKey: queryKeys.history(exchange, undefined, 10),
    queryFn: () => api.history(exchange, undefined, 10),
    refetchInterval: 10_000,
  });

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>자동 실행 모니터링(Auto Run Monitoring)</h1>
          <p>매수와 매도는 스케줄러가 자동으로 실행하고, 웹은 상태만 표시합니다.</p>
        </div>
        <span className="live-indicator">자동 갱신(Live)</span>
      </header>

      {systemQuery.error ? <ErrorPanel title="자동 실행 상태 조회 실패(Auto status failed)" error={systemQuery.error} /> : null}
      {historyQuery.error ? <ErrorPanel title="실행 이력 조회 실패(History failed)" error={historyQuery.error} /> : null}
      {systemQuery.data ? (
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
              <dd>{systemQuery.data.scheduler.exitExchange}</dd>
              <dt>보유 마켓(Position markets)</dt>
              <dd>{systemQuery.data.scheduler.exitPositionMarketCount}</dd>
              <dt>HOLD 기록(HOLD history)</dt>
              <dd>{systemQuery.data.scheduler.exitSaveHoldHistory ? '저장(Save)' : '저장 안 함(Skip)'}</dd>
            </dl>
          </article>
        </div>
      ) : null}

      <article className="panel">
        <h2>최근 자동 실행(Recent auto runs)</h2>
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
