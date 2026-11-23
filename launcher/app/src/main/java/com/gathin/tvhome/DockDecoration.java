package com.gathin.tvhome;

import android.graphics.Canvas;
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
    private final Paint crystalRim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix gradientMatrix = new Matrix();
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
                new int[]{0x70FFFFFF, 0xA6E7F9FA, 0x8AC6F0F2},
                new float[]{0f, .42f, 1f}, Shader.TileMode.CLAMP));
        crystalRim.setStyle(Paint.Style.STROKE);
        crystalRim.setStrokeWidth(1.35f * density);
        crystalRim.setShader(new LinearGradient(0f, 0f, 1f, 1f,
                new int[]{0xF5FFFFFF, 0xAEC8FAFF, 0x79A4EBF8, 0xEDFFFFFF},
                null, Shader.TileMode.CLAMP));
        edge = new GlassRim(1.5f * density, 0xD9FFFFFF, 0x1AFFFFFF, 0.5f);
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
        blur.draw(c, parent, rect, radius, 1f);
        gradientMatrix.setScale(1f, rect.height());
        gradientMatrix.postTranslate(0f, rect.top);
        band.getShader().setLocalMatrix(gradientMatrix);
        c.drawRoundRect(rect, radius, radius, band);
        gradientMatrix.setScale(rect.width(), rect.height());
        gradientMatrix.postTranslate(rect.left, rect.top);
        crystalRim.getShader().setLocalMatrix(gradientMatrix);
        c.drawRoundRect(rect, radius, radius, crystalRim);

        // Subtle icy underglow: stationary, with no tracking highlight.
        float top = rect.bottom - 1f * density;
        float bottom = top + 13f * density;
        diffuseUnderRail.setShader(new LinearGradient(0f, top, 0f, bottom,
                new int[]{0x569EF5FF, 0x1986E9FA, 0x0089E9FA},
                new float[]{0f, .46f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(rect.left + 18f * density, top,
                rect.right - 18f * density, bottom, diffuseUnderRail);
    }
}
