package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.HttpsURLConnection;

/** Non-focusable, display-only bar. No service, location permission, alarm or notification. */
public final class PrayerBar extends LinearLayout {
    private static final String TAG = "TvHome.Prayer";
    private static final String API = "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=SGR03";
    private static final String[] NAMES = {"Subuh", "Zohor", "Asar", "Maghrib", "Isyak"};
    private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    private static final int GOLD = 0xFFE4C883;
    private static final int MUTED = 0xFFDDDDDD;
    private static final int WHITE = 0xFFEEEEEE;
    private static final long RETRY_MS = 30 * 60 * 1000L;
    private static final int LIMIT = 128 * 1024;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final SharedPreferences prefs;
    private final TextView[] labels = new TextView[5];
    private final TextView[] values = new TextView[5];
    private final android.view.View[] lines = new android.view.View[5];
    private final TextView status;
    private Map<LocalDate, LocalTime[]> schedule = new HashMap<>();
    private LocalDate lastSuccess;
    private LocalDate attemptedDate;
    private long lastAttempt;
    private boolean active;
    private boolean cacheLoaded;
    private Future<?> task;
    private int generation;
    private volatile HttpsURLConnection connection;

    public PrayerBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = context.getSharedPreferences("prayer_sgr03", Context.MODE_PRIVATE);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setPadding(dp(12), dp(5), dp(12), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xA02B3342);
        bg.setStroke(dp(1), 0x40FFFFFF);
        bg.setCornerRadius(dp(15));
        setBackground(bg);
        LinearLayout info = column();
        addView(info, new LayoutParams(dp(82), LayoutParams.WRAP_CONTENT));
        TextView title = text("Waktu Solat", 11, MUTED);
        info.addView(title);
        status = text("SGR03", 10, MUTED);
        info.addView(status);
        for (int i = 0; i < 5; i++) {
            LinearLayout cell = column();
            addView(cell, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            labels[i] = text(NAMES[i], 12, MUTED);
            values[i] = text("--:--", 16, WHITE);
            cell.addView(labels[i]);
            cell.addView(values[i]);
            lines[i] = new android.view.View(context);
            LayoutParams lp = new LayoutParams(dp(22), dp(2));
            lp.topMargin = dp(2);
            cell.addView(lines[i], lp);
        }
    }

    private int dp(float n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setFocusable(false);
        return layout;
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(getContext());
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setIncludeFontPadding(false);
        v.setFocusable(false);
        return v;
    }

    void resume() {
        if (active) return;
        active = true;
        tick.run();
    }

    void pause() {
        active = false;
        generation++;
        handler.removeCallbacks(tick);
        if (task != null) { task.cancel(true); task = null; }
        HttpsURLConnection c = connection;
        if (c != null) c.disconnect();
    }

    void close() {
        pause();
        worker.shutdownNow();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            ZonedDateTime now = ZonedDateTime.now(PrayerSchedule.MALAYSIA);
            LocalDate today = now.toLocalDate();
            render(today, now.toLocalTime());
            if (task == null && (!cacheLoaded || !today.equals(lastSuccess))
                    && (!today.equals(attemptedDate) || SystemClock.elapsedRealtime() - lastAttempt >= RETRY_MS)) {
                refresh(today);
            }
            handler.postDelayed(this, 15000L);
        }
    };

    private void render(LocalDate day, LocalTime now) {
        LocalTime[] times = schedule.get(day);
        int next = times == null ? -1 : PrayerSchedule.next(times, now);
        status.setText(times == null ? "Tiada data" : next < 0 ? "Subuh esok" : "SGR03");
        status.setContentDescription("Zon SGR03, " + status.getText());
        for (int i = 0; i < 5; i++) {
            boolean highlight = i == next;
            labels[i].setTextColor(highlight ? GOLD : MUTED);
            values[i].setTextColor(highlight ? GOLD : WHITE);
            values[i].setText(times == null ? "--:--" : PrayerSchedule.display(times[i]));
            lines[i].setBackgroundColor(highlight ? GOLD : Color.TRANSPARENT);
        }
    }

    private void refresh(LocalDate day) {
        attemptedDate = day;
        lastAttempt = SystemClock.elapsedRealtime();
        final int token = generation;
        task = worker.submit(() -> {
            Map<LocalDate, LocalTime[]> data = new HashMap<>();
            LocalDate success = null;
            try {
                String cached = prefs.getString("schedule", "");
                if (!cached.isEmpty()) data = parse(cached);
                String updated = prefs.getString("updated", "");
                if (!updated.isEmpty()) success = LocalDate.parse(updated);
            } catch (Exception e) {
                Log.w(TAG, "Cached prayer schedule unavailable", e);
            }
            final Map<LocalDate, LocalTime[]> cachedData = data;
            final LocalDate cachedSuccess = success;
            handler.post(() -> {
                if (!active || token != generation) return;
                cacheLoaded = true;
                schedule = cachedData;
                lastSuccess = cachedSuccess;
                ZonedDateTime now = ZonedDateTime.now(PrayerSchedule.MALAYSIA);
                render(now.toLocalDate(), now.toLocalTime());
            });
            try {
                if (!day.equals(success) || !data.containsKey(day)) {
                    String raw = download();
                    Map<LocalDate, LocalTime[]> updated = parse(raw);
                    if (!updated.containsKey(day)) throw new IllegalArgumentException("Missing today's SGR03 times");
                    if (Thread.currentThread().isInterrupted()) return;
                    prefs.edit().putString("schedule", raw).putString("updated", day.toString()).apply();
                    data = updated;
                    success = day;
                }
            } catch (Exception e) {
                Log.w(TAG, "JAKIM unavailable; keeping date-matched offline data", e);
            } finally {
                final Map<LocalDate, LocalTime[]> result = data;
                final LocalDate resultSuccess = success;
                handler.post(() -> {
                    if (!active || token != generation) return;
                    task = null;
                    cacheLoaded = true;
                    schedule = result;
                    lastSuccess = result.containsKey(day) ? resultSuccess : null;
                    ZonedDateTime now = ZonedDateTime.now(PrayerSchedule.MALAYSIA);
                    render(now.toLocalDate(), now.toLocalTime());
                });
            }
        });
    }

    private String download() throws Exception {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        HttpsURLConnection c = (HttpsURLConnection) new URL(API).openConnection();
        connection = c;
        try {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent", "GlassTV-Parents/2.3");
            if (c.getResponseCode() != 200) throw new IllegalStateException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    if (out.size() + n > LIMIT) throw new IllegalStateException("Schedule too large");
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
            connection = null;
        }
    }

    private static Map<LocalDate, LocalTime[]> parse(String raw) throws Exception {
        if (raw.length() > LIMIT) throw new IllegalArgumentException("Schedule too large");
        JSONObject json = new JSONObject(raw);
        if (!PrayerSchedule.ZONE.equalsIgnoreCase(json.optString("zone")))
            throw new IllegalArgumentException("Wrong zone");
        JSONArray rows = json.getJSONArray("prayerTime");
        if (rows.length() == 0 || rows.length() > 31)
            throw new IllegalArgumentException("Unexpected month size");
        Map<LocalDate, LocalTime[]> data = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            LocalDate day = PrayerSchedule.parseDate(row.getString("date"));
            LocalTime[] times = new LocalTime[5];
            for (int k = 0; k < times.length; k++) times[k] = PrayerSchedule.parseTime(row.getString(KEYS[k]));
            if (!PrayerSchedule.ordered(times) || data.put(day, times) != null)
                throw new IllegalArgumentException("Invalid or duplicate prayer day");
        }
        return data;
    }
}
