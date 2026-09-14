import { cloneElement, useId, type ReactElement } from 'react';

type TooltipChildProps = {
  'aria-describedby'?: string;
  title?: string;
};

type TooltipProps = {
  children: ReactElement<TooltipChildProps>;
  content: string;
};

export function Tooltip({ children, content }: TooltipProps) {
  const tooltipId = useId();

  const child = cloneElement(children, {
    'aria-describedby': tooltipId,
    title: children.props.title ?? content,
  });

  return (
    <span className="shared-tooltip">
      {child}
      <span className="shared-tooltip__content" id={tooltipId} role="tooltip">
        {content}
      </span>
    </span>
  );
}