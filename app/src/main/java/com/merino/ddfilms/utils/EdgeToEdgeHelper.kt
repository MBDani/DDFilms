package com.merino.ddfilms.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object EdgeToEdgeHelper {

    /**
     * Applies window insets to the given view as padding.
     * @param view The view to apply padding to.
     * @param applyTop Whether to apply the top system bar inset (status bar).
     * @param applyBottom Whether to apply the bottom system bar inset (navigation bar).
     * @param applyLeft Whether to apply the left system bar inset (display cutout/landscape).
     * @param applyRight Whether to apply the right system bar inset (display cutout/landscape).
     */
    @JvmStatic
    fun applyWindowInsets(view: View, applyTop: Boolean, applyBottom: Boolean, applyLeft: Boolean, applyRight: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val left = if (applyLeft) insets.left else 0
            val top = if (applyTop) insets.top else 0
            val right = if (applyRight) insets.right else 0
            val bottom = if (applyBottom) insets.bottom else 0

            v.setPadding(
                v.paddingLeft + left,
                v.paddingTop + top,
                v.paddingRight + right,
                v.paddingBottom + bottom
            )
            
            windowInsets
        }
    }

    /**
     * Helper optimized for vertical scrolling views or root layouts.
     * Preserves existing padding and adds insets.
     */
    @JvmStatic
    fun applyWindowInsetsPending(view: View, applyTop: Boolean, applyBottom: Boolean) {
        val initialPaddingLeft = view.paddingLeft
        val initialPaddingTop = view.paddingTop
        val initialPaddingRight = view.paddingRight
        val initialPaddingBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                initialPaddingLeft,
                initialPaddingTop + (if (applyTop) insets.top else 0),
                initialPaddingRight,
                initialPaddingBottom + (if (applyBottom) insets.bottom else 0)
            )

            windowInsets
        }
    }

    /**
     * Applies top window inset as padding and increases the view's height by the inset amount.
     * Useful for Toolbars where we want to maintain a specific content height (e.g. ActionBarSize)
     * but extend the background/touch area behind the status bar.
     * @param view The view to modify (typically a Toolbar).
     * @param baseHeight The target height of the content area (e.g. getResources().getDimensionPixelSize(R.dimen.action_bar_size)).
     *                   If -1, uses the view's current height (not recommended if view is not laid out).
     */
    @JvmStatic
    fun applyWindowInsetsToHeightAndPadding(view: View, baseHeight: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val params = v.layoutParams
            if (params != null) {
                params.height = baseHeight + insets.top
                v.layoutParams = params
            }

            v.setPadding(
                v.paddingLeft,
                insets.top, // Padding matches the added height, creating a "spacer" effect at the top
                v.paddingRight,
                v.paddingBottom
            )

            windowInsets
        }
    }

    /**
     * Applies system bar insets as margins.
     */
    @JvmStatic
    fun applyWindowInsetsToMargin(view: View, applyTop: Boolean, applyBottom: Boolean, applyLeft: Boolean, applyRight: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val lp = v.layoutParams as ViewGroup.MarginLayoutParams

            if (applyTop) lp.topMargin = insets.top
            if (applyBottom) lp.bottomMargin = insets.bottom
            if (applyLeft) lp.leftMargin = insets.left
            if (applyRight) lp.rightMargin = insets.right

            v.layoutParams = lp
            windowInsets
        }
    }
}
