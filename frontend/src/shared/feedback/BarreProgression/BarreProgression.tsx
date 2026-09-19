import { useEffect, useId, useState, type CSSProperties } from 'react';
import { Row } from '../../layout/Row';
import { barreProgressionClassNames } from './style';

export type BarreProgressionProps = {
  currentValue: number;
  gradient: CSSProperties['background'];
  maxValue: number;
  title: string;
  valueFormatter?: (value: number) => string;
};

const ANIMATION_DURATION_MS = 600;

export function BarreProgression({
  currentValue,
  gradient,
  maxValue,
  title,
  valueFormatter = (value) => String(Math.round(value)),
}: BarreProgressionProps) {
  const titleId = `shared-progress-bar-title-${useId().replace(/:/g, '')}`;
  const normalizedMaxValue = normalizeMaxValue(maxValue);
  const targetValue = normalizeCurrentValue(currentValue, normalizedMaxValue);
  const [animatedValue, setAnimatedValue] = useState(0);

  useEffect(() => {
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      setAnimatedValue(targetValue);
      return undefined;
    }

    let animationFrame = 0;
    let startTime: number | undefined;

    const animate = (timestamp: number) => {
      startTime ??= timestamp;
      const elapsed = timestamp - startTime;
      const progress = Math.min(1, elapsed / ANIMATION_DURATION_MS);
      setAnimatedValue(targetValue * progress);
      if (progress < 1) {
        animationFrame = window.requestAnimationFrame(animate);
      }
    };

    animationFrame = window.requestAnimationFrame(animate);
    return () => window.cancelAnimationFrame(animationFrame);
  }, [targetValue]);

  const percentage = (animatedValue / normalizedMaxValue) * 100;
  const formattedValue = valueFormatter(animatedValue);
  const formattedMaxValue = valueFormatter(normalizedMaxValue);

  return (
    <div
      aria-labelledby={titleId}
      aria-valuemax={normalizedMaxValue}
      aria-valuemin={0}
      aria-valuenow={Math.round(animatedValue)}
      aria-valuetext={`${formattedValue} / ${formattedMaxValue}`}
      className={barreProgressionClassNames.root}
      role="progressbar"
    >
      <Row
        align="center"
        className={barreProgressionClassNames.header}
        justify="space-between"
        wrap="wrap"
      >
        <span id={titleId}>{title}</span>
        <span className={barreProgressionClassNames.value}>
          {formattedValue} / {formattedMaxValue}
        </span>
      </Row>
      <div aria-hidden="true" className="shared-progress-bar__track">
        <span
          className={barreProgressionClassNames.fill}
          style={{ background: gradient, width: `${percentage}%` }}
        />
      </div>
    </div>
  );
}

function normalizeMaxValue(value: number): number {
  return Number.isFinite(value) && value > 0 ? value : 1;
}

function normalizeCurrentValue(value: number, maxValue: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }
  return Math.min(maxValue, Math.max(0, value));
}