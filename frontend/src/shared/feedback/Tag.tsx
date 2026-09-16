import type { CSSProperties } from 'react';
import type { LucideIcon } from 'lucide-react';
import { getTagToneClassName, type TagTone } from './style';
import { Tooltip } from './Tooltip';

export type TagProps = {
  backgroundColor?: CSSProperties['backgroundColor'];
  className?: string;
  details?: string;
  icon?: LucideIcon;
  text: string;
  textColor?: string;
  tone?: TagTone;
};

export function Tag({
  backgroundColor,
  className,
  details,
  icon: Icon,
  text,
  textColor,
  tone = 'neutral',
}: TagProps) {
  const tagClassName = ['shared-tag', getTagToneClassName(tone), className].filter(Boolean).join(' ');
  const style: CSSProperties = {
    ...(backgroundColor ? { backgroundColor } : {}),
    ...(textColor ? { color: textColor } : {}),
  };
  const tag = (
    <span className={tagClassName} style={style} tabIndex={details ? 0 : undefined}>
      {Icon ? <Icon aria-hidden="true" size={16} /> : null}
      {text}
    </span>
  );

  return details ? <Tooltip content={details}>{tag}</Tooltip> : tag;
}