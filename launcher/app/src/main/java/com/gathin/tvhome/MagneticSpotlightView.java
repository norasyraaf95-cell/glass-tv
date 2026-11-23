package com.gathin.tvhome;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Owner-locked Magnetic Spotlight B.
 * The light is rendered by DockDecoration AFTER its glass tray and BEFORE the app tiles.
 * The View is only a state/animation controller; no bitmap allocations per frame.
 */
public final class MagneticSpotlightView extends View {
    static final long TRANSITION_MS = 470L;
    private static final TimeInterpolator SMOOTHERSTEP = input -> {
        float t = Math.max(0f, Math.min(1f, input));
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    };

    private RadialGradient broadShader, coreShader;
    private final Paint broadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix broadMatrix = new Matrix();
    private final Matrix coreMatrix = new Matrix();
    private final ValueAnimator travel = ValueAnimator.ofFloat(0f, 1f);
    private final float density;
    private final int[] hostAt = new int[2], viewAt = new int[2];
    private View drawingHost;
    private float fromX, targetX, positionX, y;
    private float broadX, broadY, coreX, coreY;
    private boolean hasPosition, visibleFoot;
    private int duration = 470;
    private float softnessScale = 1f;
    private String mode = CrystalAppearance.LOCKED;

    public MagneticSpotlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        // The locked Python reference adds light to the underlying pixels; ordinary
        // SrcOver tint can actually DARKEN bright alpine grass, so use additive blending.
        if (Build.VERSION.SDK_INT >= 29) {
            broadPaint.setBlendMode(android.graphics.BlendMode.PLUS);
            corePaint.setBlendMode(android.graphics.BlendMode.PLUS);
        } else {
            broadPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            corePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        }
        setAppearance(new CrystalAppearance());
        travel.setDuration(TRANSITION_MS);
        travel.setInterpolator(SMOOTHERSTEP);
        travel.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            positionX = fromX + (targetX - fromX) * f;
            repaint();
        });
    }

    void setDrawingHost(View host) {
        drawingHost = host;
        repaint();
    }

    private void repaint() {
        invalidate();
        if (drawingHost != null) drawingHost.invalidate();
    }

    private static int tint(int color, float alpha) {
        return Color.argb(Math.max(0, Math.min(255, Math.round(alpha * 255f))),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    /** Allocate new shaders ONLY on settings edits, not during DPAD animations. */
    void setAppearance(CrystalAppearance theme) {
        mode = theme.motion;
        duration = Math.max(150, Math.min(800, theme.duration));
        // 65 = exact reference footprint. Changing softness scales both light masks.
        softnessScale = 1f + (theme.spotlightSoftness - 65f) / 150f;
        // Reference mask geometry: +/-128px wide, +/-28px tall for ~200x80 cards.
        // Original locked video uses additive BGR (54,69,61) broad and (112,116,93)
        // core, not an opaque cyan wash. Preserve its luminance, shift only the hue.
        int c = theme.spotlightTint;
        int broadColor = shiftReference(0xFF3D4536,c);
        int coreColor = shiftReference(0xFF5D7470,c);
        float strength = Math.max(0f,Math.min(1.6f,theme.spotlightIntensity / 75f));
        broadShader = new RadialGradient(0f,0f,1f,
                new int[]{tint(broadColor,.45f*strength),tint(broadColor,.42f*strength),
                        tint(broadColor,.34f*strength),tint(broadColor,.19f*strength),tint(broadColor,0f)},
                new float[]{0f,.22f,.50f,.76f,1f},Shader.TileMode.CLAMP);
        coreShader = new RadialGradient(0f,0f,1f,
                new int[]{tint(coreColor,.91f*strength),tint(coreColor,.77f*strength),
                        tint(coreColor,.40f*strength),tint(coreColor,.13f*strength),tint(coreColor,0f)},
                new float[]{0f,.22f,.50f,.76f,1f},Shader.TileMode.CLAMP);
        broadPaint.setShader(broadShader);
        corePaint.setShader(coreShader);
        if ("linear".equals(theme.easing)) travel.setInterpolator(input -> input);
        else if ("soft".equals(theme.easing))
            travel.setInterpolator(new DecelerateInterpolator(1.4f));
        else travel.setInterpolator(SMOOTHERSTEP);
        if (!CrystalAppearance.LOCKED.equals(mode)) hide();
        repaint();
    }

    private static int shiftReference(int reference, int wanted) {
        final int base=0xFF9FF7FF; // default Crystal Clear palette
        int r=Math.max(0,Math.min(255,Color.red(reference)+(Color.red(wanted)-Color.red(base))/3));
        int g=Math.max(0,Math.min(255,Color.green(reference)+(Color.green(wanted)-Color.green(base))/3));
        int b=Math.max(0,Math.min(255,Color.blue(reference)+(Color.blue(wanted)-Color.blue(base))/3));
        return Color.rgb(r,g,b);
    }

    /** The card rectangle is in the root layout coordinate system. */
    void focus(Rect card, boolean snap) {
        if (card == null || card.isEmpty() || !CrystalAppearance.LOCKED.equals(mode)) return;
        float cx = card.exactCenterX();
        y = card.bottom - 1.5f * density;
        float w = card.width(), h = card.height();
        broadX = Math.max(1f, w * .64f * softnessScale);
        broadY = Math.max(1f, h * .35f * softnessScale);
        coreX = Math.max(1f, w * .64f * softnessScale);
        coreY = Math.max(1f, h * .35f * softnessScale);
        visibleFoot = true;
        if (snap || !hasPosition) {
            travel.cancel();
            fromX = targetX = positionX = cx;
            hasPosition = true;
            repaint();
            return;
        }
        travel.cancel();
        fromX = positionX; // retarget from current animated position on rapid reversal
        targetX = cx;
        if (Math.abs(targetX - fromX) < .5f) {
            positionX = targetX;
            repaint();
            return;
        }
        travel.setFloatValues(0f, 1f);
        travel.setDuration(duration);
        travel.start();
    }

    void hide() {
        travel.cancel();
        visibleFoot = false;
        hasPosition = false;
        repaint();
    }
    void stop() { hide(); }

    /** Called by the glass shelf decoration after painting the tray but before RecyclerView children. */
    void drawInTray(Canvas canvas, View grid) {
        if (!visibleFoot || !hasPosition || !CrystalAppearance.LOCKED.equals(mode)) return;
        getLocationInWindow(viewAt);
        grid.getLocationInWindow(hostAt);
        int save = canvas.save();
        canvas.translate(viewAt[0] - hostAt[0], viewAt[1] - hostAt[1]);
        drawFoot(canvas, broadShader, broadPaint, broadMatrix, broadX, broadY);
        drawFoot(canvas, coreShader, corePaint, coreMatrix, coreX, coreY);
        canvas.restoreToCount(save);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // The foreground version was hidden UNDER the glass tray. Never draw it twice.
        if (drawingHost != null || !visibleFoot || !hasPosition) return;
        drawFoot(canvas, broadShader, broadPaint, broadMatrix, broadX, broadY);
        drawFoot(canvas, coreShader, corePaint, coreMatrix, coreX, coreY);
    }

    private void drawFoot(Canvas canvas, RadialGradient shader, Paint paint, Matrix matrix,
                          float rx, float ry) {
        matrix.setScale(rx,ry);
        matrix.postTranslate(positionX,y);
        shader.setLocalMatrix(matrix);
        canvas.drawOval(positionX-rx,y-ry,positionX+rx,y+ry,paint);
    }
}
