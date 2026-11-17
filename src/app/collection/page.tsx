import type { Metadata } from "next";
import { CollectionView } from "@/components/collection-view";
import { getAllAmiibo } from "@/lib/amiibo-api";

export const metadata: Metadata = {
  title: "My Collection",
};

export default async function CollectionPage() {
  const amiiboList = await getAllAmiibo();

  return (
    <>
      <div className="mb-5">
        <h1 className="text-2xl font-semibold tracking-tight">My Collection</h1>
        <p className="mt-1 text-sm text-muted">
          Saved in this browser. Export a backup to move it to another device.
        </p>
      </div>
      <CollectionView amiiboList={amiiboList} />
    </>
  );
}
