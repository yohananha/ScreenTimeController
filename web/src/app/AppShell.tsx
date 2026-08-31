import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { colors } from '../theme/colors';
import { SproutBottomNavBar } from '../components/BottomNavBar';
import { NotificationsPrompt } from '../components/NotificationsPrompt';
import { ForegroundToast } from '../components/ForegroundToast';
import { useRequestsBadge } from '../hooks/useRequestsBadge';
import { LimitsScreen } from '../screens/limits/LimitsScreen';
import { TimeFrameScreen } from '../screens/limits/TimeFrameScreen';
import { RequestsScreen } from '../screens/requests/RequestsScreen';
import { CodesScreen } from '../screens/codes/CodesScreen';
import { SettingsScreen } from '../screens/settings/SettingsScreen';
import { HistoryScreen } from '../screens/history/HistoryScreen';

export function AppShell({ uid, familyId }: { uid: string; familyId: string }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { pendingCount } = useRequestsBadge(familyId);
  const currentRoute = location.pathname.split('/')[1] || 'limits';

  return (
    <div style={{ minHeight: '100vh', background: colors.background, display: 'flex', flexDirection: 'column' }}>
      <ForegroundToast />
      <div style={{ paddingTop: 8 }}>
        <NotificationsPrompt familyId={familyId} />
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Routes>
          <Route path="/" element={<Navigate to="/limits" replace />} />
          <Route
            path="/limits"
            element={
              <LimitsScreen
                familyId={familyId}
                onOpenHistory={() => navigate('/history')}
                onOpenTimeFrame={() => navigate('/limits/timeframe')}
              />
            }
          />
          <Route path="/limits/timeframe" element={<TimeFrameScreen familyId={familyId} onBack={() => navigate('/limits')} />} />
          <Route path="/requests" element={<RequestsScreen familyId={familyId} />} />
          <Route path="/codes" element={<CodesScreen familyId={familyId} />} />
          <Route path="/settings" element={<SettingsScreen familyId={familyId} uid={uid} />} />
          <Route path="/history" element={<HistoryScreen familyId={familyId} />} />
          <Route path="*" element={<Navigate to="/limits" replace />} />
        </Routes>
      </div>
      <SproutBottomNavBar selectedRoute={currentRoute} pendingCount={pendingCount} onTabClick={(route) => navigate(`/${route}`)} />
    </div>
  );
}
