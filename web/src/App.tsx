import { BrowserRouter } from 'react-router-dom';
import { AuthGate } from './app/AuthGate';

export function App() {
  return (
    <BrowserRouter>
      <AuthGate />
    </BrowserRouter>
  );
}
