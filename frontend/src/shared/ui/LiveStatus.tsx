import { RefreshCw, Radio } from 'lucide-react';

interface LiveStatusProps {
  updatedAt?: number;
  isFetching?: boolean;
  intervalMs?: number;
}

export function LiveStatus({ updatedAt, isFetching = false, intervalMs }: LiveStatusProps) {
  const updatedText = updatedAt ? formatClock(updatedAt) : '-';
  const intervalText = intervalMs ? `${Math.round(intervalMs / 1000)}s` : null;

  return (
    <div className={`live-status ${isFetching ? 'live-status-fetching' : ''}`} aria-live="polite">
      {isFetching ? <RefreshCw size={15} aria-hidden /> : <Radio size={15} aria-hidden />}
      <span>실시간 운영(Live)</span>
      <strong>{isFetching ? '갱신 중(Updating)' : `최근 갱신 ${updatedText}`}</strong>
      {intervalText ? <small>{intervalText}</small> : null}
    </div>
  );
}

function formatClock(value: number) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value));
}
