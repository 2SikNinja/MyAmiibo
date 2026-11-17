import { describe, expect, it } from "vitest";
import { defaultFilters, filterAmiibo, firstReleaseDate, uniqueValues } from "../filters";
import { normalizeAmiiboList } from "../normalize";
import type { Collection } from "../types";
import { rawCard, rawIsabelle, rawMario } from "./fixtures";

const amiiboList = normalizeAmiiboList([rawMario, rawIsabelle, rawCard]);
const [mario, isabelle, card] = amiiboList;
const emptyCollection: Collection = { owned: [], wishlist: [] };
const anyType = { ...defaultFilters, type: "" };

function names(filters = anyType, collection = emptyCollection): string[] {
  return filterAmiibo(amiiboList, filters, collection).map((amiibo) => amiibo.name);
}

describe("filterAmiibo", () => {
  it("defaults to figures only", () => {
    expect(names(defaultFilters)).toEqual(["Isabelle - Winter Outfit", "Mario"]);
  });

  it("searches name, character, and series without caring about case or accents", () => {
    expect(names({ ...anyType, query: "CELESTE" })).toEqual(["Céleste"]);
    expect(names({ ...anyType, query: "smash mario" })).toEqual(["Mario"]);
    expect(names({ ...anyType, query: "pokemon trainer" })).toEqual(["Céleste"]);
  });

  it("filters by amiibo series and game series", () => {
    expect(names({ ...anyType, amiiboSeries: "Animal Crossing" })).toHaveLength(2);
    expect(names({ ...anyType, gameSeries: "Super Mario" })).toEqual(["Mario"]);
  });

  it("filters by ownership", () => {
    const collection: Collection = { owned: [mario.id], wishlist: [card.id] };
    expect(names({ ...anyType, ownership: "owned" }, collection)).toEqual(["Mario"]);
    expect(names({ ...anyType, ownership: "wishlist" }, collection)).toEqual(["Céleste"]);
    expect(names({ ...anyType, ownership: "missing" }, collection)).toEqual([
      "Céleste",
      "Isabelle - Winter Outfit",
    ]);
  });

  it("sorts by release date with unreleased last", () => {
    expect(names({ ...anyType, sort: "newest" })).toEqual([
      "Isabelle - Winter Outfit",
      "Mario",
      "Céleste",
    ]);
    expect(names({ ...anyType, sort: "oldest" })).toEqual([
      "Mario",
      "Isabelle - Winter Outfit",
      "Céleste",
    ]);
  });
});

describe("helpers", () => {
  it("finds the earliest regional release", () => {
    expect(firstReleaseDate(mario)).toBe("2014-11-21");
    expect(firstReleaseDate(isabelle)).toBe("2015-11-13");
    expect(firstReleaseDate(card)).toBeNull();
  });

  it("lists unique sorted values", () => {
    expect(uniqueValues(amiiboList, "type")).toEqual(["Card", "Figure"]);
  });
});
