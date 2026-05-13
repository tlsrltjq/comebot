export type OperatingSystem = 'macos' | 'windows' | 'linux' | 'unknown';

interface NavigatorUserAgentData {
  platform?: string;
}

interface NavigatorLike {
  userAgent?: string;
  userAgentData?: NavigatorUserAgentData;
}

export interface OperatingSystemGuide {
  os: OperatingSystem;
  label: string;
  shell: string;
  runScript: string;
  schemaScript: string;
  workspacePath: string;
  shortcutModifier: string;
}

export function detectOperatingSystem(navigatorLike: NavigatorLike = globalThis.navigator ?? {}): OperatingSystem {
  const value = `${navigatorLike.userAgentData?.platform ?? ''} ${navigatorLike.userAgent ?? ''}`.toLowerCase();
  if (value.includes('mac')) {
    return 'macos';
  }
  if (value.includes('win')) {
    return 'windows';
  }
  if (value.includes('linux')) {
    return 'linux';
  }
  return 'unknown';
}

export function operatingSystemGuide(os: OperatingSystem): OperatingSystemGuide {
  if (os === 'windows') {
    return {
      os,
      label: 'Windows',
      shell: 'PowerShell / Command Prompt',
      runScript: 'scripts\\run-upbit-paper.bat',
      schemaScript: 'scripts\\apply-schema.bat',
      workspacePath: '%USERPROFILE%\\workspace\\comebot',
      shortcutModifier: 'Ctrl',
    };
  }

  if (os === 'linux') {
    return {
      os,
      label: 'Linux',
      shell: 'bash',
      runScript: 'scripts/run-upbit-paper.sh',
      schemaScript: 'scripts/apply-schema.sh',
      workspacePath: '$HOME/workspace/comebot',
      shortcutModifier: 'Ctrl',
    };
  }

  if (os === 'macos') {
    return {
      os,
      label: 'macOS',
      shell: 'zsh / bash',
      runScript: 'scripts/run-upbit-paper.sh',
      schemaScript: 'scripts/apply-schema.sh',
      workspacePath: '/Users/<user>/workspace/comebot',
      shortcutModifier: 'Cmd',
    };
  }

  return {
    os,
    label: '알 수 없음(Unknown)',
    shell: '사용자 shell(User shell)',
    runScript: 'scripts/run-upbit-paper.sh 또는 scripts\\run-upbit-paper.bat',
    schemaScript: 'scripts/apply-schema.sh 또는 scripts\\apply-schema.bat',
    workspacePath: '/Users/<user>/workspace/comebot 또는 %USERPROFILE%\\workspace\\comebot',
    shortcutModifier: 'Cmd / Ctrl',
  };
}
