import { cloneElement, useEffect, useId, useRef, useState, type FocusEvent, type ReactElement } from 'react';
import { createPortal } from 'react-dom';

type TooltipChildProps = {
  'aria-describedby'?: string;
  title?: string;
};

type TooltipProps = {
  children: ReactElement<TooltipChildProps>;
  content: string;
};

type TooltipPosition = {
  left: number;
  top: number;
};

export function Tooltip({ children, content }: TooltipProps) {
  const tooltipId = useId();
  const anchorRef = useRef<HTMLSpanElement>(null);
  const tooltipRef = useRef<HTMLSpanElement>(null);
  const [isVisible, setIsVisible] = useState(false);
  const [tooltipPosition, setTooltipPosition] = useState<TooltipPosition>({ left: 0, top: 0 });

  const child = cloneElement(children, {
    'aria-describedby': tooltipId,
    title: children.props.title ?? content,
  });

  function updateTooltipPosition() {
    const anchor = anchorRef.current;
    if (!anchor) {
      return;
    }

    const bounds = anchor.getBoundingClientRect();
    const tooltipWidth = tooltipRef.current?.getBoundingClientRect().width ?? 0;
    const viewportPadding = 12;
    const minimumLeft = Math.min(tooltipWidth / 2 + viewportPadding, window.innerWidth / 2);
    const maximumLeft = Math.max(window.innerWidth - tooltipWidth / 2 - viewportPadding, window.innerWidth / 2);
    const desiredLeft = bounds.left + bounds.width / 2;

    setTooltipPosition({
      left: Math.min(Math.max(desiredLeft, minimumLeft), maximumLeft),
      top: bounds.top - 8,
    });
  }

  useEffect(() => {
    if (!isVisible) {
      return undefined;
    }

    updateTooltipPosition();
    window.addEventListener('resize', updateTooltipPosition);
    window.addEventListener('scroll', updateTooltipPosition, true);

    return () => {
      window.removeEventListener('resize', updateTooltipPosition);
      window.removeEventListener('scroll', updateTooltipPosition, true);
    };
  }, [isVisible]);

  function handleBlur(event: FocusEvent<HTMLSpanElement>) {
    const nextFocusedElement = event.relatedTarget;
    if (!(nextFocusedElement instanceof Node) || !anchorRef.current?.contains(nextFocusedElement)) {
      setIsVisible(false);
    }
  }

  return (
    <>
      <span
        className="shared-tooltip"
        onBlur={handleBlur}
        onFocus={() => {
          setIsVisible(true);
          updateTooltipPosition();
        }}
        onMouseEnter={() => {
          setIsVisible(true);
          updateTooltipPosition();
        }}
        onMouseLeave={() => setIsVisible(false)}
        ref={anchorRef}
      >
      {child}
      </span>
      {typeof document === 'undefined'
        ? null
        : createPortal(
            <span
              className={`shared-tooltip__content${isVisible ? ' shared-tooltip__content--visible' : ''}`}
              id={tooltipId}
              role="tooltip"
              style={{ left: tooltipPosition.left, top: tooltipPosition.top }}
              ref={tooltipRef}
            >
              {content}
            </span>,
            document.body,
          )}
    </>
  );
}