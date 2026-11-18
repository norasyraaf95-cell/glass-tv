package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

/**
 * Glass TV V7 cinematic screensaver. Unlike the previous analog dream, this uses the chosen
 * pink-purple mountain wallpaper and a bold, live digital clock with a subdued date. The background
 * is STATIC while dreaming, so the 1 GB TV can release the Windy Clouds video decoder.
 *
 * There are no network requests, video playback, per-frame effects or live blur. The only timer runs
 * once per minute to keep the Malay date fresh and lightly shift the clock against image retention.
 * If the wallpaper hasn't been copied to V7's storage, a purple gradient is shown as fallback.
 */
public final class ClockDream extends DreamService {
    private static final String ZONE = "Asia/Kuala_Lumpur";
    private static final long UPDATE_MS = 60_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private Bitmap background;
    private FrameLayout scene;
    private LinearLayout clockGroup;
    private TextView date;
    private boolean dreaming;

    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!dreaming) return;
            updateDate();
            if (clockGroup != null) {
                // Subtle repositioning every minute instead of a permanently fixed OLED/LED imprint.
                float d = getResources().getDisplayMetrics().density;
                clockGroup.animate().translationX((random.nextFloat() - .5f) * 26f * d)
                        .translationY((random.nextFloat() - .5f) * 15f * d)
                        .setDuration(1100L).start();
            }
            handler.postDelayed(this, UPDATE_MS);
        }
    };

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);

        float d = getResources().getDisplayMetrics().density;
        scene = new FrameLayout(this);
        scene.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF37204C, 0xFF6E3C75, 0xFF201F43}));

        ImageView picture = new ImageView(this);
        picture.setScaleType(ImageView.ScaleType.CENTER_CROP);
        picture.setAdjustViewBounds(false);
        background = readWallpaper();
        if (background != null) picture.setImageBitmap(background);
        scene.addView(picture, new FrameLayout.LayoutParams(-1, -1));

        // Dim the static landscape enough for the digital time to remain legible in a dark room.
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x7A100E27, 0x3A21142E, 0x4E140D29}));
        scene.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        clockGroup = new LinearLayout(this);
        clockGroup.setGravity(Gravity.START);
        clockGroup.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(-2, -2,
                Gravity.START | Gravity.CENTER_VERTICAL);
        position.leftMargin = Math.round(77f * d);
        position.bottomMargin = Math.round(28f * d);
        scene.addView(clockGroup, position);

        date = new TextView(this);
        date.setTextColor(0xEDFFFFFF);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        date.setFontFeatureSettings("kern");
        updateDate();
        clockGroup.addView(date, new LinearLayout.LayoutParams(-2, -2));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.BOTTOM);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-2, -2);
        rowLp.topMargin = Math.round(6f * d);
        clockGroup.addView(timeRow, rowLp);

        TextClock time = new TextClock(this);
        time.setTimeZone(ZONE);
        time.setFormat12Hour("hh:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextColor(Color.WHITE);
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100);
        time.setTypeface(android.graphics.Typeface.create(
                "sans-serif", android.graphics.Typeface.BOLD));
        time.setLetterSpacing(-.055f);
        time.setIncludeFontPadding(false);
        timeRow.addView(time, new LinearLayout.LayoutParams(-2, -2));

        TextClock period = new TextClock(this);
        period.setTimeZone(ZONE);
        period.setFormat12Hour("a");
        period.setFormat24Hour("");
        period.setTextColor(0xE8FFFFFF);
        period.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        period.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ampm = new LinearLayout.LayoutParams(-2, -2);
        ampm.leftMargin = Math.round(13f * d);
        ampm.bottomMargin = Math.round(19f * d);
        timeRow.addView(period, ampm);

        View line = new View(this);
        line.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00FFABD7, 0xB0FF9CD8, 0x00FFABD7}));
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                Math.round(290f * d), Math.max(1, Math.round(2f * d)));
        lineLp.topMargin = Math.round(15f * d);
        clockGroup.addView(line, lineLp);

        TextView footer = new TextView(this);
        footer.setText("GLASS TV  /  SHAH ALAM");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        footer.setLetterSpacing(.14f);
        footer.setTextColor(0xA6FFFFFF);
        FrameLayout.LayoutParams footLp = new FrameLayout.LayoutParams(-2, -2,
                Gravity.START | Gravity.BOTTOM);
        footLp.leftMargin = Math.round(77f * d);
        footLp.bottomMargin = Math.round(40f * d);
        scene.addView(footer, footLp);

        setContentView(scene);
    }

    private void updateDate() {
        if (date == null) return;
        SimpleDateFormat format = new SimpleDateFormat("EEEE   |   dd MMM yyyy", new Locale("ms", "MY"));
        format.setTimeZone(TimeZone.getTimeZone(ZONE));
        String value = format.format(new Date());
        SpannableString styled = new SpannableString(value);
        int end = value.indexOf("   |");
        if (end > 0) {
            styled.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
                    0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        date.setText(styled);
    }

    /** Decode at no more than ~1280x720 so a 4K wallpaper doesn't exhaust the TV's RAM. */
    private Bitmap readWallpaper() {
        try {
            File root = getExternalFilesDir(null);
            if (root == null) return null;
            File file = new File(root, "wallpaper.jpg");
            if (!file.isFile()) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return null;
            int sample = 1;
            while ((opts.outWidth / sample) > 1280 || (opts.outHeight / sample) > 720) {
                sample *= 2;
            }
            opts.inSampleSize = sample;
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        dreaming = true;
        if (scene != null) {
            scene.setAlpha(0f);
            scene.animate().alpha(1f).setDuration(450L).start();
        }
        handler.removeCallbacks(update);
        handler.post(update);
    }

    @Override public void onDreamingStopped() {
        dreaming = false;
        handler.removeCallbacks(update);
        if (clockGroup != null) clockGroup.animate().cancel();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        dreaming = false;
        handler.removeCallbacks(update);
        if (clockGroup != null) clockGroup.animate().cancel();
        if (scene != null) {
            scene.animate().cancel();
            scene.removeAllViews();
        }
        if (background != null && !background.isRecycled()) {
            background.recycle();
            background = null;
        }
        super.onDetachedFromWindow();
    }
}
