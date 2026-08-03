import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { AppLayout } from '../components/layout/AppLayout';
import { DialogShell } from '../features/schedule/components/DialogShell';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/authQuery';
import { ScheduleWorkspace } from '../features/schedule/components/ScheduleWorkspace';
import { TimezoneForm } from '../features/user/TimezoneForm';

export function HomePage() {
  const [settingsOpen, setSettingsOpen] = useState(false);
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
    <AppLayout
      email={currentUser.data.email}
      onOpenSettings={() => setSettingsOpen(true)}
      onLogout={() => logoutMutation.mutate()}
      logoutPending={logoutMutation.isPending}
    >
      <section className="page-intro">
        <div>
          <p className="today-label">
            {new Intl.DateTimeFormat('ko-KR', {
              dateStyle: 'full',
              timeZone: currentUser.data.timezone,
            }).format(new Date())}
          </p>
          <h1>오늘의 일정을 정리해 볼까요?</h1>
          <p>예정된 시간을 확인하고 여유 있게 하루를 계획하세요.</p>
        </div>
      </section>

      {logoutMutation.error ? (
        <div className="alert" role="alert">
          {logoutMutation.error instanceof ApiError
            ? logoutMutation.error.message
            : '로그아웃하지 못했습니다.'}
        </div>
      ) : null}

      <ScheduleWorkspace timeZone={currentUser.data.timezone} />
      {settingsOpen ? (
        <DialogShell
          title="내 설정"
          description="일정을 표시할 시간대를 관리합니다."
          onClose={() => setSettingsOpen(false)}
        >
          <div className="settings-profile">
            <span className="settings-avatar">
              {currentUser.data.email.slice(0, 1).toUpperCase()}
            </span>
            <div>
              <strong>{currentUser.data.email}</strong>
              <span>CalTalk 계정</span>
            </div>
          </div>
          <section className="settings-section" aria-labelledby="timezone-setting-title">
            <div>
              <p className="eyebrow">시간 설정</p>
              <h3 id="timezone-setting-title">표시 시간대</h3>
              <p>일정 자체는 그대로 유지되고, 화면에 보이는 시간만 선택한 지역에 맞춰 바뀝니다.</p>
            </div>
            <TimezoneForm current={currentUser.data.timezone} />
          </section>
        </DialogShell>
      ) : null}
    </AppLayout>
  );
}
