# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`, package `com.example.pokemontcg`) for keeping a personal Pokémon TCG "chase list" (wishlist). Card data comes from the open-source dataset https://github.com/PokemonTCG/pokemon-tcg-data, synced into a local Room catalog; search and details work offline. (The live Pokémon TCG API at api.pokemontcg.io is deprecated, shuts down for keys on 2027-03-01 and was failing most requests, so the app no longer uses it. Its paid successor is Scrydex.) Kotlin, Jetpack Compose + Material 3, Retrofit/Gson, Coil, Coroutines/StateFlow, KSP. The README is written in Dutch.

## Commands

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # build + install on a connected device/emulator
./run_app.sh                   # boot the `Pixel_9` AVD if needed (NVIDIA PRIME offload auto-detected), install, launch
~/Android/Sdk/platform-tools/adb emu kill   # stop the emulator
./gradlew lint                 # Android lint
./gradlew testDebugUnitTest    # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.example.pokemontcg.SomeTest.someMethod"   # single test
./gradlew connectedDebugAndroidTest   # instrumented tests (app/src/androidTest); NOTE: uninstalls the app afterwards, wiping its data
```

To run instrumented tests without losing app data, install both APKs (`assembleDebug assembleDebugAndroidTest`, then `adb install -r` each) and run `adb shell am instrument -w -e class <TestClass> com.example.pokemontcg.test/androidx.test.runner.AndroidJUnitRunner`. If a physical phone is also connected, pass `-s emulator-5554` to adb (`run_app.sh` hangs with multiple devices). The Android SDK is expected at `~/Android/Sdk`. If every download fails on the emulator with a connection error, check the emulator clock (`adb shell date`): a stale snapshot breaks TLS; cold boot with `-no-snapshot-load`.

Toolchain: AGP 9.3 with built-in Kotlin (no `kotlin-android` plugin; Kotlin options go in the top-level `kotlin { compilerOptions }` block), Gradle 9.8, KSP2, compile/target SDK 37, Compose BOM 2026.09. Dependencies are declared inline in `app/build.gradle.kts` (no version catalog). The build and `./gradlew lintDebug` are warning-free; keep them that way. The `kotlinx-serialization-core` constraint in `app/build.gradle.kts` is needed for `MigrationTest` (see the comment there).

## Architecture

MVVM with a single repository and manual dependency injection:

- Manual DI: `PokemonTcgApplication` lazily creates an `AppContainer` that builds Retrofit `CatalogApi` → Room `PokemonDatabase` (DAOs) → `CatalogSync` → `PokemonRepository` → the ViewModel factory (`ui/ViewModelFactory.kt`) once per process. `MainActivity` takes the factory from the container; it is passed down through `navigation/AppNavigation.kt` into every screen, which obtains its ViewModel with `viewModel(factory = factory)`. Adding a ViewModel means adding an `initializer` in `createViewModelFactory`.
- Catalog sync (`data/catalog/CatalogSync.kt`) starts in `Application.onCreate`: one GitHub API call (`git/trees`, unauthenticated limit 60/hour per IP) lists every dataset file with its SHA; `sets/en.json` and each changed `cards/en/<setId>.json` are downloaded from raw.githubusercontent.com (newest sets first, 4 in parallel) and written per set in a transaction together with the file SHA in `catalog_files`, so an interrupted sync resumes and an unchanged dataset costs one request. Progress is exposed as `SyncState`. Card ids match the old API's ids (e.g. `sv3-125`).
- `PokemonRepository` is the only data access point. Search runs locally via `CardSearchQuery` through a `@RawQuery`: every word must match the card name (substring), the start of a word in the set name, the set code (`ptcgoCode`, e.g. `OBF`) or the card number, so `charizard 151` / `obf 125` work. Ranking: exact name, every word in the card name, name prefix, newest set, card number (numeric first). `SearchFilters` (set id, `CardType`, rarity) narrow results: Pokémon types match the delimited `types` column (",Fire,"), Trainer/Energy match `supertype`. With no words, filters alone list matches (newest set, then number; up to 1000, word searches up to 100; the UI says when results are capped). The Search header has a horizontally scrolling row of `PickerChip`s (`ui/components/FilterPickers.kt`; don't use `weight` inside them) opening `SetPickerSheet` or `OptionPickerSheet`; choices live in `SearchViewModel`'s `SavedStateHandle`. `CardDataCleanup` unifies rarity spellings and formats types during sync. Details come from the catalog, falling back to the saved chase-list row. Download failures are mapped to messages in `ui/ErrorMessages.kt`.
- Prices (`data/prices`): Cardmarket prices in EUR from TCGdex (https://api.tcgdex.net/v2/en, free, no key, asks clients to cache). TCGdex ids differ from ours (`sv03-125` vs `sv3-125`): `TcgdexMatching` maps sets by alias, then normalized name, then identical id, and cards by number ignoring zero padding; add new mismatches to its alias maps. `PriceService` caches rows in `card_prices` for 24 hours (a row with a null price means "no price on Cardmarket"; failures keep the old row) and only fetches for chase-list cards (on Home) and cards opened in Details, never for search results. `formatEuro` formats amounts in the user's locale.
- Card shapes: `CatalogCardEntity`/`CatalogCardWithSet` (catalog); the user's cards as snapshots in two tables, `ChaseCardEntity` (`chase_cards`, wanted) and `CollectionCardEntity` (`collection_cards`, owned); a card is in at most one. `CollectionDao.moveToCollection`/`moveToChaseList` move cards between them in one transaction ("catching"). `TrackedCard` (owned or not) is the combined view the Home screen currently uses; `data/model/Card` is the UI model for catalog/details.
- Each screen (`ui/home`, `ui/search`, `ui/details`) has a sealed `*UiState` class and exposes `StateFlow`s collected with `collectAsStateWithLifecycle()`. Search combines the debounced query with the catalog card count, so results refresh while the first sync is still filling the catalog.
- Navigation routes: `home` (start) → `search?setId={setId}` → `details/{cardId}`, defined in `Routes`. The optional `setId` (from the set link on Details) lands in `SearchViewModel`'s `SavedStateHandle` (created via `createSavedStateHandle()` in the ViewModel factory), which also holds later set choices.
- Set filtering: `ui/components/SetPicker.kt` holds `SetChip` and `SetPickerSheet`, shared by Search (catalog sets, keyed by set id) and Home (sets on the chase list). `chase_cards` stores set names, not ids, so Home filters by set name (names are unique in the dataset); `buildHomeState` narrows cards and the progress counts to the chosen set, then orders them by `ChaseSort` (newest, price both ways with unpriced cards last, name, set + `CardNumberOrder`). The chosen sort persists via `data/preferences/UserPreferences` (SharedPreferences), passed to `HomeViewModel` through the factory.
- Room schemas are exported to `app/schemas/` (commit them). Version 2 added the catalog tables via `AutoMigration(1, 2)`; version 6 split the collection out of the chase list (manual `MIGRATION_5_6` in `Migrations.kt`: caught cards move to `collection_cards`, `chase_cards` is rebuilt without `obtained`); version 5 added `catalog_cards.supertype`/`types` and clears all card-file SHAs so every set is re-downloaded once; version 4 added the `card_prices` cache; version 3 added `card_sets.ptcgoCode`, with an `AutoMigrationSpec` that clears the sets file's SHA so the next sync refetches it (use the same trick whenever a new column must be backfilled from the dataset). Any entity change requires a version bump plus a migration (steps in `data/database/Migrations.kt`); `MigrationTest` validates migrations against the exported schemas, and `CatalogDaoTest` checks search behaviour on real SQLite.
- Catching is done from Home (ball button on a tile) and Details (switch, only for tracked cards). Home filtering and the progress counts live in the pure function `buildHomeState` (unit-tested).

## Visual design

Pokémon-branded look, fixed brand colors (no dynamic color) defined in `ui/theme/Color.kt`: Poké red header, ink navy band/text, yellow (`CaughtYellow`) reserved for "caught" state (progress fill, card borders, switch). Fonts are bundled in `res/font` (Lilita One for display/titles, Nunito for everything else; both OFL). User-facing copy says "caught" for moving a chase-list card into the collection.

- Every screen's top bar is `ui/components/PokedexHeader`: red header drawn behind the status bar, ending in the ball's band. On Home the band is the progress bar and the ball's button slides to the caught fraction.
- Cards are shown with `CardTile`/`CardImage` at the real card aspect ratio (63:88) in an adaptive `LazyVerticalGrid`.
- Edge-to-edge: `MainActivity` calls `enableEdgeToEdge` with light status bar icons (always over red). Scaffold `innerPadding` goes into grid `contentPadding`; Search uses `contentWindowInsets = WindowInsets.safeDrawing` for the keyboard.
- The window background in `res/values(-night)/themes.xml` matches the Compose background to avoid a flash on launch.
