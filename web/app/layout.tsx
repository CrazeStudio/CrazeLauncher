import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'CrazeLauncher Web',
  description: 'A cinematic web experience for CrazeLauncher.',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
