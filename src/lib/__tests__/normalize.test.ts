import { describe, expect, it } from "vitest";
import { normalizeAmiibo, normalizeAmiiboList, normalizeDetail } from "../normalize";
import { rawCard, rawIsabelle, rawMario } from "./fixtures";

describe("normalizeAmiibo", () => {
  it("builds a unique id from head and tail", () => {
    expect(normalizeAmiibo(rawMario)?.id).toBe("0000000000000002");
  });

  it("rejects entries without head, tail, or name", () => {
    expect(normalizeAmiibo({ ...rawMario, head: undefined })).toBeNull();
    expect(normalizeAmiibo({ ...rawMario, name: undefined })).toBeNull();
  });

  it("fills missing release regions with null", () => {
    const amiibo = normalizeAmiibo({ ...rawMario, release: { na: "2014-11-21" } });
    expect(amiibo?.release).toEqual({ na: "2014-11-21", eu: null, jp: null, au: null });
  });
});

describe("normalizeAmiiboList", () => {
  it("drops duplicates and invalid entries", () => {
    const list = normalizeAmiiboList([rawMario, rawMario, { name: "Broken" }, rawIsabelle]);
    expect(list.map((amiibo) => amiibo.name)).toEqual(["Mario", "Isabelle - Winter Outfit"]);
  });

  it("returns an empty list for non-array input", () => {
    expect(normalizeAmiiboList(undefined)).toEqual([]);
  });
});

describe("normalizeDetail", () => {
  it("orders platforms newest first and skips empty ones", () => {
    const detail = normalizeDetail(rawMario);
    expect(detail?.usage.map((entry) => entry.platform)).toEqual(["Nintendo Switch", "Nintendo 3DS"]);
  });

  it("sorts games and maps usage fields", () => {
    const switchGames = normalizeDetail(rawMario)?.usage[0].games;
    expect(switchGames?.map((game) => game.gameName)).toEqual([
      "Super Mario Odyssey",
      "Super Smash Bros. Ultimate",
    ]);
    expect(switchGames?.[1].usages).toEqual([
      { description: "Battle a figure player", writesData: true },
    ]);
  });

  it("returns no usage when the API has none", () => {
    expect(normalizeDetail(rawCard)?.usage).toEqual([]);
  });
});
