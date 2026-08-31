import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en.json';
import he from './locales/he.json';

export type LangTag = 'en' | 'he';

const STORAGE_KEY = 'screentime.lang';

/** The phone/browser's own language, collapsed to what we support. */
export function detectDeviceLang(): LangTag {
  return typeof navigator !== 'undefined' && navigator.language.toLowerCase().startsWith('he') ? 'he' : 'en';
}

/** The locally cached override, or null for "follow device" — mirrors AppCompat's autoStoreLocales. */
export function getStoredLangOverride(): LangTag | null {
  if (typeof localStorage === 'undefined') return null;
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored === 'en' || stored === 'he' ? stored : null;
}

/** null clears the override, going back to "follow device". */
export function setStoredLangOverride(tag: LangTag | null): void {
  if (typeof localStorage === 'undefined') return;
  if (tag) localStorage.setItem(STORAGE_KEY, tag);
  else localStorage.removeItem(STORAGE_KEY);
}

void i18n
  .use(initReactI18next)
  .init({
    resources: { en: { translation: en }, he: { translation: he } },
    lng: getStoredLangOverride() ?? detectDeviceLang(),
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
  });

export default i18n;
