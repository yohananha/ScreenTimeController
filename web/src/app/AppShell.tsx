import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { colors } from '../theme/colors';
import { SproutBottomNavBar } from '../components/BottomNavBar';
import { NotificationsPrompt } from '../components/NotificationsPrompt';
import { ForegroundToast } from '../components/ForegroundToast';
import { LimitsScreen } from '../screens/limits/LimitsScreen';
import { TimeFrameScreen } from '../screens/limits/TimeFrameScreen';
import { SettingsScreen } from '../screens/settings/SettingsScreen';
import { HistoryScreen } from '../screens/history/HistoryScreen';
import { TodayScreen } from '../screens/today/TodayScreen';
import { RulesScreen } from '../screens/rules/RulesScreen';
import { FamilyScreen } from '../screens/family/FamilyScreen';

const REDESIGN_ROUTES = new Set(['today', 'rules', 'family']);

export function AppShell({ uid, familyId }: { uid: string; familyId: string }) {
  const navigate = useNavigate();
  const location = useLocation();
  const currentRoute = location.pathname.split('/')[1] || 'today';
  // Today/Rules/Family (design/i6c-peach-plum) are the primary experience now
  // and manage their own chrome (RedesignLayout: glows, NavPill/SideRail,
  // Unlock sheet) instead of the Sprout-era 4-tab bar below. /limits and
  // /settings stay routable — Rules and Family reuse their edit dialogs and
  // hook wiring — but are no longer linked from any nav.
  const isRedesign = REDESIGN_ROUTES.has(currentRoute);

  return (
    <div style={{ minHeight: '100vh', background: colors.background, display: 'flex', flexDirection: 'column' }}>
      <ForegroundToast />
      {!isRedesign && (
        <div style={{ paddingTop: 8 }}>
          <NotificationsPrompt familyId={familyId} />
        </div>
      )}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Routes>
          <Route path="/" element={<Navigate to="/today" replace />} />
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
          <Route path="/settings" element={<SettingsScreen familyId={familyId} uid={uid} />} />
          <Route path="/history" element={<HistoryScreen familyId={familyId} />} />
          <Route
            path="/today"
            element={<TodayScreen familyId={familyId} onOpenRules={() => navigate('/rules')} onOpenFamily={() => navigate('/family')} />}
          />
          <Route
            path="/rules"
            element={<RulesScreen familyId={familyId} onOpenFamily={() => navigate('/family')} onOpenTimeFrame={() => navigate('/rules/timeframe')} />}
          />
          <Route path="/rules/timeframe" element={<TimeFrameScreen familyId={familyId} onBack={() => navigate('/rules')} />} />
          <Route path="/family" element={<FamilyScreen familyId={familyId} uid={uid} />} />
          <Route path="*" element={<Navigate to="/today" replace />} />
        </Routes>
      </div>
      {!isRedesign && <SproutBottomNavBar selectedRoute={currentRoute} onTabClick={(route) => navigate(`/${route}`)} />}
    </div>
  );
}
