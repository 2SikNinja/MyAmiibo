"use client";

import { statusOf } from "@/lib/collection";
import { useCollection, useHydrated } from "@/lib/use-collection";

type StatusButtonsProps = {
  amiiboId: string;
  amiiboName: string;
  size?: "sm" | "lg";
};

export function StatusButtons({ amiiboId, amiiboName, size = "sm" }: StatusButtonsProps) {
  const { collection, toggle } = useCollection();
  const isHydrated = useHydrated();
  const status = isHydrated ? statusOf(collection, amiiboId) : null;

  const baseClass =
    size === "lg"
      ? "flex-1 rounded-lg border px-4 py-2.5 text-sm font-medium transition-colors"
      : "flex-1 rounded-md border px-2 py-1.5 text-xs font-medium transition-colors";

  return (
    <div className="flex gap-2">
      <button
        type="button"
        aria-pressed={status === "owned"}
        aria-label={`${status === "owned" ? "Remove" : "Mark"} ${amiiboName} as owned`}
        onClick={() => toggle(amiiboId, "owned")}
        className={`${baseClass} ${
          status === "owned"
            ? "border-owned bg-owned-soft text-owned"
            : "border-border text-muted hover:border-owned hover:text-owned"
        }`}
      >
        {status === "owned" ? "✓ Owned" : "Own it"}
      </button>
      <button
        type="button"
        aria-pressed={status === "wishlist"}
        aria-label={`${status === "wishlist" ? "Remove" : "Add"} ${amiiboName} ${
          status === "wishlist" ? "from" : "to"
        } wishlist`}
        onClick={() => toggle(amiiboId, "wishlist")}
        className={`${baseClass} ${
          status === "wishlist"
            ? "border-wish bg-wish-soft text-wish"
            : "border-border text-muted hover:border-wish hover:text-wish"
        }`}
      >
        {status === "wishlist" ? "★ Wishlist" : "Want it"}
      </button>
    </div>
  );
}
