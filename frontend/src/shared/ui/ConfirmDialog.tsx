import { AlertTriangle, X } from 'lucide-react';
import type { ReactNode } from 'react';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: ReactNode;
  confirmLabel: string;
  busy?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  busy = false,
  onCancel,
  onConfirm,
}: ConfirmDialogProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="dialog-backdrop" role="presentation">
      <section className="dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
        <div className="dialog-header">
          <AlertTriangle size={20} aria-hidden />
          <h2 id="confirm-title">{title}</h2>
          <button className="icon-button" type="button" aria-label="닫기" onClick={onCancel}>
            <X size={18} />
          </button>
        </div>
        <div className="dialog-body">{description}</div>
        <div className="dialog-actions">
          <button className="button button-secondary" type="button" onClick={onCancel} disabled={busy}>
            취소(Cancel)
          </button>
          <button className="button button-danger" type="button" onClick={onConfirm} disabled={busy}>
            {busy ? '실행 중(Running)' : confirmLabel}
          </button>
        </div>
      </section>
    </div>
  );
}
