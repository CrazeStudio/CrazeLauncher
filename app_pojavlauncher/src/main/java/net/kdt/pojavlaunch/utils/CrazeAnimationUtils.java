package net.kdt.pojavlaunch.utils;

import android.annotation.SuppressLint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

/**
 * Universal, crash-safe fluid animation & tactile touch feedback engine for CrazeLauncher.
 */
public final class CrazeAnimationUtils {

    private static final AccelerateDecelerateInterpolator PRESS_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final OvershootInterpolator RELEASE_INTERPOLATOR = new OvershootInterpolator(1.5f);
    private static final DecelerateInterpolator ENTRANCE_INTERPOLATOR = new DecelerateInterpolator(1.5f);

    private CrazeAnimationUtils() {}

    /**
     * Attaches an ultra-smooth physical spring touch feedback animation to a view.
     * Scale down on press (0.93x), spring overshoot on release (1.0x), plus tactile haptic tap.
     * Does not intercept click events (returns false).
     */
    @SuppressLint("ClickableViewAccessibility")
    public static void attachTouchFeedback(@Nullable View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    try {
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    } catch (Throwable ignored) {}
                    v.animate()
                            .scaleX(0.93f)
                            .scaleY(0.93f)
                            .setDuration(60)
                            .setInterpolator(PRESS_INTERPOLATOR)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(160)
                            .setInterpolator(RELEASE_INTERPOLATOR)
                            .start();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .setInterpolator(PRESS_INTERPOLATOR)
                            .start();
                    break;
            }
            return false;
        });
    }

    /**
     * Recursively attaches spring touch feedback to all clickable or focusable views in a view hierarchy.
     */
    public static void attachTouchFeedbackRecursively(@Nullable View view) {
        if (view == null) return;

        if (view.isClickable() || view.isFocusable() || view instanceof android.widget.Button || view instanceof android.widget.ImageButton || view.hasOnClickListeners()) {
            attachTouchFeedback(view);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachTouchFeedbackRecursively(group.getChildAt(i));
            }
        }
    }

    /**
     * Smooth cascaded slide & fade entrance for cards and views.
     */
    public static void animateEntrance(@Nullable View view, long delayMs) {
        if (view == null) return;
        view.setAlpha(0f);
        view.setTranslationY(20f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delayMs)
                .setDuration(220)
                .setInterpolator(ENTRANCE_INTERPOLATOR)
                .start();
    }

    /**
     * Smoothly staggers child views in a container with fluid entrance animation.
     */
    public static void animateStaggeredChildren(@Nullable ViewGroup container) {
        if (container == null) return;
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = container.getChildAt(i);
            if (child != null) {
                animateEntrance(child, i * 30L);
            }
        }
    }

    /**
     * Performs a punchy programmatic button click animation.
     */
    public static void animateClickBounce(@Nullable View view) {
        if (view == null) return;
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Throwable ignored) {}
        view.animate()
                .scaleX(0.91f)
                .scaleY(0.91f)
                .setDuration(60)
                .setInterpolator(PRESS_INTERPOLATOR)
                .withEndAction(() -> {
                    if (view.isAttachedToWindow()) {
                        view.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(160)
                                .setInterpolator(RELEASE_INTERPOLATOR)
                                .start();
                    }
                })
                .start();
    }
}

