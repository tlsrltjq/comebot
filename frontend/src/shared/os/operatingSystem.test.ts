import { describe, expect, it } from 'vitest';
import { detectOperatingSystem, operatingSystemGuide } from './operatingSystem';

describe('operatingSystem', () => {
  it('detects macOS and returns shell script guidance', () => {
    const os = detectOperatingSystem({ userAgentData: { platform: 'macOS' }, userAgent: '' });
    const guide = operatingSystemGuide(os);

    expect(os).toBe('macos');
    expect(guide.runScript).toBe('scripts/run-upbit-paper.sh');
    expect(guide.shortcutModifier).toBe('Cmd');
  });

  it('detects Windows and returns batch script guidance', () => {
    const os = detectOperatingSystem({ userAgentData: { platform: 'Windows' }, userAgent: '' });
    const guide = operatingSystemGuide(os);

    expect(os).toBe('windows');
    expect(guide.runScript).toBe('scripts\\run-upbit-paper.bat');
    expect(guide.workspacePath).toContain('%USERPROFILE%');
    expect(guide.shortcutModifier).toBe('Ctrl');
  });

  it('falls back to userAgent when userAgentData is unavailable', () => {
    expect(detectOperatingSystem({ userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X)' })).toBe('macos');
    expect(detectOperatingSystem({ userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' })).toBe('windows');
  });
});
