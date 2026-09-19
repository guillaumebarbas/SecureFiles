import indexHtml from '../../index.html?raw';
import iconSvg from '../../public/securefiles-icon.svg?raw';
import { describe, expect, it } from 'vitest';

describe('application branding', () => {
  it('declares the SecureFiles browser icon', () => {
    expect(indexHtml).toContain('<link rel="icon" type="image/svg+xml" href="/securefiles-icon.svg" />');
    expect(iconSvg).toContain('fill="#3967F6"');
  });
});