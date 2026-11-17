import Image from "next/image";
import Link from "next/link";
import type { Amiibo } from "@/lib/types";
import { StatusButtons } from "./status-buttons";

export function AmiiboCard({ amiibo }: { amiibo: Amiibo }) {
  return (
    <article className="group flex w-full flex-col rounded-xl border border-border bg-surface p-3 transition-shadow hover:shadow-md">
      <Link href={`/amiibo/${amiibo.id}`} className="flex flex-1 flex-col">
        <div className="relative aspect-square w-full overflow-hidden rounded-lg bg-surface-muted">
          {amiibo.image ? (
            <Image
              src={amiibo.image}
              alt={amiibo.name}
              fill
              sizes="(min-width: 1024px) 200px, (min-width: 640px) 30vw, 45vw"
              className="object-contain p-3 transition-transform group-hover:scale-105"
            />
          ) : null}
        </div>
        <h3 className="mt-3 line-clamp-2 text-sm font-semibold leading-snug">{amiibo.name}</h3>
        <p className="mt-0.5 line-clamp-1 text-xs text-muted">{amiibo.amiiboSeries}</p>
      </Link>
      <div className="mt-3">
        <StatusButtons amiiboId={amiibo.id} amiiboName={amiibo.name} />
      </div>
    </article>
  );
}
