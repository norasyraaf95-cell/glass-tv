# Glass TV V8.4 — Satin Prism Performance (EXPERIMENTAL)

> **DO NOT REPLACE THE INSTALLED V7.8.1 HOME APP.**
> This branch is based on `glass-tv-v7.8.1-synced-light-handoff`.
> The original wallpaper, user-approved five WebP dock artworks, logo/app mappings,
> clock, prayer/weather widgets, top controls and layout are not modified.

## Isolation and rollback

- Branch: `glass-tv-v8.4-performance-experimental`
- Original first-pass test ID: `com.gathin.tvhome.referencev781v84`
- Previous V8.4.1 test ID: `com.gathin.tvhome.referencev781v841`
- V8.4.2 test ID: `com.gathin.tvhome.referencev781v842`
- **Current V8.4.3 visibility-test ID:** `com.gathin.tvhome.referencev781v843` (does not replace stable or earlier experiments)
- GitHub-hosted debug signing keys may differ between builds. A new test package avoids update-signature conflicts.
- Stable reference build ID: `com.gathin.tvhome.referencev781`. The user's **installed topbar launcher** is `com.gathin.tvhome.referencev781top` — leave it untouched.
- Current fix-test version name: `2.9.8.4.3-satin-prism-visibility-experimental`
- Do not set this build as the default HOME launcher.
- To roll back, simply open the already installed V7.8.1 launcher;
  uninstalling the experimental package does not uninstall the stable package.
- Do not merge this work into the reference or main branches without physical-TV tests.

## Critical: original artwork files are per-package

`DockArtwork.java` reads the **original reference** assets from the app's
`getExternalFilesDir(null)`. Android keeps this location separate for each
application ID. Therefore a side-by-side experimental APK **will not automatically
see the V7.8.1 artwork files** installed under the stable package.

For an exact visual comparison, copy the owner's original, unmodified assets into
the *experimental* package external files directory after the experimental APK
has been installed:

`/sdcard/Android/data/com.gathin.tvhome.referencev781v843/files/`

The user's original assets currently exist at
`/sdcard/Android/data/com.gathin.tvhome.referencev781top/files/`, and were
also copied into the first experimental package. Copy from either known source;
do not regenerate any artwork.

Required originals:

1. `liquid_dock_base.webp`
2. `liquid_focus_illumination.webp`
3. `liquid_focus_reflection.webp`
4. `liquid_focus_glow.webp`
5. `liquid_static_reflections.webp`

The original source files must be supplied by the owner. Do **not** regenerate
or substitute visually similar assets. Access/copying may depend on the TV's
Android/ADB permissions. If the files are unavailable, the existing native
V7.7 fallback may appear: **stop the visual test** rather than accepting that
as a valid V8.4-versus-V7.8.1 comparison.

## V8.4.2 root cause verified (2026-11-19)

On the physical Mi TV Stick, the V8.4.1 diagnostic log showed
`GlassTV.V84: Satin enabled=false reducedMotion=true` although the copied
`config.json` contained `"reduced_motion":false`.

`Config.load()` parsed the boolean correctly but its final `new Config(...)`
hardcoded `true` in the reducedMotion argument. V8.4.2 passes the effective
`reducedMotion` variable instead. It still honours explicit reduced motion
when users set the JSON field to true. A Python source regression check now
guards the call site.

After copying the same original assets into the V8.4.2 external-files directory,
relaunch V8.4.2 and verify the TV log:
`adb logcat -d -b main -s GlassTV.V84:I`
Expected startup line: `Satin enabled=true reducedMotion=false`.
Actual DPAD transitions should then report `Dock focus ... playSweep=true`.
Measure visual difference and performance on physical TV; CI is not a substitute.

## V8.4.3 visual check after user footage (2026-11-19)

Physical-TV video showed that the V8.4.2 55% sweep was visually very subtle
and the Android cross-app transition was not perceptible. V8.4.3 adjusts the
**sweep envelope**, not the 55% intensity setting: optical peak lasts through
the centre of the card before fading. The thin Light Bridge similarly stays
visible while traversing the gap. Artwork, tile positions, pink rim, wallpaper
and widgets remain completely unchanged.

A lightweight silver press cue starts on DPAD_CENTER/ENTER **key-down** and
returns false to preserve the original Android click handling. startActivity
remains synchronous and receives the same best-effort 180ms transition
request. This *does not guarantee* a full-screen Silk Dissolve when launching
SmartTube or third-party apps. If a longer, guaranteed fade is required, that
would require either target-app/system transition control or an intentional
prelaunch delay; neither is part of this no-delay experimental build.

Before adjusting TV global animation settings, read current state using
`adb shell settings get global transition_animation_scale`,
`adb shell settings get global window_animation_scale`, and
`adb shell settings get global animator_duration_scale`.
Do not change any values without knowing and retaining their original values.

## Implemented as first experimental pass

- Static V7.8.1 artwork and focus halo layers remain source-identical.
- 218 ms illumination focus handoff (the 65% responsiveness target).
- Narrow single-pass directional satin sweep, subtle micro-refraction and
  short Fresnel/Crystal edge highlight drawn over the focused card.
- Very thin, event-only Satin Light Bridge between neighbouring cards.
- No position/scale/logo/wallpaper motion and no extra Glider rim over dock.
- Rapid D-pad input cancels previous motion and prioritizes focus response.
- Existing native OK launch stays synchronous; short tactile feedback is
  best-effort and cannot delay or force another app's Android transition.
- Quiet return focus, no repeating directional sweep on Back.
- V8.4.1: restore-focus seeding after Home, no 2.5-second sweep expiry, diagnostic `GlassTV.V84` logcat events.
- V8.4.1: requests Android's 180ms app enter/launcher exit alpha transition via `ActivityOptions`; app launches immediately with no artificial 180ms delay.
- Event-only animators, no idle loops, no live blur/texture decode in frame draw.
- Respects existing `reduced_motion` configuration and experimental
  `satin_v84.enabled` preference (default true only in this test package).

## Not yet guaranteed by this implementation

- The browser's guaranteed full-screen `Silk Dissolve 180ms` is a visual reference.
  V8.4.1 requests a 180ms Android activity transition, but Android TV firmware
  or the destination app can override/disable it. Do not promise matching
  results without a physical-TV test.
- No measured automatic FPS/frame-jank governor has been installed;
  rapid-input shedding and `reduced_motion` are the current fallbacks.
- No Motion Studio settings UI, simulated TV screensaver or optional click sound.
- Browser simulated logos/widgets are not used inside the APK.

## Acceptance checklist for the physical TV

- [ ] V7.8.1 remains installed and is still the default HOME app.
- [ ] Experimental package installs side-by-side, not over V7.8.1.
- [ ] All five unchanged owner-supplied artwork files are visible in experiment.
- [ ] At idle, screenshot comparison matches original wallpaper, glass tiles,
      selected pink rim, logos, clock/date and widgets.
- [ ] Left and right trigger only one directional sweep on focus change.
- [ ] Rapid remote input never builds up queued sweeps or changes tile order.
- [ ] OK launches the selected app immediately, especially SmartTube Stable.
- [ ] Returning from an app restores the last focused tile without flashing.
- [ ] Long-press, Move, App Info, top widgets and settings work exactly as before.
- [ ] No permanent animation, repeated allocations or dropped frames while idle.
- [ ] Measure navigation FPS and memory on the Xiaomi Mi TV Stick Android 10.
- [ ] If rendering worsens, use the existing `reduced_motion` option or
      disable `satin_v84.enabled` in the experimental package.

GitHub Actions compiles and runs unit tests, but that **does not** establish
visual equivalence, real-world smoothness or the effectiveness of fallback on TV.
