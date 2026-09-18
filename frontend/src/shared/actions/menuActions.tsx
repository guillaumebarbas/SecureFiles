import { useEffect, useId, useRef, useState, type FocusEvent, type KeyboardEvent } from 'react';
import { MoreHorizontal, type LucideIcon } from 'lucide-react';
import { BoutonMenuActions, type MenuActionColor } from './boutonMenuActions';
import { Button } from './Button';
import { menuActionsClassNames } from './style';
import { Column } from '../layout/Column';

export type MenuAction = {
  ariaLabel?: string;
  color: MenuActionColor;
  disabled?: boolean;
  href?: string;
  icon: LucideIcon;
  id: string;
  onSelect?: () => void | boolean | Promise<void | boolean>;
  text?: string;
};

export type MenuActionsProps = {
  actions: readonly MenuAction[];
  ariaLabel: string;
  emptyMessage?: string;
};

export function MenuActions({
  actions,
  ariaLabel,
  emptyMessage = 'Aucune action disponible.',
}: MenuActionsProps) {
  const [isOpen, setIsOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const menuId = `shared-menu-actions-${useId().replace(/:/g, '')}`;

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    rootRef.current?.querySelector<HTMLElement>('[role="menuitem"]:not([aria-disabled="true"])')?.focus();

    function closeWhenClickingOutside(event: Event) {
      const target = event.target;
      if (!(target instanceof Node) || !rootRef.current?.contains(target)) {
        closeMenu(false);
      }
    }

    document.addEventListener('pointerdown', closeWhenClickingOutside);
    return () => document.removeEventListener('pointerdown', closeWhenClickingOutside);
  }, [isOpen]);

  function closeMenu(restoreFocus = true) {
    setIsOpen(false);
    if (restoreFocus) {
      rootRef.current?.querySelector<HTMLButtonElement>('button')?.focus();
    }
  }

  function handleBlur(event: FocusEvent<HTMLDivElement>) {
    const nextFocusedElement = event.relatedTarget;
    if (
      isOpen
      && (!(nextFocusedElement instanceof Node) || !rootRef.current?.contains(nextFocusedElement))
    ) {
      closeMenu(false);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeMenu();
    }
  }

  async function handleAction(action: MenuAction) {
    if (action.disabled) {
      return;
    }

    const result = await action.onSelect?.();
    if (result !== false) {
      closeMenu(false);
    }
  }

  return (
    <div
      className={menuActionsClassNames.root}
      onBlur={handleBlur}
      onKeyDown={handleKeyDown}
      ref={rootRef}
    >
      <Button
        aria-controls={menuId}
        aria-expanded={isOpen}
        aria-haspopup="menu"
        aria-label={ariaLabel}
        className={menuActionsClassNames.trigger}
        icon={MoreHorizontal}
        onClick={() => setIsOpen((open) => !open)}
        variant="secondary"
      />
      {isOpen ? (
        <div
          aria-label={ariaLabel}
          className={menuActionsClassNames.menu}
          id={menuId}
          role="menu"
        >
          <Column gap="4px">
            {actions.length > 0 ? actions.map((action) => (
              <BoutonMenuActions
                ariaLabel={action.ariaLabel}
                color={action.color}
                disabled={action.disabled}
                href={action.href}
                icon={action.icon}
                key={action.id}
                onClick={() => { void handleAction(action); }}
                text={action.text}
              />
            )) : (
              <span className={menuActionsClassNames.empty}>{emptyMessage}</span>
            )}
          </Column>
        </div>
      ) : null}
    </div>
  );
}