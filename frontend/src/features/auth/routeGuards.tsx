import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router';

import { ApiError } from '../../api/errors';
import { StatusView } from '../../components/common/StatusView';
import { useCurrentUser } from './authQuery';

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

function isNetworkError(error: unknown): boolean {
  return error instanceof ApiError && error.status === 0;
}

function useAuthTimeout(timeoutMs: number) {
  const query = useCurrentUser();
  const [timedOut, setTimedOut] = useState(false);

  useEffect(() => {
    if (!query.isPending) {
      setTimedOut(false);
      return;
    }

    const timer = window.setTimeout(() => setTimedOut(true), timeoutMs);
    return () => window.clearTimeout(timer);
  }, [query.isPending, timeoutMs]);

  return { query, timedOut };
}

const AUTH_CHECK_TIMEOUT_MS = 6000;
const LOADING_TEXT = '로그인 상태를 확인하고 있습니다.';
const ERROR_TITLE = '문제가 발생했습니다.';
const ERROR_MESSAGE = '현재 서버 응답이 늦거나 연결이 불안정합니다. 잠시 후 다시 시도해 주세요.';

export function ProtectedRoute() {
  const navigate = useNavigate();
  const location = useLocation();
  const { query, timedOut } = useAuthTimeout(AUTH_CHECK_TIMEOUT_MS);

  if (query.isPending) {
    if (!timedOut) {
      return <StatusView title={LOADING_TEXT} message={LOADING_TEXT} />;
    }
    return (
      <StatusView
        title="잠깐, 로그인 확인이 지연되고 있습니다"
        message={ERROR_MESSAGE}
        actionLabel="로그인 페이지로 이동"
        onAction={() => navigate('/welcome', { replace: true })}
      />
    );
  }

  if (query.isError && isUnauthorized(query.error)) {
    return <Navigate to="/welcome" replace state={{ from: location.pathname }} />;
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
  const navigate = useNavigate();
  const { query, timedOut } = useAuthTimeout(AUTH_CHECK_TIMEOUT_MS);

  if (query.isPending) {
    if (!timedOut) {
      return <StatusView title={LOADING_TEXT} message={LOADING_TEXT} />;
    }
    return (
      <StatusView
        title="잠깐, 로그인 확인이 지연되고 있습니다"
        message={ERROR_MESSAGE}
        actionLabel="로그인 화면으로 이동"
        onAction={() => navigate('/login', { replace: true })}
      />
    );
  }

  if (query.isSuccess) return <Navigate to="/" replace />;
  if (query.isError && isUnauthorized(query.error)) return <Outlet />;
  if (query.isError && isNetworkError(query.error)) return <Outlet />;
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
