import { Navigate, Outlet, useLocation } from 'react-router';
import { ApiError } from '../../api/errors';
import { StatusView } from '../../components/common/StatusView';
import { useCurrentUser } from './authQuery';

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

export function ProtectedRoute() {
  const query = useCurrentUser();
  const location = useLocation();

  if (query.isPending) {
    return <StatusView title="로그인 상태 확인 중" message="잠시만 기다려 주세요." />;
  }
  if (query.isError && isUnauthorized(query.error)) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (query.isError) {
    return (
      <StatusView
        title="연결할 수 없습니다"
        message="로그인 상태를 확인하지 못했습니다. 네트워크 연결을 확인해 주세요."
        actionLabel="다시 시도"
        onAction={() => void query.refetch()}
      />
    );
  }
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const query = useCurrentUser();

  if (query.isPending) {
    return <StatusView title="로그인 상태 확인 중" message="잠시만 기다려 주세요." />;
  }
  if (query.isSuccess) return <Navigate to="/" replace />;
  if (query.isError && !isUnauthorized(query.error)) {
    return (
      <StatusView
        title="연결할 수 없습니다"
        message="서버 상태를 확인한 뒤 다시 시도해 주세요."
        actionLabel="다시 시도"
        onAction={() => void query.refetch()}
      />
    );
  }
  return <Outlet />;
}
