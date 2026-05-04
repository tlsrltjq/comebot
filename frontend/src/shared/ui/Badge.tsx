import type { ReactNode } from 'react';

type BadgeTone = 'neutral' | 'good' | 'warn' | 'bad' | 'info';

interface BadgeProps {
  children: ReactNode;
  tone?: BadgeTone;
}

export function Badge({ children, tone = 'neutral' }: BadgeProps) {
  return <span className={`badge badge-${tone}`}>{children}</span>;
}
