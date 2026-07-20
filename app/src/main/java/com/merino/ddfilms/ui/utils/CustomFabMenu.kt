package com.merino.ddfilms.ui.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.ArrayList

class CustomFabMenu(
    private val context: Context,
    private val parentContainer: ViewGroup
) {

    class FabMenuItem(
        val iconResId: Int,
        val label: String,
        val backgroundColorResId: Int,
        val contentDescription: String,
        val onClickListener: OnClickListener
    )

    fun interface OnClickListener {
        fun onClick()
    }

    private var overlay: View? = null
    private var mainFab: FloatingActionButton? = null
    private val menuFabs: MutableList<FloatingActionButton> = ArrayList()
    private val menuLabels: MutableList<TextView> = ArrayList()
    private val menuItems: MutableList<FabMenuItem> = ArrayList()
    var isMenuOpen = false
        private set

    private var mainFabIconResId = 0
    private var mainFabColorResId = 0
    private var overlayColorResId = 0
    private var labelBackgroundResId = 0
    private var baseMarginBottom = 88f // dp
    private var itemSpacing = 64f // dp
    private var horizontalMargin = 16f // dp
    private var labelMarginEnd = 72f // dp

    fun setMainFabIcon(iconResId: Int): CustomFabMenu {
        mainFabIconResId = iconResId
        return this
    }

    fun setMainFabColor(colorResId: Int): CustomFabMenu {
        mainFabColorResId = colorResId
        return this
    }

    fun setOverlayColor(colorResId: Int): CustomFabMenu {
        overlayColorResId = colorResId
        return this
    }

    fun setLabelBackground(backgroundResId: Int): CustomFabMenu {
        labelBackgroundResId = backgroundResId
        return this
    }

    fun setBaseMarginBottom(marginDp: Float): CustomFabMenu {
        baseMarginBottom = marginDp
        return this
    }

    fun setItemSpacing(spacingDp: Float): CustomFabMenu {
        itemSpacing = spacingDp
        return this
    }

    fun addMenuItem(item: FabMenuItem): CustomFabMenu {
        menuItems.add(item)
        return this
    }

    fun addMenuItem(
        iconResId: Int,
        label: String,
        backgroundColorResId: Int,
        contentDescription: String,
        onClickListener: OnClickListener
    ): CustomFabMenu {
        val item = FabMenuItem(
            iconResId, label, backgroundColorResId,
            contentDescription, onClickListener
        )
        return addMenuItem(item)
    }

    fun build() {
        createOverlay()
        createMainFab()
        createMenuItems()
        setupClickListeners()
    }

    private fun createOverlay() {
        val view = View(context).apply {
            id = View.generateViewId()
            val overlayParams = if (parentContainer is androidx.coordinatorlayout.widget.CoordinatorLayout) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } else {
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            layoutParams = overlayParams
            setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (overlayColorResId != 0) overlayColorResId else android.R.color.transparent
                )
            )
            visibility = View.GONE
            isClickable = true
            isFocusable = true
        }
        overlay = view
        parentContainer.addView(view)
    }

    private fun createMainFab() {
        val fab = FloatingActionButton(context).apply {
            id = View.generateViewId()
            val params = createFabLayoutParams(horizontalMargin, 16f)
            layoutParams = params

            if (mainFabIconResId != 0) {
                setImageResource(mainFabIconResId)
                imageTintList = ContextCompat.getColorStateList(context, android.R.color.white)
            }

            if (mainFabColorResId != 0) {
                backgroundTintList = ContextCompat.getColorStateList(context, mainFabColorResId)
            }
        }
        mainFab = fab
        parentContainer.addView(fab)
    }

    private fun createMenuItems() {
        for (i in menuItems.indices) {
            val item = menuItems[i]
            val marginBottom = baseMarginBottom + (itemSpacing * (i + 1))

            // Crear FAB
            val fab = FloatingActionButton(context).apply {
                id = View.generateViewId()
                val fabParams = createFabLayoutParams(horizontalMargin, marginBottom)
                layoutParams = fabParams
                setImageResource(item.iconResId)
                imageTintList = ContextCompat.getColorStateList(context, android.R.color.white)
                backgroundTintList = ContextCompat.getColorStateList(context, item.backgroundColorResId)
                size = FloatingActionButton.SIZE_MINI
                visibility = View.GONE
                contentDescription = item.contentDescription
            }

            // Crear Label
            val label = TextView(context).apply {
                id = View.generateViewId()
                val labelParams = createLabelLayoutParams(labelMarginEnd, marginBottom + 8f)
                layoutParams = labelParams
                text = item.label
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                textSize = 14f
                setPadding(dpToPx(12f), dpToPx(6f), dpToPx(12f), dpToPx(6f))
                visibility = View.GONE

                if (labelBackgroundResId != 0) {
                    setBackgroundResource(labelBackgroundResId)
                }
            }

            parentContainer.addView(fab)
            parentContainer.addView(label)

            menuFabs.add(fab)
            menuLabels.add(label)
        }
    }

    private fun createFabLayoutParams(marginEnd: Float, marginBottom: Float): ViewGroup.LayoutParams {
        return if (parentContainer is androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMarginEnd(dpToPx(marginEnd))
                this.bottomMargin = dpToPx(marginBottom)
            }
        } else {
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMarginEnd(dpToPx(marginEnd))
                this.bottomMargin = dpToPx(marginBottom)
            }
        }
    }

    private fun createLabelLayoutParams(marginEnd: Float, marginBottom: Float): ViewGroup.LayoutParams {
        return if (parentContainer is androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                setMarginEnd(dpToPx(marginEnd))
                this.bottomMargin = dpToPx(marginBottom)
            }
        } else {
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMarginEnd(dpToPx(marginEnd))
                this.bottomMargin = dpToPx(marginBottom)
            }
        }
    }

    private fun setupClickListeners() {
        mainFab?.setOnClickListener { toggleMenu() }
        overlay?.setOnClickListener { closeMenu() }

        for (i in menuFabs.indices) {
            menuFabs[i].setOnClickListener {
                menuItems[i].onClickListener.onClick()
                closeMenu()
            }
        }
    }

    fun toggleMenu() {
        if (isMenuOpen) {
            closeMenu()
        } else {
            openMenu()
        }
    }

    fun openMenu() {
        isMenuOpen = true

        mainFab?.animate()
            ?.rotation(90f)
            ?.setDuration(300)
            ?.start()

        overlay?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                ?.alpha(1f)
                ?.setDuration(300)
                ?.start()
        }

        for (i in menuFabs.indices) {
            animateFabIn(menuFabs[i], menuLabels[i], i * 50L)
        }
    }

    fun closeMenu() {
        isMenuOpen = false

        mainFab?.animate()
            ?.rotation(0f)
            ?.setDuration(300)
            ?.start()

        overlay?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.withEndAction { overlay?.visibility = View.GONE }
            ?.start()

        for (i in menuFabs.indices.reversed()) {
            val delay = (menuFabs.size - 1 - i) * 50L
            animateFabOut(menuFabs[i], menuLabels[i], delay)
        }
    }

    private fun animateFabIn(fab: FloatingActionButton, label: TextView, delay: Long) {
        fab.visibility = View.VISIBLE
        label.visibility = View.VISIBLE

        fab.scaleX = 0f
        fab.scaleY = 0f
        label.alpha = 0f

        fab.animate()
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(300)
            ?.setStartDelay(delay)
            ?.setInterpolator(OvershootInterpolator())
            ?.start()

        label.animate()
            ?.alpha(1f)
            ?.setDuration(300)
            ?.setStartDelay(delay + 100)
            ?.start()
    }

    private fun animateFabOut(fab: FloatingActionButton, label: TextView, delay: Long) {
        fab.animate()
            ?.scaleX(0f)
            ?.scaleY(0f)
            ?.setDuration(200)
            ?.setStartDelay(delay)
            ?.withEndAction { fab.visibility = View.GONE }
            ?.start()

        label.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setStartDelay(delay)
            ?.withEndAction { label.visibility = View.GONE }
            ?.start()
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun destroy() {
        overlay?.let { parentContainer.removeView(it) }
        mainFab?.let { parentContainer.removeView(it) }

        for (fab in menuFabs) {
            parentContainer.removeView(fab)
        }

        for (label in menuLabels) {
            parentContainer.removeView(label)
        }

        menuFabs.clear()
        menuLabels.clear()
        menuItems.clear()
    }
}
