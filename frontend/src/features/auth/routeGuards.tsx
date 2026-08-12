import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';

import { ApiError } from '../../api/errors';
import { StatusView } from '../../components/common/StatusView';
import { isBackendStartingError, useCurrentUser } from './authQuery';

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

function useAuthTimeout(timeoutMs: number) {
  const query = useCurrentUser();
  const [timedOut, setTimedOut] = useState(false);
  const [isManualRetrying, setIsManualRetrying] = useState(false);

  useEffect(() => {
    if (!query.isPending) {
      return;
    }

    const timer = window.setTimeout(() => setTimedOut(true), timeoutMs);
    return () => window.clearTimeout(timer);
  }, [query.isPending, timeoutMs]);

  const retryNow = async () => {
    if (isManualRetrying) return;

    setIsManualRetrying(true);
    try {
      await query.refetch({ cancelRefetch: true });
    } finally {
      setIsManualRetrying(false);
    }
  };

  return { query, timedOut, isManualRetrying, retryNow };
}

function ServerWakeupView({
  isRetrying,
  onRetry,
}: {
  isRetrying: boolean;
  onRetry: () => Promise<void>;
}) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    const timer = window.setInterval(() => setElapsedSeconds((seconds) => seconds + 1), 1_000);
    return () => window.clearInterval(timer);
  }, []);

  const activeStage = elapsedSeconds < 12 ? 0 : elapsedSeconds < 32 ? 1 : 2;

  const handleRetry = async () => {
    setElapsedSeconds(0);
    await onRetry();
  };

  return (
    <main className="server-wakeup-page" aria-live="polite" aria-busy="true">
      <section className="server-wakeup-card">
        <div className="server-wakeup-mark" aria-hidden="true">
          <span className="server-wakeup-orbit" />
          <svg viewBox="0 0 24 24">
            <path d="M7 3v3M17 3v3M4.5 8.5h15M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" />
            <path d="M8.5 13h.01M12 13h.01M15.5 13h.01M8.5 16.5h.01M12 16.5h.01" />
          </svg>
        </div>
        <p className="server-wakeup-eyebrow">CALTALK</p>
        <h1>서버를 깨우고 있습니다</h1>
        <p className="server-wakeup-description">
          잠시 쉬고 있던 무료 서버를 준비하고 있습니다.<br />
          연결되면 화면이 자동으로 열립니다.
        </p>

        <div className="server-wakeup-progress" aria-label="서버 연결 진행 중">
          {['서버 시작', '데이터 연결', '화면 확인'].map((label, index) => (
            <div
              className={index < activeStage ? 'is-complete' : index === activeStage ? 'is-active' : ''}
              key={label}
            >
              <span>{index < activeStage ? '✓' : index + 1}</span>
              <small>{label}</small>
            </div>
          ))}
        </div>

        <p className="server-wakeup-note">
          첫 접속은 보통 1분 안팎이 걸릴 수 있습니다. 이 페이지를 그대로 두셔도 됩니다.
        </p>
        <button
          type="button"
          className="server-wakeup-retry"
          disabled={isRetrying}
          aria-busy={isRetrying}
          onClick={() => void handleRetry()}
        >
          {isRetrying ? '연결 확인 중...' : '지금 다시 연결'}
        </button>
      </section>
    </main>
  );
}

const AUTH_CHECK_TIMEOUT_MS = 6_000;
const LOADING_TEXT = '로그인 상태를 확인하고 있습니다.';
const ERROR_TITLE = '문제가 발생했습니다.';
const ERROR_MESSAGE = '현재 서버 연결이 불안정합니다. 잠시 후 다시 시도해 주세요.';

export function ProtectedRoute() {
  const location = useLocation();
  const { query, timedOut, isManualRetrying, retryNow } = useAuthTimeout(AUTH_CHECK_TIMEOUT_MS);

  if (query.isPending) {
    if (!timedOut) return <StatusView title={LOADING_TEXT} message="잠시만 기다려 주세요." />;
    return <ServerWakeupView isRetrying={isManualRetrying} onRetry={retryNow} />;
  }

  if (query.isError && isUnauthorized(query.error)) {
    return <Navigate to="/welcome" replace state={{ from: location.pathname }} />;
  }

  if (query.isError && isBackendStartingError(query.error)) {
    return <ServerWakeupView isRetrying={isManualRetrying} onRetry={retryNow} />;
  }

  if (query.isError) {
    return (
      <StatusView
        title={ERROR_TITLE}
        message={ERROR_MESSAGE}
        actionLabel="다시 시도"
        onAction={() => void query.refetch()}
      />
    );
  }

  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { query, timedOut, isManualRetrying, retryNow } = useAuthTimeout(AUTH_CHECK_TIMEOUT_MS);

  if (query.isPending) {
    if (!timedOut) return <StatusView title={LOADING_TEXT} message="잠시만 기다려 주세요." />;
    return <ServerWakeupView isRetrying={isManualRetrying} onRetry={retryNow} />;
  }

  if (query.isSuccess) return <Navigate to="/" replace />;
  if (query.isError && isUnauthorized(query.error)) return <Outlet />;
  if (query.isError && isBackendStartingError(query.error)) {
    return <ServerWakeupView isRetrying={isManualRetrying} onRetry={retryNow} />;
  }
  if (query.isError) {
    return (
      <StatusView
        title={ERROR_TITLE}
        message={ERROR_MESSAGE}
        actionLabel="다시 시도"
        onAction={() => void query.refetch()}
      />
    );
  }

  return <Outlet />;
}
