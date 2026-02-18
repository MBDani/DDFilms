package com.merino.ddfilms.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EdgeToEdgeHelper {

    /**
     * Applies window insets to the given view as padding.
     * @param view The view to apply padding to.
     * @param applyTop Whether to apply the top system bar inset (status bar).
     * @param applyBottom Whether to apply the bottom system bar inset (navigation bar).
     * @param applyLeft Whether to apply the left system bar inset (display cutout/landscape).
     * @param applyRight Whether to apply the right system bar inset (display cutout/landscape).
     */
    public static void applyWindowInsets(@NonNull View view, boolean applyTop, boolean applyBottom, boolean applyLeft, boolean applyRight) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            int left = applyLeft ? insets.left : 0;
            int top = applyTop ? insets.top : 0;
            int right = applyRight ? insets.right : 0;
            int bottom = applyBottom ? insets.bottom : 0;

            v.setPadding(
                v.getPaddingLeft() + left,
                v.getPaddingTop() + top,
                v.getPaddingRight() + right,
                v.getPaddingBottom() + bottom
            );
            
            return windowInsets;
        });
    }

    /**
     * Helper optimized for vertical scrolling views or root layouts.
     * Preserves existing padding and adds insets.
     */
    public static void applyWindowInsetsPending(@NonNull View view, boolean applyTop, boolean applyBottom) {
        final int initialPaddingLeft = view.getPaddingLeft();
        final int initialPaddingTop = view.getPaddingTop();
        final int initialPaddingRight = view.getPaddingRight();
        final int initialPaddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                initialPaddingLeft,
                initialPaddingTop + (applyTop ? insets.top : 0),
                initialPaddingRight,
                initialPaddingBottom + (applyBottom ? insets.bottom : 0)
            );

            return windowInsets;
        });
    }

    /**
     * Applies top window inset as padding and increases the view's height by the inset amount.
     * Useful for Toolbars where we want to maintain a specific content height (e.g. ActionBarSize)
     * but extend the background/touch area behind the status bar.
     * @param view The view to modify (typically a Toolbar).
     * @param baseHeight The target height of the content area (e.g. getResources().getDimensionPixelSize(R.dimen.action_bar_size)).
     *                   If -1, uses the view's current height (not recommended if view is not laid out).
     */
    public static void applyWindowInsetsToHeightAndPadding(@NonNull View view, int baseHeight) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            ViewGroup.LayoutParams params = v.getLayoutParams();
            if (params != null) {
                params.height = baseHeight + insets.top;
                v.setLayoutParams(params);
            }

            v.setPadding(
                v.getPaddingLeft(),
                insets.top, // Padding matches the added height, creating a "spacer" effect at the top
                v.getPaddingRight(),
                v.getPaddingBottom()
            );

            return windowInsets;
        });
    }

    /**
     * Applies system bar insets as margins.
     */
    public static void applyWindowInsetsToMargin(@NonNull View view, boolean applyTop, boolean applyBottom, boolean applyLeft, boolean applyRight) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

            if (applyTop) lp.topMargin = insets.top;
            if (applyBottom) lp.bottomMargin = insets.bottom;
            if (applyLeft) lp.leftMargin = insets.left;
            if (applyRight) lp.rightMargin = insets.right;

            v.setLayoutParams(lp);
            return windowInsets;
        });
    }
}
