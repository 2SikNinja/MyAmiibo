import type { Amiibo, Collection } from "./types";

export type SeriesProgress = {
  series: string;
  owned: number;
  total: number;
  percent: number;
};

export type CollectionStats = {
  ownedCount: number;
  wishlistCount: number;
  totalCount: number;
  percent: number;
  series: SeriesProgress[];
};

function toPercent(part: number, whole: number): number {
  return whole === 0 ? 0 : Math.round((part / whole) * 100);
}

/**
 * Progress per amiibo series. Only series where the user owns at least one
 * amiibo are listed, sorted by completion and then by name.
 */
export function collectionStats(amiiboList: Amiibo[], collection: Collection): CollectionStats {
  const ownedIds = new Set(collection.owned);
  const knownIds = new Set(amiiboList.map((amiibo) => amiibo.id));
  const seriesTotals = new Map<string, { owned: number; total: number }>();

  for (const amiibo of amiiboList) {
    const entry = seriesTotals.get(amiibo.amiiboSeries) ?? { owned: 0, total: 0 };
    entry.total += 1;
    if (ownedIds.has(amiibo.id)) entry.owned += 1;
    seriesTotals.set(amiibo.amiiboSeries, entry);
  }

  const series = Array.from(seriesTotals, ([name, entry]) => ({
    series: name,
    owned: entry.owned,
    total: entry.total,
    percent: toPercent(entry.owned, entry.total),
  }))
    .filter((entry) => entry.owned > 0)
    .sort((a, b) => b.percent - a.percent || b.owned - a.owned || a.series.localeCompare(b.series));

  const ownedCount = collection.owned.filter((id) => knownIds.has(id)).length;
  const wishlistCount = collection.wishlist.filter((id) => knownIds.has(id)).length;

  return {
    ownedCount,
    wishlistCount,
    totalCount: amiiboList.length,
    percent: toPercent(ownedCount, amiiboList.length),
    series,
  };
}
