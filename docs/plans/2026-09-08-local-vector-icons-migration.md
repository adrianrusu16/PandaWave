# Local Vector Icons Migration Implementation Plan — Corrected

**Implementation record:** See [migration results](../performance/local-vector-icons-migration.md)
for changes, verification, visual parity, measured APK savings, and timing limitations.
The checklists below retain the repeatable implementation procedure.

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Supersedes:** `docs/superpowers/plans/2026-09-07-local-vector-icons-migration.md`

**Goal:** Remove PandaWave's Compose Material Icons dependency and replace the current 22 semantic icon entries with 21 local Android `VectorDrawable` assets, preserving current legacy Material Icon geometry and behavior while reducing build dependency/dex-processing work.

**Architecture:** `:core:designsystem` owns the local vector assets and the semantic `PandaIcon`/`PandaWaveIcons` API. `:core:ui` and feature modules consume `PandaIcon` and render it with `painterResource`; no UI contract exposes Material `ImageVector`. A Gradle verification task checks Kotlin imports, direct Gradle dependency declarations, and the version catalog without scanning its own checker source for forbidden strings.

**Tech Stack:** Kotlin 2.3.21, Jetpack Compose BOM 2026.08.00, Material 3, Android VectorDrawable XML, Gradle Kotlin DSL, JUnit 6, PowerShell 7+, Git.

**Spec:** Approved local-vector design plus both review rounds. This corrected copy lives with the implementation; the Downloads document is retained as the supplied reference.

## Global Constraints

- Preserve the existing `PandaWaveIcons` semantic property names.
- Use local Android `VectorDrawable` XML as the committed runtime representation.
- Do not commit SVG runtime assets, an icon font, or copied Compose `ImageVector` Kotlin geometry.
- Preserve the **legacy Material Icons Outlined** shapes for visual parity; do not silently replace them with Material Symbols.
- Pin vendored source to `google/material-design-icons` commit `0cbb08816df07faaae3dca060d4ebb10b66c214f`.
- Keep 22 semantic catalogue entries backed by 21 distinct migration assets; `Library` and `MusicLibrary` share `pandawave_ic_library_music.xml`.
- Preserve automatic RTL mirroring only for `Queue`, `VolumeDown`, and `VolumeUp`.
- Do not change icon dimensions, labels, content descriptions, tint colors, focus behavior, test tags, click behavior, playback behavior, or navigation behavior.
- Preserve the existing PandaWave branding resources (`pandawave_ic_launcher_foreground`, `pandawave_ic_launcher_monochrome`, `pandawave_ic_logo`, `pandawave_ic_panda_paw`, and `pandawave_ic_splash_mark`) byte-for-byte unless a separate change explicitly requires otherwise.
- Remove `androidx.compose.material:material-icons-extended` from all **six current modules** that declare it:
  - `:core:ui`
  - `:feature:appshell`
  - `:feature:home`
  - `:feature:library`
  - `:feature:nowplaying`
  - `:feature:search`
- Remove the `androidx-compose-material-icons-extended` version-catalog alias.
- Do not introduce `androidx.compose.material:material-icons-core` as a replacement.
- The final repository must contain no Kotlin import from `androidx.compose.material.icons`.
- The existing `qualityCheck` task must run the Material Icons boundary check in CI.
- Performance claims are limited to **build dependency reduction**. Local XML vectors still require normal Android resource loading/parsing through `painterResource`; this migration does not claim faster runtime icon rendering.
- Dex merging will continue for the app's other dependencies. The expected result is removal of Material Icons' contribution to dependency processing/dexing, not removal of the dex pipeline itself.
- Benchmark before/after under the same machine/JDK/Gradle/cache conditions, preserve reports outside `build/`, and record multiple runs plus a normal incremental-edit scenario.
- Do not combine this work with unrelated UI redesign, Gradle optimization, CI restructuring, dependency cleanup, or runtime performance work.

---

## Approved Design Contract

### Target dependency direction

```text
core:designsystem
    ├── src/main/res/drawable/pandawave_ic_<migration-icon>.xml
    ├── PandaIcon
    └── PandaWaveIcons
             ↑
          core:ui
             ↑
          features
```

`core:designsystem` owns icon identity and geometry.

`core:ui` owns reusable rendering components.

Feature modules may select semantic `PandaWaveIcons`, but they must not know about `Icons.Outlined`, `ImageVector`, or raw `R.drawable` IDs for the migrated icon set.

### Semantic mapping

| Semantic property | Existing source | Local drawable | Auto-mirror |
|---|---|---|---:|
| `Home` | `Icons.Outlined.Home` | `pandawave_ic_home.xml` | No |
| `Library` | `Icons.Outlined.LibraryMusic` | `pandawave_ic_library_music.xml` | No |
| `MusicLibrary` | `Icons.Outlined.LibraryMusic` | `pandawave_ic_library_music.xml` | No |
| `NowPlaying` | `Icons.Outlined.PlayCircleOutline` | `pandawave_ic_play_circle_outline.xml` | No |
| `Search` | `Icons.Outlined.Search` | `pandawave_ic_search.xml` | No |
| `Settings` | `Icons.Outlined.Settings` | `pandawave_ic_settings.xml` | No |
| `Profile` | `Icons.Outlined.AccountCircle` | `pandawave_ic_account_circle.xml` | No |
| `Album` | `Icons.Outlined.Album` | `pandawave_ic_album.xml` | No |
| `Energy` | `Icons.Outlined.Bolt` | `pandawave_ic_bolt.xml` | No |
| `Nature` | `Icons.Outlined.Eco` | `pandawave_ic_eco.xml` | No |
| `Favorite` | `Icons.Outlined.FavoriteBorder` | `pandawave_ic_favorite_border.xml` | No |
| `Equalizer` | `Icons.Outlined.GraphicEq` | `pandawave_ic_graphic_eq.xml` | No |
| `Relax` | `Icons.Outlined.Spa` | `pandawave_ic_spa.xml` | No |
| `Microphone` | `Icons.Outlined.Mic` | `pandawave_ic_mic.xml` | No |
| `Pause` | `Icons.Outlined.Pause` | `pandawave_ic_pause.xml` | No |
| `Play` | `Icons.Outlined.PlayArrow` | `pandawave_ic_play_arrow.xml` | No |
| `Shuffle` | `Icons.Outlined.Shuffle` | `pandawave_ic_shuffle.xml` | No |
| `SkipNext` | `Icons.Outlined.SkipNext` | `pandawave_ic_skip_next.xml` | No |
| `SkipPrevious` | `Icons.Outlined.SkipPrevious` | `pandawave_ic_skip_previous.xml` | No |
| `Queue` | `Icons.AutoMirrored.Outlined.QueueMusic` | `pandawave_ic_queue_music.xml` | **Yes** |
| `VolumeDown` | `Icons.AutoMirrored.Outlined.VolumeDown` | `pandawave_ic_volume_down.xml` | **Yes** |
| `VolumeUp` | `Icons.AutoMirrored.Outlined.VolumeUp` | `pandawave_ic_volume_up.xml` | **Yes** |

### Target API

Create:

`core/designsystem/src/main/kotlin/com/adrianrusu/pandawave/core/designsystem/icons/PandaIcon.kt`

```kotlin
package com.adrianrusu.pandawave.core.designsystem.icons

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
value class PandaIcon internal constructor(
    @DrawableRes val resourceId: Int,
)
```

Create:

`core/designsystem/src/main/kotlin/com/adrianrusu/pandawave/core/designsystem/icons/PandaWaveIcons.kt`

```kotlin
package com.adrianrusu.pandawave.core.designsystem.icons

import com.adrianrusu.pandawave.core.designsystem.R

object PandaWaveIcons {
    val Home = PandaIcon(R.drawable.pandawave_ic_home)
    val Library = PandaIcon(R.drawable.pandawave_ic_library_music)
    val NowPlaying = PandaIcon(R.drawable.pandawave_ic_play_circle_outline)
    val Search = PandaIcon(R.drawable.pandawave_ic_search)
    val Settings = PandaIcon(R.drawable.pandawave_ic_settings)
    val Profile = PandaIcon(R.drawable.pandawave_ic_account_circle)

    val Album = PandaIcon(R.drawable.pandawave_ic_album)
    val Energy = PandaIcon(R.drawable.pandawave_ic_bolt)
    val Nature = PandaIcon(R.drawable.pandawave_ic_eco)
    val Favorite = PandaIcon(R.drawable.pandawave_ic_favorite_border)
    val Equalizer = PandaIcon(R.drawable.pandawave_ic_graphic_eq)
    val MusicLibrary = Library
    val Relax = PandaIcon(R.drawable.pandawave_ic_spa)

    val Microphone = PandaIcon(R.drawable.pandawave_ic_mic)
    val Pause = PandaIcon(R.drawable.pandawave_ic_pause)
    val Play = PandaIcon(R.drawable.pandawave_ic_play_arrow)
    val Shuffle = PandaIcon(R.drawable.pandawave_ic_shuffle)
    val SkipNext = PandaIcon(R.drawable.pandawave_ic_skip_next)
    val SkipPrevious = PandaIcon(R.drawable.pandawave_ic_skip_previous)
    val Queue = PandaIcon(R.drawable.pandawave_ic_queue_music)
    val VolumeDown = PandaIcon(R.drawable.pandawave_ic_volume_down)
    val VolumeUp = PandaIcon(R.drawable.pandawave_ic_volume_up)
}
```

The constructor is `internal`: consumers select from `PandaWaveIcons` rather than constructing arbitrary semantic icons. The `resourceId` remains readable by UI renderers so they can call `painterResource`.

---

# File Map

## Create

```text
core/designsystem/src/main/kotlin/com/adrianrusu/pandawave/core/designsystem/icons/PandaIcon.kt
core/designsystem/src/main/kotlin/com/adrianrusu/pandawave/core/designsystem/icons/PandaWaveIcons.kt

core/designsystem/src/main/res/drawable/pandawave_ic_home.xml
core/designsystem/src/main/res/drawable/pandawave_ic_library_music.xml
core/designsystem/src/main/res/drawable/pandawave_ic_play_circle_outline.xml
core/designsystem/src/main/res/drawable/pandawave_ic_search.xml
core/designsystem/src/main/res/drawable/pandawave_ic_settings.xml
core/designsystem/src/main/res/drawable/pandawave_ic_account_circle.xml
core/designsystem/src/main/res/drawable/pandawave_ic_album.xml
core/designsystem/src/main/res/drawable/pandawave_ic_bolt.xml
core/designsystem/src/main/res/drawable/pandawave_ic_eco.xml
core/designsystem/src/main/res/drawable/pandawave_ic_favorite_border.xml
core/designsystem/src/main/res/drawable/pandawave_ic_graphic_eq.xml
core/designsystem/src/main/res/drawable/pandawave_ic_spa.xml
core/designsystem/src/main/res/drawable/pandawave_ic_mic.xml
core/designsystem/src/main/res/drawable/pandawave_ic_pause.xml
core/designsystem/src/main/res/drawable/pandawave_ic_play_arrow.xml
core/designsystem/src/main/res/drawable/pandawave_ic_shuffle.xml
core/designsystem/src/main/res/drawable/pandawave_ic_skip_next.xml
core/designsystem/src/main/res/drawable/pandawave_ic_skip_previous.xml
core/designsystem/src/main/res/drawable/pandawave_ic_queue_music.xml
core/designsystem/src/main/res/drawable/pandawave_ic_volume_down.xml
core/designsystem/src/main/res/drawable/pandawave_ic_volume_up.xml

core/designsystem/src/test/kotlin/com/adrianrusu/pandawave/core/designsystem/icons/PandaWaveIconAssetsContractTest.kt

docs/third-party/material-design-icons.md
third_party/material-design-icons/LICENSE
```

## Modify — render/type migrations

```text
core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/navigation/BambooNavigationItem.kt
core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/miniplayer/BambooMiniPlayer.kt
core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/playback/BambooPlayPauseButton.kt
core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/discovery/BambooDiscoveryComponents.kt

feature/appshell/src/main/kotlin/com/adrianrusu/pandawave/appshell/presentation/AppShellScreen.kt
feature/nowplaying/src/main/kotlin/com/adrianrusu/pandawave/feature/nowplaying/NowPlayingRoute.kt
```

Task 1 must additionally list any current source file that imports the old catalogue. Those files receive a package-import migration. The concrete rendering contracts above are not optional: they currently use `imageVector` and/or accept `ImageVector`.

## Modify — dependency removal

```text
core/ui/build.gradle.kts
feature/appshell/build.gradle.kts
feature/home/build.gradle.kts
feature/library/build.gradle.kts
feature/nowplaying/build.gradle.kts
feature/search/build.gradle.kts
gradle/libs.versions.toml
build.gradle.kts
```

## Delete

```text
core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/icons/PandaWaveIcons.kt
```

No `.github/workflows/ci.yml` change is required for this migration because the existing Android fast validation already runs `qualityCheck`; the new guard is attached there.

---

# Task 1: Establish a Verified Baseline and Complete Inventory

**Files:** none committed.

**Interfaces:**
- Consumes: current branch before migration.
- Produces: complete dependency/source inventory and benchmark artifacts stored outside `build/`.

- [ ] **Step 1: Create an isolated worktree**

At implementation time use the `superpowers:using-git-worktrees` skill before editing.

- [ ] **Step 2: Confirm the worktree is clean**

PowerShell:

```powershell
git status --short

if ($LASTEXITCODE -ne 0) {
    throw "git status failed"
}
```

Expected: no tracked modifications.

- [ ] **Step 3: Discover only source roots that actually exist**

```powershell
$sourceRoots =
    @("app", "core", "feature", "provider", "rro") |
    Where-Object { Test-Path $_ }

$sourceRoots
```

Expected in the current repository: at least `app`, `core`, and `feature`.

All later PowerShell `rg` commands use `$sourceRoots`; they must not pass nonexistent directories.

- [ ] **Step 4: Inventory every icon reference and every `ImageVector` contract**

```powershell
& rg -n `
    'androidx\.compose\.material\.icons|PandaWaveIcons|ImageVector|material-icons' `
    @sourceRoots `
    gradle `
    --glob '!**/build/**'

if ($LASTEXITCODE -gt 1) {
    throw "ripgrep inventory failed with exit code $LASTEXITCODE"
}
```

Record the complete output before editing.

The inventory must explicitly account for these known renderer/type sites:

```text
core/ui/.../navigation/BambooNavigationItem.kt
core/ui/.../miniplayer/BambooMiniPlayer.kt
core/ui/.../playback/BambooPlayPauseButton.kt
core/ui/.../discovery/BambooDiscoveryComponents.kt
feature/appshell/.../AppShellScreen.kt
feature/nowplaying/.../NowPlayingRoute.kt
```

If additional `PandaWaveIcons`/`ImageVector` sites exist, append them to the implementation checklist before Task 3.

- [ ] **Step 5: Verify the six current direct dependency declarations**

```powershell
$expectedIconDependencyFiles = @(
    "core/ui/build.gradle.kts",
    "feature/appshell/build.gradle.kts",
    "feature/home/build.gradle.kts",
    "feature/library/build.gradle.kts",
    "feature/nowplaying/build.gradle.kts",
    "feature/search/build.gradle.kts"
)

foreach ($file in $expectedIconDependencyFiles) {
    $match = Select-String `
        -Path $file `
        -SimpleMatch 'implementation(libs.androidx.compose.material.icons.extended)'

    if (-not $match) {
        throw "Expected Material Icons declaration not found in $file"
    }
}
```

Expected: all six files match.

- [ ] **Step 6: Verify the resolved Debug dependency exists before migration**

```powershell
.\gradlew.bat :app:dependencyInsight `
    --configuration debugRuntimeClasspath `
    --dependency material-icons-extended `
    --console=plain
```

Expected: the report includes:

```text
androidx.compose.material:material-icons-extended
```

- [ ] **Step 7: Create a benchmark output directory outside `build/`**

```powershell
$benchmarkRoot =
    Join-Path $env:TEMP `
        ("pandawave-icon-migration-" + (Get-Date -Format "yyyyMMdd-HHmmss"))

$beforeRoot = Join-Path $benchmarkRoot "before"

New-Item -ItemType Directory -Force -Path $beforeRoot | Out-Null

$benchmarkRoot
```

Keep this path. Task 7 reuses the same root for `after`.

- [ ] **Step 8: Warm Gradle/dependency transforms once**

This benchmark intentionally uses a **warm dependency/transform cache**, because the migration is meant to improve normal developer/CI builds rather than simulate a first-ever machine setup.

```powershell
.\gradlew.bat :app:assembleDebug `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

- [ ] **Step 9: Measure three clean-output builds with Gradle build cache disabled**

`clean` removes project outputs but does **not** clear Gradle's dependency/transform cache. That is intentional and must be kept identical before/after.

```powershell
$cleanCsv = Join-Path $beforeRoot "clean-build-seconds.csv"
"run,seconds" | Set-Content $cleanCsv

1..3 | ForEach-Object {
    $run = $_

    $elapsed = Measure-Command {
        .\gradlew.bat clean :app:assembleDebug `
            '-PpandaEngine.buildNative=false' `
            --no-build-cache `
            --profile `
            --console=plain

        if ($LASTEXITCODE -ne 0) {
            throw "Clean benchmark build $run failed"
        }
    }

    "$run,$($elapsed.TotalSeconds)" | Add-Content $cleanCsv

    $profileDest = Join-Path $beforeRoot "clean-profile-$run"
    Copy-Item "build/reports/profile" $profileDest -Recurse
}
```

From each profile record:

```text
total Gradle execution time
:app:mergeExtDexDebug duration
:app:mergeDebugJavaResource duration
:core:ui Kotlin compilation duration
```

Do not claim a result from one run; use median/typical values across the three.

- [ ] **Step 10: Measure a normal incremental Kotlin edit**

This is separate from the controlled clean-output benchmark.

Use `BambooMiniPlayer.kt`, which exists before and after the migration:

```powershell
$incrementalTarget =
    "core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/miniplayer/BambooMiniPlayer.kt"

$backup = Join-Path $benchmarkRoot "BambooMiniPlayer.kt.baseline"
Copy-Item $incrementalTarget $backup

$incrementalCsv = Join-Path $beforeRoot "incremental-build-seconds.csv"
"run,seconds" | Set-Content $incrementalCsv

try {
    1..3 | ForEach-Object {
        $original = Get-Content -LiteralPath $backup -Raw
        if (([regex]::Matches($original, '\.fillMaxWidth\(\)')).Count -ne 1) {
            throw "Expected exactly one benchmark anchor in $incrementalTarget"
        }
        $fraction = @("0.991f", "0.992f", "0.993f")[$_ - 1]
        $edited = $original.Replace(".fillMaxWidth()", ".fillMaxWidth($fraction)")
        Set-Content -LiteralPath $incrementalTarget -Value $edited -Encoding utf8NoBOM -NoNewline

        $elapsed = Measure-Command {
            .\gradlew.bat :app:assembleDebug `
                '-PpandaEngine.buildNative=false' `
                --console=plain

            if ($LASTEXITCODE -ne 0) {
                throw "Incremental benchmark build $_ failed"
            }
        }

        "$_,$($elapsed.TotalSeconds)" | Add-Content $incrementalCsv
    }
}
finally {
    Copy-Item $backup $incrementalTarget -Force
}

git diff --exit-code -- $incrementalTarget

if ($LASTEXITCODE -ne 0) {
    throw "Incremental benchmark did not restore $incrementalTarget"
}
```

This benchmark uses normal cache settings and changes a function-body float literal, so bytecode actually changes. Each sample starts from its saved source; the exact original bytes are restored in `finally`. Do not substitute a comment-only edit.

- [ ] **Step 11: Run the existing verification baseline**

```powershell
.\gradlew.bat qualityCheck :app:lintDebug test `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

No commit for Task 1.

---

# Task 2: Vendor Only the 21 Migration Vectors and Add the Design-System API

**Files:**
- Create the 21 migration drawable files listed in **File Map**.
- Create `PandaIcon.kt`.
- Create `PandaWaveIcons.kt`.
- Create `PandaWaveIconAssetsContractTest.kt`.
- Create provenance/license files.

**Interfaces:**
- Consumes: pinned legacy Google Material Icons Android XML.
- Produces: local asset set plus `PandaIcon` and `PandaWaveIcons`.

- [ ] **Step 1: Write the failing asset contract test**

Create:

`core/designsystem/src/test/kotlin/com/adrianrusu/pandawave/core/designsystem/icons/PandaWaveIconAssetsContractTest.kt`

```kotlin
package com.adrianrusu.pandawave.core.designsystem.icons

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PandaWaveIconAssetsContractTest {
    private val rootDir = File(requireNotNull(System.getProperty("pandawave.rootDir")))
    private val drawableDir =
        File(rootDir, "core/designsystem/src/main/res/drawable")

    private val migrationAssets =
        setOf(
            "pandawave_ic_account_circle.xml",
            "pandawave_ic_album.xml",
            "pandawave_ic_bolt.xml",
            "pandawave_ic_eco.xml",
            "pandawave_ic_favorite_border.xml",
            "pandawave_ic_graphic_eq.xml",
            "pandawave_ic_home.xml",
            "pandawave_ic_library_music.xml",
            "pandawave_ic_mic.xml",
            "pandawave_ic_pause.xml",
            "pandawave_ic_play_arrow.xml",
            "pandawave_ic_play_circle_outline.xml",
            "pandawave_ic_queue_music.xml",
            "pandawave_ic_search.xml",
            "pandawave_ic_settings.xml",
            "pandawave_ic_shuffle.xml",
            "pandawave_ic_skip_next.xml",
            "pandawave_ic_skip_previous.xml",
            "pandawave_ic_spa.xml",
            "pandawave_ic_volume_down.xml",
            "pandawave_ic_volume_up.xml",
        )

    private val autoMirroredAssets =
        setOf(
            "pandawave_ic_queue_music.xml",
            "pandawave_ic_volume_down.xml",
            "pandawave_ic_volume_up.xml",
        )

    @Test
    fun `all migration assets exist and keep the 24dp vector contract`() {
        assertEquals(21, migrationAssets.size)

        migrationAssets.forEach { name ->
            val file = File(drawableDir, name)
            assertTrue(file.isFile, "Missing $name")

            val text = file.readText()

            assertTrue("<vector" in text, name)
            assertTrue("""android:width="24dp""" in text, name)
            assertTrue("""android:height="24dp""" in text, name)
            assertTrue("""android:viewportWidth="24""" in text, name)
            assertTrue("""android:viewportHeight="24""" in text, name)
            assertFalse("android:tint=" in text, name)
        }
    }

    @Test
    fun `only the three legacy auto mirrored migration assets are mirrored`() {
        migrationAssets.forEach { name ->
            val text = File(drawableDir, name).readText()
            val autoMirrored = """android:autoMirrored="true"""" in text

            assertEquals(name in autoMirroredAssets, autoMirrored, name)
        }
    }
}
```

Important: the test checks only the explicit 21-file migration set. It does **not** enumerate all `pandawave_ic_*` resources and therefore does not reject launcher/logo/paw/splash assets.

- [ ] **Step 2: Run the focused test and confirm red**

```powershell
.\gradlew.bat :core:designsystem:testDebugUnitTest `
    --tests '*PandaWaveIconAssetsContractTest' `
    --console=plain
```

Expected: FAIL because the migration files do not exist yet.

- [ ] **Step 3: Checkout the pinned upstream repository into a temporary directory**

```powershell
$upstreamRoot = Join-Path $env:TEMP ("pandawave-material-design-icons-" + [guid]::NewGuid().ToString("N"))

git clone `
    --filter=blob:none `
    --no-checkout `
    https://github.com/google/material-design-icons.git `
    $upstreamRoot

git -C $upstreamRoot checkout `
    0cbb08816df07faaae3dca060d4ebb10b66c214f

if ($LASTEXITCODE -ne 0) {
    throw "Failed to checkout pinned Material Icons source"
}
```

- [ ] **Step 4: Copy exactly the 21 legacy Outlined 24dp vectors**

```powershell
$iconNames = @(
    "home",
    "library_music",
    "play_circle_outline",
    "search",
    "settings",
    "account_circle",
    "album",
    "bolt",
    "eco",
    "favorite_border",
    "graphic_eq",
    "spa",
    "mic",
    "pause",
    "play_arrow",
    "shuffle",
    "skip_next",
    "skip_previous",
    "queue_music",
    "volume_down",
    "volume_up"
)

$destination =
    "core/designsystem/src/main/res/drawable"

$migrationFiles = @()

foreach ($iconName in $iconNames) {
    $fileName = "outline_${iconName}_24.xml"

    $candidates =
        @(
            Get-ChildItem `
                -Path (Join-Path $upstreamRoot "android") `
                -Recurse `
                -File `
                -Filter $fileName |
            Where-Object {
                $_.FullName -match
                    '[\\/]materialiconsoutlined[\\/]black[\\/]res[\\/]drawable[\\/]'
            }
        )

    if ($candidates.Count -ne 1) {
        throw "Expected exactly one legacy Outlined source for $iconName; found $($candidates.Count)"
    }

    $target =
        Join-Path $destination "pandawave_ic_${iconName}.xml"

    Copy-Item $candidates[0].FullName $target
    $migrationFiles += $target
}

if ($migrationFiles.Count -ne 21) {
    throw "Expected 21 copied migration files"
}
```

- [ ] **Step 5: Normalize only those 21 copied files**

The upstream vectors contain `android:tint="?attr/colorControlNormal"`. Remove that attribute only from `$migrationFiles`; Compose `Icon` supplies the runtime tint.

```powershell
foreach ($file in $migrationFiles) {
    $text = Get-Content $file -Raw

    $text =
        $text -replace
            '\s+android:tint="\?attr/colorControlNormal"',
            ''

    Set-Content `
        -Path $file `
        -Value $text `
        -Encoding utf8NoBOM
}
```

Do not use a glob such as `pandawave_ic_*.xml` for normalization.

- [ ] **Step 6: Add auto-mirroring only to the three migration resources**

```powershell
$autoMirroredFiles = @(
    "core/designsystem/src/main/res/drawable/pandawave_ic_queue_music.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_volume_down.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_volume_up.xml"
)

foreach ($file in $autoMirroredFiles) {
    $text = Get-Content $file -Raw

    if ($text -notmatch 'android:autoMirrored=') {
        $text =
            $text -replace
                '(<vector\b[^>]*android:viewportHeight="24")',
                ('$1' + "`r`n    android:autoMirrored=`"true`"")
    }

    Set-Content `
        -Path $file `
        -Value $text `
        -Encoding utf8NoBOM
}
```

The asset contract test is the final authority: exactly those three must contain `android:autoMirrored="true"`.

- [ ] **Step 7: Verify existing branding assets were not modified**

```powershell
$brandingAssets = @(
    "core/designsystem/src/main/res/drawable/pandawave_ic_launcher_foreground.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_launcher_monochrome.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_logo.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_panda_paw.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_splash_mark.xml"
)

git diff --exit-code -- $brandingAssets

if ($LASTEXITCODE -ne 0) {
    throw "Existing PandaWave branding resources changed during icon migration"
}
```

- [ ] **Step 8: Create `PandaIcon.kt`**

Use the exact target API shown under **Approved Design Contract**.

- [ ] **Step 9: Create `PandaWaveIcons.kt`**

Use the exact target API shown under **Approved Design Contract**.

- [ ] **Step 10: Preserve upstream licensing/provenance**

Create:

`docs/third-party/material-design-icons.md`

```markdown
# Google Material Icons subset

PandaWave vendors a small subset of the legacy Google Material Icons
Outlined Android vector resources instead of depending on Compose
`material-icons-extended`.

Source repository: `google/material-design-icons`

Pinned source commit:

`0cbb08816df07faaae3dca060d4ebb10b66c214f`

Source style: legacy Material Icons, Outlined, 24dp.

The vendored subset is stored under:

`core/designsystem/src/main/res/drawable/pandawave_ic_*.xml`

Only the explicitly documented migration icon set is sourced from Google;
PandaWave branding assets in the same drawable directory are unrelated.

The upstream Material Icons project is licensed under Apache License 2.0.
The corresponding upstream license is copied to:

`third_party/material-design-icons/LICENSE`
```

Copy the pinned upstream `LICENSE` file:

```powershell
New-Item `
    -ItemType Directory `
    -Force `
    -Path "third_party/material-design-icons" |
    Out-Null

Copy-Item `
    (Join-Path $upstreamRoot "LICENSE") `
    "third_party/material-design-icons/LICENSE"
```

- [ ] **Step 11: Run asset/API verification**

```powershell
.\gradlew.bat `
    :core:designsystem:testDebugUnitTest `
    :core:designsystem:assembleDebug `
    --console=plain
```

Expected: PASS.

- [ ] **Step 12: Commit**

```powershell
git add `
    core/designsystem/src/main/kotlin/com/adrianrusu/pandawave/core/designsystem/icons `
    core/designsystem/src/main/res/drawable `
    core/designsystem/src/test/kotlin/com/adrianrusu/pandawave/core/designsystem/icons `
    docs/third-party/material-design-icons.md `
    third_party/material-design-icons/LICENSE

git commit -m "feat(designsystem): vendor PandaWave vector icons"
```

---

# Task 3: Migrate All Shared `core:ui` Renderers from `ImageVector` to `PandaIcon`

**Files:**
- Modify `BambooNavigationItem.kt`.
- Modify `BambooMiniPlayer.kt`.
- Modify `BambooPlayPauseButton.kt`.
- Modify `BambooDiscoveryComponents.kt`.

**Interfaces:**
- Consumes: `PandaIcon` and `PandaWaveIcons` from `:core:designsystem`.
- Produces: reusable UI components that render local vectors through `painterResource`.

## 3A. `BambooNavigationItem.kt`

- [ ] **Step 1: Replace the model type**

Remove:

```kotlin
import androidx.compose.ui.graphics.vector.ImageVector
```

Add:

```kotlin
import androidx.compose.ui.res.painterResource
import com.adrianrusu.pandawave.core.designsystem.icons.PandaIcon
```

Change:

```kotlin
data class BambooNavigationItemModel(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val showLabel: Boolean,
    val enabled: Boolean = true,
)
```

to:

```kotlin
data class BambooNavigationItemModel(
    val id: String,
    val label: String,
    val icon: PandaIcon,
    val selected: Boolean,
    val showLabel: Boolean,
    val enabled: Boolean = true,
)
```

- [ ] **Step 2: Change navigation icon rendering**

Replace:

```kotlin
Icon(
    imageVector = model.icon,
    contentDescription = if (model.showLabel) null else model.label,
    tint = contentColor,
    modifier = Modifier.size(tokens.components.iconMedium),
)
```

with:

```kotlin
Icon(
    painter = painterResource(model.icon.resourceId),
    contentDescription = if (model.showLabel) null else model.label,
    tint = contentColor,
    modifier = Modifier.size(tokens.components.iconMedium),
)
```

## 3B. `BambooMiniPlayer.kt`

- [ ] **Step 3: Move the catalogue import and add painter loading**

Replace:

```kotlin
import com.adrianrusu.pandawave.core.ui.icons.PandaWaveIcons
```

with:

```kotlin
import androidx.compose.ui.res.painterResource
import com.adrianrusu.pandawave.core.designsystem.icons.PandaWaveIcons
```

- [ ] **Step 4: Convert previous/next controls**

Replace each:

```kotlin
Icon(
    imageVector = PandaWaveIcons.SkipPrevious,
    ...
)
```

and:

```kotlin
Icon(
    imageVector = PandaWaveIcons.SkipNext,
    ...
)
```

with:

```kotlin
Icon(
    painter = painterResource(PandaWaveIcons.SkipPrevious.resourceId),
    ...
)
```

and:

```kotlin
Icon(
    painter = painterResource(PandaWaveIcons.SkipNext.resourceId),
    ...
)
```

Do not change content descriptions or button modifiers.

## 3C. `BambooPlayPauseButton.kt`

- [ ] **Step 5: Move the catalogue import**

Replace:

```kotlin
import com.adrianrusu.pandawave.core.ui.icons.PandaWaveIcons
```

with:

```kotlin
import com.adrianrusu.pandawave.core.designsystem.icons.PandaWaveIcons
```

The file already imports `painterResource`.

- [ ] **Step 6: Convert only the Pause branch**

Replace:

```kotlin
Icon(
    imageVector = PandaWaveIcons.Pause,
    contentDescription = pauseContentDescription,
    modifier = Modifier.size(iconSize),
)
```

with:

```kotlin
Icon(
    painter = painterResource(PandaWaveIcons.Pause.resourceId),
    contentDescription = pauseContentDescription,
    modifier = Modifier.size(iconSize),
)
```

Leave the existing Play branch unchanged:

```kotlin
painterResource(id = R.drawable.pandawave_ic_panda_paw)
```

The Panda paw is a branding asset, not part of this migration.

## 3D. `BambooDiscoveryComponents.kt`

- [ ] **Step 7: Replace `ImageVector` imports/types**

Remove:

```kotlin
import androidx.compose.ui.graphics.vector.ImageVector
import com.adrianrusu.pandawave.core.ui.icons.PandaWaveIcons
```

Add:

```kotlin
import androidx.compose.ui.res.painterResource
import com.adrianrusu.pandawave.core.designsystem.icons.PandaIcon
import com.adrianrusu.pandawave.core.designsystem.icons.PandaWaveIcons
```

Change:

```kotlin
fun BambooCategoryCard(
    category: BambooCategoryItem,
    icon: ImageVector,
    ...
)
```

to:

```kotlin
fun BambooCategoryCard(
    category: BambooCategoryItem,
    icon: PandaIcon,
    ...
)
```

- [ ] **Step 8: Convert every icon renderer in the file**

Convert all of these categories:

```text
PandaWaveIcons.Play in BambooMediaHeroCard
PandaWaveIcons.Play in BambooMediaListRow
BambooCategoryCard(icon)
PandaWaveIcons.Search in BambooSearchBar
PandaWaveIcons.Microphone in BambooSearchBar
```

Pattern:

```kotlin
Icon(
    painter = painterResource(PandaWaveIcons.Play.resourceId),
    contentDescription = null,
)
```

For a parameter:

```kotlin
Icon(
    painter = painterResource(icon.resourceId),
    contentDescription = null,
)
```

Preserve every existing tint, size, padding, and content description.

- [ ] **Step 9: Confirm `core:ui` no longer uses `ImageVector` for PandaWave icons**

```powershell
& rg -n `
    'imageVector\s*=\s*(PandaWaveIcons|icon)|icon:\s*ImageVector|core\.ui\.icons\.PandaWaveIcons' `
    "core/ui/src/main/kotlin"

if ($LASTEXITCODE -eq 0) {
    throw "core:ui still contains migrated ImageVector/PandaWaveIcons usage"
}

if ($LASTEXITCODE -gt 1) {
    throw "ripgrep failed"
}
```

This command is scoped to migrated icon patterns; unrelated `ImageVector` use is not automatically forbidden if it exists for another purpose.

- [ ] **Step 10: Compile/test `core:ui` while the dependency still exists temporarily**

```powershell
.\gradlew.bat `
    :core:ui:compileDebugKotlin `
    :core:ui:testDebugUnitTest `
    --console=plain
```

Expected: PASS.

- [ ] **Step 11: Commit**

```powershell
# Keep these changes together with Task 4; commit after all callers compile.
```

---

# Task 4: Migrate Feature/AppShell Icon Types and Renderers

**Files:**
- Modify `AppShellScreen.kt`.
- Modify `NowPlayingRoute.kt`.
- Modify every additional old-catalogue import found in Task 1.

**Interfaces:**
- Consumes: `PandaIcon`/`PandaWaveIcons`.
- Produces: feature code with no dependency on the old `core.ui.icons` package and no PandaWave `ImageVector` rendering.

## 4A. `AppShellScreen.kt`

- [ ] **Step 1: Change destination icon type**

Remove:

```kotlin
import androidx.compose.ui.graphics.vector.ImageVector
import com.adrianrusu.pandawave.core.ui.icons.PandaWaveIcons
```

Add:

```kotlin
import com.adrianrusu.pandawave.core.designsystem.icons.PandaIcon
import com.adrianrusu.pandawave.core.designsystem.icons.PandaWaveIcons
```

Change:

```kotlin
private val PandaWaveDestination.icon: ImageVector
```

to:

```kotlin
private val PandaWaveDestination.icon: PandaIcon
```

Keep the existing `when` mapping unchanged.

`BambooNavigationItem` performs the actual rendering after Task 3.

## 4B. `NowPlayingRoute.kt`

- [ ] **Step 2: Replace imports**

Remove:

```kotlin
import androidx.compose.ui.graphics.vector.ImageVector
import com.adrianrusu.pandawave.core.ui.icons.PandaWaveIcons
```

Add:

```kotlin
import androidx.compose.ui.res.painterResource
import com.adrianrusu.pandawave.core.designsystem.icons.PandaIcon
import com.adrianrusu.pandawave.core.designsystem.icons.PandaWaveIcons
```

- [ ] **Step 3: Change all icon-accepting helper signatures**

Change:

```kotlin
private fun TransportRoundAction(
    icon: ImageVector,
    ...
)
```

to:

```kotlin
private fun TransportRoundAction(
    icon: PandaIcon,
    ...
)
```

Change:

```kotlin
private fun SecondaryRoundAction(
    icon: ImageVector,
    ...
)
```

to:

```kotlin
private fun SecondaryRoundAction(
    icon: PandaIcon,
    ...
)
```

Change:

```kotlin
private fun QuickActionButton(
    icon: ImageVector,
    ...
)
```

to:

```kotlin
private fun QuickActionButton(
    icon: PandaIcon,
    ...
)
```

- [ ] **Step 4: Convert helper renderers**

Within those helpers replace:

```kotlin
Icon(
    imageVector = icon,
    ...
)
```

with:

```kotlin
Icon(
    painter = painterResource(icon.resourceId),
    ...
)
```

- [ ] **Step 5: Convert direct PandaWave icon renderers**

Convert every direct form:

```kotlin
imageVector = PandaWaveIcons.<Name>
```

to:

```kotlin
painter = painterResource(PandaWaveIcons.<Name>.resourceId)
```

Known direct usages include the decorative `Nature` icon and `VolumeDown`/`VolumeUp`; use `rg` to catch the complete file rather than relying on this list:

```powershell
& rg -n 'imageVector\s*=' `
    "feature/nowplaying/src/main/kotlin/com/adrianrusu/pandawave/feature/nowplaying/NowPlayingRoute.kt"

if ($LASTEXITCODE -gt 1) {
    throw "ripgrep failed"
}
```

After migration, no `imageVector =` usage associated with the migrated PandaWave controls should remain.

## 4C. Remaining old-catalogue imports

- [ ] **Step 6: Replace every remaining old package import**

Discover dynamically:

```powershell
$sourceRoots =
    @("app", "core", "feature", "provider", "rro") |
    Where-Object { Test-Path $_ }

& rg -l `
    'com\.adrianrusu\.pandawave\.core\.ui\.icons\.PandaWaveIcons' `
    @sourceRoots `
    --glob '!**/build/**'

if ($LASTEXITCODE -gt 1) {
    throw "ripgrep failed"
}
```

For every returned file replace:

```kotlin
import com.adrianrusu.pandawave.core.ui.icons.PandaWaveIcons
```

with:

```kotlin
import com.adrianrusu.pandawave.core.designsystem.icons.PandaWaveIcons
```

If any returned file passes a `PandaWaveIcon` into an `ImageVector` parameter, migrate that parameter/rendering to `PandaIcon` + `painterResource` before continuing. Do not stop at the import change.

- [ ] **Step 7: Compile the application source graph**

```powershell
.\gradlew.bat :app:compileDebugKotlin `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

- [ ] **Step 8: Assert the old catalogue package is no longer referenced**

```powershell
$sourceRoots =
    @("app", "core", "feature", "provider", "rro") |
    Where-Object { Test-Path $_ }

& rg -n `
    'com\.adrianrusu\.pandawave\.core\.ui\.icons\.PandaWaveIcons' `
    @sourceRoots `
    --glob '!**/build/**'

if ($LASTEXITCODE -eq 0) {
    throw "Old PandaWaveIcons package still referenced"
}

if ($LASTEXITCODE -gt 1) {
    throw "ripgrep failed"
}
```

- [ ] **Step 9: Commit**

```powershell
git add feature app core
git commit -m "refactor: migrate PandaWave icon contracts and renderers"
```

---

# Task 5: Add the Gradle Guard, Then Remove Material Icons from All Six Modules

**Files:**
- Modify `build.gradle.kts`.
- Modify six module `build.gradle.kts` files.
- Modify `gradle/libs.versions.toml`.
- Delete old catalogue file.

**Interfaces:**
- Consumes: completed `PandaIcon` migration.
- Produces: dependency-free Material Icons state plus a self-safe CI guard.

- [ ] **Step 1: Add `verifyNoComposeMaterialIcons` using dependency declarations, source imports, and version-catalog content**

Register the typed task through the existing build-logic convention plugin. Root `qualityCheck` depends on its task name:

Implement the guard in the existing Groovy build-logic convention plugin:

- `build-logic/src/main/groovy/com/adrianrusu/pandawave/buildlogic/VerifyNoComposeMaterialIconsTask.groovy`: typed task with `@InputFiles` Kotlin source files, `@Input` lists of configured dependency and catalog violations, and an explicit root-directory property for diagnostics. Its action reads only those properties; no `Project`, `subprojects`, `Configuration`, or script instance is accessed during execution.
- `PandaWaveUiContractPlugin.groovy`: register `verifyNoComposeMaterialIcons`, scope Kotlin sources to each included project's `src` directory, and snapshot configured dependency coordinates and parsed `VersionCatalogsExtension` library coordinates during configuration. Capture final String values, never Gradle model objects, in task inputs.
- Check both `androidx.compose.material:material-icons-core` and `androidx.compose.material:material-icons-extended`.
- Inspect parsed catalogs rather than regex-matching TOML: reject unused aliases in string, `module`, and `group`/`name` forms, irrespective of field order.
- Detect actual Kotlin import lines, so prose comments, checker code and unrelated cache/worktree copies do not cause false positives.

Test the real convention plugin through Gradle TestKit in `VerifyNoComposeMaterialIconsPluginTest.groovy`. Cover forbidden imports, direct dependencies, unused aliases in every notation, safe catalogs and excluded nested checkouts. Run twice with `--configuration-cache`, checking both initial storage and reuse. Keep caching enabled.

```powershell
.\gradlew.bat -p build-logic test --console=plain
```

The production task implementation is the executable reference; do not restore the earlier `doLast` project-inspection snippet.

This guard does **not** scan `build.gradle.kts` text for its own forbidden strings. The build script may contain the coordinates as checker constants without self-failing.

It catches:

```text
material-icons-extended direct module declarations
material-icons-core direct module declarations
Kotlin androidx.compose.material.icons imports
version-catalog declarations for core or extended
```

- [ ] **Step 2: Wire the guard into `qualityCheck`**

Change:

```kotlin
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs Kotlin formatting and static analysis checks."
    dependsOn(
        "spotlessCheck",
        "detekt",
        "verifyPandaWaveIdentity",
        "verifyPandaWaveUiContract",
    )
}
```

to:

```kotlin
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs Kotlin formatting and static analysis checks."
    dependsOn(
        "spotlessCheck",
        "detekt",
        "verifyPandaWaveIdentity",
        "verifyPandaWaveUiContract",
        "verifyNoComposeMaterialIcons",
    )
}
```

- [ ] **Step 3: Run the guard and confirm it is red before dependency deletion**

```powershell
.\gradlew.bat verifyNoComposeMaterialIcons --console=plain
```

Expected: FAIL and identify direct dependencies/version-catalog entries still present.

- [ ] **Step 4: Delete the legacy catalogue**

```powershell
git rm `
    "core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/icons/PandaWaveIcons.kt"
```

- [ ] **Step 5: Remove the dependency from all six current module declarations**

Delete this line:

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

from exactly:

```text
core/ui/build.gradle.kts
feature/appshell/build.gradle.kts
feature/home/build.gradle.kts
feature/library/build.gradle.kts
feature/nowplaying/build.gradle.kts
feature/search/build.gradle.kts
```

- [ ] **Step 6: Remove the version-catalog alias**

Delete from `gradle/libs.versions.toml`:

```toml
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
```

Do not add a `material-icons-core` alias.

- [ ] **Step 7: Run the Gradle guard and confirm green**

```powershell
.\gradlew.bat verifyNoComposeMaterialIcons --console=plain
```

Expected: PASS.

- [ ] **Step 8: Verify Kotlin imports with a Windows-safe dynamic root scan**

```powershell
$sourceRoots =
    @("app", "core", "feature", "provider", "rro") |
    Where-Object { Test-Path $_ }

& rg -n `
    'androidx\.compose\.material\.icons' `
    @sourceRoots `
    --glob '!**/build/**'

if ($LASTEXITCODE -eq 0) {
    throw "Material Icons Kotlin import remains"
}

if ($LASTEXITCODE -gt 1) {
    throw "ripgrep failed"
}
```

Expected: no matches, exit code `1`.

Do **not** include root `build.gradle.kts` in this raw text scan; the checker intentionally contains forbidden coordinate strings.

- [ ] **Step 9: Verify the version catalog directly**

```powershell
$catalogMatches =
    Select-String `
        -Path "gradle/libs.versions.toml" `
        -Pattern 'material-icons-(core|extended)'

if ($catalogMatches) {
    $catalogMatches
    throw "Material Icons version-catalog entry remains"
}
```

Expected: no matches.

- [ ] **Step 10: Verify resolved Debug runtime coordinates**

```powershell
$dependencyReport =
    .\gradlew.bat :app:dependencies `
        --configuration debugRuntimeClasspath `
        --console=plain |
    Out-String

if ($LASTEXITCODE -ne 0) {
    throw "Gradle dependency inspection failed: $LASTEXITCODE"
}
if ($dependencyReport -notmatch '(?m)^debugRuntimeClasspath\b' -or $dependencyReport -match '\bFAILED\b') {
    throw "Missing or unresolved Debug dependency report"
}

if (
    $dependencyReport -match
        'androidx\.compose\.material:material-icons-(core|extended)'
) {
    throw "Material Icons still resolved in debugRuntimeClasspath"
}
```

Expected: no matching resolved coordinate.

- [ ] **Step 11: Run compile/test/quality checks**

```powershell
.\gradlew.bat `
    qualityCheck `
    :app:lintDebug `
    test `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

- [ ] **Step 12: Commit the guard + dependency removal**

```powershell
git add `
    build.gradle.kts `
    gradle/libs.versions.toml `
    core/ui/build.gradle.kts `
    feature/appshell/build.gradle.kts `
    feature/home/build.gradle.kts `
    feature/library/build.gradle.kts `
    feature/nowplaying/build.gradle.kts `
    feature/search/build.gradle.kts `
    core/ui/src/main/kotlin

git commit -m "perf: remove Compose Material Icons dependency"
```

---

# Task 6: Verify Visual, Semantic, and RTL Parity

**Files:** no production files unless parity verification finds a mismatch.

**Interfaces:**
- Consumes: completed migration.
- Produces: evidence that this is dependency replacement, not redesign.

- [ ] **Step 1: Run available connected UI tests**

With a configured emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

If the full suite is intentionally unavailable locally, run the repository's existing AppShell/navigation/MediaBrowser connected subset rather than inventing new test names.

- [ ] **Step 2: Verify LTR surfaces manually or with existing screenshot tooling**

Inspect at minimum:

```text
Navigation:
- Home
- Library
- Search
- Profile
- Now Playing/settings destinations when shown

Mini-player:
- Skip Previous
- Pause state
- Skip Next
- existing Panda paw Play state unchanged

Discovery/search:
- Play action
- Search
- Microphone
- Category icons

Now Playing:
- Nature decoration
- transport actions
- secondary actions
- quick actions
- Queue
- VolumeDown
- VolumeUp
```

Acceptance criteria:

```text
same legacy shape/silhouette
same rendered size
same color/tint
same selected/unselected state
same content description
same focus/click behavior
no clipping
no accidental theme tint
```

- [ ] **Step 3: Verify RTL parity**

Use an RTL locale or Android developer force-RTL.

Acceptance criteria:

```text
Queue mirrors exactly as the previous AutoMirrored icon did
VolumeDown mirrors exactly as before
VolumeUp mirrors exactly as before

SkipNext retains its previous non-AutoMirrored behavior
SkipPrevious retains its previous non-AutoMirrored behavior
```

This is parity with current code, not a new RTL-design policy.

- [ ] **Step 4: If an icon differs, fix the local legacy vector only**

Do not switch to Material Symbols to hide a mismatch.

Compare the local asset with the pinned legacy source and correct the copied vector/normalization.

If a parity fix is needed:

```powershell
git add core/designsystem/src/main/res/drawable
git commit -m "fix(designsystem): preserve legacy icon parity"
```

---

# Task 7: Repeat the Controlled Benchmark and Final Verification

**Files:** none required.

**Interfaces:**
- Consumes: final migration and `$benchmarkRoot` created in Task 1.
- Produces: before/after evidence under identical cache conditions and a merge-ready branch.

- [ ] **Step 1: Create the `after` benchmark directory**

```powershell
$afterRoot = Join-Path $benchmarkRoot "after"

New-Item -ItemType Directory -Force -Path $afterRoot |
    Out-Null
```

If the shell session from Task 1 was closed, set `$benchmarkRoot` to the printed path retained from Task 1.

- [ ] **Step 2: Warm the post-migration dependency/transform state once**

```powershell
.\gradlew.bat :app:assembleDebug `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

- [ ] **Step 3: Repeat the same three clean-output builds**

```powershell
$cleanCsv = Join-Path $afterRoot "clean-build-seconds.csv"
"run,seconds" | Set-Content $cleanCsv

1..3 | ForEach-Object {
    $run = $_

    $elapsed = Measure-Command {
        .\gradlew.bat clean :app:assembleDebug `
            '-PpandaEngine.buildNative=false' `
            --no-build-cache `
            --profile `
            --console=plain

        if ($LASTEXITCODE -ne 0) {
            throw "Post-migration clean benchmark build $run failed"
        }
    }

    "$run,$($elapsed.TotalSeconds)" | Add-Content $cleanCsv

    $profileDest = Join-Path $afterRoot "clean-profile-$run"
    Copy-Item "build/reports/profile" $profileDest -Recurse
}
```

Compare the median/typical values with Task 1.

Primary metrics:

```text
clean build wall-clock time
:app:mergeExtDexDebug duration
```

Secondary metrics:

```text
:app:mergeDebugJavaResource
:core:ui Kotlin compile duration
```

- [ ] **Step 4: Repeat the same incremental Kotlin-edit benchmark**

```powershell
$incrementalTarget =
    "core/ui/src/main/kotlin/com/adrianrusu/pandawave/core/ui/miniplayer/BambooMiniPlayer.kt"

$backup = Join-Path $benchmarkRoot "BambooMiniPlayer.kt.after"
Copy-Item $incrementalTarget $backup

$incrementalCsv = Join-Path $afterRoot "incremental-build-seconds.csv"
"run,seconds" | Set-Content $incrementalCsv

try {
    1..3 | ForEach-Object {
        $original = Get-Content -LiteralPath $backup -Raw
        if (([regex]::Matches($original, '\.fillMaxWidth\(\)')).Count -ne 1) {
            throw "Expected exactly one benchmark anchor in $incrementalTarget"
        }
        $fraction = @("0.991f", "0.992f", "0.993f")[$_ - 1]
        $edited = $original.Replace(".fillMaxWidth()", ".fillMaxWidth($fraction)")
        Set-Content -LiteralPath $incrementalTarget -Value $edited -Encoding utf8NoBOM -NoNewline

        $elapsed = Measure-Command {
            .\gradlew.bat :app:assembleDebug `
                '-PpandaEngine.buildNative=false' `
                --console=plain

            if ($LASTEXITCODE -ne 0) {
                throw "Post-migration incremental benchmark $_ failed"
            }
        }

        "$_,$($elapsed.TotalSeconds)" | Add-Content $incrementalCsv
    }
}
finally {
    Copy-Item $backup $incrementalTarget -Force
}

git diff --exit-code -- $incrementalTarget

if ($LASTEXITCODE -ne 0) {
    throw "Post-migration benchmark did not restore $incrementalTarget"
}
```

Do not expect the incremental build to improve by the same amount as a clean/dex-heavy build; external dependency transforms may already be cached/up-to-date in the normal edit loop.

- [ ] **Step 5: Record results without overstating causality**

Create a local summary in `$benchmarkRoot`:

```powershell
@"
Migration: Compose Material Icons -> 21 local VectorDrawable assets

Controlled conditions:
- same checkout machine
- same JDK
- same Gradle version
- same native-disabled property
- warm dependency/transform caches
- clean-output runs use --no-build-cache
- three clean-output samples before and after
- three normal incremental-edit samples before and after

Primary expected structural result:
- material-icons-core/extended absent from debugRuntimeClasspath

Performance interpretation:
- report measured before/after values
- do not claim XML vectors render faster
- do not claim dex merging disappears
"@ | Set-Content (Join-Path $benchmarkRoot "README.txt")
```

- [ ] **Step 6: Run final dependency/import guard**

```powershell
.\gradlew.bat verifyNoComposeMaterialIcons --console=plain
```

Expected: PASS.

- [ ] **Step 7: Run final quality/lint/test verification**

```powershell
.\gradlew.bat `
    qualityCheck `
    :app:lintRelease `
    test `
    '-PpandaEngine.buildNative=false' `
    '-Ppandawave.verificationAppLinkHost=ci.pandawave.dev' `
    --console=plain
```

Expected: PASS.

- [ ] **Step 8: Assemble Debug**

```powershell
.\gradlew.bat :app:assembleDebug `
    '-PpandaEngine.buildNative=false' `
    --console=plain
```

Expected: PASS.

- [ ] **Step 9: Inspect the final diff**

```powershell
git diff master...HEAD --stat
git diff master...HEAD
```

The diff should be limited to:

```text
21 local legacy Material vector resources
PandaIcon/PandaWaveIcons design-system API
core UI renderer/type migrations
feature/AppShell renderer/type/import migrations
six dependency removals
version-catalog alias removal
self-safe verification task
asset contract test
third-party attribution/license
```

No unrelated layout, typography, focus, playback, navigation, CI scheduling, or Gradle-memory change belongs in this branch.

- [ ] **Step 10: Confirm branding assets remain untouched relative to master**

```powershell
$brandingAssets = @(
    "core/designsystem/src/main/res/drawable/pandawave_ic_launcher_foreground.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_launcher_monochrome.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_logo.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_panda_paw.xml",
    "core/designsystem/src/main/res/drawable/pandawave_ic_splash_mark.xml"
)

git diff --exit-code master...HEAD -- $brandingAssets

if ($LASTEXITCODE -ne 0) {
    throw "Unrelated branding asset changed"
}
```

Expected: no diff.

- [ ] **Step 11: Confirm the worktree is clean**

```powershell
git status --short
```

Expected: no uncommitted changes.

---

# Expected Final State

## Build dependencies

These six module declarations are gone:

```text
:core:ui
:feature:appshell
:feature:home
:feature:library
:feature:nowplaying
:feature:search
```

The version-catalog alias is gone.

The Debug runtime classpath contains neither:

```text
androidx.compose.material:material-icons-extended
androidx.compose.material:material-icons-core
```

## Kotlin contracts

No source imports:

```kotlin
androidx.compose.material.icons.*
```

No migrated reusable contract uses:

```kotlin
icon: ImageVector
```

Instead:

```kotlin
icon: PandaIcon
```

and rendering is:

```kotlin
Icon(
    painter = painterResource(icon.resourceId),
    ...
)
```

## Semantic API remains stable

Callers still use:

```kotlin
PandaWaveIcons.Home
PandaWaveIcons.Library
PandaWaveIcons.NowPlaying
PandaWaveIcons.Search
PandaWaveIcons.Profile
PandaWaveIcons.Play
PandaWaveIcons.Pause
PandaWaveIcons.Queue
```

They do not know which drawable backs the semantic icon.

## Existing branding assets remain separate

These are not part of the 21-asset migration set:

```text
pandawave_ic_launcher_foreground.xml
pandawave_ic_launcher_monochrome.xml
pandawave_ic_logo.xml
pandawave_ic_panda_paw.xml
pandawave_ic_splash_mark.xml
```

The migration test and normalization procedure never glob over them.

---

# Commit / Review Boundaries

Recommended commits:

```text
1. feat(designsystem): vendor PandaWave vector icons
2. refactor: migrate PandaWave icon contracts and renderers
3. perf: remove Compose Material Icons dependency
4. optional fix(designsystem): preserve legacy icon parity
```

Commit Tasks 3 and 4 together after compiling all callers. Commit the guard and dependency removal together after verification turns green. Each committed implementation boundary must compile; do not commit an intentionally broken guard.

---

# Rollback Rules

- If a specific shape is wrong, replace only that local vector from the pinned legacy source.
- If `PandaIcon` causes a type/rendering problem, fix the shared renderer API; do not reintroduce `ImageVector` merely to keep the dependency.
- If a feature fails to compile after dependency deletion, locate remaining direct `Icons.*`, `ImageVector`, or old-catalogue usage and migrate it explicitly.
- Do not use Material Symbols as an emergency visual substitute when parity with the old icon is required.
- Do not reintroduce `material-icons-extended` to fix a missing resource without first identifying the exact missing semantic entry.

---

# Self-Review

## Corrections incorporated

- The Gradle guard no longer scans its own `build.gradle.kts` text for the forbidden strings it defines.
- The guard checks both `material-icons-extended` and `material-icons-core`.
- The guard checks Kotlin imports, configured direct dependencies, and all parsed version-catalog library coordinates, including unused aliases; its task action is configuration-cache compatible.
- The concrete renderer migration includes:
  - `BambooNavigationItem`
  - `BambooMiniPlayer`
  - `BambooPlayPauseButton`
  - `BambooDiscoveryComponents`
  - `AppShellScreen`
  - `NowPlayingRoute`
- Dependency removal explicitly covers all six current modules.
- The asset test examines only the explicit 21 migration files.
- Asset normalization examines only those 21 copied files.
- Existing launcher/logo/paw/splash branding resources are explicitly protected.
- Baseline reports are copied outside `build/`.
- Benchmark conditions explicitly distinguish project outputs, Gradle build cache, and warm dependency/transform caches.
- Benchmarks use three clean-output runs plus three real function-body incremental edits before and after. PowerShell Gradle property arguments are quoted.
- PowerShell commands dynamically include only source roots that exist, so a missing `provider`/`rro` directory cannot turn a successful empty scan into an `rg` path error.
- The plan does not claim runtime rendering acceleration from XML vectors.
- The plan does not claim dex merging disappears.

## Type consistency

One migrated semantic icon type is used across modules:

```kotlin
PandaIcon
```

One semantic catalogue:

```kotlin
PandaWaveIcons
```

One resource rendering path:

```kotlin
painterResource(icon.resourceId)
```

No task introduces a competing Material icon dependency, icon font, SVG runtime loader, or copied Compose icon source.
