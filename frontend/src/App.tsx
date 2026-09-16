import { startTransition, useEffect, useState } from 'react';
import { FolderOpen, Home, LayoutGrid } from 'lucide-react';
import { DashboardPage } from './pages/Dashboard/DashboardPage';
import { FilesPage } from './pages/Files/FilesPage';
import { SharedComponentsShowcasePage } from './pages/SharedComponentsShowcase/SharedComponentsShowcasePage';
import { Header } from './shared/layout/Header/Header';
import { SideNavBar } from './shared/layout/SideNavBar/SideNavBar';
import type { NavItemDefinition } from './shared/layout/NavItem/NavItem';
import { appClassNames } from './app/style';

export type AppRoute = '/' | '/components' | '/files';

const applicationVersion = '0.1.0';

const navigationItems: readonly NavItemDefinition[] = [
  { href: '/', icon: Home, label: 'Dashboard' },
  { href: '/components', icon: LayoutGrid, label: 'Bibliotheque' },
  { href: '/files', icon: FolderOpen, label: 'Fichiers' },
];

const routeLabels: Record<AppRoute, string> = {
  '/': 'Vue d\'ensemble',
  '/components': 'Bibliotheque de composants',
  '/files': 'Registre des fichiers',
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

  return (
    <div className={appClassNames.shell}>
      <SideNavBar
        activePath={currentRoute}
        backendStatus="unknown"
        items={navigationItems}
        onNavigate={handleNavigate}
        version={applicationVersion}
      />
      <div className={appClassNames.content}>
        <Header title="SecureFiles">
          <p>Console de depot securise</p>
          <p className={appClassNames.routeLabel}>{routeLabels[currentRoute]}</p>
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