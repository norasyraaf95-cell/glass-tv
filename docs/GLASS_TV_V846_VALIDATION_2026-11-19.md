# Glass TV V8.4.6 — validation record (2026-11-19)

This is a **source-only candidate**, not an APK release. The TV remains on `com.gathin.tvhome.referencev781v845`. No install, package clearing, ADB mutation or default HOME changes were made.

## What was tested

**Exact GitHub blob matching before JVM tests:**
- `SatinTiming.java` blob `454e903025f7e820124e06520fe052db7a6301a3`
- `SatinMotionPolicy.java` blob `2b7c3b4ca8291ff344b62070681e6d7e0537275d`
- `FrameCueHandoff.java` blob `bbb5b24ace2dd6a7a75dce1721555c3759dfc8a4`
- `SatinLaunchPolicy.java` blob `947eaa275079300ee449a9d448cc0cefa0b8add9`

These exact Java source blobs were compiled with local `javac` using small simulated `android.os`, `android.view`, and `android.util` stubs (not a real Android SDK). A standalone validation harness passed **24/24 assertions**:
- 218ms focus and 270ms optics constants, 145ms rapid sweep threshold;
- first frame committed launches once, timeout fallback launches once;
- frame/timeout race does not double-launch;
- double OK is suppressed while pending;
- onPause cancellation prevents a pending launch;
- OEM exception / missing callback support launches immediately;
- `SatinLaunchPolicy` refuses a rapid second click within 450ms, accepts later clicks and a monotonic clock reset.

**Additional read-only GitHub source contract checks:** 16/16 assertions passed for SmartTube-only handoff, 64ms maximum, fallback, focus/optical sync, OEM fade, animation parameters and rapid DPAD behavior.

**Isolation and regression scope:**
- Prototype release suffix is `.referencev781v846`, versionCode `30`, versionName `2.9.8.4.6-motion-handoff-prototype`.
- Source hashes for `MainActivity.java`, `ShahAlamWidgets.java`, `WindyCloudView.java`, `DockArtwork.java`, and `Config.java` match the V8.4.5 base branch. Prayer API, weather media code and reference artwork loading are untouched.
- The V8.4.5 experimental branch and `main` were not modified.

## What is still NOT verified

1. Full Android Gradle compile and `testReleaseUnitTest` execution: **not run** because an Android SDK/Gradle environment was not available in this session. The Java stub test is NOT a substitute for this.
2. No APK created or installed: **intentionally not run**, per user instruction.
3. No physical-TV rendering or performance tests. `registerFrameCommitCallback` indicates a frame was submitted; it does not prove the frame is on-screen. Android TV 10/OEM may still black-screen during SmartTube startup.
4. The old 7.87% janky-frame baseline was not captured under a controlled comparison sequence. No claim of improved FPS or lower jank has been validated.
5. Frame-aware versus immediate-launch A/B must be checked on the actual Xiaomi TV. `satin_v84/frame_cue_handoff` is an internal SharedPreferences toggle; it does not yet have an end-user settings UI.

## Release gate

Before any APK candidate:
- Run `python3 -m unittest discover -s tests -p 'test_satin_*.py' -v` and `./gradlew testReleaseUnitTest` in an Android SDK/Gradle environment.
- Confirm no regressions and obtain explicit user authorization for an isolated APK build.
- Then install the isolated package side by side with V8.4.5. Ensure its **per-package external assets** (five artwork atlases, floating prism topbar images, wallpaper, config, optional `windy_clouds.mp4`) are available before any visual comparison.
- Physically test ten SmartTube launches each for immediate/frame-aware, 30 rapid DPAD changes, Home/Back/reboot, Prayer Time and Weather, and comparable `gfxinfo` frame timing.
- Keep V8.4.5 as the current default; preserve `referencev781top` as additional rollback.
