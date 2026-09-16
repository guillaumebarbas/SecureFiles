import { startTransition, useEffect, useState } from 'react';
import { CircleUserRound, Home, LayoutGrid } from 'lucide-react';
import { checkBackendHealth } from './api/filesApi';
import { DashboardPage } from './pages/Dashboard/DashboardPage';
import { FilesPage } from './pages/Files/FilesPage';
import { ProfilePage } from './pages/Profile/ProfilePage';
import { SharedComponentsShowcasePage } from './pages/SharedComponentsShowcase/SharedComponentsShowcasePage';
import type { BackendStatusValue } from './shared/layout/BackendStatus/style';
import { Header } from './shared/layout/Header/Header';
import { Login } from './shared/layout/Login/Login';
import { SideNavBar } from './shared/layout/SideNavBar/SideNavBar';
import type { NavItemDefinition } from './shared/layout/NavItem/NavItem';
import { appClassNames } from './app/style';

export type AppRoute = '/' | '/components' | '/files' | '/profile';

const applicationVersion = '0.1.0';

const navigationItems: readonly NavItemDefinition[] = [
  { href: '/', icon: Home, label: 'Dashboard' },
  { href: '/profile', icon: CircleUserRound, label: 'Profil' },
  { href: '/components', icon: LayoutGrid, label: 'Bibliotheque' },
];

const routeMetadata: Record<AppRoute, { eyebrow: string; title: string }> = {
  '/': {
    eyebrow: 'Vue d\'ensemble',
    title: 'Dashboard',
  },
  '/components': {
    eyebrow: 'Bibliotheque partagee',
    title: 'Composants reutilisables',
  },
  '/files': {
    eyebrow: 'Registre des fichiers',
    title: 'Fichiers',
  },
  '/profile': {
    eyebrow: 'Compte',
    title: 'Profil',
  },
};

function routeFromPathname(pathname: string): AppRoute {
  if (pathname === '/files') {
    return '/files';
  }

  if (pathname === '/components') {
    return '/components';
  }

  if (pathname === '/profile') {
    return '/profile';
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

  if (route === '/profile') {
    return <ProfilePage />;
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
        <Header eyebrow={currentRouteMetadata.eyebrow} title={currentRouteMetadata.title}>
          <Login />
        </Header>
        <main className={appClassNames.main}>
          <div className={appClassNames.view} key={currentRoute}>
            {renderCurrentPage(currentRoute)}
          </div>
        </main>
      </div>
    </div>
  );
}