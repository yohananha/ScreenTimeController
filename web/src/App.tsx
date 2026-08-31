import { BrowserRouter } from 'react-router-dom';
import { AuthGate } from './app/AuthGate';
import { LocaleProvider } from './i18n/LocaleProvider';

export function App() {
  return (
    <LocaleProvider>
      <BrowserRouter>
        <AuthGate />
      </BrowserRouter>
    </LocaleProvider>
  );
}
