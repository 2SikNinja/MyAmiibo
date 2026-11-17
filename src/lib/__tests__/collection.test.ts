import { describe, expect, it } from "vitest";
import { emptyCollection, parseCollection, setStatus, statusOf, toggleStatus } from "../collection";
import { normalizeAmiiboList } from "../normalize";
import { collectionStats } from "../stats";
import { rawCard, rawIsabelle, rawMario } from "./fixtures";

const marioId = "0000000000000002";
const isabelleId = "0181000002430502";

describe("parseCollection", () => {
  it("handles empty and malformed input", () => {
    expect(parseCollection(null)).toEqual(emptyCollection);
    expect(parseCollection("not json")).toEqual(emptyCollection);
    expect(parseCollection("[1,2]")).toEqual(emptyCollection);
  });

  it("drops invalid ids, duplicates, and wishlist entries already owned", () => {
    const parsed = parseCollection(
      JSON.stringify({
        owned: [marioId, marioId, "bad", 42],
        wishlist: [marioId, isabelleId],
      }),
    );
    expect(parsed).toEqual({ owned: [marioId], wishlist: [isabelleId] });
  });
});

describe("status changes", () => {
  it("keeps owned and wishlist exclusive", () => {
    const wished = setStatus(emptyCollection, marioId, "wishlist");
    const owned = setStatus(wished, marioId, "owned");
    expect(owned).toEqual({ owned: [marioId], wishlist: [] });
    expect(statusOf(owned, marioId)).toBe("owned");
  });

  it("toggling the active status clears it", () => {
    const owned = toggleStatus(emptyCollection, marioId, "owned");
    expect(toggleStatus(owned, marioId, "owned")).toEqual(emptyCollection);
  });

  it("does not mutate the input", () => {
    const original = { owned: [marioId], wishlist: [] };
    setStatus(original, isabelleId, "owned");
    expect(original).toEqual({ owned: [marioId], wishlist: [] });
  });
});

describe("collectionStats", () => {
  const amiiboList = normalizeAmiiboList([rawMario, rawIsabelle, rawCard]);

  it("counts progress per series and ignores unknown ids", () => {
    const stats = collectionStats(amiiboList, {
      owned: [isabelleId, "ffffffffffffffff"],
      wishlist: [marioId],
    });
    expect(stats.ownedCount).toBe(1);
    expect(stats.wishlistCount).toBe(1);
    expect(stats.percent).toBe(33);
    expect(stats.series).toEqual([
      { series: "Animal Crossing", owned: 1, total: 2, percent: 50 },
    ]);
  });

  it("is empty for an empty collection", () => {
    const stats = collectionStats(amiiboList, emptyCollection);
    expect(stats.series).toEqual([]);
    expect(stats.percent).toBe(0);
  });
});
