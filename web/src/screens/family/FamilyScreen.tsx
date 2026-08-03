import { useState } from 'react';
import { colors } from '../../theme/colors';
import { radius } from '../../theme/radius';
import { typography } from '../../theme/typography';
import { useResponsivePadding } from '../../theme/breakpoints';
import { TopHeader } from '../../components/TopHeader';
import { SproutPrimaryButton, SproutGhostButton, SproutDangerButton } from '../../components/SproutButton';
import { CodeSlotInput } from '../../components/CodeSlotInput';
import { Modal } from '../../components/Modal';
import { useFamily } from '../../hooks/useFamily';
import { usePairedDevices } from '../../hooks/usePairedDevices';
import { isAdmin, isOwner, type Family, type FamilyRole } from '../../models/Family';
import type { PairedDevice } from '../../models/PairedDevice';

export function FamilyScreen({ familyId, uid }: { familyId: string; uid: string }) {
  const { state, removeMember, generateInvite } = useFamily(familyId);
  const hPad = useResponsivePadding();

  return (
    <div style={{ minHeight: '100%', background: colors.background, display: 'flex', justifyContent: 'center' }}>
      <div style={{ width: '100%', maxWidth: 600, padding: `0 ${hPad}px 24px`, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <TopHeader familyName="Family" parentInitial="P" />
        <div style={{ paddingTop: 4, paddingBottom: 6 }}>
          <h1 style={{ ...typography.display, fontSize: 26, color: colors.ink, margin: 0 }}>Family & devices</h1>
          <p style={{ ...typography.caption, color: colors.inkMuted, marginTop: 5 }}>Co-parents and your paired TV.</p>
        </div>

        {state.family && (
          <MembersSection
            family={state.family}
            currentUid={uid}
            onRemove={(memberUid) => removeMember(memberUid)}
            onGenerateInvite={generateInvite}
            inviteCode={state.inviteCode}
          />
        )}

        <PairTvSection familyId={familyId} />

        {state.error && (
          <p style={{ ...typography.caption, color: colors.overText, background: colors.overContainer, borderRadius: radius.input, padding: '10px 14px' }}>
            {state.error}
          </p>
        )}
      </div>
    </div>
  );
}

function MembersSection({
  family,
  currentUid,
  onRemove,
  onGenerateInvite,
  inviteCode,
}: {
  family: Family;
  currentUid: string;
  onRemove: (uid: string) => void;
  onGenerateInvite: () => void;
  inviteCode: string | null;
}) {
  const [showInvitePanel, setShowInvitePanel] = useState(false);
  const currentIsAdmin = isAdmin(family, currentUid);
  const members = Object.entries(family.members).sort(([uidA], [uidB]) => {
    const selfRank = (u: string) => (u === currentUid ? 0 : 1);
    const roleRank = (u: string) => (isOwner(family, u) ? 0 : family.members[u] === 'ADMIN' ? 1 : 2);
    return selfRank(uidA) - selfRank(uidB) || roleRank(uidA) - roleRank(uidB);
  });

  return (
    <div style={{ background: colors.surface, borderRadius: radius.card, padding: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: 12 }}>
        <span style={{ ...typography.headline, color: colors.ink }}>Parents</span>
        <span style={{ ...typography.caption, color: colors.inkMuted }}>
          {Object.keys(family.members).length} {Object.keys(family.members).length === 1 ? 'member' : 'members'}
        </span>
      </div>

      {members.map(([memberUid, role], index) => (
        <div key={memberUid}>
          {index > 0 && <div style={{ height: 1, background: colors.outline }} />}
          <MemberRow
            initial={memberUid === currentUid ? 'P' : 'C'}
            displayName={memberUid === currentUid ? 'You' : 'Co-parent'}
            isOwner={isOwner(family, memberUid)}
            isSelf={memberUid === currentUid}
            role={role}
            showActions={currentIsAdmin && memberUid !== currentUid && !isOwner(family, memberUid)}
            onRemove={() => onRemove(memberUid)}
          />
        </div>
      ))}

      <div style={{ height: 1, background: colors.outline }} />
      <div
        onClick={() => {
          setShowInvitePanel((v) => !v);
          if (!showInvitePanel) onGenerateInvite();
        }}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 12,
          border: '1.5px dashed #DDCFC2',
          borderRadius: 16,
          padding: 14,
          marginTop: 12,
          cursor: 'pointer',
        }}
      >
        <div style={{ width: 30, height: 30, borderRadius: '50%', background: colors.accentContainer, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          +
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ ...typography.headline, color: colors.ink }}>Invite a parent</div>
          <div style={{ ...typography.caption, color: colors.inkMuted }}>They join as a co-parent</div>
        </div>
      </div>

      {showInvitePanel && <InvitePanel inviteCode={inviteCode} onRefresh={onGenerateInvite} />}
    </div>
  );
}

function MemberRow({
  initial,
  displayName,
  isOwner: owner,
  isSelf,
  role,
  showActions,
  onRemove,
}: {
  initial: string;
  displayName: string;
  isOwner: boolean;
  isSelf: boolean;
  role: FamilyRole;
  showActions: boolean;
  onRemove: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  // Matches mobile's MemberRow: role promote/demote has no wired UI control today either
  // (the composable accepts a setRole callback but the row only exposes Remove/Cancel).

  const badge = owner && isSelf
    ? { bg: colors.accent, fg: colors.ink, label: 'Owner · you' }
    : owner
      ? { bg: colors.accent, fg: colors.ink, label: 'Owner' }
      : { bg: colors.accentContainer, fg: '#5B4D69', label: 'Co-parent' };

  return (
    <div data-role={role}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0' }}>
        <div style={{ width: 38, height: 38, borderRadius: '50%', background: colors.primary, display: 'flex', alignItems: 'center', justifyContent: 'center', ...typography.headline, color: colors.surface }}>
          {initial}
        </div>
        <div style={{ flex: 1, ...typography.bodyStrong, color: colors.ink }}>{displayName}</div>
        <span style={{ background: badge.bg, color: badge.fg, borderRadius: radius.pill, padding: '4px 9px', ...typography.caption, fontWeight: 800 }}>
          {badge.label}
        </span>
        {showActions && (
          <button
            onClick={() => setExpanded((v) => !v)}
            aria-label="Options"
            style={{ width: 36, height: 36, borderRadius: '50%', background: colors.surfaceSunken, border: 'none' }}
          >
            ⋮
          </button>
        )}
      </div>
      {expanded && showActions && (
        <div style={{ display: 'flex', gap: 8, paddingBottom: 8 }}>
          <SproutDangerButton onClick={() => setShowConfirm(true)} style={{ flex: 1 }}>
            Remove from family
          </SproutDangerButton>
          <SproutGhostButton onClick={() => setExpanded(false)} style={{ flex: 1 }}>
            Cancel
          </SproutGhostButton>
        </div>
      )}
      {showConfirm && (
        <Modal onClose={() => setShowConfirm(false)}>
          <h2 style={{ ...typography.headline, color: colors.ink, margin: '0 0 8px' }}>Remove co-parent?</h2>
          <p style={{ ...typography.body, color: colors.inkMuted }}>They will lose access to this family immediately.</p>
          <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
            <SproutGhostButton onClick={() => setShowConfirm(false)}>Cancel</SproutGhostButton>
            <SproutDangerButton
              onClick={() => {
                onRemove();
                setShowConfirm(false);
              }}
            >
              Remove
            </SproutDangerButton>
          </div>
        </Modal>
      )}
    </div>
  );
}

function InvitePanel({ inviteCode, onRefresh }: { inviteCode: string | null; onRefresh: () => void }) {
  const [copied, setCopied] = useState(false);
  return (
    <div style={{ background: colors.accentContainer, borderRadius: 20, padding: 16, marginTop: 12, display: 'flex', flexDirection: 'column', gap: 12 }}>
      <span style={{ ...typography.caption, color: colors.inkMuted }}>
        Share this code with the parent you're inviting. It expires in 48 hours.
      </span>
      {inviteCode ? (
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ flex: 1, background: colors.surface, borderRadius: radius.pill, padding: '10px 16px', ...typography.bodyStrong, color: colors.ink }}>
            {inviteCode}
          </div>
          <SproutPrimaryButton
            onClick={() => {
              void navigator.clipboard.writeText(inviteCode);
              setCopied(true);
            }}
          >
            {copied ? 'Copied!' : 'Copy'}
          </SproutPrimaryButton>
        </div>
      ) : (
        <span style={{ ...typography.caption, color: colors.inkMuted }}>Generating code…</span>
      )}
      <SproutGhostButton onClick={onRefresh} style={{ width: '100%' }}>
        Generate a new code
      </SproutGhostButton>
    </div>
  );
}

function PairTvSection({ familyId }: { familyId: string }) {
  const { devices, state, claim, reset, rename, unpair } = usePairedDevices(familyId);
  const [code, setCode] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [renaming, setRenaming] = useState<PairedDevice | null>(null);
  const [confirmUnpairId, setConfirmUnpairId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  if (state.success && showForm) {
    setCode('');
    setShowForm(false);
    reset();
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 2px' }}>
        <span style={{ ...typography.headline, color: colors.ink }}>TVs</span>
        <span style={{ ...typography.caption, color: colors.inkMuted }}>
          {devices.length === 0 ? 'None paired' : `${devices.length} paired`}
        </span>
      </div>

      {devices.length === 0 && !showForm ? (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 14,
            border: '1.5px dashed #DDCFC2',
            borderRadius: 24,
            padding: '22px 26px',
            textAlign: 'center',
          }}
        >
          <div style={{ width: 74, height: 74, borderRadius: '50%', background: '#F2EAE2', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 30 }}>
            📺
          </div>
          <span style={{ ...typography.title, color: colors.ink }}>No TV paired</span>
          <span style={{ ...typography.body, color: colors.inkMuted }}>
            Open ScreenTime on your Android TV and enter the 6-digit code it shows.
          </span>
          <SproutPrimaryButton onClick={() => setShowForm(true)}>Pair a TV</SproutPrimaryButton>
        </div>
      ) : (
        <div style={{ background: colors.ink, borderRadius: radius.card, overflow: 'hidden' }}>
          {devices.map((device, index) => (
            <div key={device.id}>
              {index > 0 && <div style={{ height: 0.5, background: 'rgba(255,255,255,0.07)' }} />}
              <DeviceRow
                device={device}
                isExpanded={expandedId === device.id}
                isConfirmingUnpair={confirmUnpairId === device.id}
                onToggle={() => setExpandedId(expandedId === device.id ? null : device.id)}
                onRename={() => setRenaming(device)}
                onRequestUnpair={() => setConfirmUnpairId(device.id)}
                onCancelUnpair={() => setConfirmUnpairId(null)}
                onConfirmUnpair={() => {
                  unpair(familyId, device.id);
                  setConfirmUnpairId(null);
                  setExpandedId(null);
                }}
              />
            </div>
          ))}
        </div>
      )}

      {devices.length > 0 &&
        (showForm ? (
          <div style={{ background: colors.surface, borderRadius: radius.card, padding: 20, display: 'flex', flexDirection: 'column', gap: 14 }}>
            <span style={{ ...typography.headline, color: colors.ink }}>Pair a TV</span>
            <span style={{ ...typography.body, color: colors.inkMuted }}>
              Enter the 6-digit code shown on the TV's pairing screen.
            </span>
            <CodeSlotInput value={code} onValueChange={setCode} />
            <div style={{ display: 'flex', gap: 8 }}>
              <SproutPrimaryButton onClick={() => claim(code, familyId)} disabled={code.length !== 6 || state.busy} style={{ flex: 1 }}>
                {state.busy ? 'Pairing…' : 'Pair'}
              </SproutPrimaryButton>
              <SproutGhostButton
                onClick={() => {
                  setShowForm(false);
                  setCode('');
                  reset();
                }}
                style={{ flex: 1 }}
              >
                Cancel
              </SproutGhostButton>
            </div>
            {state.message && (
              <span style={{ ...typography.caption, color: state.success ? colors.positiveText : colors.overText }}>
                {state.message}
              </span>
            )}
          </div>
        ) : (
          <SproutGhostButton onClick={() => setShowForm(true)} style={{ width: '100%' }}>
            + Pair another TV
          </SproutGhostButton>
        ))}

      {renaming && (
        <Modal onClose={() => setRenaming(null)}>
          <RenameDeviceForm
            device={renaming}
            onCancel={() => setRenaming(null)}
            onSave={(name) => {
              rename(renaming.id, name);
              setRenaming(null);
            }}
          />
        </Modal>
      )}
    </div>
  );
}

function DeviceRow({
  device,
  isExpanded,
  isConfirmingUnpair,
  onToggle,
  onRename,
  onRequestUnpair,
  onCancelUnpair,
  onConfirmUnpair,
}: {
  device: PairedDevice;
  isExpanded: boolean;
  isConfirmingUnpair: boolean;
  onToggle: () => void;
  onRename: () => void;
  onRequestUnpair: () => void;
  onCancelUnpair: () => void;
  onConfirmUnpair: () => void;
}) {
  return (
    <div>
      <div onClick={onToggle} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '11px 14px', cursor: 'pointer' }}>
        <div style={{ width: 30, height: 30, borderRadius: 8, background: 'rgba(255,255,255,0.07)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          📺
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ ...typography.bodyStrong, color: colors.surface }}>{device.name}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 2 }}>
            <span style={{ width: 5, height: 5, borderRadius: '50%', background: colors.positiveDisplay, display: 'inline-block' }} />
            <span style={{ ...typography.caption, color: '#9FE9CE' }}>Online</span>
          </div>
        </div>
        <span style={{ color: 'rgba(255,255,255,0.35)' }}>{isExpanded ? '▲' : '▼'}</span>
      </div>
      {isExpanded && (
        <div style={{ background: colors.darkSurface, padding: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ display: 'flex', gap: 8 }}>
            <StatChip label="Today" value="0m" />
            <StatChip label="Paired" value="Active" />
          </div>
          {isConfirmingUnpair ? (
            <div style={{ background: '#4A2230', border: '0.5px solid #7A3A48', borderRadius: 12, padding: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
              <span style={{ ...typography.caption, fontWeight: 700, color: '#FFD9D4' }}>
                Unpair this TV? Limits stop applying until you pair it again.
              </span>
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={onConfirmUnpair} style={{ flex: 1, background: colors.overDisplay, border: 'none', borderRadius: radius.pill, padding: '10px 0', color: '#fff', ...typography.label }}>
                  Unpair
                </button>
                <button onClick={onCancelUnpair} style={{ border: '1px solid #6A5A7E', background: 'none', borderRadius: radius.pill, padding: '10px 18px', color: colors.background, ...typography.label }}>
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 8 }}>
              <button onClick={onRename} style={{ flex: 1, border: '1px solid #6A5A7E', background: 'none', borderRadius: radius.pill, padding: '8px 0', color: colors.background, ...typography.label }}>
                Rename
              </button>
              <button onClick={onRequestUnpair} style={{ flex: 1, border: '1px solid #6A5A7E', background: 'none', borderRadius: radius.pill, padding: '8px 0', color: '#FFB7AF', ...typography.label }}>
                Unpair TV
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function StatChip({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ flex: 1, background: 'rgba(255,255,255,0.06)', borderRadius: 9, padding: '8px 10px' }}>
      <div style={{ ...typography.caption, color: colors.darkMutedText }}>{label}</div>
      <div style={{ ...typography.bodyStrong, color: colors.surface }}>{value}</div>
    </div>
  );
}

function RenameDeviceForm({
  device,
  onCancel,
  onSave,
}: {
  device: PairedDevice;
  onCancel: () => void;
  onSave: (name: string) => void;
}) {
  const [name, setName] = useState(device.name);
  return (
    <div>
      <h2 style={{ ...typography.headline, color: colors.ink, margin: '0 0 12px' }}>Rename TV</h2>
      <input
        value={name}
        onChange={(e) => setName(e.target.value)}
        style={{ width: '100%', padding: 10, borderRadius: radius.input, border: `1px solid ${colors.outline}` }}
      />
      <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
        <SproutGhostButton onClick={onCancel}>Cancel</SproutGhostButton>
        <SproutPrimaryButton onClick={() => onSave(name.trim() || device.name)}>Save</SproutPrimaryButton>
      </div>
    </div>
  );
}
