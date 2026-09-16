import { NextResponse } from 'next/server';

export async function GET() {
  return NextResponse.json({
    name: 'CrazeLauncher Web',
    status: 'ready',
    integration: 'android-bridge-ready',
  });
}
