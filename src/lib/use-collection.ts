"use client";

import { useCallback, useSyncExternalStore } from "react";
import { emptyCollection, parseCollection, toggleStatus } from "./collection";
import type { Collection, CollectionStatus } from "./types";

export const storageKey = "amiibo-collection-v1";

const listeners = new Set<() => void>();
let cachedRaw: string | null | undefined;
let cachedCollection: Collection = emptyCollection;

function readStorage(): string | null {
  try {
    return window.localStorage.getItem(storageKey);
  } catch {
    return null;
  }
}

function getSnapshot(): Collection {
  const raw = readStorage();
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedCollection = parseCollection(raw);
  }
  return cachedCollection;
}

function getServerSnapshot(): Collection {
  return emptyCollection;
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  const onStorage = (event: StorageEvent) => {
    if (event.key === storageKey) listener();
  };
  window.addEventListener("storage", onStorage);
  return () => {
    listeners.delete(listener);
    window.removeEventListener("storage", onStorage);
  };
}

export function saveCollection(collection: Collection): void {
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(collection));
  } catch {
    // Storage can be unavailable (private mode, quota). The UI still updates
    // for this session through the in-memory cache.
    cachedRaw = JSON.stringify(collection);
    cachedCollection = collection;
  }
  listeners.forEach((listener) => listener());
}

export function useCollection() {
  const collection = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  const toggle = useCallback((id: string, status: CollectionStatus) => {
    saveCollection(toggleStatus(getSnapshot(), id, status));
  }, []);

  return { collection, toggle };
}

/** True after hydration. Collection-dependent UI waits for this. */
export function useHydrated(): boolean {
  return useSyncExternalStore(
    noopSubscribe,
    () => true,
    () => false,
  );
}

function noopSubscribe(): () => void {
  return () => {};
}
