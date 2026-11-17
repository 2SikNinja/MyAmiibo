export type ReleaseDates = {
  na: string | null;
  eu: string | null;
  jp: string | null;
  au: string | null;
};

export type Amiibo = {
  /** head + tail from AmiiboAPI. Unique per amiibo. */
  id: string;
  name: string;
  character: string;
  amiiboSeries: string;
  gameSeries: string;
  type: string;
  image: string;
  release: ReleaseDates;
};

export type GameUsage = {
  gameName: string;
  usages: { description: string; writesData: boolean }[];
};

export type PlatformUsage = {
  platform: string;
  games: GameUsage[];
};

export type AmiiboDetail = Amiibo & {
  usage: PlatformUsage[];
};

export type CollectionStatus = "owned" | "wishlist";

export type Collection = {
  owned: string[];
  wishlist: string[];
};
