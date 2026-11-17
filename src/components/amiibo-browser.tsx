"use client";

import { useDeferredValue, useMemo, useState } from "react";
import {
  allValue,
  defaultFilters,
  filterAmiibo,
  uniqueValues,
  type AmiiboFilters,
  type OwnershipFilter,
  type SortOrder,
} from "@/lib/filters";
import type { Amiibo } from "@/lib/types";
import { useCollection, useHydrated } from "@/lib/use-collection";
import { AmiiboCard } from "./amiibo-card";

const pageSize = 48;

const ownershipOptions: { value: OwnershipFilter; label: string }[] = [
  { value: "all", label: "All" },
  { value: "owned", label: "Owned" },
  { value: "wishlist", label: "Wishlist" },
  { value: "missing", label: "Not owned" },
];

const sortOptions: { value: SortOrder; label: string }[] = [
  { value: "name", label: "Name (A to Z)" },
  { value: "newest", label: "Newest release" },
  { value: "oldest", label: "Oldest release" },
];

const selectClass =
  "w-full rounded-lg border border-border px-3 py-2 text-sm focus:border-accent focus:outline-none";

export function AmiiboBrowser({ amiiboList }: { amiiboList: Amiibo[] }) {
  const [filters, setFilters] = useState<AmiiboFilters>(defaultFilters);
  const [visibleCount, setVisibleCount] = useState(pageSize);
  const { collection } = useCollection();
  const isHydrated = useHydrated();
  const deferredFilters = useDeferredValue(filters);

  const typeOptions = useMemo(() => uniqueValues(amiiboList, "type"), [amiiboList]);

  // Series dropdowns only list values that exist for the chosen type.
  const typeScoped = useMemo(
    () => (filters.type ? amiiboList.filter((amiibo) => amiibo.type === filters.type) : amiiboList),
    [amiiboList, filters.type],
  );
  const amiiboSeriesOptions = useMemo(() => uniqueValues(typeScoped, "amiiboSeries"), [typeScoped]);
  const gameSeriesOptions = useMemo(() => uniqueValues(typeScoped, "gameSeries"), [typeScoped]);

  const results = useMemo(
    () => filterAmiibo(amiiboList, deferredFilters, collection),
    [amiiboList, deferredFilters, collection],
  );

  function updateFilters(changes: Partial<AmiiboFilters>) {
    setFilters((current) => {
      const next = { ...current, ...changes };
      if (changes.type !== undefined && changes.type !== current.type) {
        next.amiiboSeries = allValue;
        next.gameSeries = allValue;
      }
      return next;
    });
    setVisibleCount(pageSize);
  }

  const hasActiveFilters =
    JSON.stringify({ ...filters, sort: defaultFilters.sort }) !==
    JSON.stringify({ ...defaultFilters });

  return (
    <div>
      <div className="rounded-xl border border-border bg-surface p-4">
        <label htmlFor="search" className="sr-only">
          Search amiibo
        </label>
        <input
          id="search"
          type="search"
          value={filters.query}
          onChange={(event) => updateFilters({ query: event.target.value })}
          placeholder="Search by name, character, or series"
          autoComplete="off"
          className="w-full rounded-lg border border-border bg-surface-muted px-4 py-2.5 text-base focus:border-accent focus:outline-none"
        />

        <div className="mt-3 grid grid-cols-2 gap-3 md:grid-cols-5">
          <FilterSelect
            label="Type"
            value={filters.type}
            onChange={(value) => updateFilters({ type: value })}
            options={typeOptions}
            allLabel="All types"
          />
          <FilterSelect
            label="Amiibo series"
            value={filters.amiiboSeries}
            onChange={(value) => updateFilters({ amiiboSeries: value })}
            options={amiiboSeriesOptions}
            allLabel="All series"
          />
          <FilterSelect
            label="Game series"
            value={filters.gameSeries}
            onChange={(value) => updateFilters({ gameSeries: value })}
            options={gameSeriesOptions}
            allLabel="All game series"
          />
          <label className="text-xs font-medium text-muted">
            Collection
            <select
              className={`${selectClass} mt-1 text-foreground`}
              value={filters.ownership}
              onChange={(event) =>
                updateFilters({ ownership: event.target.value as OwnershipFilter })
              }
            >
              {ownershipOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <label className="col-span-2 text-xs font-medium text-muted md:col-span-1">
            Sort
            <select
              className={`${selectClass} mt-1 text-foreground`}
              value={filters.sort}
              onChange={(event) => updateFilters({ sort: event.target.value as SortOrder })}
            >
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="mt-4 flex items-center justify-between text-sm text-muted">
        <p aria-live="polite">
          {results.length.toLocaleString()} {results.length === 1 ? "result" : "results"}
          {!isHydrated && filters.ownership !== "all" ? " (loading collection)" : ""}
        </p>
        {hasActiveFilters ? (
          <button
            type="button"
            onClick={() => updateFilters({ ...defaultFilters, sort: filters.sort })}
            className="text-accent hover:underline"
          >
            Reset filters
          </button>
        ) : null}
      </div>

      {results.length === 0 ? (
        <div className="mt-10 rounded-xl border border-dashed border-border p-10 text-center text-muted">
          No amiibo match these filters.
        </div>
      ) : (
        <ul className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
          {results.slice(0, visibleCount).map((amiibo) => (
            <li key={amiibo.id} className="flex">
              <AmiiboCard amiibo={amiibo} />
            </li>
          ))}
        </ul>
      )}

      {visibleCount < results.length ? (
        <div className="mt-6 flex justify-center">
          <button
            type="button"
            onClick={() => setVisibleCount((count) => count + pageSize)}
            className="rounded-lg border border-border bg-surface px-5 py-2 text-sm font-medium hover:border-accent hover:text-accent"
          >
            Show more ({(results.length - visibleCount).toLocaleString()} left)
          </button>
        </div>
      ) : null}
    </div>
  );
}

type FilterSelectProps = {
  label: string;
  value: string;
  options: string[];
  allLabel: string;
  onChange: (value: string) => void;
};

function FilterSelect({ label, value, options, allLabel, onChange }: FilterSelectProps) {
  return (
    <label className="text-xs font-medium text-muted">
      {label}
      <select
        className={`${selectClass} mt-1 text-foreground`}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        <option value={allValue}>{allLabel}</option>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}
