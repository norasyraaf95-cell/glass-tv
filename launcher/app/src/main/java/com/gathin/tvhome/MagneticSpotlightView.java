package com.gathin.tvhome;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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
        softnessScale = .75f + theme.spotlightSoftness / 130f;
        int c = theme.spotlightTint;
        float strength = Math.max(0f, Math.min(1f, theme.spotlightIntensity / 100f));
        broadShader = new RadialGradient(0f, 0f, 1f,
                new int[]{tint(c,.50f*strength),tint(c,.39f*strength),
                        tint(c,.19f*strength),tint(c,.04f*strength),tint(c,0f)},
                new float[]{0f,.22f,.50f,.76f,1f}, Shader.TileMode.CLAMP);
        coreShader = new RadialGradient(0f, 0f, 1f,
                new int[]{tint(c,.88f*strength),tint(c,.70f*strength),
                        tint(c,.41f*strength),tint(c,.10f*strength),tint(c,0f)},
                new float[]{0f,.24f,.50f,.76f,1f}, Shader.TileMode.CLAMP);
        broadPaint.setShader(broadShader);
        corePaint.setShader(coreShader);
        if ("linear".equals(theme.easing)) travel.setInterpolator(input -> input);
        else if ("soft".equals(theme.easing))
            travel.setInterpolator(new DecelerateInterpolator(1.4f));
        else travel.setInterpolator(SMOOTHERSTEP);
        if (!CrystalAppearance.LOCKED.equals(mode)) hide();
        repaint();
    }

    /** The card rectangle is in the root layout coordinate system. */
    void focus(Rect card, boolean snap) {
        if (card == null || card.isEmpty() || !CrystalAppearance.LOCKED.equals(mode)) return;
        float cx = card.exactCenterX();
        y = card.bottom - 1.5f * density;
        float w = card.width(), h = card.height();
        broadX = Math.max(1f, w * 1.34f * softnessScale);
        broadY = Math.max(1f, h * .64f * softnessScale);
        coreX = Math.max(1f, w * .78f * softnessScale);
        coreY = Math.max(1f, h * .34f * softnessScale);
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
