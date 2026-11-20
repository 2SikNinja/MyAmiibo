# MyAmiibo

Search every amiibo, see which games it works with, and track the ones you own or want. Available as a web app (Next.js) at [amiibo.ppeternnguyen.com](https://amiibo.ppeternnguyen.com), a native iOS app (SwiftUI), and a native Android app (Kotlin and Jetpack Compose). All three use the same backup format, so a collection can move between them.

Data comes from [AmiiboAPI](https://www.amiiboapi.org/), which moved from amiiboapi.com to amiiboapi.org. Collections are stored on the device (browser `localStorage` on the web, `UserDefaults` on iOS, `SharedPreferences` on Android). There is no account or backend.

## Features

- Search by name, character, or series (ignores case and accents)
- Filter by type (figure, card, yarn, band), amiibo series, game series, and ownership
- Sort by name or release date
- Detail page with release dates by region and every game the amiibo works with on Switch 2, Switch, Wii U, and 3DS, including which features save data to the figure
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

## Android app

Kotlin and Jetpack Compose with Material 3, Navigation Compose, and ViewModels with `StateFlow`. Networking uses OkHttp and kotlinx.serialization, and images load with Coil. Minimum Android 8.0 (API 26), targeting API 36.

Open the `android` folder in Android Studio and run the `app` configuration, or from the command line:

```bash
cd android
./gradlew testDebugUnitTest   # JVM unit tests for the core logic
./gradlew lintDebug
./gradlew installDebug        # installs on a connected device or emulator
```

```
android/app/src/main/java/com/ppeternnguyen/myamiibo/
  core/        Plain Kotlin: models, decoding, filters, stats, collection (unit tested on the JVM)
  data/        AmiiboAPI client, disk-cached catalog repository, collection storage
  ui/          ViewModels, navigation, theme
    screens/   Browse, detail, collection
    components/ Cards, status buttons, filter sheet
```

Like the iOS app, it caches the catalog on disk, so it opens instantly and works offline. It refreshes once a day or on pull to refresh. Each bottom tab keeps its own back stack.

## CI

GitHub Actions runs on every pull request:

- **Web:** lint, typecheck, unit tests, and a production build
- **iOS:** Swift tests and a simulator build on macOS
- **Android:** unit tests, lint, and a debug build. The debug APK is uploaded as a build artifact, so it can be installed on any Android phone.
- **Android smoke test:** boots an emulator, installs the app, and walks through browse, detail, collection, and dark mode against the live API, saving screenshots along the way
