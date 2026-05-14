import { useQuery } from '@tanstack/react-query';
import { ShieldAlert, ShieldCheck, ShieldX } from 'lucide-react';
import { api, queryKeys } from '../../shared/api/client';
import { useExchangeMode } from '../../shared/exchange/ExchangeModeContext';
import { formatCurrency, formatNumber } from '../../shared/format';
import { Badge } from '../../shared/ui/Badge';
import { ErrorPanel } from '../../shared/ui/ErrorPanel';
import { MetricCard } from '../../shared/ui/MetricCard';

export function RiskPage() {
  const { exchange } = useExchangeMode();
  const riskQuery = useQuery({
    queryKey: queryKeys.riskStatus(exchange),
    queryFn: () => api.riskStatus(exchange),
    refetchInterval: 5_000,
  });

  if (riskQuery.isLoading) {
    return <div className="page-state">리스크 상태를 불러오는 중</div>;
  }

  if (riskQuery.error || !riskQuery.data) {
    return <ErrorPanel error={riskQuery.error} />;
  }

  const risk = riskQuery.data;
  const currency = exchange === 'BINANCE' ? 'USDT' : 'KRW';

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>리스크(Risk)</h1>
          <p>현재 PAPER 리스크 정책과 차단 기준을 읽기 전용으로 확인합니다.</p>
        </div>
        <Badge tone="info">{exchange}</Badge>
      </header>

      <div className="metric-grid">
        <MetricCard label="최대 주문(Max Order)" value={formatCurrency(risk.maxOrderAmount, currency)} detail="자동 PAPER BUY 상한" />
        <MetricCard label="익절(Take Profit)" value={`${formatNumber(risk.takeProfitRate, 3)}%`} detail="보유 PAPER SELL 기준" />
        <MetricCard label="손절(Stop Loss)" value={`${formatNumber(risk.stopLossRate, 3)}%`} detail="손실 제한 기준" />
        <MetricCard label="일일 주문(Daily Orders)" value={formatNumber(risk.dailyOrderLimit)} detail={risk.dailyRiskEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'} />
      </div>

      <div className="section-grid">
        <article className="panel">
          <div className="panel-title-row">
            <h2>쏠림 리스크(Concentration)</h2>
            {risk.concentration.enabled ? <ShieldAlert size={20} /> : <ShieldCheck size={20} />}
          </div>
          <dl className="definition-list">
            <dt>거래소(Exchange)</dt>
            <dd>{risk.concentration.exchange}</dd>
            <dt>적용(Guard)</dt>
            <dd>{risk.concentration.enabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>경고(Warning)</dt>
            <dd>{`${formatNumber(risk.concentration.warningExposureRate, 0)}%`}</dd>
            <dt>차단(Block)</dt>
            <dd>{`${formatNumber(risk.concentration.blockExposureRate, 0)}%`}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>반복 손절 Cooldown</h2>
            {risk.stopLossCooldown.enabled ? <ShieldAlert size={20} /> : <ShieldCheck size={20} />}
          </div>
          <dl className="definition-list">
            <dt>적용(Enabled)</dt>
            <dd>{risk.stopLossCooldown.enabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>관찰 범위(Window)</dt>
            <dd>{risk.stopLossCooldown.window}</dd>
            <dt>트리거(Trigger)</dt>
            <dd>{`${risk.stopLossCooldown.triggerCount}회`}</dd>
            <dt>차단 시간(Duration)</dt>
            <dd>{risk.stopLossCooldown.duration}</dd>
          </dl>
        </article>

        <article className="panel">
          <div className="panel-title-row">
            <h2>거래 제한(Trading Controls)</h2>
            <ShieldX size={20} />
          </div>
          <dl className="definition-list">
            <dt>허용 마켓(Allowed)</dt>
            <dd>{risk.allowedMarkets.join(', ') || '-'}</dd>
            <dt>포지션 청산(Exit)</dt>
            <dd>{risk.positionExitEnabled ? '켜짐(Enabled)' : '꺼짐(Disabled)'}</dd>
            <dt>일일 손실(Daily Loss)</dt>
            <dd>{formatCurrency(risk.dailyLossLimit, currency)}</dd>
            <dt>실거래(Real Trading)</dt>
            <dd>구현 안 됨(Not implemented)</dd>
          </dl>
        </article>
      </div>
    </section>
  );
}
