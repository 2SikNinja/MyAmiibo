"use client";

import { useEffect } from "react";

export default function ErrorPage({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-2xl font-semibold">Couldn&apos;t load amiibo data</h1>
      <p className="mt-2 text-muted">
        AmiiboAPI may be down or slow right now. Your collection is safe in this browser.
      </p>
      <button
        type="button"
        onClick={() => retry()}
        className="mt-6 rounded-lg bg-accent px-5 py-2 text-sm font-medium text-white hover:opacity-90"
      >
        Try again
      </button>
    </div>
  );
}
