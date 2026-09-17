export type BackendStatusValue = 'offline' | 'online' | 'unknown';

export const backendStatusClassNames = {
  indicator: 'backend-status__indicator',
  root: 'backend-status',
} as const;

export const backendStatusLabels: Record<BackendStatusValue, string> = {
  offline: 'Service offline',
  online: 'Service online',
  unknown: 'Vérification du service...',
};