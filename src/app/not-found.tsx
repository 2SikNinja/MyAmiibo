import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <h1 className="text-2xl font-semibold">Amiibo not found</h1>
      <p className="mt-2 text-muted">That amiibo doesn&apos;t exist or the link is broken.</p>
      <Link
        href="/"
        className="mt-6 inline-block rounded-lg bg-accent px-5 py-2 text-sm font-medium text-white hover:opacity-90"
      >
        Browse amiibo
      </Link>
    </div>
  );
}
