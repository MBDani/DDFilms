package com.merino.ddfilms.ui.utils;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class CustomFabMenu {

    public static class FabMenuItem {
        private int iconResId;
        private String label;
        private int backgroundColorResId;
        private OnClickListener onClickListener;
        private String contentDescription;

        public FabMenuItem(int iconResId, String label, int backgroundColorResId,
                           String contentDescription, OnClickListener onClickListener) {
            this.iconResId = iconResId;
            this.label = label;
            this.backgroundColorResId = backgroundColorResId;
            this.contentDescription = contentDescription;
            this.onClickListener = onClickListener;
        }

        public int getIconResId() { return iconResId; }
        public String getLabel() { return label; }
        public int getBackgroundColorResId() { return backgroundColorResId; }
        public OnClickListener getOnClickListener() { return onClickListener; }
        public String getContentDescription() { return contentDescription; }
    }

    public interface OnClickListener {
        void onClick();
    }

    private Context context;
    private ViewGroup parentContainer;
    private FloatingActionButton mainFab;
    private View overlay;
    private List<FloatingActionButton> menuFabs;
    private List<TextView> menuLabels;
    private List<FabMenuItem> menuItems;
    private boolean isMenuOpen = false;

    private int mainFabIconResId;
    private int mainFabColorResId;
    private int overlayColorResId;
    private int labelBackgroundResId;
    private float baseMarginBottom = 88f; // dp
    private float itemSpacing = 64f; // dp
    private float horizontalMargin = 16f; // dp
    private float labelMarginEnd = 72f; // dp

    public CustomFabMenu(Context context, ViewGroup parentContainer) {
        this.context = context;
        this.parentContainer = parentContainer;
        this.menuFabs = new ArrayList<>();
        this.menuLabels = new ArrayList<>();
        this.menuItems = new ArrayList<>();
    }

    public CustomFabMenu setMainFabIcon(int iconResId) {
        this.mainFabIconResId = iconResId;
        return this;
    }

    public CustomFabMenu setMainFabColor(int colorResId) {
        this.mainFabColorResId = colorResId;
        return this;
    }

    public CustomFabMenu setOverlayColor(int colorResId) {
        this.overlayColorResId = colorResId;
        return this;
    }

    public CustomFabMenu setLabelBackground(int backgroundResId) {
        this.labelBackgroundResId = backgroundResId;
        return this;
    }

    public CustomFabMenu setBaseMarginBottom(float marginDp) {
        this.baseMarginBottom = marginDp;
        return this;
    }

    public CustomFabMenu setItemSpacing(float spacingDp) {
        this.itemSpacing = spacingDp;
        return this;
    }

    public CustomFabMenu addMenuItem(FabMenuItem item) {
        this.menuItems.add(item);
        return this;
    }

    public CustomFabMenu addMenuItem(int iconResId, String label, int backgroundColorResId,
                                     String contentDescription, OnClickListener onClickListener) {
        FabMenuItem item = new FabMenuItem(iconResId, label, backgroundColorResId,
                contentDescription, onClickListener);
        return addMenuItem(item);
    }

    public void build() {
        createOverlay();
        createMainFab();
        createMenuItems();
        setupClickListeners();
    }

    private void createOverlay() {
        overlay = new View(context);
        overlay.setId(View.generateViewId());

        ViewGroup.LayoutParams overlayParams;

        if (parentContainer instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            overlayParams = new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        } else {
            overlayParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        overlay.setLayoutParams(overlayParams);
        overlay.setBackgroundColor(ContextCompat.getColor(context,
                overlayColorResId != 0 ? overlayColorResId : android.R.color.transparent));
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        parentContainer.addView(overlay);
    }

    private void createMainFab() {
        mainFab = new FloatingActionButton(context);
        mainFab.setId(View.generateViewId());

        // Configurar layout params según el tipo de container
        ViewGroup.LayoutParams params = createFabLayoutParams(horizontalMargin, 16f);
        mainFab.setLayoutParams(params);

        if (mainFabIconResId != 0) {
            mainFab.setImageResource(mainFabIconResId);
        }

        if (mainFabColorResId != 0) {
            mainFab.setBackgroundTintList(ContextCompat.getColorStateList(context, mainFabColorResId));
        }

        parentContainer.addView(mainFab);
    }

    private void createMenuItems() {
        for (int i = 0; i < menuItems.size(); i++) {
            FabMenuItem item = menuItems.get(i);
            float marginBottom = baseMarginBottom + (itemSpacing * (i + 1));

            // Crear FAB
            FloatingActionButton fab = new FloatingActionButton(context);
            fab.setId(View.generateViewId());

            ViewGroup.LayoutParams fabParams = createFabLayoutParams(horizontalMargin, marginBottom);
            fab.setLayoutParams(fabParams);

            fab.setImageResource(item.getIconResId());
            fab.setBackgroundTintList(ContextCompat.getColorStateList(context, item.getBackgroundColorResId()));
            fab.setSize(FloatingActionButton.SIZE_MINI);
            fab.setVisibility(View.GONE);
            fab.setContentDescription(item.getContentDescription());

            // Crear Label
            TextView label = new TextView(context);
            label.setId(View.generateViewId());

            ViewGroup.LayoutParams labelParams = createLabelLayoutParams(labelMarginEnd, marginBottom + 8f);
            label.setLayoutParams(labelParams);

            label.setText(item.getLabel());
            label.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            label.setTextSize(14f);
            label.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
            label.setVisibility(View.GONE);

            if (labelBackgroundResId != 0) {
                label.setBackgroundResource(labelBackgroundResId);
            }

            parentContainer.addView(fab);
            parentContainer.addView(label);

            menuFabs.add(fab);
            menuLabels.add(label);
        }
    }

    private ViewGroup.LayoutParams createFabLayoutParams(float marginEnd, float marginBottom) {
        if (parentContainer instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                    new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            params.setMarginEnd(dpToPx(marginEnd));
            params.bottomMargin = dpToPx(marginBottom);

            return params;
        } else {
            // Para otros tipos de layout (FrameLayout, etc.)
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            params.setMarginEnd(dpToPx(marginEnd));
            params.bottomMargin = dpToPx(marginBottom);

            return params;
        }
    }

    private ViewGroup.LayoutParams createLabelLayoutParams(float marginEnd, float marginBottom) {
        if (parentContainer instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                    new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );

            params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
            params.setMarginEnd(dpToPx(marginEnd));
            params.bottomMargin = dpToPx(marginBottom);

            return params;
        } else {
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            params.setMarginEnd(dpToPx(marginEnd));
            params.bottomMargin = dpToPx(marginBottom);

            return params;
        }
    }

    private void setupClickListeners() {
        mainFab.setOnClickListener(v -> toggleMenu());

        overlay.setOnClickListener(v -> closeMenu());

        for (int i = 0; i < menuFabs.size(); i++) {
            final int index = i;
            menuFabs.get(i).setOnClickListener(v -> {
                menuItems.get(index).getOnClickListener().onClick();
                closeMenu();
            });
        }
    }

    public void toggleMenu() {
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    public void openMenu() {
        isMenuOpen = true;

        // Rotar el FAB principal
        mainFab.animate()
                .rotation(90f)
                .setDuration(300)
                .start();

        // Mostrar overlay
        overlay.setVisibility(View.VISIBLE);
        overlay.setAlpha(0f);
        overlay.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Animar la aparición de los items
        for (int i = 0; i < menuFabs.size(); i++) {
            animateFabIn(menuFabs.get(i), menuLabels.get(i), i * 50L);
        }
    }

    public void closeMenu() {
        isMenuOpen = false;

        // Rotar el FAB principal de vuelta
        mainFab.animate()
                .rotation(0f)
                .setDuration(300)
                .start();

        // Ocultar overlay
        overlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> overlay.setVisibility(View.GONE))
                .start();

        // Animar la desaparición de los items (en orden inverso)
        for (int i = menuFabs.size() - 1; i >= 0; i--) {
            long delay = (menuFabs.size() - 1 - i) * 50L;
            animateFabOut(menuFabs.get(i), menuLabels.get(i), delay);
        }
    }

    private void animateFabIn(FloatingActionButton fab, TextView label, long delay) {
        fab.setVisibility(View.VISIBLE);
        label.setVisibility(View.VISIBLE);

        fab.setScaleX(0f);
        fab.setScaleY(0f);
        label.setAlpha(0f);

        fab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(delay)
                .setInterpolator(new OvershootInterpolator())
                .start();

        label.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(delay + 100)
                .start();
    }

    private void animateFabOut(FloatingActionButton fab, TextView label, long delay) {
        fab.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(200)
                .setStartDelay(delay)
                .withEndAction(() -> fab.setVisibility(View.GONE))
                .start();

        label.animate()
                .alpha(0f)
                .setDuration(200)
                .setStartDelay(delay)
                .withEndAction(() -> label.setVisibility(View.GONE))
                .start();
    }

    private int dpToPx(float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public boolean isMenuOpen() {
        return isMenuOpen;
    }

    public void destroy() {
        if (parentContainer != null) {
            if (overlay != null) parentContainer.removeView(overlay);
            if (mainFab != null) parentContainer.removeView(mainFab);

            for (FloatingActionButton fab : menuFabs) {
                parentContainer.removeView(fab);
            }

            for (TextView label : menuLabels) {
                parentContainer.removeView(label);
            }
        }

        menuFabs.clear();
        menuLabels.clear();
        menuItems.clear();
    }
}