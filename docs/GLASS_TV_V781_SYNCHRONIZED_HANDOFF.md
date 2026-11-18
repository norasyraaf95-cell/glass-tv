# Glass TV V7.8.1 — Synchronized Light Handoff (motion-only correction)

## Frozen scope
Original wallpaper (`v75_wallpaper.jpg`), all top widgets, navigation, five cards, labels and brand logo/SmartTube mapping remain unchanged. The installed V7.8 package remains the default.

## Why V7.8 motion was not the approved reference
Its pre-rendered focused atlas changed the *complete* tile, including branding and glass material, and had strong focus differences for YouTube but weak differences for the other four tiles.

## V7.8.1 architecture
- Five static artwork cards: `liquid_dock_base.webp` (1500 × 156). YouTube's permanently baked glow is moderated, but the approved artwork and wordmark are kept.
- Transparent focus illumination only: `liquid_focus_illumination.webp` (1500 × 156). No branding, no moving borders; zero alpha at the card seams.
- Diffuse exterior aura: `liquid_focus_glow.webp` (340 × 205). No ellipse outline.
- Per-card soft reactive floor pool: `liquid_focus_reflection.webp` (1500 × 94), with brand-appropriate soft colours.
- Existing static room floor: `liquid_static_reflections.webp`, unchanged.
- A single reusable `ValueAnimator` per visible dock card drives three alpha values with the *same* easing/timing: focus illustration 1.0; aura 0.76; floor 0.70.
- Easing PathInterpolator(.20, .0, .22, 1), 240ms. Rapid DPAD reversal always starts from the currently displayed value.
- No full-card bitmap crossfade, scaling, moving outline, frame translation or black shadows.
- Android TV app package: `com.gathin.tvhome.referencev781`, installed separately for verification.

## Asset ZIP setup (after installing the V7.8.1 APK)
Use `GlassTV_V781_Approved_Motion_Assets.zip`. It contains the original wallpaper, config, static floor artwork, base/focus illumination, halo and per-app reflection. Push to the Mi TV Stick's `/sdcard/Download/` with Bugjaeger before running:

```sh
mkdir -p /sdcard/Android/data/com.gathin.tvhome.referencev781/files
cp /sdcard/Download/GlassTV_V781_Approved_Motion_Assets.zip /sdcard/Android/data/com.gathin.tvhome.referencev781/files/v781_asset_pack.zip
am force-stop com.gathin.tvhome.referencev781
monkey -p com.gathin.tvhome.referencev781 -c android.intent.category.LEANBACK_LAUNCHER 1
ls -lh /sdcard/Android/data/com.gathin.tvhome.referencev781/files/
```

The app unpacks the ZIP on startup; Android ZIP contains top-level file paths only. Do not evaluate V7.8.1's fallback UI.

## Visual validation
Preview animation is composited in Python using the same artwork layers and the same alpha curve, not generated with AI. Device cannot be claimed 100% identical until comparing 5-10 seconds of video and an at-rest screenshot from the Mi TV Stick. Do not make V7.8.1 default until its rendering is approved.
