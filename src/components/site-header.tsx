import Image from "next/image";
import Link from "next/link";
import { Suspense } from "react";
import { NavLinks, NavLinksView } from "./nav-links";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-20 border-b border-border bg-surface/90 backdrop-blur">
      <div className="mx-auto flex h-14 w-full max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2 font-semibold tracking-tight">
          <Image src="/logo.png" alt="" width={28} height={28} className="rounded-md" />
          <span>Amiibo Search Finder</span>
        </Link>
        <Suspense fallback={<NavLinksView pathname={null} />}>
          <NavLinks />
        </Suspense>
      </div>
    </header>
  );
}
