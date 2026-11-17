import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { StatusButtons } from "@/components/status-buttons";
import { UsageList } from "@/components/usage-list";
import { getAmiiboDetail } from "@/lib/amiibo-api";
import type { ReleaseDates } from "@/lib/types";

const regionLabels: Record<keyof ReleaseDates, string> = {
  na: "North America",
  eu: "Europe",
  jp: "Japan",
  au: "Australia",
};

export async function generateMetadata(props: PageProps<"/amiibo/[id]">): Promise<Metadata> {
  const { id } = await props.params;
  const amiibo = await getAmiiboDetail(id);
  if (!amiibo) return { title: "Amiibo not found" };
  return {
    title: amiibo.name,
    description: `${amiibo.name} (${amiibo.amiiboSeries}): release dates and every game it works with.`,
  };
}

function formatDate(date: string | null): string {
  if (!date) return "Not released";
  const parsed = new Date(`${date}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime())) return date;
  return parsed.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    timeZone: "UTC",
  });
}

async function AmiiboDetails(props: PageProps<"/amiibo/[id]">) {
  const { id } = await props.params;
  const amiibo = await getAmiiboDetail(id);
  if (!amiibo) notFound();

  const gameCount = amiibo.usage.reduce((total, platform) => total + platform.games.length, 0);

  return (
    <div className="grid gap-8 md:grid-cols-[320px_1fr]">
      <aside className="space-y-4 md:sticky md:top-20 md:self-start">
        <div className="relative aspect-square overflow-hidden rounded-xl border border-border bg-surface">
          {amiibo.image ? (
            <Image
              src={amiibo.image}
              alt={amiibo.name}
              fill
              priority
              sizes="(min-width: 768px) 320px, 100vw"
              className="object-contain p-6"
            />
          ) : null}
        </div>
        <StatusButtons amiiboId={amiibo.id} amiiboName={amiibo.name} size="lg" />
        <dl className="divide-y divide-border rounded-xl border border-border bg-surface text-sm">
          {(
            [
              ["Character", amiibo.character],
              ["Amiibo series", amiibo.amiiboSeries],
              ["Game series", amiibo.gameSeries],
              ["Type", amiibo.type],
            ] as const
          ).map(([label, value]) => (
            <div key={label} className="flex justify-between gap-4 px-4 py-2.5">
              <dt className="text-muted">{label}</dt>
              <dd className="text-right font-medium">{value}</dd>
            </div>
          ))}
        </dl>
        <div className="rounded-xl border border-border bg-surface text-sm">
          <h2 className="border-b border-border px-4 py-2.5 font-semibold">Release dates</h2>
          <dl className="divide-y divide-border">
            {(Object.keys(regionLabels) as (keyof ReleaseDates)[]).map((region) => (
              <div key={region} className="flex justify-between gap-4 px-4 py-2.5">
                <dt className="text-muted">{regionLabels[region]}</dt>
                <dd className="tabular-nums">{formatDate(amiibo.release[region])}</dd>
              </div>
            ))}
          </dl>
        </div>
      </aside>

      <section>
        <h1 className="text-3xl font-semibold tracking-tight">{amiibo.name}</h1>
        <p className="mt-1 text-muted">
          {gameCount > 0
            ? `Works with ${gameCount} ${gameCount === 1 ? "game" : "games"}`
            : "No known game usage yet"}
        </p>
        <div className="mt-6">
          <UsageList usage={amiibo.usage} />
        </div>
      </section>
    </div>
  );
}

function DetailsSkeleton() {
  return (
    <div className="grid animate-pulse gap-8 md:grid-cols-[320px_1fr]" aria-hidden>
      <div className="aspect-square rounded-xl bg-surface-muted" />
      <div className="space-y-3">
        <div className="h-9 w-2/3 rounded bg-surface-muted" />
        <div className="h-5 w-1/3 rounded bg-surface-muted" />
        <div className="mt-6 h-40 rounded-xl bg-surface-muted" />
      </div>
    </div>
  );
}

export default function AmiiboPage(props: PageProps<"/amiibo/[id]">) {
  return (
    <>
      <Link href="/" className="mb-5 inline-block text-sm text-muted hover:text-foreground">
        ← Back to browse
      </Link>
      <Suspense fallback={<DetailsSkeleton />}>
        <AmiiboDetails {...props} />
      </Suspense>
    </>
  );
}
