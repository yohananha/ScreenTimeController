import { getMessaging, isSupported, type Messaging } from 'firebase/messaging';
import { app } from './app';

let cached: Messaging | null | undefined;

/** FCM Web isn't supported in every browser (e.g. Safari <16.4, some in-app webviews). */
export async function getMessagingIfSupported(): Promise<Messaging | null> {
  if (cached !== undefined) return cached;
  cached = (await isSupported()) ? getMessaging(app) : null;
  return cached;
}
