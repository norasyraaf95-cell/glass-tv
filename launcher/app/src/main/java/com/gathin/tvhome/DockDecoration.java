package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
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
        band.setColor(0x12FFFFFF);
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
        // Reference screenshot uses FIVE SEPARATE dark-glass app cards, not a large
        // tray behind them. Preserve area geometry for focus coordination only.
        area.set(rect);

        // V7.8 paints its approved floor reflections in a ROOT-level ImageView
        // using the provided exact source texture. Avoid drawing a second line
        // or another glow here; duplicated rails were the V7.6 visual regression.
        if (DockArtwork.get(parent.getContext()).valid) return;

        // This is STATIONARY decorative illumination, not a focus indicator.
        // It never changes position/width when the remote is pressed; unlike
        // previous mockup videos, there is no light travelling between apps.
        float lineY = rect.bottom + 4f * density;
        float left = rect.left + 9f * density;
        float right = rect.right - 9f * density;
        if (right > left) {
            staticRail.setShader(new LinearGradient(left, lineY, right, lineY,
                    new int[]{0x00E5A4FF, 0xAFCB7DFF, 0xD68E94FA, 0x9BF4A7FF, 0x00E5A4FF},
                    new float[]{0f, .24f, .52f, .78f, 1f}, Shader.TileMode.CLAMP));
            c.drawRoundRect(left, lineY, right, lineY + 1.5f * density,
                    1f * density, 1f * density, staticRail);

            diffuseUnderRail.setShader(new LinearGradient(0f, lineY, 0f, lineY + 21f*density,
                    new int[]{0x39C48CE7, 0x13AF6BDA, 0x00AD73D8},
                    new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(left, lineY + 1.5f*density, right,
                    lineY + 21f*density, diffuseUnderRail);
        }
    }
}
