import { AmiiboBrowser } from "@/components/amiibo-browser";
import { getAllAmiibo } from "@/lib/amiibo-api";

export default async function HomePage() {
  const amiiboList = await getAllAmiibo();

  return (
    <>
      <div className="mb-5">
        <h1 className="text-2xl font-semibold tracking-tight">Find an amiibo</h1>
        <p className="mt-1 text-sm text-muted">
          Browse {amiiboList.length.toLocaleString()} amiibo, check what they unlock, and track your
          collection.
        </p>
      </div>
      <AmiiboBrowser amiiboList={amiiboList} />
    </>
  );
}
