import type { MetadataRoute } from "next";
import { getAllAmiibo } from "@/lib/amiibo-api";
import { siteUrl } from "@/lib/site";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const amiiboList = await getAllAmiibo();

  return [
    { url: siteUrl, changeFrequency: "weekly", priority: 1 },
    { url: `${siteUrl}/collection`, changeFrequency: "monthly", priority: 0.5 },
    ...amiiboList.map((amiibo) => ({
      url: `${siteUrl}/amiibo/${amiibo.id}`,
      changeFrequency: "monthly" as const,
      priority: 0.7,
    })),
  ];
}
