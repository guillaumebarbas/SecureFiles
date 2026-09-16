import { startTransition, useEffect, useState } from 'react';
import { Home, LayoutGrid } from 'lucide-react';
import { checkBackendHealth } from './api/filesApi';
import { DashboardPage } from './pages/Dashboard/DashboardPage';
import { FilesPage } from './pages/Files/FilesPage';
import { SharedComponentsShowcasePage } from './pages/SharedComponentsShowcase/SharedComponentsShowcasePage';
import type { BackendStatusValue } from './shared/layout/BackendStatus/style';
import { Header } from './shared/layout/Header/Header';
import { SideNavBar } from './shared/layout/SideNavBar/SideNavBar';
import type { NavItemDefinition } from './shared/layout/NavItem/NavItem';
import { appClassNames } from './app/style';

export type AppRoute = '/' | '/components' | '/files';

const applicationVersion = '0.1.0';

const navigationItems: readonly NavItemDefinition[] = [
  { href: '/', icon: Home, label: 'Dashboard' },
  { href: '/components', icon: LayoutGrid, label: 'Bibliotheque' },
];

const routeMetadata: Record<AppRoute, { description: string; eyebrow: string; title: string }> = {
  '/': {
    description: 'Une vue d\'accueil pour suivre rapidement l\'etat du service et retrouver les zones de travail.',
    eyebrow: 'Vue d\'ensemble',
    title: 'Dashboard',
  },
  '/components': {
    description: 'Une page de reference pour verifier les composants de la console et leurs etats accessibles.',
    eyebrow: 'Bibliotheque partagee',
    title: 'Composants reutilisables',
  },
  '/files': {
    description: 'Un registre pret a recevoir les fichiers et leurs statuts de scan.',
    eyebrow: 'Registre des fichiers',
    title: 'Fichiers',
  },
};

function routeFromPathname(pathname: string): AppRoute {
  if (pathname === '/files') {
    return '/files';
  }

  if (pathname === '/components') {
    return '/components';
  }

  return '/';
}

function renderCurrentPage(route: AppRoute) {
  if (route === '/') {
    return <DashboardPage />;
  }

  if (route === '/components') {
    return <SharedComponentsShowcasePage />;
  }

  return <FilesPage />;
}

export function App() {
  const [currentRoute, setCurrentRoute] = useState<AppRoute>(() => routeFromPathname(window.location.pathname));
  const [backendStatus, setBackendStatus] = useState<BackendStatusValue>('unknown');

  useEffect(() => {
    let active = true;

    checkBackendHealth().then((status) => {
      if (active) {
        setBackendStatus(status);
      }
    });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    function handlePopState() {
      startTransition(() => {
        setCurrentRoute(routeFromPathname(window.location.pathname));
      });
    }

    window.addEventListener('popstate', handlePopState);

    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  function handleNavigate(href: string) {
    const nextRoute = routeFromPathname(href);

    if (nextRoute === currentRoute) {
      return;
    }

    window.history.pushState({}, '', nextRoute);
    startTransition(() => {
      setCurrentRoute(nextRoute);
    });
  }

  const currentRouteMetadata = routeMetadata[currentRoute];

  return (
    <div className={appClassNames.shell}>
      <SideNavBar
        activePath={currentRoute}
        backendStatus={backendStatus}
        items={navigationItems}
        onNavigate={handleNavigate}
        version={applicationVersion}
      />
      <div className={appClassNames.content}>
        <Header
          description={currentRouteMetadata.description}
          eyebrow={currentRouteMetadata.eyebrow}
          title={currentRouteMetadata.title}
        />
        <main className={appClassNames.main}>
          <div className={appClassNames.view} key={currentRoute}>
            {renderCurrentPage(currentRoute)}
          </div>
        </main>
      </div>
    </div>
  );
}