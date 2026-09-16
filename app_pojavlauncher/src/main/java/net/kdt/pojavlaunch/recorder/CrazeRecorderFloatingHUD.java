package net.kdt.pojavlaunch.recorder;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import git.artdeell.mojo.R;

public class CrazeRecorderFloatingHUD implements CrazeRecorderManager.RecordStateListener {
    private final Activity mActivity;
    private final ViewGroup mParentContainer;
    private View mRootView;

    private LinearLayout mDragArea;
    private ImageView mStatusDot;
    private TextView mTimerTextView;
    private View mDivider;
    private LinearLayout mActionsContainer;

    private ImageButton mBtnPause;
    private ImageButton mBtnStop;
    private ImageButton mBtnScreenshot;
    private ImageButton mBtnControls;
    private ImageButton mBtnMic;
    private ImageButton mBtnSettings;
    private ImageButton mBtnCollapse;

    private ObjectAnimator mPulseAnimator;
    private boolean mIsCollapsed = false;
    private boolean mIsAttached = false;

    private float mInitialTouchX, mInitialTouchY;
    private float mInitialX, mInitialY;

    public CrazeRecorderFloatingHUD(Activity activity, ViewGroup parentContainer) {
        this.mActivity = activity;
        this.mParentContainer = parentContainer;
        initView();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView() {
        mRootView = LayoutInflater.from(mActivity).inflate(R.layout.floating_crazerecorder_hud, mParentContainer, false);

        mDragArea = mRootView.findViewById(R.id.crazerecorder_hud_drag_area);
        mStatusDot = mRootView.findViewById(R.id.crazerecorder_hud_dot);
        mTimerTextView = mRootView.findViewById(R.id.crazerecorder_hud_timer);
        mDivider = mRootView.findViewById(R.id.crazerecorder_hud_divider);
        mActionsContainer = mRootView.findViewById(R.id.crazerecorder_hud_actions);

        mBtnPause = mRootView.findViewById(R.id.crazerecorder_hud_btn_pause);
        mBtnStop = mRootView.findViewById(R.id.crazerecorder_hud_btn_stop);
        mBtnScreenshot = mRootView.findViewById(R.id.crazerecorder_hud_btn_screenshot);
        mBtnControls = mRootView.findViewById(R.id.crazerecorder_hud_btn_controls);
        mBtnMic = mRootView.findViewById(R.id.crazerecorder_hud_btn_mic);
        mBtnSettings = mRootView.findViewById(R.id.crazerecorder_hud_btn_settings);
        mBtnCollapse = mRootView.findViewById(R.id.crazerecorder_hud_btn_collapse);

        // Setup Pulse Animation for Red Dot
        mPulseAnimator = ObjectAnimator.ofFloat(mStatusDot, "alpha", 1f, 0.2f);
        mPulseAnimator.setDuration(700);
        mPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);

        // Actions
        net.kdt.pojavlaunch.utils.CrazeAnimationUtils.attachTouchFeedbackRecursively(mRootView);

        mBtnPause.setOnClickListener(v -> {
            CrazeRecorderManager manager = CrazeRecorderManager.getInstance();
            if (manager.isRecording()) {
                manager.pause(mActivity);
            } else if (manager.isPaused()) {
                manager.resume(mActivity);
            }
        });

        mBtnStop.setOnClickListener(v -> CrazeRecorderManager.getInstance().stop(mActivity));

        mBtnScreenshot.setOnClickListener(v -> {
            net.kdt.pojavlaunch.game.GameView gv = mActivity.findViewById(git.artdeell.mojo.R.id.main_game_render_view);
            if (gv != null) {
                CrazeScreenshotHelper.takeCleanGameScreenshot(mActivity, gv);
            }
        });

        mBtnControls.setOnClickListener(v -> {
            CrazeRecorderManager.getInstance().toggleGhostControls();
            updateControlsEyeIcon();
        });

        mBtnMic.setOnClickListener(v -> {
            LauncherPreferences.PREF_RECORDER_MIC_ENABLED = !LauncherPreferences.PREF_RECORDER_MIC_ENABLED;
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("recorder_mic_enabled", LauncherPreferences.PREF_RECORDER_MIC_ENABLED).apply();
            updateMicIcon();
        });

        mBtnSettings.setOnClickListener(v -> {
            if (mActivity instanceof CrazeRecorderDialogHost) {
                ((CrazeRecorderDialogHost) mActivity).openCrazeRecorderDialog();
            }
        });

        mBtnCollapse.setOnClickListener(v -> toggleCollapse());

        // Draggable
        mDragArea.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mInitialX = mRootView.getX();
                    mInitialY = mRootView.getY();
                    mInitialTouchX = event.getRawX();
                    mInitialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - mInitialTouchX;
                    float deltaY = event.getRawY() - mInitialTouchY;
                    mRootView.setX(mInitialX + deltaX);
                    mRootView.setY(mInitialY + deltaY);
                    return true;
            }
            return false;
        });

        updateControlsEyeIcon();
        updateMicIcon();

        mRootView.setVisibility(View.GONE);
    }

    public void attach() {
        if (!mIsAttached && mParentContainer != null) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.topMargin = 40;
            lp.leftMargin = 40;
            mParentContainer.addView(mRootView, lp);
            mIsAttached = true;
            CrazeRecorderManager.getInstance().addListener(this);
        }
    }

    public void detach() {
        if (mIsAttached && mParentContainer != null) {
            CrazeRecorderManager.getInstance().removeListener(this);
            mParentContainer.removeView(mRootView);
            mIsAttached = false;
        }
    }

    private void toggleCollapse() {
        mIsCollapsed = !mIsCollapsed;
        mActionsContainer.setVisibility(mIsCollapsed ? View.GONE : View.VISIBLE);
        mDivider.setVisibility(mIsCollapsed ? View.GONE : View.VISIBLE);
        mBtnCollapse.setImageResource(mIsCollapsed ? R.drawable.ic_craze_chevron_down : R.drawable.ic_craze_chevron_right);
    }

    private void updateControlsEyeIcon() {
        boolean hidden = CrazeRecorderManager.getInstance().areControlsHidden();
        mBtnControls.setImageResource(hidden ? R.drawable.ic_eye_hidden : R.drawable.ic_eye_visible);
    }

    private void updateMicIcon() {
        mBtnMic.setImageResource(LauncherPreferences.PREF_RECORDER_MIC_ENABLED ? R.drawable.ic_record_mic_on : R.drawable.ic_record_mic_off);
    }

    @Override
    public void onStateChanged(int state) {
        if (state == CrazeRecorderManager.STATE_RECORDING) {
            mRootView.setVisibility(LauncherPreferences.PREF_RECORDER_SHOW_HUD ? View.VISIBLE : View.GONE);
            mBtnPause.setImageResource(R.drawable.ic_record_pause);
            mStatusDot.setImageResource(R.drawable.ic_record_dot);
            if (!mPulseAnimator.isStarted()) {
                mPulseAnimator.start();
            }
            updateControlsEyeIcon();
        } else if (state == CrazeRecorderManager.STATE_PAUSED) {
            mRootView.setVisibility(LauncherPreferences.PREF_RECORDER_SHOW_HUD ? View.VISIBLE : View.GONE);
            mBtnPause.setImageResource(R.drawable.ic_record_play);
            mPulseAnimator.cancel();
            mStatusDot.setAlpha(1f);
            mStatusDot.setImageResource(R.drawable.ic_record_pause);
        } else {
            // STATE_IDLE
            mPulseAnimator.cancel();
            mRootView.setVisibility(View.GONE);
            mTimerTextView.setText("00:00");
        }
    }

    @Override
    public void onDurationUpdated(long durationMillis, String formattedDuration) {
        mTimerTextView.setText(formattedDuration);
    }

    @Override
    public void onError(String message) {
        mPulseAnimator.cancel();
        mRootView.setVisibility(View.GONE);
    }

    public interface CrazeRecorderDialogHost {
        void openCrazeRecorderDialog();
    }
}
