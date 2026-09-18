import { createPortal } from 'react-dom';
import { useEffect, useId, useLayoutEffect, useRef, useState, type FocusEvent, type KeyboardEvent } from 'react';
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

type MenuPosition = {
  left: number;
  top: number;
};

const MENU_GAP = 6;
const VIEWPORT_MARGIN = 8;

function calculateMenuPosition(triggerRect: DOMRect, menuRect: DOMRect): MenuPosition {
  const belowTop = triggerRect.bottom + MENU_GAP;
  const aboveTop = triggerRect.top - menuRect.height - MENU_GAP;
  const top = belowTop + menuRect.height <= window.innerHeight - VIEWPORT_MARGIN
    ? belowTop
    : Math.max(VIEWPORT_MARGIN, aboveTop);
  const maxLeft = Math.max(
    VIEWPORT_MARGIN,
    window.innerWidth - menuRect.width - VIEWPORT_MARGIN,
  );
  const left = Math.min(
    maxLeft,
    Math.max(VIEWPORT_MARGIN, triggerRect.right - menuRect.width),
  );

  return { left, top };
}

export function MenuActions({
  actions,
  ariaLabel,
  emptyMessage = 'Aucune action disponible.',
}: MenuActionsProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState<MenuPosition | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const menuId = `shared-menu-actions-${useId().replace(/:/g, '')}`;

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    menuRef.current?.querySelector<HTMLElement>('[role="menuitem"]:not([aria-disabled="true"])')?.focus();

    function closeWhenClickingOutside(event: Event) {
      const target = event.target;
      const clickedInsideMenu = target instanceof Node && menuRef.current?.contains(target);
      const clickedInsideRoot = target instanceof Node && rootRef.current?.contains(target);
      if (!clickedInsideMenu && !clickedInsideRoot) {
        closeMenu(false);
      }
    }

    document.addEventListener('pointerdown', closeWhenClickingOutside);
    return () => document.removeEventListener('pointerdown', closeWhenClickingOutside);
  }, [isOpen]);

  useLayoutEffect(() => {
    if (!isOpen) {
      setMenuPosition(null);
      return undefined;
    }

    function updateMenuPosition() {
      const trigger = rootRef.current?.querySelector<HTMLButtonElement>('button');
      const menu = menuRef.current;
      if (!trigger || !menu) {
        return;
      }

      setMenuPosition(calculateMenuPosition(trigger.getBoundingClientRect(), menu.getBoundingClientRect()));
    }

    updateMenuPosition();
    window.addEventListener('resize', updateMenuPosition);
    window.addEventListener('scroll', updateMenuPosition, true);
    return () => {
      window.removeEventListener('resize', updateMenuPosition);
      window.removeEventListener('scroll', updateMenuPosition, true);
    };
  }, [isOpen]);

  function closeMenu(restoreFocus = true) {
    setIsOpen(false);
    if (restoreFocus) {
      rootRef.current?.querySelector<HTMLButtonElement>('button')?.focus();
    }
  }

  function handleBlur(event: FocusEvent<HTMLDivElement>) {
    const nextFocusedElement = event.relatedTarget;
    const focusRemainsInMenu = nextFocusedElement instanceof Node
      && (rootRef.current?.contains(nextFocusedElement) || menuRef.current?.contains(nextFocusedElement));
    if (
      isOpen
      && !focusRemainsInMenu
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

  const menu = isOpen ? (
    <div
      aria-label={ariaLabel}
      className={menuActionsClassNames.menu}
      id={menuId}
      onBlur={handleBlur}
      onKeyDown={handleKeyDown}
      ref={menuRef}
      role="menu"
      style={{
        left: menuPosition?.left ?? 0,
        top: menuPosition?.top ?? 0,
        visibility: menuPosition ? 'visible' : 'hidden',
      }}
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
  ) : null;

  return (
    <div
      className={[
        menuActionsClassNames.root,
        isOpen ? menuActionsClassNames.open : '',
      ].filter(Boolean).join(' ')}
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
      {menu ? createPortal(menu, document.body) : null}
    </div>
  );
}