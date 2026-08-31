import { useEffect, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

/** Keeps <html lang>/<html dir> in sync with the active i18next language. */
export function LocaleProvider({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation();

  useEffect(() => {
    document.documentElement.lang = i18n.language;
    document.documentElement.dir = i18n.dir();
  }, [i18n, i18n.language]);

  return <>{children}</>;
}
