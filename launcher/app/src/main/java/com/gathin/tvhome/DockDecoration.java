package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.RadialGradient;
import android.view.animation.LinearInterpolator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Draws the glass tray behind the first (dock) row and adds a gap below it: the blurred wallpaper
 * behind it, an even veil and a light rim, with no brighter top.
 */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tintOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ambientGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix ambientMatrix = new Matrix();
    private final ValueAnimator ambientPulse = ValueAnimator.ofFloat(.70f,1f);
    private float ambientPulseAlpha = 1f;
    private RecyclerView ambientHost;
    private boolean foreground = true;
    private MagneticSpotlightView spotlight;
    private CrystalAppearance appearance = new CrystalAppearance();
    private final Paint crystalRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topGlaze = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baseSheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix gradientMatrix = new Matrix();
    private final Matrix edgeMatrix = new Matrix();
    private final Matrix underMatrix = new Matrix();
    /** A fixed, non-animated optical rail under all 5 cards (part of approved static reference). */
    private final Paint staticRail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diffuseUnderRail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GlassRim edge;
    private Glider glider;
    private final int[] at = new int[2], gliderAt = new int[2];
    private final Backdrop.Fill blur = new Backdrop.Fill();

    private final RectF rect = new RectF();
    /** The tray as last drawn, in the grid's coordinates: where the focus shape shows on it. */
    final RectF area = new RectF();

    DockDecoration(int dockSize, float density) {
        this.dockSize = dockSize;
        this.density = density;
        // Same glass as the top-bar pills (GlassDrawable): an even veil and a shining edge.
        band.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                new int[]{0x42FFFFFF, 0x65CAEAF0, 0x5A83CFDD},
                new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP));
        topGlaze.setShader(new LinearGradient(0f, 0f, 0f, 1f,
                new int[]{0xC7FFFFFF, 0x51FFFFFF, 0x00FFFFFF},
                new float[]{0f, .36f, 1f}, Shader.TileMode.CLAMP));
        baseSheen.setStyle(Paint.Style.STROKE);
        baseSheen.setStrokeWidth(1.5f * density);
        baseSheen.setStrokeCap(Paint.Cap.ROUND);
        baseSheen.setShader(new LinearGradient(0f,0f,1f,0f,
                new int[]{0x00FFFFFF,0xE7C8FCFF,0xEAFFFFFF,0xC7A4F8FF,0x00FFFFFF},
                new float[]{0f,.20f,.50f,.80f,1f},Shader.TileMode.CLAMP));
        diffuseUnderRail.setShader(new LinearGradient(0f,0f,0f,1f,
                new int[]{0x569EF5FF,0x1986E9FA,0x0089E9FA},
                new float[]{0f,.46f,1f},Shader.TileMode.CLAMP));
        crystalRim.setStyle(Paint.Style.STROKE);
        crystalRim.setStrokeWidth(1.35f * density);
        crystalRim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                new int[]{0xF5FFFFFF, 0xAEC8FAFF, 0x79A4EBF8, 0xEDFFFFFF},
                null, Shader.TileMode.CLAMP));
        edge = new GlassRim(1.5f * density, 0xD9FFFFFF, 0x1AFFFFFF, 0.5f);
        ambientPulse.setRepeatCount(ValueAnimator.INFINITE);
        ambientPulse.setRepeatMode(ValueAnimator.REVERSE);
        ambientPulse.setInterpolator(new LinearInterpolator());
        ambientPulse.addUpdateListener(animation -> {
            ambientPulseAlpha = (float) animation.getAnimatedValue();
            if (ambientHost != null) ambientHost.invalidate();
        });
    }

    void setSpotlight(MagneticSpotlightView light, RecyclerView grid) {
        spotlight = light;
        ambientHost = grid;
        if (light != null) light.setDrawingHost(grid);
    }

    void setForeground(boolean active) {
        foreground = active;
        updateAmbientPulse();
    }

    void release() {
        ambientPulse.cancel();
        ambientHost = null;
    }

    private void updateAmbientPulse() {
        boolean breathe = foreground && appearance.ambientEnabled
                && appearance.ambientIntensity > 0 && "breathing".equals(appearance.ambientMode);
        if (breathe) {
            long duration = appearance.ambientBreathingSpeed == 0 ? 3400L : 2200L;
            if (!ambientPulse.isRunning() || ambientPulse.getDuration() != duration) {
                ambientPulse.cancel();
                ambientPulse.setDuration(duration);
                ambientPulse.start();
            }
        } else {
            ambientPulse.cancel();
            ambientPulseAlpha = 1f;
            if (ambientHost != null) ambientHost.invalidate();
        }
    }

    void setAppearance(CrystalAppearance value) {
        appearance = value.copy();
        tintOverlay.setColor(appearance.dockTint);
        int color = appearance.ambientColor & 0x00FFFFFF;
        ambientGlowPaint.setShader(new RadialGradient(.5f,.61f,.78f,
                new int[]{(0x9A<<24)|color,(0x3B<<24)|color,color},
                new float[]{0f,.48f,1f}, Shader.TileMode.CLAMP));
        updateAmbientPulse();
    }


    /** The focus shape: the tray's glass would hide it, so the tray paints it over the glass itself. */
    void setGlider(Glider glider) {
        this.glider = glider;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && pos < dockSize) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) (12 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        boolean any = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0 || pos >= dockSize) continue;
            View card = child.findViewById(R.id.card);
            if (card == null) continue;
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop() + child.getTranslationY();
            float r = l + card.getWidth();
            float b = t + card.getHeight();
            if (!any) {
                rect.set(l, t, r, b);
                any = true;
            } else {
                rect.union(l, t, r, b);
            }
        }
        if (!any) return;
        area.set(rect); // Preserve focus coordination geometry.
        if (DockArtwork.get(parent.getContext()).valid) return;

        // One floating shelf beneath five independent glass app cards.
        // Wallpaper blur is cached off the UI thread, never recalculated on DPAD focus.
        rect.inset(-10f * density, -8f * density);
        float radius = Math.min(27f * density, rect.height() * .5f);
        float opacity = Math.max(0f,Math.min(1f,appearance.dockOpacity / 58f));
        blur.draw(c, parent, rect, radius, opacity);
        gradientMatrix.setScale(1f, rect.height());
        gradientMatrix.postTranslate(0f, rect.top);
        band.getShader().setLocalMatrix(gradientMatrix);
        band.setAlpha(Math.round(255f * opacity));
        c.drawRoundRect(rect, radius, radius, band);
        tintOverlay.setAlpha(Math.round(28f * opacity));
        c.drawRoundRect(rect, radius, radius, tintOverlay);
        gradientMatrix.setScale(1f, rect.height());
        gradientMatrix.postTranslate(0f, rect.top);
        topGlaze.getShader().setLocalMatrix(gradientMatrix);
        // Respect Reflection Intensity for all stationary white tray highlights.
        float reflection = appearance.dockReflection / 100f;
        if (reflection > 0f) {
            topGlaze.setAlpha(Math.round(255f * opacity * reflection));
            c.drawRoundRect(rect, radius, radius, topGlaze);
        }
        gradientMatrix.setScale(rect.width(), rect.height());
        gradientMatrix.postTranslate(rect.left, rect.top);
        crystalRim.getShader().setLocalMatrix(gradientMatrix);
        if (appearance.dockBorder > 0) {
            crystalRim.setAlpha(Math.round(255f * appearance.dockBorder / 100f));
            c.drawRoundRect(rect, radius, radius, crystalRim);
        }
        edgeMatrix.setScale(rect.width(),1f);
        edgeMatrix.postTranslate(rect.left,rect.bottom);
        baseSheen.getShader().setLocalMatrix(edgeMatrix);
        if (reflection > 0f) {
            baseSheen.setAlpha(Math.round(255f * reflection));
            c.drawLine(rect.left + radius, rect.bottom - 2f*density,
                    rect.right - radius, rect.bottom - 2f*density, baseSheen);
        }

        // Subtle icy underglow: stationary, with no tracking highlight.
        float top = rect.bottom - 1f * density;
        float bottom = top + 13f * density;
        underMatrix.setScale(1f,Math.max(1f,bottom-top));
        underMatrix.postTranslate(0f,top);
        diffuseUnderRail.getShader().setLocalMatrix(underMatrix);
        if (reflection > 0f) {
            diffuseUnderRail.setAlpha(Math.round(255f * reflection));
            c.drawRect(rect.left + 18f * density, top,
                    rect.right - 18f * density, bottom, diffuseUnderRail);
        }
        // Ambient sits on top of tray and UNDER all independent app cards,
        // separate from Magnetic Spotlight B. Shader is created only on settings change.
        if (appearance.ambientEnabled && appearance.ambientIntensity > 0) {
            float spread = .75f + appearance.ambientSpread / 100f * .85f;
            ambientMatrix.setScale(rect.width() * spread, rect.height() * spread);
            ambientMatrix.postTranslate(rect.centerX() - rect.width() * spread * .5f,
                    rect.centerY() - rect.height() * spread * .5f);
            ambientGlowPaint.getShader().setLocalMatrix(ambientMatrix);
            ambientGlowPaint.setAlpha(Math.round(200f * appearance.ambientIntensity / 100f
                    * ambientPulseAlpha));
            c.drawRoundRect(rect, radius, radius, ambientGlowPaint);
        }
        // True Option B layering: wallpaper -> glass tray -> ambient -> moving light -> cards.
        if (spotlight != null) spotlight.drawInTray(c, parent);
    }
}
