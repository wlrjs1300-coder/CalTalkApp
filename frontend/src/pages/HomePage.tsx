import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { logout } from '../api/auth';
import { ApiError } from '../api/errors';
import { AppLayout } from '../components/layout/AppLayout';
import { DialogShell } from '../features/schedule/components/DialogShell';
import { currentUserQueryKey, useCurrentUser } from '../features/auth/authQuery';
import { ScheduleWorkspace } from '../features/schedule/components/ScheduleWorkspace';
import { SettingsPanel } from '../features/user/SettingsPanel';

export function HomePage() {
  const [settingsOpen, setSettingsOpen] = useState(false);
  const currentUser = useCurrentUser();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const logoutMutation = useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: currentUserQueryKey });
      navigate('/welcome', { replace: true });
    },
  });

  useEffect(() => {
    const previousScrollRestoration = window.history.scrollRestoration;
    const resetDocumentScroll = () => {
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    };

    window.history.scrollRestoration = 'manual';
    resetDocumentScroll();
    const resetScrollFrame = window.requestAnimationFrame(resetDocumentScroll);

    document.documentElement.classList.add('calendar-page-lock');
    document.body.classList.add('calendar-page-lock');
    return () => {
      window.cancelAnimationFrame(resetScrollFrame);
      window.history.scrollRestoration = previousScrollRestoration;
      document.documentElement.classList.remove('calendar-page-lock');
      document.body.classList.remove('calendar-page-lock');
    };
  }, []);

  if (!currentUser.data) return null;

  return (
    <AppLayout
      email={currentUser.data.email}
      mainClassName="calendar-main"
      onOpenSettings={() => setSettingsOpen(true)}
      onLogout={() => logoutMutation.mutate()}
      logoutPending={logoutMutation.isPending}
    >
      {logoutMutation.error ? (
        <div className="alert" role="alert">
          {logoutMutation.error instanceof ApiError
            ? logoutMutation.error.message
            : '로그아웃하지 못했습니다.'}
        </div>
      ) : null}

      <ScheduleWorkspace
        timeZone={currentUser.data.timezone}
        onDateSelect={(dateKey) => navigate(`/day/${dateKey}`)}
      />
      {settingsOpen ? (
        <DialogShell
          title="설정"
          onClose={() => setSettingsOpen(false)}
        >
          <SettingsPanel onSaved={() => setSettingsOpen(false)} />
        </DialogShell>
      ) : null}
    </AppLayout>
  );
}
