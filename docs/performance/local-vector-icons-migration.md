# Local vector icon migration results

The migration replaces Compose Material Icons with 21 local XML vectors backing
22 semantic icon names. The six direct `material-icons-extended` dependencies and
their catalog alias are removed. `PandaIcon` and `PandaWaveIcons` now belong to
`:core:designsystem`; consumers load the resources with `painterResource`.

The [corrected implementation plan](../plans/2026-09-08-local-vector-icons-migration.md)
records the reviewed design. [Asset provenance](../third-party/material-design-icons.md)
records the pinned Google sources and retained Apache 2.0 license.

## APK and DEX evidence

Measurements on September 8, 2026 compare baseline `002084e95` with this migration,
using the same Windows machine, Temurin 21, Gradle 9.5.0, and Debug build settings.
Native engine compilation was disabled in both builds with
`'-PpandaEngine.buildNative=false'`. These are Debug measurements; they do not
predict release APK savings after shrinking or include a native-engine comparison.

| Measurement | Baseline | Local vectors |
| --- | ---: | ---: |
| Debug APK bytes, normal application ID | 83,754,610 | 52,181,968 |
| Uncompressed DEX bytes, visual-test builds | 80,913,660 | 49,318,012 |
| DEX files | 26 | 26 |
| `Landroidx/compose/material/icons/` in DEX | Present | Absent |

The normal Debug APK is **31,572,642 bytes smaller (37.7%, about 30.1 MiB)**.
The visual-test builds used a temporary application ID suffix and were eight bytes
larger on both sides. Their DEX scan confirms the icon package was removed.
DEX merging still runs for the app's remaining dependencies. This migration
removes Material Icons' contribution to that work; XML resources still need normal
resource loading and parsing at runtime.

## Rendering evidence

A temporary Compose instrumentation gallery rendered all 22 semantic icons at
24dp in 48dp cells, using the previous ImageVectors on the baseline and resource
painters after migration. Both builds ran on the same AAOS 15 x86_64 emulator.
Comparison of the 288 × 192 PNGs found **zero changed pixels** in both LTR and RTL.
The screenshots are retained here:

| Direction | Baseline | Local vectors |
| --- | --- | --- |
| LTR | [Before](local-vector-icons/before-Ltr.png) | [After](local-vector-icons/after-Ltr.png) |
| RTL | [Before](local-vector-icons/before-Rtl.png) | [After](local-vector-icons/after-Rtl.png) |

Only Queue, VolumeDown, and VolumeUp retain upstream automatic mirroring. All 21
vectors have the original path geometry and styles, 24dp size and 24 × 24 viewport.
Theme tint was removed so each existing Compose call site supplies its tint.
The five launcher, logo, paw, and splash branding resources are unchanged.
Temporary test sources and application ID changes were removed after capture.

The existing `AppShellInsetsTest` fails identically on baseline and migration at
line 73: expected 14px, found 10px. That result prevents claiming a fully passing
emulator suite. No inset or layout fix is included in this migration.

## Build timings

Three clean-output builds and three incremental edits were run serially per
revision on September 8. Each clean run used:

```powershell
.\gradlew.bat clean :app:assembleDebug '-PpandaEngine.buildNative=false' --no-build-cache --profile --console=plain
```

Dependency and transform caches remained warm; configuration cache was enabled.
Each incremental run changed the actual `BambooMiniPlayer` function body from
`.fillMaxWidth()` to a different fraction and ran normal `:app:assembleDebug`.
The original source bytes were restored after the experiment. All twelve builds
succeeded. Wall times include Gradle startup and configuration.

| Scenario | Baseline runs (seconds) | Local-vector runs (seconds) | Baseline median | Local median |
| --- | --- | --- | ---: | ---: |
| Clean outputs, build cache disabled | 736.811, 109.244, 98.530 | 366.643, 205.740, 92.662 | 109.244 | 205.740 |
| Incremental function-body edit | 25.089, 8.470, 7.917 | 69.997, 6.979, 7.358 | 8.470 | 7.358 |

Selected clean-build task medians from Gradle's profiles:

| Task | Baseline seconds | Local-vector seconds |
| --- | ---: | ---: |
| `:app:mergeExtDexDebug` | 23.958 | 21.873 |
| `:app:mergeDebugJavaResource` | 8.316 | 6.430 |
| `:core:ui:compileDebugKotlin` | 6.166 | 9.052 |

These timings are **inconclusive for overall build speed**. Large variation and
unequal configuration/daemon warmup dominate the samples; even clean medians
move in the opposite direction from some individual task medians. No build-speed
percentage or runtime acceleration is claimed. The removed dependency and DEX
bytes are the reliable evidence of reduced input size. Raw wall-time samples are
retained in [before-times.csv](local-vector-icons/before-times.csv) and
[after-times.csv](local-vector-icons/after-times.csv); extracted task durations are
in [clean-task-times.csv](local-vector-icons/clean-task-times.csv).

## Verification

The migration adds an explicit 21-asset XML contract test and a typed
`verifyNoComposeMaterialIcons` Gradle task wired into `qualityCheck`. The guard
checks included modules' Kotlin imports, configured direct dependencies, and
parsed version-catalog coordinates, including unused aliases. It rejects both
`material-icons-core` and `material-icons-extended`.

Four Gradle TestKit tests passed again on September 13. They cover forbidden
imports with file/line diagnostics, direct dependencies and catalog notations,
source-scan boundaries, and configuration-cache reuse that still detects a newly
introduced forbidden import.

An independent final code review found no actionable issues in the resource API,
consumer changes, dependency removal, or configuration-cache guard implementation.
`graphify update .` refreshed the local code graph after the implementation.

The September 8 full `qualityCheck :app:lintRelease test` run passed. The final
September 13 `qualityCheck :app:lintRelease test :app:assembleDebug` run also
passed: 1,276 actionable tasks, with 10 executed and 1,266 up to date. The
resolved `debugRuntimeClasspath` contained neither `material-icons-core` nor
`material-icons-extended`. Two repository guard runs with configuration cache
passed, and the second reused the stored cache entry.
