import type { PropsWithChildren, RefObject } from 'react';
import { useEffect, useRef } from 'react';

interface DialogShellProps extends PropsWithChildren {
  title: string;
  onClose: () => void;
  closeDisabled?: boolean;
}

export function DialogShell({ title, onClose, closeDisabled, children }: DialogShellProps) {
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previousFocus =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
    dialogRef.current?.focus();
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !closeDisabled) onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      previousFocus?.focus();
    };
  }, [closeDisabled, onClose]);

  return (
    <div className="dialog-backdrop" role="presentation">
      <div
        className="card dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
        tabIndex={-1}
        ref={dialogRef as RefObject<HTMLDivElement>}
      >
        <div className="dialog-header">
          <h2 id="dialog-title">{title}</h2>
          <button
            className="icon-button"
            type="button"
            aria-label="닫기"
            disabled={closeDisabled}
            onClick={onClose}
          >
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
