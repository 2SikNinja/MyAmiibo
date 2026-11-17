import type { Collection, CollectionStatus } from "./types";

export const emptyCollection: Collection = { owned: [], wishlist: [] };

const idPattern = /^[0-9a-f]{16}$/i;

function cleanIds(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return Array.from(
    new Set(value.filter((id): id is string => typeof id === "string" && idPattern.test(id))),
  );
}

/** Parses stored or imported JSON into a valid collection. Never throws. */
export function parseCollection(json: string | null): Collection {
  if (!json) return emptyCollection;
  try {
    const data = JSON.parse(json) as Partial<Record<keyof Collection, unknown>>;
    const owned = cleanIds(data?.owned);
    const ownedSet = new Set(owned);
    const wishlist = cleanIds(data?.wishlist).filter((id) => !ownedSet.has(id));
    return { owned, wishlist };
  } catch {
    return emptyCollection;
  }
}

export function statusOf(collection: Collection, id: string): CollectionStatus | null {
  if (collection.owned.includes(id)) return "owned";
  if (collection.wishlist.includes(id)) return "wishlist";
  return null;
}

/**
 * Sets the status of an amiibo. Owned and wishlist are exclusive, so marking
 * one removes it from the other. Passing null removes it from both.
 */
export function setStatus(
  collection: Collection,
  id: string,
  status: CollectionStatus | null,
): Collection {
  const owned = collection.owned.filter((existingId) => existingId !== id);
  const wishlist = collection.wishlist.filter((existingId) => existingId !== id);
  if (status === "owned") owned.push(id);
  if (status === "wishlist") wishlist.push(id);
  return { owned, wishlist };
}

/** Clicking the active status clears it; clicking the other one switches. */
export function toggleStatus(
  collection: Collection,
  id: string,
  status: CollectionStatus,
): Collection {
  return setStatus(collection, id, statusOf(collection, id) === status ? null : status);
}
