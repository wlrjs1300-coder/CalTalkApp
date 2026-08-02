interface StatusViewProps {
  title: string;
  message: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function StatusView({ title, message, actionLabel, onAction }: StatusViewProps) {
  return (
    <main className="centered-page" aria-live="polite">
      <section className="card status-card">
        <p className="eyebrow">CalTalk</p>
        <h1>{title}</h1>
        <p>{message}</p>
        {actionLabel && onAction ? (
          <button type="button" onClick={onAction}>
            {actionLabel}
          </button>
        ) : null}
      </section>
    </main>
  );
}
