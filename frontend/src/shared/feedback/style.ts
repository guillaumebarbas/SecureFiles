export type TagTone = 'danger' | 'info' | 'neutral' | 'success' | 'warning';

export function getTagToneClassName(tone: TagTone) {
  return `shared-tag--${tone}`;
}