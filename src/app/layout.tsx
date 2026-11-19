import type { Metadata } from "next";
import { SiteHeader } from "@/components/site-header";
import { siteDescription, siteName, siteUrl } from "@/lib/site";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: `${siteName}: Amiibo Search and Collection Tracker`,
    template: `%s | ${siteName}`,
  },
  description: siteDescription,
  applicationName: siteName,
  openGraph: {
    type: "website",
    siteName,
    url: "/",
  },
  twitter: {
    card: "summary_large_image",
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="flex min-h-full flex-col font-sans">
        <SiteHeader />
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 sm:px-6">{children}</main>
        <footer className="border-t border-border py-6 text-center text-xs text-muted">
          Data from{" "}
          <a
            className="underline hover:text-foreground"
            href="https://www.amiiboapi.org/"
            target="_blank"
            rel="noreferrer"
          >
            AmiiboAPI
          </a>
          . Not affiliated with Nintendo. Your collection is stored only in this browser.
        </footer>
      </body>
    </html>
  );
}
