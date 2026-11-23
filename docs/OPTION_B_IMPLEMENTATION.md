# Crystal Clear — Option B: Magnetic Spotlight implementation

## Exact reference
- Owner-approved video: `GlassTV_Movement_02_Magnetic_Spotlight.mp4`
- Reference video SHA-256: `3be54adc9c03fc7f5965c4ee0b9fa0a1f2864b1a5669ecfb9199aecac9294537`
- Locked specification: `docs/LOCKED_MOVEMENT_OPTION_B_MAGNETIC_SPOTLIGHT.md`.
- The selected mockup uses a 470 ms quintic *smootherstep* transition, with a broad softly diffused moving icy aqua/white pool beneath the dock and a simultaneous 470 ms crossfade of stationary tile rims.

## Implemented
- `MagneticSpotlightView.java`: two zero-allocation-per-frame radial ellipses, layer behind the dock, reusable shader matrix and ValueAnimator, 470 ms smootherstep. Retargets from current animated location on rapid DPAD reversal; snaps on Home resume so there is no start-up sweep.
- `LiquidCrystalDockDrawable.java`: white/cyan focused tile light crossfades in 470 ms with the same smootherstep; labels and tile positions are untouched.
- `MainActivity.java` and `activity_main.xml`: foot attaches to the wallpaper-level view layer and follows real focused dock card screen positions. Stops onPause and disappears when leaving the dock.
- `AppAdapter.java`: the old narrow Satin travelling sweep/bridge is suppressed ONLY for the Option B preview package. SmartTube immediate-launch and the app click logic are unchanged.
- `applicationIdSuffix '.crystalclearmagneticb'`: isolated, **not HOME eligible**. Installed `crystalclearhome` stays default and unchanged. No system package is disabled or uninstalled.

## Before first preview launch
After installing `GlassTV_Magnetic_Spotlight_B.apk` via Bugjaeger, copy owner's exact wallpaper and config from their working Home-ready app:
```sh
mkdir -p /sdcard/Android/data/com.gathin.tvhome.crystalclearmagneticb/files
cp /sdcard/Android/data/com.gathin.tvhome.crystalclearhome/files/config.json /sdcard/Android/data/com.gathin.tvhome.crystalclearhome/files/wallpaper.jpg /sdcard/Android/data/com.gathin.tvhome.crystalclearmagneticb/files/
am start -n com.gathin.tvhome.crystalclearmagneticb/com.gathin.tvhome.MainActivity
```

## Manual acceptance on the physical TV
1. Verify unchanged clock/widgets, original wallpaper, white dock text and five app tiles.
2. LEFT/RIGHT at normal and rapid speeds: soft footprint slides continuously with no scanning line, no moving border, no tile wobble.
3. Focus rim crossfades and footprint arrives simultaneously (~470ms) after one press; reverse direction retargets fluidly.
4. Launch SmartTube immediately; pressing BACK restores focused app without ghost spotlight animation.
5. Only after TV comparison and owner's approval consider a new default-Home release. Do not replace the existing default automatically.

Static video cannot guarantee identical hardware blending; physical TV comparison remains required.
