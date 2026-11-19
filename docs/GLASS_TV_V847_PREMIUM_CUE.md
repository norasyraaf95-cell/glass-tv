# Glass TV V8.4.7 — Premium SmartTube OK cue experiment

**Status:** isolated experimental branch; not a final release. Tested V8.4.6 is the visual/performance baseline; preserve its installed package and V8.4.5 HOME default.

## Why V8.4.6 was insufficient

Owner-tested V8.4.6: Satin Light Bridge, Prayer, Weather/Windy Clouds, and HOME behavior are correct; SmartTube black gap is shorter, but the planned visual OK animation is still not visible. The 64ms frame-commit handoff only gives Android a chance to submit one cue frame; the actual destination window transition is controlled by OEM/SmartTube.

This prototype instead composes a **launcher-owned prelaunch cue** and *then* calls `startActivity`. It cannot animate SmartTube's window directly.

## Visual choreography (only SmartTube dock tile)

- Preserve five original bitmap cards, pink focus border, wallpaper, Prayer, Weather, and existing 218ms/270ms focus/bridge behavior.
- Selected dock card gently compresses to 97.8% of current scale (nothing changes at idle).
- Home background darkens to a maximum of 17% with the selected card excluded from the darkening.
- A thin neutral silver border traces the existing card shape: no full-screen white flash, glow, or layout movement.
- Entire cue completes in **120ms** or **180ms** of actual uptime, followed by native Android Activity handoff.
- Uses `Choreographer` instead of `ValueAnimator`, specifically because this Android 10 TV uses animator scale 0.5x. This experiment does not alter any Android global animation settings.
- If frames stop arriving, fallback launches at requested duration + 64ms; overlay errors launch immediately. If HOME/DPAD changes focus before the cue completes, abandon the pending launch. Duplicated OKs do not queue launches; failure resets the overlay and repeat guard.
- Only the SmartTube tile uses this cue. Other apps continue to use the existing non-delayed Activity launch path.

## A/B modes: same one package

Package: `com.gathin.tvhome.referencev781v847` (release suffix), versionCode 31. **Never set as default Home during test.**

An explicit ADB start selects mode and displays a short Toast:
```powershell
# baseline: previous V8.4.6 bounded frame-commit route
.\adb.exe shell am start -n com.gathin.tvhome.referencev781v847/com.gathin.tvhome.MainActivity --ei glass_tv_launch_cue_ms 0

# 120ms prelaunch visual, default for V8.4.7
.\adb.exe shell am start -n com.gathin.tvhome.referencev781v847/com.gathin.tvhome.MainActivity --ei glass_tv_launch_cue_ms 120

# 180ms prelaunch visual
.\adb.exe shell am start -n com.gathin.tvhome.referencev781v847/com.gathin.tvhome.MainActivity --ei glass_tv_launch_cue_ms 180
```
The value is stored only in this experimental package under `satin_v847`. Unsupported values are rejected. To see timing logs:
```powershell
.\adb.exe logcat -d -b main -s GlassTV.V847:I GlassTV.V84:I
```

## Release gating and owner acceptance

1. Run Python source regression suite and Android `testReleaseUnitTest`.
2. Build a distinct APK only if both pass, inspect `aapt dump badging` for `referencev781v847`, compute SHA-256 and upload as GitHub Actions artifact. Do not auto-install or set HOME.
3. Before comparing visuals, copy **V8.4.6's validated** external artwork/config/wallpaper/weather video/prayer backup to the new package's files folder; a fresh package has separate files.
4. Test each mode with five to ten warm launches and a few cold launches. Capture a high-FPS mobile video from the moment OK is pressed through SmartTube display. Evaluate visible cue, black gap, total perceived startup, no flash or wallpaper changes. Collect `GlassTV.V847` log timing.
5. Try rapid DPAD and BACK/HOME: no persistent dim, no stale focus or duplicate launch. Confirm exact Prayer and Weather/Windy Clouds visuals and correct five dock cards.
6. Compare controlled frame stats against V8.4.6. *Do not* claim smoothness improvement before the physical TV comparison.

## Important limitations

A 120/180ms prelaunch animation is an intentional trade-off: it may increase app-open latency. Android/OEM still controls the app-to-app transition and may show a remaining black gap. If the cue feels delayed or frame timing worsens, switch to baseline mode 0 or remain on V8.4.6; do not force this experiment into the default launcher.
