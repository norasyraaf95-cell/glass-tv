# Crystal Clear V2 frosted preview (manual TV test only)

Based on the **identical V8.5.1 Immediate** source, with the installed launcher untouched.

## Safety
- Preview package: `com.gathin.tvhome.crystalclearv2`, version 37.
- Installed original remains `com.gathin.tvhome.referencev781v851immediate`.
- This preview is intentionally **not** registered as a HOME app. Launch it from the Android TV apps list / Bugjaeger.
- No package removal, debloat, or changes to existing applications.
- Never copy the old `v781_asset_pack.zip` or purple `*.webp` dock assets into the preview.
- Use the owner's existing alpine lake wallpaper, not a generated approximation.
- Keep a separate phone copy of the original TV files backup before testing.

## Design
- White/navy prayer and weather information cards with cached wallpaper-aligned blur.
- Icy cyan active-prayer pill; never hardcode times or weather data.
- Native frosted glass tiles with a single continuous floating tray, no purple V7.8 bitmap override.
- White clock with cyan underline, same app links, focus navigation, and SmartTube immediate launch logic.
- Weather displays live readings but has no button action; windy video is off on the preview.
- Original V8.5.1 Satin directional glint and between-card bridge restored on the native glass dock.
- Five dock titles all white (including Prime Video); textured wallpaper remains visible through lighter glass.

## Owner's wallpaper and config (after preview is installed)
The preview app has its OWN external-files directory:
`/sdcard/Android/data/com.gathin.tvhome.crystalclearv2/files/`

Use Bugjaeger to copy the owner-approved optimized wallpaper named `wallpaper.jpg`
and the existing `config.json` into the PREVIEW directory only. Preserve the original TV directory:
`/sdcard/Android/data/com.gathin.tvhome.referencev781v851immediate/files/`

Do not replace the original wallpaper or move other apps. The configured wallpaper and
settings are loaded on the next start of the preview. If both copies are not present,
the preview should not be treated as a finished visual comparison.

## Validation gates
1. GitHub Actions `Glass TV Crystal Clear frosted preview` job finishes successfully.
2. Verify distinct package from `aapt dump badging` in CI, plus APK checksum.
3. Test preview **without selecting it as default Home**; first check clock, prayer/weather API data, cloud toggle, apps.
4. Verify optimized *exact owner photo* and visual crop on the physical MiTV-AESP0.
5. Check all five dock focus states, rapid navigation and immediate SmartTube launch.
6. Compare physical TV screenshot with the owner's approved Crystal Clear mockup and adjust the palette/placement; static mockup values are not live data.
7. Owner explicitly approves before any default-Home changes.

Original backup: branch `backup-before-crystal-clear-20261123`.
