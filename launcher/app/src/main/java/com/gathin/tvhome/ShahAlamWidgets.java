package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, optional information for Shah Alam. No location permission, polling service or
 * WebView. Data is fetched only from Home, on a single background thread, and cached across boots.
 * Prayer zone SGR01 (JAKIM timetable via api.waktusolat.online).
 * Weather: Open-Meteo (model-derived; no API key).
 */
final class ShahAlamWidgets {
    private static final TimeZone MY = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    private static final long WEATHER_MS = 45L * 60_000L;
    private static final long PRAYER_MS = 24L * 60L * 60_000L;
    private static final long RETRY_MS = 30L * 60_000L;
    // A failed prayer fetch must not leave the new experimental package empty for 30 minutes.
    private static final long PRAYER_RETRY_MS = 2L * 60_000L;
    private static final String PRAYER_LOG = "GlassTV.Prayer";
    private static final long MINUTE_MS = 60_000L;
    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=3.0738&longitude=101.5183"
                    + "&current=temperature_2m,weather_code&timezone=Asia%2FKuala_Lumpur";
    private final SharedPreferences cache;
    private final File verifiedPrayerFile;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView temp, condition, prayerName, prayerTime;
    private final TextView[] prayerSlots;
    private final android.graphics.drawable.GradientDrawable activePrayer;
    private static final String[] PRAYER_KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    private static final String[] PRAYER_LABELS = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private CrystalAppearance appearance = new CrystalAppearance();
    private boolean active;
    private boolean destroyed;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active || destroyed) return;
            paintCached();
            refreshIfDue();
            handler.postDelayed(this, MINUTE_MS);
        }
    };

    ShahAlamWidgets(Activity activity) {
        cache = activity.getSharedPreferences("shah_alam_widgets_v1", Activity.MODE_PRIVATE);
        File external = activity.getExternalFilesDir(null);
        verifiedPrayerFile = external == null ? null : new File(external, "prayer_sgr01.json");
        temp = activity.findViewById(R.id.customWeatherTemp);
        condition = activity.findViewById(R.id.customWeatherCondition);
        prayerName = activity.findViewById(R.id.customPrayerName);
        prayerTime = activity.findViewById(R.id.customPrayerTime);
        prayerSlots = new TextView[]{
                activity.findViewById(R.id.prayerFajr),
                activity.findViewById(R.id.prayerDhuhr),
                activity.findViewById(R.id.prayerAsr),
                activity.findViewById(R.id.prayerMaghrib),
                activity.findViewById(R.id.prayerIsha)
        };
        float dp = activity.getResources().getDisplayMetrics().density;
        activePrayer = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xE87AF4F6, 0xE85FDCEB, 0xD87DCDF4});
        activePrayer.setCornerRadius(11 * dp);
        activePrayer.setStroke(Math.max(1, Math.round(.8f * dp)), 0xC7FFFFFF);
        for (TextView slot : prayerSlots) {
            slot.setPadding(0, 5, 0, 5);
        }
    }

    void setAppearance(CrystalAppearance theme) {
        appearance = theme.copy();
        activePrayer.setColor(CrystalAppearance.mix(theme.widgetAccent, 0xFFFFFFFF,
                1f - theme.widgetHighlight / 100f));
        int ink = theme.widgetText;
        temp.setTextColor(ink);
        condition.setTextColor(ink);
        prayerName.setTextColor(ink);
        prayerTime.setTextColor(ink);
        paintCached();
    }

    void resume() {
        active = true;
        handler.removeCallbacks(tick);
        // A verified API JSON may arrive after a previous failed attempt.
        // Pick it up immediately on Home return instead of waiting for retry.
        String month = format("yyyy-MM", new Date());
        if (!month.equals(cache.getString("prayer_month", ""))
                && verifiedPrayerFile != null && verifiedPrayerFile.isFile()) {
            network.execute(() -> {
                if (!destroyed && restoreVerifiedBackup(month)) {
                    handler.post(() -> { if (active) paintCached(); });
                }
            });
        }
        tick.run();
    }

    void pause() {
        active = false;
        handler.removeCallbacks(tick);
    }

    void destroy() {
        pause();
        destroyed = true;
        network.shutdownNow();
    }

    private void refreshIfDue() {
        long now = System.currentTimeMillis();
        String month = format("yyyy-MM", new Date(now));
        boolean weatherDue = now - cache.getLong("weather_fetched", 0) > WEATHER_MS
                && now - cache.getLong("weather_attempt", 0) > RETRY_MS;
        boolean prayerDue = (!month.equals(cache.getString("prayer_month", ""))
                || now - cache.getLong("prayer_fetched", 0) > PRAYER_MS)
                && now - cache.getLong("prayer_attempt", 0) > PRAYER_RETRY_MS;
        if (!weatherDue && !prayerDue) return;
        SharedPreferences.Editor e = cache.edit();
        if (weatherDue) e.putLong("weather_attempt", now);
        if (prayerDue) e.putLong("prayer_attempt", now);
        e.apply();
        network.execute(() -> {
            if (weatherDue && !destroyed) fetchWeather();
            if (prayerDue && !destroyed) {
                // An optional, owner-verified API JSON gives new packages an
                // offline-safe first month even if Android TV HTTPS is blocked.
                // Never replace fresher successfully fetched data.
                restoreVerifiedBackup(month);
                fetchPrayers(month);
            }
            if (!destroyed) handler.post(() -> { if (active) paintCached(); });
        });
    }

    private void fetchWeather() {
        try {
            String json = get(WEATHER_URL);
            JSONObject current = new JSONObject(json).getJSONObject("current");
            double value = current.getDouble("temperature_2m");
            int weatherCode = current.getInt("weather_code");
            if (value < -30 || value > 60) return;
            cache.edit().putString("weather_json", json)
                    .putLong("weather_fetched", System.currentTimeMillis()).apply();
        } catch (Exception ignored) {
            // Keep last known value instead of inventing weather while offline.
        }
    }

    private void fetchPrayers(String month) {
        try {
            String[] ym = month.split("-");
            String url = "https://api.waktusolat.online/api/solat/SGR01?month="
                    + Integer.parseInt(ym[1]) + "&year=" + ym[0];
            Log.i(PRAYER_LOG, "Fetching prayer schedule month=" + month);
            String json = get(url);
            if (!saveVerifiedPrayers(json, month, "HTTPS")) {
                Log.w(PRAYER_LOG, "Prayer response was rejected: missing valid schedule for " + month);
            }
        } catch (Exception error) {
            // Never overwrite previously valid cache with errors or made-up times.
            Log.w(PRAYER_LOG, "Prayer fetch failed: " + error.getClass().getSimpleName()
                    + " " + error.getMessage());
        }
    }

    /**
     * Optional fallback installed by the owner from the SAME live SGR01 API.
     * A new package cannot read another app's private SharedPreferences.
     * The file is not a hardcoded timetable, and is rejected in a new month.
     */
    private boolean restoreVerifiedBackup(String month) {
        if (month.equals(cache.getString("prayer_month", ""))) return false;
        if (verifiedPrayerFile == null || !verifiedPrayerFile.isFile()) return false;
        if (verifiedPrayerFile.length() < 100 || verifiedPrayerFile.length() > 100_000) {
            Log.w(PRAYER_LOG, "External prayer backup outside safe file-size limits");
            return false;
        }
        try (FileInputStream input = new FileInputStream(verifiedPrayerFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            StringBuilder body = new StringBuilder();
            char[] buffer = new char[2048];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                body.append(buffer, 0, count);
                if (body.length() > 100_000) return false;
            }
            return saveVerifiedPrayers(body.toString(), month, "verified external API file");
        } catch (Exception error) {
            Log.w(PRAYER_LOG, "Cannot load verified external prayer file: "
                    + error.getClass().getSimpleName() + " " + error.getMessage());
            return false;
        }
    }

    /** Accept data only for the present Malaysian calendar month and today's valid five times. */
    private boolean saveVerifiedPrayers(String json, String month, String origin) {
        try {
            if (!month.equals(format("yyyy-MM", new Date()))) return false;
            JSONArray items = new JSONObject(json).optJSONArray("prayerTime");
            if (items == null || items.length() < 25) return false;
            String today = format("dd-MMM-yyyy", new Date());
            JSONObject match = null;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null && today.equalsIgnoreCase(item.optString("date"))) {
                    match = item;
                    break;
                }
            }
            if (match == null) return false;
            for (String key : PRAYER_KEYS) {
                if (shortTime(match.optString(key)) == null) return false;
            }
            cache.edit().putString("prayer_json", json).putString("prayer_month", month)
                    .putLong("prayer_fetched", System.currentTimeMillis()).apply();
            Log.i(PRAYER_LOG, "Prayer schedule ready origin=" + origin + " month=" + month
                    + " entries=" + items.length() + " today=" + today);
            return true;
        } catch (Exception error) {
            Log.w(PRAYER_LOG, "Invalid prayer response from " + origin + ": "
                    + error.getClass().getSimpleName());
            return false;
        }
    }

    private static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(6000);
        c.setReadTimeout(6000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "GlassTVCustomV1/1.0");
        try {
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            try (InputStream stream = c.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                StringBuilder out = new StringBuilder();
                char[] chars = new char[2048];
                int n;
                while ((n = reader.read(chars)) >= 0) {
                    out.append(chars, 0, n);
                    if (out.length() > 100_000) throw new IllegalStateException("response too large");
                }
                return out.toString();
            }
        } finally {
            c.disconnect();
        }
    }

    private void paintCached() {
        try {
            JSONObject current = new JSONObject(cache.getString("weather_json", "{}"))
                    .optJSONObject("current");
            if (current != null && current.has("temperature_2m")) {
                temp.setText(String.format(Locale.US, "%.0f°C", current.getDouble("temperature_2m")));
                condition.setText(weatherDescription(current.optInt("weather_code", -1)));
            } else {
                temp.setText("--°C");
                condition.setText("Cuaca belum tersedia");
            }
        } catch (Exception e) {
            temp.setText("--°C");
            condition.setText("Cuaca belum tersedia");
        }

        prayerName.setText("Prayer Times");
        prayerTime.setText("");
        for (int i = 0; i < prayerSlots.length; i++) {
            prayerSlots[i].setText(PRAYER_LABELS[i] + "\n--:--");
            prayerSlots[i].setTextColor(appearance.widgetText);
            prayerSlots[i].setBackground(null);
        }
        try {
            String month = format("yyyy-MM", new Date());
            if (!month.equals(cache.getString("prayer_month", ""))) {
                prayerName.setText("Schedule unavailable");
                return;
            }
            JSONArray times = new JSONObject(cache.getString("prayer_json", "{}")).optJSONArray("prayerTime");
            if (times == null) {
                prayerName.setText("Schedule unavailable");
                return;
            }
            String date = format("dd-MMM-yyyy", new Date());
            JSONObject entry = null;
            for (int i = 0; i < times.length(); i++) {
                JSONObject one = times.optJSONObject(i);
                if (one != null && date.equalsIgnoreCase(one.optString("date"))) {
                    entry = one;
                    break;
                }
            }
            if (entry == null) {
                prayerName.setText("Schedule unavailable");
                return;
            }
            Calendar now = Calendar.getInstance(MY);
            int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            // Highlight the prayer period we're currently in, not an expired upcoming time.
            // Before Subuh, focus Subuh as the next time; after Isyak, keep Isyak highlighted.
            int current = -1;
            int upcoming = -1;
            for (int i = 0; i < PRAYER_KEYS.length; i++) {
                String time = shortTime(entry.optString(PRAYER_KEYS[i]));
                if (time == null) continue;
                prayerSlots[i].setText(PRAYER_LABELS[i] + "\n" + time);
                int at = Integer.parseInt(time.substring(0, 2)) * 60
                        + Integer.parseInt(time.substring(3, 5));
                if (minute >= at) current = i;
                else if (upcoming < 0) upcoming = i;
            }
            int selected = current >= 0 ? current : upcoming;
            if (selected >= 0) {
                prayerSlots[selected].setBackground(activePrayer);
                prayerSlots[selected].setTextColor(appearance.widgetText);
            }
        } catch (Exception e) {
            prayerName.setText("Schedule unavailable");
        }
    }

    private static String shortTime(String s) {
        if (s.length() >= 5 && Character.isDigit(s.charAt(0))
                && Character.isDigit(s.charAt(1)) && s.charAt(2) == ':'
                && Character.isDigit(s.charAt(3)) && Character.isDigit(s.charAt(4)))
            return s.substring(0, 5);
        return null;
    }

    private static String format(String pattern, Date time) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.ENGLISH);
        f.setTimeZone(MY);
        return f.format(time);
    }

    private static String weatherDescription(int code) {
        if (code == 0) return "Clear · Shah Alam";
        if (code <= 3 && code > 0) return "Cloudy · Shah Alam";
        if (code == 45 || code == 48) return "Foggy · Shah Alam";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82))
            return "Rain · Shah Alam";
        if (code >= 95) return "Storm · Shah Alam";
        return "Shah Alam";
    }
}
