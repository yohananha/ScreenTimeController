/**
 * A one-off message from the developer, read from config/announcement and
 * edited by hand in the Firebase console. Changing `id` shows it again to
 * everyone who already dismissed the previous one.
 */
export interface Announcement {
  id: string;
  title: string;
  body: string;
  ctaEmail: string | null;
  emailSubject: string | null;
}
