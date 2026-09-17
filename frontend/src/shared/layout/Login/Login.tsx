import {
  useId,
  useRef,
  useState,
  useEffect,
  type FocusEvent,
  type FormEvent,
  type KeyboardEvent,
  type MouseEvent,
} from 'react';
import { Code2, CircleUserRound, LogIn, LogOut, ShieldCheck, UserRound, type LucideIcon } from 'lucide-react';
import type { UserRole } from '../../../api/filesApi';
import { Button } from '../../actions/Button';
import { loginClassNames } from './style';

export type LoginUser = {
  name: string;
  roles?: UserRole[];
};

export type LoginCredentials = {
  name: string;
  password: string;
};

export type RegisterCredentials = LoginCredentials & {
  roles: UserRole[];
};

type LoginProps = {
  className?: string;
  error?: string;
  isSubmitting?: boolean;
  openRequest?: number;
  onLogin?: (credentials: LoginCredentials) => void | Promise<void>;
  onLogout?: () => void | Promise<void>;
  onRegister?: (credentials: RegisterCredentials) => void | Promise<void>;
  user?: LoginUser;
};

const roleOptions: readonly {
  value: UserRole;
  label: string;
  icon: LucideIcon;
  disabled?: boolean;
}[] = [
  { value: 'developpeur', label: 'Développeur', icon: Code2 },
  { value: 'admin', label: 'Administrateur', icon: ShieldCheck, disabled: true },
  { value: 'utilisateur', label: 'Utilisateur', icon: UserRound },
];

export function Login({
  className,
  error,
  isSubmitting = false,
  openRequest = 0,
  onLogin,
  onLogout,
  onRegister,
  user,
}: LoginProps) {
  const fieldIdPrefix = useId().replace(/:/g, '');
  const rootRef = useRef<HTMLDivElement>(null);
  const lastOpenRequest = useRef(0);
  const [formMode, setFormMode] = useState<'login' | 'register'>('login');
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [pseudo, setPseudo] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<UserRole[]>(['utilisateur']);
  const [submissionPending, setSubmissionPending] = useState(false);
  const rootClassName = [loginClassNames.root, className].filter(Boolean).join(' ');
  const panelId = `${fieldIdPrefix}-login-panel`;
  const pseudoId = `${fieldIdPrefix}-pseudo`;
  const passwordId = `${fieldIdPrefix}-password`;
  const passwordHintId = `${fieldIdPrefix}-password-hint`;

  useEffect(() => {
    if (openRequest <= lastOpenRequest.current) {
      return;
    }

    lastOpenRequest.current = openRequest;
    if (!user) {
      setFormMode('login');
      setIsPanelOpen(true);
    }
  }, [openRequest, user]);

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
    setSelectedRoles(['utilisateur']);
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

  async function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPseudo = pseudo.trim();

    if (!normalizedPseudo) {
      return;
    }

    if (formMode === 'register' && (password.length < 8 || password.length > 255)) {
      return;
    }

    setSubmissionPending(true);
    try {
      if (formMode === 'register') {
        const roles = roleOptions
          .filter((role) => !role.disabled && selectedRoles.includes(role.value))
          .map((role) => role.value);
        await onRegister?.({ name: normalizedPseudo, password, roles });
      } else {
        await onLogin?.({ name: normalizedPseudo, password });
      }
      closePanel();
    } catch {
      return;
    } finally {
      setSubmissionPending(false);
    }
  }

  function showForm(mode: 'login' | 'register') {
    setFormMode(mode);
    setPassword('');
    setSelectedRoles(['utilisateur']);
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
            tabIndex={-1}
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
                  aria-describedby={formMode === 'register' ? passwordHintId : undefined}
                  id={passwordId}
                  maxLength={formMode === 'register' ? 255 : undefined}
                  minLength={formMode === 'register' ? 8 : undefined}
                  name="password"
                  onChange={(event) => setPassword(event.target.value)}
                  required
                  type="password"
                  value={password}
                />
              </label>
              {formMode === 'register' ? (
                <small className={loginClassNames.passwordHint} id={passwordHintId}>
                  8 à 255 caractères.
                </small>
              ) : null}
              {formMode === 'register' ? (
                <fieldset className={loginClassNames.roles}>
                  <legend>Rôles du compte</legend>
                  <div className={loginClassNames.roleGrid}>
                    {roleOptions.map(({ disabled, icon: RoleIcon, label, value }) => {
                      const isSelected = selectedRoles.includes(value);
                      return (
                        <button
                          aria-pressed={isSelected}
                          className={[
                            loginClassNames.role,
                            isSelected ? loginClassNames.roleSelected : undefined,
                          ].filter(Boolean).join(' ')}
                          disabled={disabled}
                          key={value}
                          onClick={() => {
                            setSelectedRoles((currentRoles) => {
                              if (currentRoles.includes(value)) {
                                return currentRoles.length === 1
                                  ? currentRoles
                                  : currentRoles.filter((role) => role !== value);
                              }
                              return [...currentRoles, value];
                            });
                          }}
                          type="button"
                        >
                          <RoleIcon aria-hidden="true" size={17} />
                          <span>{label}</span>
                        </button>
                      );
                    })}
                  </div>
                </fieldset>
              ) : null}
              {error ? <p aria-live="polite" className={loginClassNames.error} role="alert">{error}</p> : null}
              <Button
                disabled={isSubmitting || submissionPending}
                icon={LogIn}
                type="submit"
                variant="primary"
              >
                {isSubmitting || submissionPending
                  ? 'Traitement...'
                  : formMode === 'login' ? 'Se connecter' : "S'inscrire"}
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
