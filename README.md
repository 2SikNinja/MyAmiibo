# Amiibo Search Finder

Search every amiibo, see which games it works with, and track the ones you own or want. Available as a web app (Next.js) and a native iOS app (SwiftUI). Both use the same backup format, so a collection can move between them.

Data comes from [AmiiboAPI](https://www.amiiboapi.com/). Collections are stored on the device (browser `localStorage` on the web, `UserDefaults` on iOS). There is no account or backend.

## Features

- Search by name, character, or series (ignores case and accents)
- Filter by type (figure, card, yarn, band), amiibo series, game series, and ownership
- Sort by name or release date
- Detail page with release dates by region and every game the amiibo works with on Switch, Wii U, and 3DS, including which features save data to the figure
- Mark amiibo as owned or wishlist (the two are exclusive)
- Collection stats: total owned, wishlist count, and completion per series
- Export and import a JSON backup

## Web app

Next.js 16 (App Router, Cache Components), React 19, TypeScript, Tailwind CSS 4.

```bash
npm install
npm run dev        # http://localhost:3000
npm test           # Vitest unit tests
npm run typecheck
npm run lint
npm run build
```

The AmiiboAPI catalog is fetched on the server and cached for a day with `"use cache"` and `cacheLife("days")`. Set `AMIIBO_API_BASE` to point at a different API (for example a local mock).

```
src/
  app/                 Routes: / (browse), /amiibo/[id] (detail), /collection
  components/          UI components
  lib/
    amiibo-api.ts      Server-only AmiiboAPI client with caching
    normalize.ts       Raw API JSON to app models
    filters.ts         Search, filters, and sorting
    stats.ts           Collection stats
    collection.ts      Owned/wishlist logic and backup parsing
    use-collection.ts  localStorage-backed collection hook
```

## iOS app

SwiftUI, iOS 17+, using `@Observable`, `NavigationStack`, and async/await. The Xcode project is generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen) so it never has merge conflicts.

```bash
brew install xcodegen
cd ios
xcodegen generate
open AmiiboFinder.xcodeproj
```

Run the core logic tests from the command line:

```bash
cd ios/AmiiboCore
swift test
```

```
ios/
  project.yml          XcodeGen spec
  AmiiboCore/          Swift package: models, API client, decoding, filters, stats, tests
  AmiiboFinder/
    Stores/            CatalogStore (disk-cached catalog), CollectionStore
    Views/             Browse, filters, detail, collection screens
```

The catalog is cached on disk, so the app opens instantly and works offline. It refreshes once a day or on pull to refresh.

## CI

GitHub Actions runs lint, typecheck, unit tests, and a production build for the web app. It also runs the Swift tests and builds the iOS app for the simulator on macOS.
