import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { peachPlumColor, peachPlumRadius } from '../../theme/tokens';
import { WhiteCard, wideCardStyle } from '../../components/WhiteCard';
import { SettingsRow } from '../../components/SettingsRow';
import { PrimaryButton, SecondaryButton } from '../../components/Button';
import { CodeSlotInput } from '../../components/CodeSlotInput';
import { Modal } from '../../components/Modal';
import { RedesignLayout } from '../redesign/RedesignLayout';
import { useFamily } from '../../hooks/useFamily';
import { usePairedDevices } from '../../hooks/usePairedDevices';
import { useLanguage } from '../../hooks/useLanguage';
import { useNotificationStatus } from '../../hooks/useNotifications';
import { isAdmin, isOwner } from '../../models/Family';
import type { PairedDevice } from '../../models/PairedDevice';
import type { LangTag } from '../../i18n/i18n';

export function FamilyScreen({ familyId, uid }: { familyId: string; uid: string }) {
  const { t } = useTranslation();
  const { state, removeMember, generateInvite } = useFamily(familyId);
  const { devices, state: pairState, claim, reset, rename, unpair } = usePairedDevices(familyId);
  const { select, current } = useLanguage(uid, familyId);
  const notifications = useNotificationStatus(familyId);

  const [showInvite, setShowInvite] = useState(false);
  const [confirmRemove, setConfirmRemove] = useState<string | null>(null);
  const [renaming, setRenaming] = useState<PairedDevice | null>(null);
  const [confirmUnpair, setConfirmUnpair] = useState<string | null>(null);
  const [showPairForm, setShowPairForm] = useState(false);
  const [pairCode, setPairCode] = useState('');
  const [showLanguagePicker, setShowLanguagePicker] = useState(false);

  if (pairState.success && showPairForm) {
    setPairCode('');
    setShowPairForm(false);
    reset();
  }

  const family = state.family;
  const currentIsAdmin = family ? isAdmin(family, uid) : false;
  const members = family
    ? Object.entries(family.members).sort(([a], [b]) => {
        const selfRank = (u: string) => (u === uid ? 0 : 1);
        const roleRank = (u: string) => (isOwner(family, u) ? 0 : family.members[u] === 'ADMIN' ? 1 : 2);
        return selfRank(a) - selfRank(b) || roleRank(a) - roleRank(b);
      })
    : [];

  const languageValue = current === 'en' ? t('settings.languageEnglish') : current === 'he' ? t('settings.languageHebrew') : t('settings.languageSystem');
  const notificationsValue =
    notifications.status === 'granted'
      ? t('settings.notificationsOn')
      : notifications.status === 'denied'
        ? t('settings.notificationsBlocked')
        : notifications.status === 'unsupported'
          ? t('settings.notificationsUnsupported')
          : t('settings.notificationsOff');

  const parentsCard = (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {members.map(([memberUid], index) => {
        const isSelf = memberUid === uid;
        const owner = family ? isOwner(family, memberUid) : false;
        const name = isSelf ? t('settings.you') : (state.displayNames[memberUid] ?? t('family.secondParent'));
        const caption = isSelf && owner ? t('settings.ownerYou') : owner ? t('settings.owner') : t('settings.coParent');
        const canRemove = currentIsAdmin && !isSelf && !owner;
        return (
          <div
            key={memberUid}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              height: 62,
              borderTop: index > 0 ? `1px solid ${peachPlumColor.hairline}` : 0,
              cursor: canRemove ? 'pointer' : 'default',
            }}
            onClick={canRemove ? () => setConfirmRemove(memberUid) : undefined}
          >
            <span
              style={{
                width: 38,
                height: 38,
                borderRadius: '50%',
                background: isSelf ? peachPlumColor.peach : peachPlumColor.tint,
                color: peachPlumColor.ink,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 15,
                fontWeight: 600,
                flexShrink: 0,
              }}
            >
              {name.charAt(0).toUpperCase()}
            </span>
            <span style={{ display: 'flex', flexDirection: 'column', gap: 2, flexGrow: 1 }}>
              <span style={{ fontSize: 16, fontWeight: 600 }}>{name}</span>
              <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{caption}</span>
            </span>
          </div>
        );
      })}
      <button
        onClick={() => {
          setShowInvite((v) => !v);
          if (!showInvite) generateInvite();
        }}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          height: 62,
          padding: 0,
          border: 0,
          borderTop: members.length > 0 ? `1px solid ${peachPlumColor.hairline}` : 0,
          background: 'transparent',
          color: peachPlumColor.ink,
          textAlign: 'start',
          width: '100%',
          fontFamily: 'Rubik, system-ui, sans-serif',
          cursor: 'pointer',
        }}
      >
        <span style={{ width: 38, height: 38, borderRadius: '50%', border: `1.5px dashed ${peachPlumColor.muted}`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={peachPlumColor.ink} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
        </span>
        <span style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <span style={{ fontSize: 16, fontWeight: 600 }}>{t('settings.inviteParentTitle')}</span>
          <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('settings.inviteParentSubtitle')}</span>
        </span>
      </button>
      {showInvite && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, background: peachPlumColor.tint, borderRadius: peachPlumRadius.strip, padding: 14, margin: '4px 0 12px' }}>
          <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('settings.shareInviteCode')}</span>
          {state.inviteCode ? (
            <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
              <span style={{ flexGrow: 1, background: peachPlumColor.surface, borderRadius: 999, padding: '10px 16px', fontSize: 16, fontWeight: 600 }}>{state.inviteCode}</span>
              <PrimaryButton style={{ height: 44, fontSize: 14 }} onClick={() => void navigator.clipboard.writeText(state.inviteCode!)}>
                {t('settings.copy')}
              </PrimaryButton>
            </div>
          ) : (
            <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('settings.generatingCode')}</span>
          )}
        </div>
      )}
    </div>
  );

  const tvsSection = (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: 16, borderRadius: peachPlumRadius.card, background: peachPlumColor.ink, color: peachPlumColor.surface }}>
        {devices.length === 0 && <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.navInactive }}>{t('today.noTvPaired')}</span>}
        {devices.map((device, index) => (
          <div key={device.id} style={{ display: 'flex', flexDirection: 'column', gap: 12, paddingTop: index > 0 ? 12 : 0, borderTop: index > 0 ? '1px solid rgba(255,255,255,0.12)' : 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ width: 38, height: 38, borderRadius: peachPlumRadius.iconTile, background: 'rgba(255,255,255,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={peachPlumColor.surface} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <rect x="3" y="5" width="18" height="12" rx="2" />
                  <path d="M8 21h8" />
                </svg>
              </span>
              <span style={{ display: 'flex', flexDirection: 'column', gap: 2, flexGrow: 1 }}>
                <span style={{ fontSize: 16, fontWeight: 600 }}>{device.name}</span>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, fontWeight: 500, color: '#9FE9CE' }}>
                  <span style={{ width: 7, height: 7, borderRadius: '50%', background: peachPlumColor.ok, animation: 'pp-live-pulse 2s ease-out infinite' }} />
                  {t('settings.online')}
                </span>
              </span>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                onClick={() => setRenaming(device)}
                style={{ height: 40, padding: '0 16px', borderRadius: 999, border: '1.5px solid rgba(255,255,255,0.3)', background: 'transparent', color: peachPlumColor.surface, fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 13, fontWeight: 600 }}
              >
                {t('settings.rename')}
              </button>
              <button
                onClick={() => setConfirmUnpair(device.id)}
                style={{ height: 40, padding: '0 16px', borderRadius: 999, border: '1.5px solid rgba(255,255,255,0.3)', background: 'transparent', color: '#FFD3CD', fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 13, fontWeight: 600 }}
              >
                {t('settings.unpair')}
              </button>
            </div>
          </div>
        ))}
      </div>
      {!showPairForm ? (
        <button
          onClick={() => setShowPairForm(true)}
          style={{ height: 48, padding: '0 4px', border: 0, background: 'transparent', color: peachPlumColor.ink, fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 14, fontWeight: 600, textAlign: 'start', width: '100%' }}
        >
          {t('settings.pairAnotherTv')}
        </button>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 12 }}>
          <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('settings.enterCodeShown')}</span>
          <CodeSlotInput value={pairCode} onValueChange={setPairCode} />
          <div style={{ display: 'flex', gap: 8 }}>
            <PrimaryButton style={{ flexGrow: 1 }} disabled={pairCode.length !== 6 || pairState.busy} onClick={() => claim(pairCode, familyId)}>
              {pairState.busy ? t('settings.pairing') : t('settings.pair')}
            </PrimaryButton>
            <SecondaryButton
              style={{ flexGrow: 1 }}
              onClick={() => {
                setShowPairForm(false);
                setPairCode('');
                reset();
              }}
            >
              {t('common.cancel')}
            </SecondaryButton>
          </div>
          {pairState.message && <span style={{ fontSize: 13, fontWeight: 500, color: pairState.success ? peachPlumColor.okText : peachPlumColor.over }}>{pairState.message}</span>}
        </div>
      )}
    </div>
  );

  const settingsCard = (
    <>
      <SettingsRow label={t('settings.language')} value={languageValue} onClick={() => setShowLanguagePicker(true)} />
      <SettingsRow
        label={t('settings.notifications')}
        value={notificationsValue}
        onClick={notifications.status === 'default' ? notifications.enable : undefined}
        showTopBorder
      />
    </>
  );

  return (
    <RedesignLayout active="family" familyId={familyId}>
      {({ isWide }) => (
        <>
          <div style={{ position: 'relative', flexGrow: 1, display: 'flex', flexDirection: 'column', width: '100%', maxWidth: isWide ? 640 : 430, margin: '0 auto', padding: isWide ? '36px 40px' : '56px 22px 22px', boxSizing: 'border-box', gap: isWide ? 22 : 0 }}>
            {!isWide && (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: 18 }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                  <span style={{ fontWeight: 600, fontSize: 30, lineHeight: 1, letterSpacing: '-0.03em' }}>{t('topHeader.family')}</span>
                  <span style={{ fontSize: 13, fontWeight: 500, color: peachPlumColor.muted }}>{t('family.subtitle')}</span>
                </div>
                <span style={{ width: 36, height: 36, borderRadius: '50%', background: peachPlumColor.ink, color: peachPlumColor.surface, fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  P
                </span>
              </div>
            )}
            {isWide && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <span style={{ fontWeight: 600, fontSize: 34, lineHeight: 1, letterSpacing: '-0.03em' }}>{t('topHeader.family')}</span>
                <span style={{ fontSize: 14, fontWeight: 500, color: peachPlumColor.muted }}>{t('family.subtitle')}</span>
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', marginTop: isWide ? 0 : 0 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', paddingBottom: 6 }}>
                <span style={{ fontWeight: 600, fontSize: 18, letterSpacing: '-0.01em' }}>{t('settings.parents')}</span>
              </div>
              {isWide ? <div style={wideCardStyle}>{parentsCard}</div> : <WhiteCard>{parentsCard}</WhiteCard>}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', marginTop: 22 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', paddingBottom: 6 }}>
                <span style={{ fontWeight: 600, fontSize: 18, letterSpacing: '-0.01em' }}>{t('settings.tvs')}</span>
              </div>
              {tvsSection}
            </div>

            <div style={{ marginTop: isWide ? 0 : 22 }}>{isWide ? <div style={wideCardStyle}>{settingsCard}</div> : <WhiteCard>{settingsCard}</WhiteCard>}</div>
          </div>

          {confirmRemove && (
            <Modal onClose={() => setConfirmRemove(null)}>
              <h2 style={{ margin: '0 0 8px', fontSize: 18, fontWeight: 600, color: peachPlumColor.ink }}>{t('settings.removeCoParentTitle')}</h2>
              <p style={{ margin: 0, fontSize: 14, color: peachPlumColor.muted }}>{t('settings.removeCoParentBody')}</p>
              <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
                <SecondaryButton onClick={() => setConfirmRemove(null)}>{t('common.cancel')}</SecondaryButton>
                <PrimaryButton
                  style={{ background: peachPlumColor.over }}
                  onClick={() => {
                    removeMember(confirmRemove);
                    setConfirmRemove(null);
                  }}
                >
                  {t('common.remove')}
                </PrimaryButton>
              </div>
            </Modal>
          )}

          {renaming && (
            <Modal onClose={() => setRenaming(null)}>
              <RenameForm device={renaming} onCancel={() => setRenaming(null)} onSave={(name) => { rename(renaming.id, name); setRenaming(null); }} />
            </Modal>
          )}

          {confirmUnpair && (
            <Modal onClose={() => setConfirmUnpair(null)}>
              <p style={{ margin: '0 0 16px', fontSize: 14, color: peachPlumColor.ink }}>{t('settings.unpairConfirm')}</p>
              <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                <SecondaryButton onClick={() => setConfirmUnpair(null)}>{t('common.cancel')}</SecondaryButton>
                <PrimaryButton
                  style={{ background: peachPlumColor.over }}
                  onClick={() => {
                    unpair(familyId, confirmUnpair);
                    setConfirmUnpair(null);
                  }}
                >
                  {t('settings.unpair')}
                </PrimaryButton>
              </div>
            </Modal>
          )}

          {showLanguagePicker && (
            <Modal onClose={() => setShowLanguagePicker(false)}>
              <LanguagePicker current={current} onSelect={(tag) => { select(tag); setShowLanguagePicker(false); }} />
            </Modal>
          )}
        </>
      )}
    </RedesignLayout>
  );
}

function RenameForm({ device, onCancel, onSave }: { device: PairedDevice; onCancel: () => void; onSave: (name: string) => void }) {
  const { t } = useTranslation();
  const [name, setName] = useState(device.name);
  return (
    <div>
      <h2 style={{ margin: '0 0 12px', fontSize: 18, fontWeight: 600, color: peachPlumColor.ink }}>{t('settings.renameTvTitle')}</h2>
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        style={{ width: '100%', boxSizing: 'border-box', padding: 10, borderRadius: peachPlumRadius.input, border: `1.5px solid ${peachPlumColor.hairline}`, fontFamily: 'Rubik, system-ui, sans-serif', fontSize: 15 }}
      />
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SecondaryButton onClick={onCancel}>{t('common.cancel')}</SecondaryButton>
        <PrimaryButton onClick={() => onSave(name.trim() || device.name)}>{t('common.save')}</PrimaryButton>
      </div>
    </div>
  );
}

function LanguagePicker({ current, onSelect }: { current: LangTag | null; onSelect: (tag: LangTag | null) => void }) {
  const { t } = useTranslation();
  const options: { tag: LangTag | null; label: string }[] = [
    { tag: null, label: t('settings.languageSystem') },
    { tag: 'en', label: t('settings.languageEnglish') },
    { tag: 'he', label: t('settings.languageHebrew') },
  ];
  return (
    <div>
      <h2 style={{ margin: '0 0 12px', fontSize: 18, fontWeight: 600, color: peachPlumColor.ink }}>{t('settings.language')}</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {options.map((opt) => (
          <button
            key={opt.tag ?? 'system'}
            onClick={() => onSelect(opt.tag)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 4px',
              border: 0,
              background: 'transparent',
              fontFamily: 'Rubik, system-ui, sans-serif',
              fontSize: 15,
              fontWeight: 500,
              color: peachPlumColor.ink,
              textAlign: 'start',
            }}
          >
            {opt.label}
            {current === opt.tag && <span style={{ color: peachPlumColor.ink, fontWeight: 700 }}>✓</span>}
          </button>
        ))}
      </div>
    </div>
  );
}
