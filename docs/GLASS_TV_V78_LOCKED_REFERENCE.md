# Glass TV V7.8 — Locked Reference / Soft Light Handoff

## Frozen source
- User-approved static design: `1000219457.png` (1536 × 864).
- User original wallpaper (not regenerated): `v75_wallpaper.jpg`.
- User-approved motion preview: `Glass_TV_V76_Soft_Crossfade_Alternative_720p.mp4` (60fps).
- This branch is sourced from V7.7 only as a code foundation, not its visual artwork.

## Different from failed V7.6 / V7.7
- No procedural oval focus, no radial bubbles, no moving line, no scrolling rectangle,
  no card scale or elevation animation.
- `DockArtwork` decodes a supplied atlas of 5 NORMAL and 5 FOCUSED pixel-derived glass cards.
  Logo glyphs, glass depth, refractions and highlights come from the SAME LOCKED IMAGE.
- DPAD focus calls TWO independent 240ms hardware-view alpha fades on *stationary* layers.
  Rapid direction reversals continue from current opacity.
- One static reflection texture drawn at bottom, under cards; no duplicate rail.
- Ambient full-screen tint is disabled while navigating the five dock entries.
- Wallpaper and config are supplied in the same ZIP, unpacked to this package's own
  app-specific files BEFORE `Config.load`. This avoids the black/blank wallpaper regression.

## Asset pack
Filename on Mi TV Stick:
`/sdcard/Android/data/com.gathin.tvhome.referencev78/files/v78_asset_pack.zip`

Its contents:
`wallpaper.jpg`, `config.json`, `liquid_dock_base.webp`,
`liquid_dock_focused.webp`, `liquid_focus_glow.webp`,
`liquid_static_reflections.webp`.

The user has the zip in this ChatGPT conversation as:
`GlassTV_V78_Approved_Asset_Pack.zip`.

The ZIP must be copied to the TV once through Bugjaeger. Loading it does not
require special Android storage permissions because it lives inside the new
package's own external files dir. Without the ZIP, V7.8 shows a fallback glass
material, *not* the final locked reference. Do not evaluate fallback appearance!

## Bugjaeger install and test
1. Install APK for `com.gathin.tvhome.referencev78` while V7.7 remains default.
2. In Bugjaeger Files, push the ZIP from your phone into TV `/sdcard/Download/`.
3. In Interactive Shell run:
```sh
mkdir -p /sdcard/Android/data/com.gathin.tvhome.referencev78/files
cp /sdcard/Download/GlassTV_V78_Approved_Asset_Pack.zip /sdcard/Android/data/com.gathin.tvhome.referencev78/files/v78_asset_pack.zip
cp /sdcard/Android/data/com.gathin.tvhome.referencev77/files/windy_clouds.mp4 /sdcard/Android/data/com.gathin.tvhome.referencev78/files/windy_clouds.mp4
am force-stop com.gathin.tvhome.referencev78
am start -n com.gathin.tvhome.referencev78/com.gathin.tvhome.MainActivity
```
4. If the cloud file is missing, that copy may fail but the dock/wallpaper still load.

## Release gate
- Passing GitHub CI proves compile/tests only.
- Need TV screenshot for static dock + wallpaper; video of left/right across all 5 tiles,
  including rapid 3x right and reverse.
- Compare against the exact stored reference; tune if necessary.
- Do NOT change the default HOME from working V7.7 until user approves.
