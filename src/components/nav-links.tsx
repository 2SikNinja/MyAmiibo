"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/", label: "Browse" },
  { href: "/collection", label: "My Collection" },
];

function isLinkActive(href: string, pathname: string | null): boolean {
  if (!pathname) return false;
  return href === "/" ? pathname === "/" || pathname.startsWith("/amiibo") : pathname.startsWith(href);
}

/** Renders the nav. A null pathname (during prerender) highlights nothing. */
export function NavLinksView({ pathname }: { pathname: string | null }) {
  return (
    <nav className="flex items-center gap-1 text-sm">
      {links.map((link) => {
        const isActive = isLinkActive(link.href, pathname);
        return (
          <Link
            key={link.href}
            href={link.href}
            aria-current={isActive ? "page" : undefined}
            className={`rounded-md px-3 py-1.5 transition-colors ${
              isActive
                ? "bg-accent-soft font-medium text-accent"
                : "text-muted hover:bg-surface-muted hover:text-foreground"
            }`}
          >
            {link.label}
          </Link>
        );
      })}
    </nav>
  );
}

export function NavLinks() {
  const pathname = usePathname();
  return <NavLinksView pathname={pathname} />;
}
