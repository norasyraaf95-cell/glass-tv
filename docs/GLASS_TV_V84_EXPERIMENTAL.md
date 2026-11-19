# Glass TV V8.4 — Satin Prism Performance (EXPERIMENTAL)

> **DO NOT REPLACE THE INSTALLED V7.8.1 HOME APP.**
> This branch is based on `glass-tv-v7.8.1-synced-light-handoff`.
> The original wallpaper, user-approved five WebP dock artworks, logo/app mappings,
> clock, prayer/weather widgets, top controls and layout are not modified.

## Isolation and rollback

- Branch: `glass-tv-v8.4-performance-experimental`
- Experimental release ID: `com.gathin.tvhome.referencev781v84`
- Stable reference release ID: `com.gathin.tvhome.referencev781`
- Experimental version name: `2.9.8.4-satin-prism-performance-experimental`
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

`/sdcard/Android/data/com.gathin.tvhome.referencev781v84/files/`

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
- Event-only animators, no idle loops, no live blur/texture decode in frame draw.
- Respects existing `reduced_motion` configuration and experimental
  `satin_v84.enabled` preference (default true only in this test package).

## Not yet guaranteed by this implementation

- The browser's full-screen `Silk Dissolve 180ms` is a visual reference.
  Android and the target application own the real activity transition.
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
