import type { PlatformUsage } from "@/lib/types";

export function UsageList({ usage }: { usage: PlatformUsage[] }) {
  if (usage.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-border p-8 text-center text-sm text-muted">
        AmiiboAPI doesn&apos;t list any game features for this amiibo.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {usage.map((platformUsage, platformIndex) => (
        <details
          key={platformUsage.platform}
          open={platformIndex === 0}
          className="group rounded-xl border border-border bg-surface"
        >
          <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 font-semibold">
            <span>{platformUsage.platform}</span>
            <span className="flex items-center gap-2 text-sm font-normal text-muted">
              {platformUsage.games.length} {platformUsage.games.length === 1 ? "game" : "games"}
              <span aria-hidden className="transition-transform group-open:rotate-180">
                ▾
              </span>
            </span>
          </summary>
          <ul className="divide-y divide-border border-t border-border">
            {platformUsage.games.map((game) => (
              <li key={game.gameName} className="px-4 py-3">
                <p className="font-medium">{game.gameName}</p>
                {game.usages.length > 0 ? (
                  <ul className="mt-1 space-y-1 text-sm text-muted">
                    {game.usages.map((usage, usageIndex) => (
                      <li key={usageIndex} className="flex items-start gap-2">
                        <span>{usage.description}</span>
                        {usage.writesData ? (
                          <span className="shrink-0 rounded bg-accent-soft px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-accent">
                            Saves data
                          </span>
                        ) : null}
                      </li>
                    ))}
                  </ul>
                ) : null}
              </li>
            ))}
          </ul>
        </details>
      ))}
    </div>
  );
}
