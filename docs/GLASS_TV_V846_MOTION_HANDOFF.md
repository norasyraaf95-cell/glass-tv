# Glass TV V8.4.6 — Motion / SmartTube handoff prototype

**Status:** source-only engineering branch. Not built, installed, or physically tested.

- Base: `glass-tv-v8.4-performance-experimental` (V8.4.5)
- Working branch: `glass-tv-v846-motion-handoff-development`
- TV baseline: Xiaomi `MiTV-AESP0` / Android 10; 163/2072 janky frames (7.87%), p95 20ms, p99 57ms from a non-isolated `gfxinfo` capture.
- Existing installed HOME package: `com.gathin.tvhome.referencev781v845`. Do not disable, uninstall, or change it during source review.
- The original `referencev781top` package must remain available as rollback.

## V8.4.6 code changes

1. **SmartTube press cue:** Existing DPAD_CENTER/ENTER key-down starts the optical acknowledgment. For SmartTube on the five-card dock, the click initiates a bounded frame-aware handoff. Android API 29+ `ViewTreeObserver.registerFrameCommitCallback` allows a launch after the next cue frame has been submitted to the renderer. Fallback occurs at 64ms or launch is immediate when the view is detached/non-hardware-accelerated/unsupported. A duplicate OK during a pending handoff is ignored. Callback and timeout race safely; launching occurs once.
2. **A/B path:** Preference `satin_v84/frame_cue_handoff` defaults to true in this source-only prototype. Setting it false in a development variant keeps immediate launch; the original best-effort 180ms `ActivityOptions` fade remains. All other app launches keep their immediate path.
3. **Dock synchronization:** Focus illumination remains 218ms (priority response). `SatinPrismMotion` and `SatinLightBridge` share 270ms optics motion and a matched ease curve. This replaces the older 395ms sweep which visually outlasted the 270ms bridge.
4. **Rapid DPAD:** The existing 145ms decorative-sweep gate stays intact; focus illumination still retargets from its *current alpha*, while obsolete glints stop. No motion is queued.
5. **Safety:** Press and return motion keep their previous duration and easing. Frame handoff cancels on Home pause, and OEM callback errors fall back to immediate launch.

## Unchanged scope

No edits to `ShahAlamWidgets.java`, `WindyCloudView.java`, wallpaper, five approved WebP dock artworks, topbar assets, prayer API, activity manifest, application ID, Gradle configuration or Android settings. The reference/production branches are untouched.

## Verification

Source regression checks:
```sh
python3 -m unittest discover -s tests -p 'test_satin_*.py' -v
cd launcher && ./gradlew testReleaseUnitTest
```

**Do not run `assembleRelease` or install anything** until the user authorizes a candidate APK. Source contract tests are not proof of device performance.

Later on-device test (only after approval):
- Film SmartTube at high frame rate, cold and warm, comparing frame-aware mode against immediate mode. Measure from OK feedback to app visibility and black-screen duration separately.
- Ten launch trials per variant, one press per trial, check any duplicated app start, errors and latency.
- Thirty Left/Right rapid inputs; verify indicator/cue consistency, retained focus, no stacked glints.
- Back/Home/reboot and 24h/next-month prayer refresh; confirm accurate timetable and original weather video, toggle, and topbar visual assets.
- Capture `dumpsys gfxinfo` after controlled DPAD sequences (reset counters before each), compare janky frames and p95/p99 against a matching V8.4.5 run. A non-controlled 7.87% figure is just historical baseline.
- Reject candidate on regressions: inaccurate prayer, missing weather asset, dock art changes, app launch failure, more jank, unexpected input delay, Home instability.

## Platform caveat

Android 10 frame commit means **submitted to the swap chain**, not necessarily visible on the physical panel. OEM cross-task/SmartTube transition can still display black. This prototype cannot guarantee a zero-black launch or a full-screen dissolve. User footage and device instrumentation determine whether the next release is acceptable.
