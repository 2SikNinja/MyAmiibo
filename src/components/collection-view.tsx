"use client";

import Link from "next/link";
import { useMemo, useRef, useState } from "react";
import { parseCollection } from "@/lib/collection";
import { collectionStats } from "@/lib/stats";
import type { Amiibo, CollectionStatus } from "@/lib/types";
import { saveCollection, useCollection, useHydrated } from "@/lib/use-collection";
import { AmiiboCard } from "./amiibo-card";

export function CollectionView({ amiiboList }: { amiiboList: Amiibo[] }) {
  const { collection } = useCollection();
  const isHydrated = useHydrated();
  const [activeTab, setActiveTab] = useState<CollectionStatus>("owned");
  const [importMessage, setImportMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const stats = useMemo(() => collectionStats(amiiboList, collection), [amiiboList, collection]);

  const amiiboById = useMemo(
    () => new Map(amiiboList.map((amiibo) => [amiibo.id, amiibo])),
    [amiiboList],
  );

  const tabItems = useMemo(() => {
    const ids = activeTab === "owned" ? collection.owned : collection.wishlist;
    return ids
      .map((id) => amiiboById.get(id))
      .filter((amiibo): amiibo is Amiibo => Boolean(amiibo))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [activeTab, collection, amiiboById]);

  function exportCollection() {
    const blob = new Blob([JSON.stringify(collection, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "amiibo-collection.json";
    link.click();
    URL.revokeObjectURL(url);
  }

  async function importCollection(file: File) {
    const imported = parseCollection(await file.text());
    if (imported.owned.length === 0 && imported.wishlist.length === 0) {
      setImportMessage("That file has no amiibo in it, so nothing changed.");
      return;
    }
    const hasExisting = collection.owned.length + collection.wishlist.length > 0;
    if (hasExisting && !window.confirm("Replace your current collection with this backup?")) {
      return;
    }
    saveCollection(imported);
    setImportMessage(
      `Imported ${imported.owned.length} owned and ${imported.wishlist.length} wishlist amiibo.`,
    );
  }

  if (!isHydrated) {
    return <CollectionSkeleton />;
  }

  return (
    <div className="space-y-8">
      <section className="grid gap-3 sm:grid-cols-3">
        <StatCard label="Owned" value={stats.ownedCount} detail={`${stats.percent}% of ${stats.totalCount.toLocaleString()}`} />
        <StatCard label="Wishlist" value={stats.wishlistCount} detail="amiibo you want" />
        <StatCard label="Series started" value={stats.series.length} detail={`${stats.series.filter((entry) => entry.percent === 100).length} complete`} />
      </section>

      {stats.series.length > 0 ? (
        <section>
          <h2 className="text-lg font-semibold">Series progress</h2>
          <ul className="mt-3 grid gap-3 md:grid-cols-2">
            {stats.series.map((entry) => (
              <li key={entry.series} className="rounded-lg border border-border bg-surface p-3">
                <div className="flex items-baseline justify-between gap-3 text-sm">
                  <span className="truncate font-medium">{entry.series}</span>
                  <span className="shrink-0 tabular-nums text-muted">
                    {entry.owned}/{entry.total}
                  </span>
                </div>
                <div
                  className="mt-2 h-2 overflow-hidden rounded-full bg-surface-muted"
                  role="progressbar"
                  aria-label={`${entry.series} progress`}
                  aria-valuenow={entry.percent}
                  aria-valuemin={0}
                  aria-valuemax={100}
                >
                  <div
                    className={`h-full rounded-full ${entry.percent === 100 ? "bg-owned" : "bg-accent"}`}
                    style={{ width: `${entry.percent}%` }}
                  />
                </div>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div role="tablist" className="flex gap-1 rounded-lg bg-surface-muted p-1 text-sm">
            {(["owned", "wishlist"] as const).map((tab) => (
              <button
                key={tab}
                type="button"
                role="tab"
                aria-selected={activeTab === tab}
                onClick={() => setActiveTab(tab)}
                className={`rounded-md px-4 py-1.5 font-medium ${
                  activeTab === tab ? "bg-surface shadow-sm" : "text-muted hover:text-foreground"
                }`}
              >
                {tab === "owned" ? `Owned (${stats.ownedCount})` : `Wishlist (${stats.wishlistCount})`}
              </button>
            ))}
          </div>
          <div className="flex gap-2 text-sm">
            <button
              type="button"
              onClick={exportCollection}
              className="rounded-lg border border-border bg-surface px-3 py-1.5 hover:border-accent hover:text-accent"
            >
              Export backup
            </button>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="rounded-lg border border-border bg-surface px-3 py-1.5 hover:border-accent hover:text-accent"
            >
              Import backup
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="application/json,.json"
              className="hidden"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) void importCollection(file);
                event.target.value = "";
              }}
            />
          </div>
        </div>
        {importMessage ? (
          <p role="status" className="mt-3 text-sm text-muted">
            {importMessage}
          </p>
        ) : null}

        {tabItems.length === 0 ? (
          <div className="mt-6 rounded-xl border border-dashed border-border p-10 text-center text-muted">
            {activeTab === "owned"
              ? "You haven't marked any amiibo as owned yet."
              : "Your wishlist is empty."}{" "}
            <Link href="/" className="text-accent hover:underline">
              Browse amiibo
            </Link>
          </div>
        ) : (
          <ul className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
            {tabItems.map((amiibo) => (
              <li key={amiibo.id} className="flex">
                <AmiiboCard amiibo={amiibo} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function StatCard({ label, value, detail }: { label: string; value: number; detail: string }) {
  return (
    <div className="rounded-xl border border-border bg-surface p-4">
      <p className="text-sm text-muted">{label}</p>
      <p className="mt-1 text-3xl font-semibold tabular-nums">{value.toLocaleString()}</p>
      <p className="mt-1 text-xs text-muted">{detail}</p>
    </div>
  );
}

export function CollectionSkeleton() {
  return (
    <div className="grid animate-pulse gap-3 sm:grid-cols-3" aria-hidden>
      {[0, 1, 2].map((index) => (
        <div key={index} className="h-28 rounded-xl bg-surface-muted" />
      ))}
    </div>
  );
}
