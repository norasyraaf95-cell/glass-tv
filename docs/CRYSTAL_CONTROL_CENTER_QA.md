# Crystal Control Center — QA gates (2026-11-24)

## Scope, safety and isolation
- Development branch: `crystal-control-center-development-20261124`.
- Independent Android APK: `com.gathin.tvhome.crystalcontrolpreview`, version 41; NOT a HOME candidate.
- Existing default launcher `com.gathin.tvhome.crystalclearmagneticbhome` untouched.
- Preview is based on the approved Crystal Clear Home, retaining exact wallpaper composition, prayer/weather data, apps, click/launch behaviour and white dock typography.
- Old purple WebP dock pack disabled in this package; native frosted material is used.
- Do NOT instruct user to run `pm disable`, `pm clear`, `pm uninstall` or `cmd package set-home-activity` to test this preview.

## Features in source
1. **Crystal Control Center**: gear OK opens overlay. Hold OK opens Android Settings. BACK cancels uncommitted edits. Home stays beneath panel and updates live.
2. **Widgets**: tint, text colour, opacity and active prayer highlight. ShahAlamWidgets retains live data fetching.
3. **Dock**: tint, glass opacity, rim opacity and reflection intensity. Five apps and labels untouched.
4. **Magnetic Spotlight**: colour, strength and softness. Default Option B = 470ms smootherstep.
5. **Movement**: magnetic (locked B default), bloom, Satin Refraction, instant; custom duration/easing. Reset to locked Option B.
6. **Adaptive Glass**: sample wallpaper once on change; adapt palette only when enabled. Sensitivity, soft/balanced/strong.
7. **Wallpaper**: TV/USB picker via Storage Access Framework, fit/crop, brightness, saturation, softness blur (one-shot off UI thread), restore original.
8. **Themes**: locked reset, Emerald, Sapphire, Sunset. JSON import/export (Create Document picker or backup in app files), Save, Cancel.

## Rendering correction
`RecyclerView.ItemDecoration.onDraw()` now layers:

  wallpaper -> cached tray blur + tint + rim -> diffuse Option B spotlight -> child tiles and labels.

`MagneticSpotlightView` holds focus position and shader state, but **paints through DockDecoration**, avoiding the previous below-tray occlusion.
- Quintic smootherstep `6t^5 - 15t^4 + 10t^3`; default 470ms, no layout animations during focus.
- Rapid DPAD reversals retarget from current position.
- Separate stationary focus rim crossfade in 470ms.
- Dock gradients cached and transformed via reusable matrices: no new `LinearGradient` in `onDraw`.
- Shader palette updates on settings changes, not during DPAD animation.

## User data and rollback
- Appearance saved to package-private SharedPreferences, plus app-scoped `appearance.json` backup.
- New image staged as `wallpaper_pending.jpg`; only Save commits to `wallpaper.jpg`.
- On first startup, copy original wallpaper to `wallpaper_original.jpg` if available.
- Save backs up previous wallpaper to `wallpaper_previous.jpg`.
- Cancel deletes pending file and restores current saved wallpaper/appearance, without changing the existing default launcher.
- Export JSON via Android document picker or into app-scoped `CrystalClear_Theme_Backup.json`.

## CI verification
1. Python regression tests for Home isolation, UI controls, no cloud video toggle, live prayer/weather, file backups and focus order.
2. `./gradlew testReleaseUnitTest assembleRelease` with Java 17/Android 34.
3. `aapt dump badging` verifies unique package and version, `aapt dump xmltree` verifies no HOME intent filter.
4. Artifact SHA-256 manifest and APK. **No claim of physical TV verification** from CI alone.

## Physical TV acceptance (required before making default)
1. Copy EXACT approved original photo and config only into `/sdcard/Android/data/com.gathin.tvhome.crystalcontrolpreview/files`; leave old packages intact.
2. Launch with `am start -n com.gathin.tvhome.crystalcontrolpreview/com.gathin.tvhome.MainActivity`.
3. Confirm 5 app tiles, SmartTube, widget API data and weather click disabled.
4. Record Left->Right->Left; compare Option B video frame-by-frame. Qualitative closeness, no compositor/jank claim without TV footage.
5. Gear OK opens Control Center, gear hold opens system settings. Change widget opacity and spotlight colour, verify behind panel; CANCEL and confirm originals return.
6. SAVE changes, relaunch preview and verify persistence. Test locked Option B Reset.
7. Select another image, test Fit/Crop and brightness, cancel; confirm original. Then select/Save a test image and Restore Original.
8. Export theme via file picker, import, verify properties; try invalid JSON (must show error, no crash).
9. Return Home button: existing Magnetic B Home should still be the default throughout.
10. When user approves TV test, plan separate stable-signing release. GitHub Actions debug signing is NOT suitable for long-term same-package updates. Use a protected GitHub secret keystore before promoting; do not commit private signing material.

## Known test boundary
GitHub compilation/unit/contract tests can verify code and packaging but **not** 60-FPS GPU smoothness, Xiaomi TV SAF file picker availability or perfect pixel/video parity. These require real device tests.
