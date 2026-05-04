interface ErrorPanelProps {
  title?: string;
  error: unknown;
}

export function ErrorPanel({ title = '요청 실패(Request failed)', error }: ErrorPanelProps) {
  const message = error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다(Unknown error).';
  return (
    <div className="error-panel" role="alert">
      <strong>{title}</strong>
      <span>{message}</span>
    </div>
  );
}
