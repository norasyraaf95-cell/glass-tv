# Glass TV — Floating Prism Top Bar / Date Option 03

## Approved spec
- Original FINAL user app: `com.gathin.tvhome.referencev781`. **Do not alter or replace this default launcher**.
- Test application: `com.gathin.tvhome.referencev781top`. Built side by side.
- No changes to 5 dock app tiles, wallpaper, focus/glow handoff, reflection, logo art, SmartTube mapping, top widget positions, widget text, data source or interaction.
- Date above clock: `Thursday  |  19 Nov` as **live English** Malaysia date, updated with existing minuteTick and onResume.
- Floating Prism applied to **both widget backgrounds ONLY** using independent translucent pre-rendered WEBP assets.
- All native Prayer Times, weather, icons, active prayer highlight and clock remain live/unmodified.

## Skin files
- `floating_prism_prayer.webp` — 1185×165 RGBA, scaled into existing 395dp×55dp capsule.
- `floating_prism_weather.webp` — 462×165 RGBA, scaled into existing 154dp×55dp capsule.
- Stored in `/sdcard/Android/data/com.gathin.tvhome.referencev781top/files/`.
- `FloatingPrismTopBarDrawable` loads these at app startup. If absent/invalid, retains existing `PremiumGlassDrawable` as fallback.
- No runtime image generation, no moving border/ellipse, no view elevation, no blur every frame.
- ZIP pack `GlassTV_V781_Floating_Prism_TopBar_Assets.zip` also contains the original, unchanged V7.8.1 wallpaper/config/dock art.

## Bugjaeger (after installing test APK)
Use Bugjaeger Files to push the complete asset ZIP from phone to TV `/sdcard/Download/`; confirm with `ls -lh`.
```sh
mkdir -p /sdcard/Android/data/com.gathin.tvhome.referencev781top/files
cp /sdcard/Download/GlassTV_V781_Floating_Prism_TopBar_Assets.zip /sdcard/Android/data/com.gathin.tvhome.referencev781top/files/v781_asset_pack.zip
am force-stop com.gathin.tvhome.referencev781top
monkey -p com.gathin.tvhome.referencev781top -c android.intent.category.LEANBACK_LAUNCHER 1
ls -lh /sdcard/Android/data/com.gathin.tvhome.referencev781top/files/
```

## Future no-rebuild skin swap
Overwrite `floating_prism_prayer.webp` and/or `floating_prism_weather.webp` directly with Bugjaeger in that same folder and restart the test launcher. The archive installer has an install marker and never overwrites artwork on subsequent starts; to change widget material, edit those two extracted WebP files, **not** the ZIP. They must contain a transparent background and represent material without any prayer/weather typography. Runtime text remains native for sharpness and updates.

## Release gate
Build success is not visual validation. Match the approved TV screenshot in a device screencap; verify date/calendar update, clocks, five dock focus transitions and live widgets. Only when approved switch HOME. Installed V7.8.1 is untouched.
