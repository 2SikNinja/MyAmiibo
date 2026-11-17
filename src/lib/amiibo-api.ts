import "server-only";
import { cacheLife } from "next/cache";
import { normalizeAmiiboList, normalizeDetail, type RawAmiibo } from "./normalize";
import type { Amiibo, AmiiboDetail } from "./types";

const apiBase = process.env.AMIIBO_API_BASE ?? "https://www.amiiboapi.com/api";

async function fetchJson(path: string): Promise<unknown> {
  const response = await fetch(`${apiBase}${path}`, {
    headers: { Accept: "application/json" },
  });
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new Error(`AmiiboAPI request failed (${response.status}) for ${path}`);
  }
  return response.json();
}

/** Every amiibo, without game usage data. Refreshed daily. */
export async function getAllAmiibo(): Promise<Amiibo[]> {
  "use cache";
  cacheLife("days");

  const body = (await fetchJson("/amiibo/")) as { amiibo?: unknown } | null;
  const list = normalizeAmiiboList(body?.amiibo);
  if (list.length === 0) {
    throw new Error("AmiiboAPI returned no amiibo");
  }
  return list.sort((a, b) => a.name.localeCompare(b.name));
}

/** A single amiibo with game usage, or null when the id is unknown. */
export async function getAmiiboDetail(id: string): Promise<AmiiboDetail | null> {
  "use cache";
  cacheLife("days");

  if (!/^[0-9a-f]{16}$/i.test(id)) return null;
  const body = (await fetchJson(`/amiibo/?id=${id}&showusage`)) as {
    amiibo?: RawAmiibo | RawAmiibo[];
  } | null;
  const raw = Array.isArray(body?.amiibo) ? body.amiibo[0] : body?.amiibo;
  return raw ? normalizeDetail(raw) : null;
}
