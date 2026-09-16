import {
  useId,
  useRef,
  useState,
  type FocusEvent,
  type FormEvent,
  type KeyboardEvent,
  type MouseEvent,
} from 'react';
import { CircleUserRound, LogIn, LogOut } from 'lucide-react';
import { Button } from '../../actions/Button';
import { loginClassNames } from './style';

export type LoginUser = {
  name: string;
};

type LoginProps = {
  className?: string;
  onLogin?: (user: LoginUser) => void;
  onLogout?: () => void;
  onRegister?: (user: LoginUser) => void;
  user?: LoginUser;
};

export function Login({ className, onLogin, onLogout, onRegister, user }: LoginProps) {
  const fieldIdPrefix = useId().replace(/:/g, '');
  const rootRef = useRef<HTMLDivElement>(null);
  const [formMode, setFormMode] = useState<'login' | 'register'>('login');
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [pseudo, setPseudo] = useState('');
  const rootClassName = [loginClassNames.root, className].filter(Boolean).join(' ');
  const panelId = `${fieldIdPrefix}-login-panel`;
  const pseudoId = `${fieldIdPrefix}-pseudo`;
  const passwordId = `${fieldIdPrefix}-password`;

  function openAccountMenu() {
    if (user) {
      setIsPanelOpen(true);
    }
  }

  function openConnectionForm() {
    if (!user) {
      setFormMode('login');
      setIsPanelOpen(true);
    }
  }

  function closePanel() {
    setIsPanelOpen(false);
    setFormMode('login');
    setPassword('');
    setPseudo('');
  }

  function handleBlur(event: FocusEvent<HTMLDivElement>) {
    const nextFocusedElement = event.relatedTarget;

    if (
      !nextFocusedElement ||
      !(nextFocusedElement instanceof Node) ||
      !rootRef.current?.contains(nextFocusedElement)
    ) {
      closePanel();
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      closePanel();
    }
  }

  function handleMouseLeave(event: MouseEvent<HTMLDivElement>) {
    const nextHoveredElement = event.relatedTarget;

    if (rootRef.current?.contains(document.activeElement)) {
      return;
    }

    if (
      !nextHoveredElement ||
      !(nextHoveredElement instanceof Node) ||
      !rootRef.current?.contains(nextHoveredElement)
    ) {
      closePanel();
    }
  }

  function handleFocus() {
    openAccountMenu();
  }

  function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPseudo = pseudo.trim();

    if (!normalizedPseudo) {
      return;
    }

    if (formMode === 'register') {
      onRegister?.({ name: normalizedPseudo });
    } else {
      onLogin?.({ name: normalizedPseudo });
    }

    closePanel();
  }

  function showForm(mode: 'login' | 'register') {
    setFormMode(mode);
    setPassword('');
    setIsPanelOpen(true);
  }

  function handleLogout() {
    closePanel();
    onLogout?.();
  }

  return (
    <div
      className={rootClassName}
      ref={rootRef}
      onBlur={handleBlur}
      onFocus={handleFocus}
      onKeyDown={handleKeyDown}
      onMouseEnter={handleFocus}
      onMouseLeave={handleMouseLeave}
    >
      {user ? (
        <>
          <button
            aria-expanded={isPanelOpen}
            aria-haspopup="menu"
            className={loginClassNames.account}
            onClick={openAccountMenu}
            type="button"
          >
            <CircleUserRound aria-hidden="true" size={18} />
            <span>{user.name}</span>
          </button>
          <div
            aria-label="Actions du compte"
            className={loginClassNames.menu}
            hidden={!isPanelOpen}
            role="menu"
          >
            <button
              className={loginClassNames.action}
              onClick={handleLogout}
              role="menuitem"
              type="button"
            >
              <LogOut aria-hidden="true" size={17} />
              <span>Se déconnecter</span>
            </button>
          </div>
        </>
      ) : (
        <>
          <Button
            aria-controls={panelId}
            aria-expanded={isPanelOpen}
            aria-haspopup="dialog"
            className={loginClassNames.primary}
            icon={LogIn}
            onClick={openConnectionForm}
            type="button"
            variant="primary"
          >
            Se connecter
          </Button>
          <div
            aria-label={formMode === 'login' ? 'Connexion' : 'Inscription'}
            className={loginClassNames.panel}
            id={panelId}
            hidden={!isPanelOpen}
            role="dialog"
          >
            <form
              aria-label={formMode === 'login' ? 'Connexion' : 'Inscription'}
              className={loginClassNames.form}
              onSubmit={handleFormSubmit}
            >
              <strong className={loginClassNames.formTitle}>
                {formMode === 'login' ? 'Se connecter' : "S'inscrire"}
              </strong>
              <label className={loginClassNames.field} htmlFor={pseudoId}>
                <span>Pseudo</span>
                <input
                  autoComplete="username"
                  id={pseudoId}
                  name="pseudo"
                  onChange={(event) => setPseudo(event.target.value)}
                  required
                  type="text"
                  value={pseudo}
                />
              </label>
              <label className={loginClassNames.field} htmlFor={passwordId}>
                <span>Mot de passe</span>
                <input
                  autoComplete={formMode === 'login' ? 'current-password' : 'new-password'}
                  id={passwordId}
                  name="password"
                  onChange={(event) => setPassword(event.target.value)}
                  required
                  type="password"
                  value={password}
                />
              </label>
              <Button icon={LogIn} type="submit" variant="primary">
                {formMode === 'login' ? 'Se connecter' : "S'inscrire"}
              </Button>
              <button
                className={loginClassNames.switch}
                onClick={() => showForm(formMode === 'login' ? 'register' : 'login')}
                type="button"
              >
                {formMode === 'login' ? 'Pas encore inscrit' : 'Déjà inscrit'}
              </button>
            </form>
          </div>
        </>
      )}
    </div>
  );
}
