import type { Amiibo, AmiiboDetail, GameUsage, PlatformUsage } from "./types";

/** Shape of a single amiibo as returned by https://www.amiiboapi.org */
export type RawAmiibo = {
  amiiboSeries?: string;
  character?: string;
  gameSeries?: string;
  head?: string;
  tail?: string;
  image?: string;
  name?: string;
  type?: string;
  release?: Partial<Record<"na" | "eu" | "jp" | "au", string | null>>;
  [key: string]: unknown;
};

type RawGame = {
  gameName?: string;
  amiiboUsage?: { Usage?: string; write?: boolean }[];
};

const platformLabels: Record<string, string> = {
  games3DS: "Nintendo 3DS",
  gamesWiiU: "Wii U",
  gamesSwitch: "Nintendo Switch",
  gamesSwitch2: "Nintendo Switch 2",
};

const platformOrder = ["gamesSwitch2", "gamesSwitch", "gamesWiiU", "games3DS"];

export function normalizeAmiibo(raw: RawAmiibo): Amiibo | null {
  if (!raw.head || !raw.tail || !raw.name) return null;
  return {
    id: `${raw.head}${raw.tail}`,
    name: raw.name,
    character: raw.character ?? raw.name,
    amiiboSeries: raw.amiiboSeries ?? "Unknown",
    gameSeries: raw.gameSeries ?? "Unknown",
    type: raw.type ?? "Unknown",
    image: raw.image ?? "",
    release: {
      na: raw.release?.na ?? null,
      eu: raw.release?.eu ?? null,
      jp: raw.release?.jp ?? null,
      au: raw.release?.au ?? null,
    },
  };
}

export function normalizeAmiiboList(rawList: unknown): Amiibo[] {
  if (!Array.isArray(rawList)) return [];
  const seenIds = new Set<string>();
  const result: Amiibo[] = [];
  for (const raw of rawList) {
    const amiibo = normalizeAmiibo(raw as RawAmiibo);
    if (amiibo && !seenIds.has(amiibo.id)) {
      seenIds.add(amiibo.id);
      result.push(amiibo);
    }
  }
  return result;
}

function normalizeGames(rawGames: unknown): GameUsage[] {
  if (!Array.isArray(rawGames)) return [];
  return (rawGames as RawGame[])
    .filter((game) => typeof game.gameName === "string")
    .map((game) => ({
      gameName: game.gameName as string,
      usages: (game.amiiboUsage ?? [])
        .filter((usage) => typeof usage.Usage === "string" && usage.Usage.trim())
        .map((usage) => ({
          description: (usage.Usage as string).trim(),
          writesData: Boolean(usage.write),
        })),
    }))
    .sort((a, b) => a.gameName.localeCompare(b.gameName));
}

function platformLabel(key: string): string {
  return platformLabels[key] ?? key.replace(/^games/, "");
}

export function normalizeUsage(raw: RawAmiibo): PlatformUsage[] {
  const platformKeys = Object.keys(raw).filter(
    (key) => key.startsWith("games") && Array.isArray(raw[key]),
  );
  platformKeys.sort((a, b) => {
    const rankA = platformOrder.indexOf(a);
    const rankB = platformOrder.indexOf(b);
    return (rankA === -1 ? 99 : rankA) - (rankB === -1 ? 99 : rankB);
  });
  return platformKeys
    .map((key) => ({ platform: platformLabel(key), games: normalizeGames(raw[key]) }))
    .filter((platformUsage) => platformUsage.games.length > 0);
}

export function normalizeDetail(raw: RawAmiibo): AmiiboDetail | null {
  const amiibo = normalizeAmiibo(raw);
  if (!amiibo) return null;
  return { ...amiibo, usage: normalizeUsage(raw) };
}
