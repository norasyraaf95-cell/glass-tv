package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
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
    private static final long MINUTE_MS = 60_000L;
    private static final String WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=3.0738&longitude=101.5183"
                    + "&current=temperature_2m,weather_code&timezone=Asia%2FKuala_Lumpur";
    private final SharedPreferences cache;
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TextView temp, condition, prayerName, prayerTime;
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
        temp = activity.findViewById(R.id.customWeatherTemp);
        condition = activity.findViewById(R.id.customWeatherCondition);
        prayerName = activity.findViewById(R.id.customPrayerName);
        prayerTime = activity.findViewById(R.id.customPrayerTime);
    }

    void resume() {
        active = true;
        handler.removeCallbacks(tick);
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
                && now - cache.getLong("prayer_attempt", 0) > RETRY_MS;
        if (!weatherDue && !prayerDue) return;
        SharedPreferences.Editor e = cache.edit();
        if (weatherDue) e.putLong("weather_attempt", now);
        if (prayerDue) e.putLong("prayer_attempt", now);
        e.apply();
        network.execute(() -> {
            if (weatherDue && !destroyed) fetchWeather();
            if (prayerDue && !destroyed) fetchPrayers(month);
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
            String json = get(url);
            JSONObject obj = new JSONObject(json);
            JSONArray times = obj.optJSONArray("prayerTime");
            if (times == null || times.length() < 25) return;
            cache.edit().putString("prayer_json", json).putString("prayer_month", month)
                    .putLong("prayer_fetched", System.currentTimeMillis()).apply();
        } catch (Exception ignored) {
            // Keep last known timetable; never substitute calculated or example times.
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

        prayerName.setText("Waktu Solat · SGR01");
        prayerTime.setText("Memuatkan jadual...");
        try {
            String month = format("yyyy-MM", new Date());
            if (!month.equals(cache.getString("prayer_month", ""))) {
                prayerTime.setText("Jadual belum tersedia");
                return;
            }
            JSONArray times = new JSONObject(cache.getString("prayer_json", "{}")).optJSONArray("prayerTime");
            if (times == null) {
                prayerTime.setText("Jadual belum tersedia");
                return;
            }
            Date today = new Date();
            String date = format("dd-MMM-yyyy", today);
            JSONObject entry = null;
            for (int i = 0; i < times.length(); i++) {
                JSONObject one = times.optJSONObject(i);
                if (one != null && date.equalsIgnoreCase(one.optString("date"))) {
                    entry = one;
                    break;
                }
            }
            if (entry == null) {
                prayerTime.setText("Jadual belum tersedia");
                return;
            }
            String[] keys = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
            String[] names = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
            Calendar now = Calendar.getInstance(MY);
            int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            for (int i = 0; i < keys.length; i++) {
                String time = shortTime(entry.optString(keys[i]));
                if (time == null) continue;
                int at = Integer.parseInt(time.substring(0, 2)) * 60
                        + Integer.parseInt(time.substring(3, 5));
                if (minute < at) {
                    prayerName.setText("Solat seterusnya · Shah Alam");
                    prayerTime.setText(names[i] + "  " + time);
                    return;
                }
            }
            // After Isyak show a truthful status rather than today's Subuh as a future time.
            prayerName.setText("Waktu Solat · Shah Alam");
            prayerTime.setText("Selesai untuk hari ini");
        } catch (Exception e) {
            prayerTime.setText("Jadual belum tersedia");
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
        if (code == 0) return "Cerah · Shah Alam";
        if (code <= 3 && code > 0) return "Berawan · Shah Alam";
        if (code == 45 || code == 48) return "Berkabus · Shah Alam";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82))
            return "Hujan · Shah Alam";
        if (code >= 95) return "Ribut · Shah Alam";
        return "Shah Alam";
    }
}
