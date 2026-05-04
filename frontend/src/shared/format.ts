export function formatNumber(value: string | number | null | undefined, fractionDigits = 0) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return String(value);
  }
  return new Intl.NumberFormat('ko-KR', {
    maximumFractionDigits: fractionDigits,
  }).format(number);
}

export function formatKrw(value: string | number | null | undefined) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return String(value);
  }
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(number);
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(date);
}
