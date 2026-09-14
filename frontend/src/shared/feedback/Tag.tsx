import type { CSSProperties } from 'react';
import type { LucideIcon } from 'lucide-react';
import { getTagToneClassName, type TagTone } from './style';

export type TagProps = {
  backgroundColor?: CSSProperties['backgroundColor'];
  className?: string;
  icon?: LucideIcon;
  text: string;
  textColor?: string;
  tone?: TagTone;
};

export function Tag({ backgroundColor, className, icon: Icon, text, textColor, tone = 'neutral' }: TagProps) {
  const tagClassName = ['shared-tag', getTagToneClassName(tone), className].filter(Boolean).join(' ');
  const style: CSSProperties = {
    ...(backgroundColor ? { backgroundColor } : {}),
    ...(textColor ? { color: textColor } : {}),
  };

  return (
    <span className={tagClassName} style={style}>
      {Icon ? <Icon aria-hidden="true" size={16} /> : null}
      {text}
    </span>
  );
}