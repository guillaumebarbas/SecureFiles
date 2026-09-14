import { useEffect, useState } from 'react';
import { FolderOpen, LayoutGrid } from 'lucide-react';
import { FilesPage } from './pages/Files/FilesPage';
import { SharedComponentsShowcasePage } from './pages/SharedComponentsShowcase/SharedComponentsShowcasePage';
import { Header } from './shared/layout/Header/Header';
import { SideNavBar } from './shared/layout/SideNavBar/SideNavBar';
import type { NavItemDefinition } from './shared/layout/NavItem/NavItem';
import { appClassNames } from './app/style';

export type AppRoute = '/' | '/files';

const applicationVersion = '0.1.0';

const navigationItems: readonly NavItemDefinition[] = [
  { href: '/', icon: LayoutGrid, label: 'Bibliotheque' },
  { href: '/files', icon: FolderOpen, label: 'Fichiers' },
];

const routeLabels: Record<AppRoute, string> = {
  '/': 'Bibliotheque de composants',
  '/files': 'Registre des fichiers',
};

function routeFromPathname(pathname: string): AppRoute {
  return pathname === '/files' ? '/files' : '/';
}

export function App() {
  const [currentRoute, setCurrentRoute] = useState<AppRoute>(() => routeFromPathname(window.location.pathname));

  useEffect(() => {
    function handlePopState() {
      setCurrentRoute(routeFromPathname(window.location.pathname));
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
    setCurrentRoute(nextRoute);
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
          {currentRoute === '/files' ? <FilesPage /> : <SharedComponentsShowcasePage />}
        </main>
      </div>
    </div>
  );
}