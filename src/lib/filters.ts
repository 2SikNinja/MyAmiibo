import type { Amiibo, Collection } from "./types";

export type OwnershipFilter = "all" | "owned" | "wishlist" | "missing";
export type SortOrder = "name" | "newest" | "oldest";

export type AmiiboFilters = {
  query: string;
  type: string;
  amiiboSeries: string;
  gameSeries: string;
  ownership: OwnershipFilter;
  sort: SortOrder;
};

export const defaultFilters: AmiiboFilters = {
  query: "",
  type: "Figure",
  amiiboSeries: "",
  gameSeries: "",
  ownership: "all",
  sort: "name",
};

export const allValue = "";

function normalizeText(text: string): string {
  return text
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .trim();
}

/** Earliest known release date across regions, as an ISO date string. */
export function firstReleaseDate(amiibo: Amiibo): string | null {
  const dates = Object.values(amiibo.release).filter(
    (date): date is string => typeof date === "string" && date.length > 0,
  );
  if (dates.length === 0) return null;
  return dates.sort()[0];
}

export function uniqueValues(amiiboList: Amiibo[], key: "type" | "amiiboSeries" | "gameSeries"): string[] {
  return Array.from(new Set(amiiboList.map((amiibo) => amiibo[key]))).sort((a, b) =>
    a.localeCompare(b),
  );
}

export function filterAmiibo(
  amiiboList: Amiibo[],
  filters: AmiiboFilters,
  collection: Collection,
): Amiibo[] {
  const queryTerms = normalizeText(filters.query).split(/\s+/).filter(Boolean);
  const ownedIds = new Set(collection.owned);
  const wishlistIds = new Set(collection.wishlist);

  const matches = amiiboList.filter((amiibo) => {
    if (filters.type && amiibo.type !== filters.type) return false;
    if (filters.amiiboSeries && amiibo.amiiboSeries !== filters.amiiboSeries) return false;
    if (filters.gameSeries && amiibo.gameSeries !== filters.gameSeries) return false;

    if (filters.ownership === "owned" && !ownedIds.has(amiibo.id)) return false;
    if (filters.ownership === "wishlist" && !wishlistIds.has(amiibo.id)) return false;
    if (filters.ownership === "missing" && ownedIds.has(amiibo.id)) return false;

    if (queryTerms.length > 0) {
      const haystack = normalizeText(
        `${amiibo.name} ${amiibo.character} ${amiibo.amiiboSeries} ${amiibo.gameSeries}`,
      );
      if (!queryTerms.every((term) => haystack.includes(term))) return false;
    }
    return true;
  });

  return sortAmiibo(matches, filters.sort);
}

export function sortAmiibo(amiiboList: Amiibo[], sort: SortOrder): Amiibo[] {
  const sorted = [...amiiboList];
  if (sort === "name") {
    return sorted.sort((a, b) => a.name.localeCompare(b.name));
  }
  const direction = sort === "newest" ? -1 : 1;
  return sorted.sort((a, b) => {
    const dateA = firstReleaseDate(a);
    const dateB = firstReleaseDate(b);
    if (dateA === dateB) return a.name.localeCompare(b.name);
    // Unreleased or unknown dates always go last.
    if (!dateA) return 1;
    if (!dateB) return -1;
    return dateA < dateB ? -direction : direction;
  });
}
