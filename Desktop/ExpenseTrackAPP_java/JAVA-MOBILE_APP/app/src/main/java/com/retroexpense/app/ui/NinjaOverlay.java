package com.retroexpense.app.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.retroexpense.app.R;

/**
 * Lightweight overlay that shows the retro ninja loader. It can bounce while data
 * is loading and then perch on the top edge afterward, following the user.
 */
public class NinjaOverlay {

    private enum State {
        HIDDEN,
        LOADING,
        PERCHED
    }

    private static NinjaOverlay instance;

    private final Context appContext;
    private final FrameLayout overlayRoot;
    private final FrameLayout badgeContainer;
    private final ImageView ninjaView;
    private final TextView statusText;
    private final ProgressBar stealthLoader;

    private ObjectAnimator hoverAnimator;
    private State state = State.HIDDEN;
    private ViewGroup currentParent;

    private NinjaOverlay(Context context) {
        this.appContext = context.getApplicationContext();
        overlayRoot = new FrameLayout(appContext);
        overlayRoot.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlayRoot.setClickable(false);
        overlayRoot.setFocusable(false);
        overlayRoot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        badgeContainer = new FrameLayout(appContext);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        badgeParams.gravity = Gravity.TOP | Gravity.END;
        badgeParams.topMargin = statusBarOffset() + dp(16);
        badgeParams.rightMargin = dp(16);
        badgeContainer.setLayoutParams(badgeParams);
        badgeContainer.setBackground(ContextCompat.getDrawable(appContext, R.drawable.ninja_badge_bg));
        badgeContainer.setPadding(dp(8), dp(8), dp(8), dp(8));
        badgeContainer.setClickable(false);
        badgeContainer.setFocusable(false);

        ninjaView = new ImageView(appContext);
        ninjaView.setImageDrawable(ContextCompat.getDrawable(appContext, R.drawable.ic_retro_ninja));
        FrameLayout.LayoutParams ninjaParams = new FrameLayout.LayoutParams(dp(72), dp(72));
        ninjaParams.gravity = Gravity.CENTER_HORIZONTAL;
        ninjaView.setLayoutParams(ninjaParams);

        statusText = new TextView(appContext);
        statusText.setTextColor(ContextCompat.getColor(appContext, R.color.terminal_green_bright));
        statusText.setTextSize(12f);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        textParams.bottomMargin = dp(4);
        statusText.setLayoutParams(textParams);

        stealthLoader = new ProgressBar(appContext);
        stealthLoader.setIndeterminate(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            stealthLoader.setIndeterminateTintList(ContextCompat.getColorStateList(appContext, R.color.terminal_green));
        }
        FrameLayout.LayoutParams loaderParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        loaderParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        loaderParams.bottomMargin = dp(32);
        stealthLoader.setLayoutParams(loaderParams);

        badgeContainer.setClipToPadding(false);


        badgeContainer.addView(ninjaView);
        badgeContainer.addView(stealthLoader);
        badgeContainer.addView(statusText);
        overlayRoot.addView(badgeContainer);
        overlayRoot.setVisibility(View.GONE);
    }

    public static NinjaOverlay getInstance(Context context) {
        if (instance == null) {
            instance = new NinjaOverlay(context);
        }
        return instance;
    }

    public void attach(ViewGroup ownerRoot) {
        if (ownerRoot == null) return;
        if (overlayRoot.getParent() == ownerRoot) {
            currentParent = ownerRoot;
            return;
        }
        detachInternal();
        ownerRoot.addView(overlayRoot);
        currentParent = ownerRoot;
        applyState();
    }

    public void attach(android.app.Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof ViewGroup) {
            attach((ViewGroup) content);
        }
    }

    public void detach(android.app.Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        if (content instanceof ViewGroup && content == currentParent) {
            detachInternal();
        }
    }

    public void showLoadingMessage(@Nullable CharSequence message) {
        state = State.LOADING;
        overlayRoot.setVisibility(View.VISIBLE);
        statusText.setVisibility(message == null ? View.GONE : View.VISIBLE);
        statusText.setText(message);
        stealthLoader.setVisibility(View.VISIBLE);
        badgeContainer.setElevation(dp(6));
        badgeContainer.bringToFront();
        startHover();
    }

    public void perchOnTop(@Nullable CharSequence message) {
        state = State.PERCHED;
        overlayRoot.setVisibility(View.VISIBLE);
        stealthLoader.setVisibility(View.GONE);
        statusText.setVisibility(message == null ? View.GONE : View.VISIBLE);
        statusText.setText(message);
        stopHover();
        badgeContainer.setTranslationY(0f);
        badgeContainer.animate()
                .translationYBy(-dp(10))
                .setDuration(200)
                .withEndAction(() -> badgeContainer.animate()
                        .translationY(0f)
                        .setDuration(160)
                        .start())
                .start();
    }

    public void hide() {
        overlayRoot.setVisibility(View.GONE);
        state = State.HIDDEN;
        stopHover();
    }

    public boolean isLoading() {
        return state == State.LOADING;
    }

    private void applyState() {
        switch (state) {
            case LOADING:
                showLoadingMessage(statusText.getText());
                break;
            case PERCHED:
                perchOnTop(statusText.getText());
                break;
            case HIDDEN:
            default:
                hide();
                break;
        }
    }

    private void startHover() {
        if (hoverAnimator != null && hoverAnimator.isRunning()) {
            return;
        }
        hoverAnimator = ObjectAnimator.ofFloat(ninjaView, View.TRANSLATION_Y, 0f, dp(8));
        hoverAnimator.setDuration(500);
        hoverAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        hoverAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        hoverAnimator.start();
    }

    private void stopHover() {
        if (hoverAnimator != null) {
            hoverAnimator.cancel();
            hoverAnimator = null;
            ninjaView.setTranslationY(0f);
        }
    }

    private void detachInternal() {
        if (overlayRoot.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlayRoot.getParent()).removeView(overlayRoot);
        }
        currentParent = null;
    }

    private int dp(int value) {
        float density = appContext.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private int statusBarOffset() {
        int resourceId = appContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return appContext.getResources().getDimensionPixelSize(resourceId);
        }
        return dp(24);
    }
}

