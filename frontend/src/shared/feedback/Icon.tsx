import type { LucideIcon } from 'lucide-react';

type IconProps = {
  className?: string;
  icon: LucideIcon;
  label?: string;
  size?: number;
};

export function Icon({ className, icon: IconComponent, label, size = 18 }: IconProps) {
  return (
    <IconComponent
      aria-hidden={label ? undefined : true}
      aria-label={label}
      className={className}
      size={size}
    />
  );
}