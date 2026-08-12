import { BrowserRouter, Route, Routes } from 'react-router';
import { ProtectedRoute, PublicOnlyRoute } from '../../features/auth/routeGuards';
import { HomePage } from '../../pages/HomePage';
import { LoginPage } from '../../pages/LoginPage';
import { NotFoundPage } from '../../pages/NotFoundPage';
import { SignupPage } from '../../pages/SignupPage';
import { WelcomePage } from '../../pages/WelcomePage';
import { DailySchedulePage } from '../../pages/DailySchedulePage';
import { DailyScheduleDetailPage } from '../../pages/DailyScheduleDetailPage';

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/welcome" element={<WelcomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/day/:date" element={<DailySchedulePage />} />
          <Route path="/day/:date/new" element={<DailyScheduleDetailPage />} />
          <Route path="/day/:date/event/:scheduleId" element={<DailyScheduleDetailPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
