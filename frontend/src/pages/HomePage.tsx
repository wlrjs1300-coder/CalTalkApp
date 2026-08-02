import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { AppLayout } from '../components/layout/AppLayout';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/authQuery';
import { ScheduleWorkspace } from '../features/schedule/components/ScheduleWorkspace';
import { TimezoneForm } from '../features/user/TimezoneForm';

export function HomePage() {
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const logoutMutation = useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
      navigate('/login', { replace: true });
    },
  });

  if (!currentUser.data) return null;

  return (
    <AppLayout>
      <section className="welcome-panel">
        <div>
          <p className="eyebrow">현재 사용자</p>
          <h1>안녕하세요.</h1>
          <p className="user-email">{currentUser.data.email}</p>
        </div>
        <button
          className="secondary-button"
          type="button"
          disabled={logoutMutation.isPending}
          onClick={() => logoutMutation.mutate()}
        >
          {logoutMutation.isPending ? '로그아웃 중…' : '로그아웃'}
        </button>
      </section>

      {logoutMutation.error ? (
        <div className="alert" role="alert">
          {logoutMutation.error instanceof ApiError
            ? logoutMutation.error.message
            : '로그아웃하지 못했습니다.'}
        </div>
      ) : null}

      <section className="dashboard-grid profile-grid">
        <article className="card profile-card">
          <p className="eyebrow">설정</p>
          <h2>내 시간대</h2>
          <strong>{currentUser.data.timezone}</strong>
          <TimezoneForm current={currentUser.data.timezone} />
        </article>
        <article className="card profile-card">
          <p className="eyebrow">저장 기준</p>
          <h2>UTC 절대 시각</h2>
          <p className="muted">
            시간대를 바꿔도 저장된 일정 시각은 유지되고 화면 표시만 다시 계산됩니다.
          </p>
        </article>
      </section>
      <ScheduleWorkspace timeZone={currentUser.data.timezone} />
    </AppLayout>
  );
}
