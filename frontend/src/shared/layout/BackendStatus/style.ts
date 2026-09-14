export type BackendStatusValue = 'offline' | 'online' | 'unknown';

export const backendStatusClassNames = {
  indicator: 'backend-status__indicator',
  root: 'backend-status',
} as const;

export const backendStatusLabels: Record<BackendStatusValue, string> = {
  offline: 'Backend indisponible',
  online: 'Backend disponible',
  unknown: 'Etat du backend inconnu',
};