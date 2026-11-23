package com.gathin.tvhome;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Owner-locked Option B: Magnetic Spotlight, matched to the 7.7 s reference video.
 * A diffuse optical footprint follows the focused dock tile over exactly 470 ms.
 *
 * No travelling line, gliding ring, bitmap decode, RenderEffect or per-frame allocations.
 * The card stays still; LiquidCrystalDockDrawable separately crossfades its rim in 470 ms.
 * This View sits behind the dock, above the wallpaper, and never consumes remote input.
 */
public final class MagneticSpotlightView extends View {
    static final long TRANSITION_MS = 470L;

    private static final TimeInterpolator SMOOTHERSTEP = input -> {
        float t = Math.max(0f, Math.min(1f, input));
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    };

    // Both ellipses approximate the two Gaussian light footprints from Option B video:
    // a broad low-alpha pool and a tighter bright white/cyan core.
    private final RadialGradient broadShader = new RadialGradient(0f, 0f, 1f,
            new int[]{0x54BCEFFD, 0x46BCEFFD, 0x1CA7E5FA, 0x04A7E5FA, 0x00A7E5FA},
            new float[]{0f, .22f, .50f, .76f, 1f}, Shader.TileMode.CLAMP);
    private final RadialGradient coreShader = new RadialGradient(0f, 0f, 1f,
            new int[]{0xB6D7FCFF, 0x91D1FAFF, 0x4AA9F0FA, 0x0DA9F0FA, 0x00A9F0FA},
            new float[]{0f, .24f, .50f, .76f, 1f}, Shader.TileMode.CLAMP);
    private final Paint broadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix broadMatrix = new Matrix();
    private final Matrix coreMatrix = new Matrix();
    private final ValueAnimator travel = ValueAnimator.ofFloat(0f, 1f);
    private final float density;

    private float fromX, targetX, positionX, y;
    private float broadX, broadY, coreX, coreY;
    private boolean hasPosition;
    private boolean visibleFoot;

    public MagneticSpotlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        broadPaint.setShader(broadShader);
        corePaint.setShader(coreShader);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        travel.setDuration(TRANSITION_MS);
        travel.setInterpolator(SMOOTHERSTEP);
        travel.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            positionX = fromX + (targetX - fromX) * f;
            invalidate();
        });
    }

    /** The tile rect must already be expressed in the parent root's coordinates. */
    void focus(Rect card, boolean snap) {
        if (card == null || card.isEmpty()) return;
        float cx = card.exactCenterX();
        float cy = card.bottom - 1.5f * density;
        float w = card.width(), h = card.height();
        // Relative to Option B's 200x80 px cards, matching the video at any TV density.
        broadX = Math.max(1f, w * 1.34f);
        broadY = Math.max(1f, h * .64f);
        coreX = Math.max(1f, w * .78f);
        coreY = Math.max(1f, h * .34f);
        y = cy;
        visibleFoot = true;

        if (snap || !hasPosition) {
            travel.cancel();
            fromX = targetX = positionX = cx;
            hasPosition = true;
            invalidate();
            return;
        }
        // Retarget from the CURRENT interpolated light position on rapid D-pad reversal,
        // never from a stale tile centre (which would create a visible jump).
        travel.cancel();
        fromX = positionX;
        targetX = cx;
        if (Math.abs(targetX - fromX) < .5f) {
            positionX = targetX;
            invalidate();
            return;
        }
        travel.setFloatValues(0f, 1f);
        travel.setDuration(TRANSITION_MS);
        travel.start();
    }

    void hide() {
        travel.cancel();
        visibleFoot = false;
        hasPosition = false;
        invalidate();
    }

    void stop() {
        travel.cancel();
        visibleFoot = false;
        hasPosition = false;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!visibleFoot || !hasPosition) return;
        drawFoot(canvas, broadShader, broadPaint, broadMatrix, broadX, broadY);
        drawFoot(canvas, coreShader, corePaint, coreMatrix, coreX, coreY);
    }

    private void drawFoot(Canvas canvas, RadialGradient gradient, Paint paint, Matrix matrix,
                          float rx, float ry) {
        matrix.setScale(rx, ry);
        matrix.postTranslate(positionX, y);
        gradient.setLocalMatrix(matrix);
        canvas.drawOval(positionX - rx, y - ry, positionX + rx, y + ry, paint);
    }
}
