# Glass TV V7.7 — Reference Match (NOT YET DEVICE-APPROVED)

## Frozen references
- Final static reference: `GlassTV_V77_GlassA_on_OriginalWallpaperB_1536x864.png` (approved in ChatGPT).
- Original unmodified wallpaper: `v75_wallpaper.jpg` (1536x864, from the user's V7.5 device).
- Earlier V7.5 card atlas: `locked_tiles_v75.webp` (1500x152, reference only; do not stretch it).
- Motion reference: `Glass_TV_V76_Soft_Crossfade_Alternative_720p.mp4` (Soft Light Handoff).
- DO NOT change positioning of clock, prayer, weather, wireless/settings icons, or the wallpaper crop.

## V7.7 technical approach
- Build from the known-good V7.5 branch, not the faulty V7.6 implementation.
- `LiquidCrystalDockDrawable` owns only the 5 static dark-smoked glass materials and focus illumination.
- Logos are existing native `VectorDrawable` resources. Wordmarks are white native `TextView` text; Prime has its cyan vector smile.
- Exactly **one** reusable animator per card for alpha-only focus illumination: **240ms**.
- All glass dimensions, logo positions, font size and the wallpaper remain stationary during focus handoff.
- No extra `focusAura` view, no ellipse outline, no travelling rim, no bounce, no moving shadow, no card zoom.
- `DockDecoration` draws a very faint fixed ambient rail from the approved static image; it does not track focus.
- The existing `Glider` is hidden on the dock and retained only for existing grid/menu navigation.
- Dock background ambient tint is constant on app-to-app navigation.
- English text, but prayer times/weather remain dynamic and must never be hardcoded to the mockup's time.

## Install-side assets / DO NOT skip
Every preview APK is installed as a separate package. Android app-private storage is NOT shared.
Copy only user-approved original files from the installed V7.5 to the new V7.7 folder with Bugjaeger shell AFTER installing V7.7:

```sh
cp /sdcard/Android/data/com.gathin.tvhome.referencev75/files/wallpaper.jpg /sdcard/Android/data/com.gathin.tvhome.referencev77/files/wallpaper.jpg
cp /sdcard/Android/data/com.gathin.tvhome.referencev75/files/windy_clouds.mp4 /sdcard/Android/data/com.gathin.tvhome.referencev77/files/windy_clouds.mp4
cp /sdcard/Android/data/com.gathin.tvhome.referencev75/files/config.json /sdcard/Android/data/com.gathin.tvhome.referencev77/files/config.json
am force-stop com.gathin.tvhome.referencev77
am start -n com.gathin.tvhome.referencev77/com.gathin.tvhome.MainActivity
```

Do not copy `locked_tiles_v75.webp`: V7.7 intentionally uses independent native material and sharp brand marks.

## Release gates (all required)
1. GitHub CI must compile and release APK successfully.
2. Verify correct original wallpaper on the physical TV, not a dark fallback.
3. Compare static focused states (YouTube, Netflix, Prime, Spotify, Apps) on device to frozen static image; adjust shader values/layout if needed.
4. Compare DPAD transitions to the locked video, including three rapid taps and rapid left-right reversal.
5. Check zero bubble, white circle, double line, black shadow, text/logo scale or clip.
6. Confirm YouTube-looking tile opens `org.smarttube.stable`.
7. Only after approval: consider making V7.7 default. NEVER delete or update V7.5 during development.

**Status:** Code implementation prepared; NOT visually confirmed on Android TV. CI build success alone is not pixel-perfect validation.
