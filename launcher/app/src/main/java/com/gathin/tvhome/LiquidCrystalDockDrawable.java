package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;


import androidx.annotation.NonNull;

/**
 * V7.7 Liquid Crystal dock material.
 *
 * Static geometry and brand marks stay completely still. On DPAD focus, only
 * focusFraction changes (a 240ms in-place light crossfade). There is NO travelling
 * frame, moving ellipse, changing tile scale, animated line, or elevation shadow.
 * No bitmaps are allocated, decoded or blurred in onDraw.
 */
final class LiquidCrystalDockDrawable extends Drawable {
    static final long HANDOFF_MS = MagneticSpotlightView.TRANSITION_MS;

    private final View host;
    private final float density;
    private final Backdrop.Fill wallpaperGlass = new Backdrop.Fill();

    private final RectF frame = new RectF();
    private final RectF inset = new RectF();
    private final Path topLens = new Path();
    private final Path lowerCurve = new Path();
    private final Matrix matrix = new Matrix();

    private final Paint smoke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topReflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lens = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bottomSheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusWash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint focusEdge = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ValueAnimator focusFade = ValueAnimator.ofFloat(0f, 1f);
    private float focusFraction;
    private float opacity = 1f;
    private CrystalAppearance appearance = new CrystalAppearance();
    private final Paint tintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int brand = -1;

    LiquidCrystalDockDrawable(View host) {
        this.host = host;
        density = host.getResources().getDisplayMetrics().density;

        // Crystal Clear V2: optically thin white/silver tint keeps the meadow visible through glass.
        smoke.setShader(new LinearGradient(0, 0, 0, 1,
                new int[]{0x54F4FFFE, 0x89D9EFF2, 0x85A7D1D7},
                new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP));
        topReflection.setShader(new LinearGradient(0, 0, 0, 1,
                new int[]{0xCBFFFFFF, 0x49FFFFFF, 0x00FFFFFF},
                new float[]{0f, .30f, .80f}, Shader.TileMode.CLAMP));
        lens.setShader(new LinearGradient(0, 0, 0, 1,
                new int[]{0x98FFFFFF, 0x3FA9F4FF, 0x00FFFFFF},
                new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP));
        focusWash.setShader(new RadialGradient(.5f, .66f, .9f,
                new int[]{0x92AEF9FF, 0x4A83E8F9, 0x00CEFAFF},
                new float[]{0f, .53f, 1f}, Shader.TileMode.CLAMP));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.35f * density);
        focusEdge.setStyle(Paint.Style.STROKE);
        focusEdge.setStrokeWidth(1.55f * density);
        bottomSheen.setStyle(Paint.Style.STROKE);
        bottomSheen.setStrokeWidth(1.65f * density);
        bottomSheen.setStrokeCap(Paint.Cap.ROUND);
        setBrand(0);

        focusFade.setDuration(HANDOFF_MS);
        // Short, controllable easing, with no bounce or spring overshoot.
        // Match video Option B precisely: 470ms quintic smootherstep, no bounce.
        focusFade.setInterpolator(input -> {
            float t = Math.max(0f, Math.min(1f, input));
            return t * t * t * (t * (t * 6f - 15f) + 10f);
        });
        focusFade.addUpdateListener(animation -> {
            focusFraction = (float) animation.getAnimatedValue();
            invalidateSelf();
        });
    }

    void setAppearance(CrystalAppearance value) {
        appearance = value.copy();
        tintPaint.setColor(appearance.dockTint);
        int tint = appearance.spotlightTint;
        // Keep the subtle alpha envelope of the locked reference: only hue changes.
        int rgb = tint & 0x00FFFFFF;
        focusWash.setShader(new RadialGradient(.5f,.66f,.9f,
                new int[]{(0x92 << 24) | rgb, (0x4A << 24) | rgb, rgb},
                new float[]{0f,.53f,1f},Shader.TileMode.CLAMP));
        focusEdge.setShader(new LinearGradient(0f,0f,1f,1f,
                new int[]{0xF8FFFFFF, tint, 0xEEFFFFFF},
                null,Shader.TileMode.CLAMP));
        int ms = "instant".equals(appearance.motion) ? 0 : appearance.duration;
        focusFade.setDuration(ms);
        if ("linear".equals(appearance.easing)) focusFade.setInterpolator(i->i);
        else if ("soft".equals(appearance.easing))
            focusFade.setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f));
        else focusFade.setInterpolator(i->{float t=Math.max(0f,Math.min(1f,i));return t*t*t*(t*(t*6f-15f)+10f);});
        invalidateSelf();
    }

    void setBrand(int value) {
        if (brand == value) return;
        brand = value;
        int[] edgeColors = value == 2
                ? new int[]{0xF0FFFFFF, 0xC0A9EDF8, 0xB1FFFFFF}
                : new int[]{0xF0FFFFFF, 0xC0A9EDF8, 0xB1FFFFFF};
        rim.setShader(new LinearGradient(0, 0, 1, .7f, edgeColors,
                null, Shader.TileMode.CLAMP));
        bottomSheen.setShader(new LinearGradient(0, 0, 1, 0,
                value == 2 ? new int[]{0x0097F6FF, 0xD1B7F8FF, 0xCDE0FFFF}
                        : new int[]{0x0097F6FF, 0xD1B7F8FF, 0xCDE0FFFF},
                null, Shader.TileMode.CLAMP));
        focusEdge.setShader(new LinearGradient(0, 0, 1, 1,
                new int[]{0xF8FFFFFF, 0xE3B5F7FF, 0xEE79E5FF},
                null, Shader.TileMode.CLAMP));
        invalidateSelf();
    }

    void syncFocus(boolean focused) {
        focusFade.cancel();
        focusFraction = focused ? 1f : 0f;
        invalidateSelf();
    }

    void animateFocus(boolean focused) {
        focusFade.cancel();
        // Restart from current fractional intensity when DPAD changes quickly.
        float target = focused ? 1f : 0f;
        if (Math.abs(target - focusFraction) < .0001f) return;
        focusFade.setFloatValues(focusFraction, target);
        if ("instant".equals(appearance.motion)) {
            focusFraction = target;
            invalidateSelf();
            return;
        }
        focusFade.setDuration(appearance.duration);
        focusFade.start();
    }

    void stopFocus() { focusFade.cancel(); }

    @Override public void draw(@NonNull Canvas canvas) {
        frame.set(getBounds());
        if (frame.isEmpty()) return;

        float w = frame.width(), h = frame.height();
        float r = Math.min(h * .5f, 19f * density);

        // Wallpaper-aligned cached blurred glass is precomputed outside the draw loop.
        float deckAlpha = Math.max(0f,Math.min(1f,appearance.dockOpacity / 58f));
        wallpaperGlass.draw(canvas, host, frame, r, opacity * deckAlpha);

        matrix.setScale(1f, h);
        matrix.postTranslate(0f, frame.top);
        smoke.getShader().setLocalMatrix(matrix);
        topReflection.getShader().setLocalMatrix(matrix);
        lens.getShader().setLocalMatrix(matrix);

        smoke.setAlpha(Math.round(255f * opacity * deckAlpha));
        canvas.drawRoundRect(frame, r, r, smoke);
        // Reflection slider governs ALL static shine, including the upper curved lens.
        float reflectionFactor = appearance.dockReflection / 100f;
        float borderFactor = appearance.dockBorder / 100f;
        topReflection.setAlpha(Math.round(175f * opacity * deckAlpha * reflectionFactor));
        canvas.drawRoundRect(frame, r, r, topReflection);

        // Fixed, wide glass-refraction crease: never moves during focus.
        topLens.reset();
        topLens.moveTo(frame.left + .08f*w, frame.top + .07f*h);
        topLens.cubicTo(frame.left+.30f*w, frame.top+.14f*h,
                frame.left+.64f*w, frame.top+.03f*h, frame.right-.08f*w, frame.top+.13f*h);
        topLens.cubicTo(frame.left+.76f*w, frame.top+.30f*h,
                frame.left+.43f*w, frame.top+.24f*h, frame.left+.08f*w, frame.top+.31f*h);
        topLens.close();
        lens.setAlpha(Math.round(150f * opacity * deckAlpha * reflectionFactor));
        canvas.drawPath(topLens, lens);

        // Permanent micro-rim is part of the GLASS, not a travelling focus indicator.
        inset.set(frame);
        inset.inset(.95f*density, .95f*density);
        matrix.setScale(w, h);
        matrix.postTranslate(frame.left, frame.top);
        rim.getShader().setLocalMatrix(matrix);
        rim.setAlpha(Math.round(195f * opacity * borderFactor));
        canvas.drawRoundRect(inset, Math.max(0f,r-.95f*density),
                Math.max(0f,r-.95f*density), rim);

        // Refraction along the lower third of the stationary glass tile.
        lowerCurve.reset();
        lowerCurve.moveTo(frame.left + .10f*w, frame.bottom-.21f*h);
        lowerCurve.cubicTo(frame.left+.40f*w, frame.bottom-.05f*h,
                frame.left+.65f*w, frame.bottom-.12f*h, frame.right-.08f*w, frame.bottom-.17f*h);
        bottomSheen.getShader().setLocalMatrix(matrix);
        bottomSheen.setAlpha(Math.round(150f * opacity * reflectionFactor));
        canvas.drawPath(lowerCurve, bottomSheen);

        tintPaint.setAlpha(Math.round(18f * opacity * deckAlpha));
        canvas.drawRoundRect(frame,r,r,tintPaint);
        if (focusFraction > .001f) {
            // Only alpha changes; the radiance is clipped to the stationary rounded tile.
            focusWash.getShader().setLocalMatrix(matrix);
            focusWash.setAlpha(Math.min(255,Math.round(232f * opacity * focusFraction * appearance.spotlightIntensity / 75f)));
            canvas.drawRoundRect(frame, r, r, focusWash);
            focusEdge.getShader().setLocalMatrix(matrix);
            // Keep glowing focus WASH; the hard white focus-edge respects Border Opacity.
            focusEdge.setAlpha(Math.round(190f * opacity * focusFraction * borderFactor));
            canvas.drawRoundRect(inset, Math.max(0f,r-.95f*density),
                    Math.max(0f,r-.95f*density), focusEdge);
        }
    }

    @Override public void setAlpha(int alpha) {
        opacity = Math.max(0f, Math.min(1f, alpha / 255f));
        invalidateSelf();
    }
    @Override public void setColorFilter(ColorFilter colorFilter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
