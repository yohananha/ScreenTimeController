import { useEffect, useState } from 'react';
import * as repo from '../firebase/firestoreRepository';
import i18n, { detectDeviceLang, getStoredLangOverride, setStoredLangOverride, type LangTag } from '../i18n/i18n';

/**
 * Ports the Android LanguageViewModel's 3-tier design: localStorage (cold-start
 * cache), users/{uid}.language (portable preference), families/{id}/settings/language
 * (what the TV and push notifications render).
 *
 * Loop guard: the collector below only ever calls i18n.changeLanguage — it never
 * writes back to Firestore. Writes happen exclusively in select(). And it only
 * applies the remote value when it differs from the current language, so it can't
 * cycle.
 */
export function useLanguage(uid: string | null, familyId: string | null) {
  const [override, setOverride] = useState<LangTag | null>(getStoredLangOverride());

  useEffect(() => {
    const onLanguageChanged = () => setOverride(getStoredLangOverride());
    i18n.on('languageChanged', onLanguageChanged);
    return () => i18n.off('languageChanged', onLanguageChanged);
  }, []);

  useEffect(() => {
    if (!uid) return;
    return repo.subscribeUserLanguage(uid, (remoteTag) => {
      const applied = remoteTag ?? detectDeviceLang();
      setStoredLangOverride(remoteTag);
      if (applied !== i18n.language) void i18n.changeLanguage(applied);
      else setOverride(remoteTag);
    });
  }, [uid]);

  function select(tag: LangTag | null) {
    setStoredLangOverride(tag);
    setOverride(tag);
    void i18n.changeLanguage(tag ?? detectDeviceLang());
    if (uid) void repo.setUserLanguage(uid, tag);
    // The TV can't honor "follow the browser's language" — resolve to a concrete tag.
    if (familyId) void repo.setLanguage(familyId, tag ?? detectDeviceLang());
  }

  return { select, current: override };
}
