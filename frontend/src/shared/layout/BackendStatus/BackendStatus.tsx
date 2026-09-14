import { backendStatusClassNames, backendStatusLabels, type BackendStatusValue } from './style';
import { Row } from '../Row';

type BackendStatusProps = {
  onClick?: () => void;
  status: BackendStatusValue;
};

export function BackendStatus({ onClick, status }: BackendStatusProps) {
  const label = backendStatusLabels[status];
  const className = [backendStatusClassNames.root, `backend-status--${status}`].join(' ');

  return (
    <button aria-label={label} className={className} onClick={onClick} type="button">
      <Row align="center" as="span" gap="8px">
        <span aria-hidden="true" className={backendStatusClassNames.indicator} />
        <span>{label}</span>
      </Row>
    </button>
  );
}