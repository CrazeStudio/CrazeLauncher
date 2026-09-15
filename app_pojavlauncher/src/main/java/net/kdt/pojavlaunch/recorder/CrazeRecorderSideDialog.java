package net.kdt.pojavlaunch.recorder;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.kdt.SideDialogView;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import git.artdeell.mojo.R;

public class CrazeRecorderSideDialog extends SideDialogView implements CrazeRecorderManager.RecordStateListener {
    private final Activity mActivity;

    private ImageView mStatusIcon;
    private TextView mStatusTitle;
    private TextView mStatusTimer;
    private Button mBtnMainAction;
    private Button mBtnPauseAction;

    private Switch mSwitchExcludeControls;
    private Switch mSwitchMic;
    private Switch mSwitchHud;

    private Spinner mSpinnerResolution;
    private Spinner mSpinnerFps;
    private Spinner mSpinnerBitrate;
    private Button mBtnRecordings;

    private static final String[] RESOLUTIONS = {"1080p (1920x1080)", "720p (1280x720)", "Native Display"};
    private static final int[] RESOLUTION_VALUES = {1080, 720, 0};

    private static final String[] FRAME_RATES = {"60 FPS (Smooth)", "30 FPS (Battery Saver)"};
    private static final int[] FPS_VALUES = {60, 30};

    private static final String[] BITRATES = {"4 Mbps (Standard)", "8 Mbps (High)", "12 Mbps (Ultra)", "16 Mbps (Maximum)"};
    private static final int[] BITRATE_VALUES = {4000000, 8000000, 12000000, 16000000};

    public CrazeRecorderSideDialog(Activity activity, ViewGroup parent) {
        super(activity, parent, R.layout.dialog_crazerecorder);
        this.mActivity = activity;
        setTitle(R.string.crazer_recorder_title);
        setEndButtonListener(android.R.string.ok, v -> disappear(false));
    }

    @Override
    protected void onInflate() {
        if (mDialogContent == null) return;
        mStatusIcon = mDialogContent.findViewById(R.id.crazerecorder_status_icon);
        mStatusTitle = mDialogContent.findViewById(R.id.crazerecorder_status_title);
        mStatusTimer = mDialogContent.findViewById(R.id.crazerecorder_status_timer);
        mBtnMainAction = mDialogContent.findViewById(R.id.crazerecorder_btn_main_action);
        mBtnPauseAction = mDialogContent.findViewById(R.id.crazerecorder_btn_pause_action);

        mSwitchExcludeControls = mDialogContent.findViewById(R.id.crazerecorder_switch_exclude_controls);
        mSwitchMic = mDialogContent.findViewById(R.id.crazerecorder_switch_mic);
        mSwitchHud = mDialogContent.findViewById(R.id.crazerecorder_switch_hud);

        mSpinnerResolution = mDialogContent.findViewById(R.id.crazerecorder_spinner_resolution);
        mSpinnerFps = mDialogContent.findViewById(R.id.crazerecorder_spinner_fps);
        mSpinnerBitrate = mDialogContent.findViewById(R.id.crazerecorder_spinner_bitrate);
        mBtnRecordings = mDialogContent.findViewById(R.id.crazerecorder_btn_recordings);

        setupSwitches();
        setupSpinners();
        setupButtons();
    }

    @Override
    protected void onAppear() {
        CrazeRecorderManager.getInstance().addListener(this);
        updateUI(CrazeRecorderManager.getInstance().getState());
    }

    @Override
    protected void onDisappear() {
        CrazeRecorderManager.getInstance().removeListener(this);
    }

    @Override
    protected void onDestroy() {
        CrazeRecorderManager.getInstance().removeListener(this);
    }

    public void cancel() {
        disappear(false);
    }

    private void setupSwitches() {
        mSwitchExcludeControls.setChecked(LauncherPreferences.PREF_RECORDER_EXCLUDE_CONTROLS);
        mSwitchExcludeControls.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LauncherPreferences.PREF_RECORDER_EXCLUDE_CONTROLS = isChecked;
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("recorder_exclude_controls", isChecked).apply();
            if (CrazeRecorderManager.getInstance().isRecording() || CrazeRecorderManager.getInstance().isPaused()) {
                CrazeRecorderManager.getInstance().setControlsHidden(isChecked);
            }
        });

        mSwitchMic.setChecked(LauncherPreferences.PREF_RECORDER_MIC_ENABLED);
        mSwitchMic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LauncherPreferences.PREF_RECORDER_MIC_ENABLED = isChecked;
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("recorder_mic_enabled", isChecked).apply();
        });

        mSwitchHud.setChecked(LauncherPreferences.PREF_RECORDER_SHOW_HUD);
        mSwitchHud.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LauncherPreferences.PREF_RECORDER_SHOW_HUD = isChecked;
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean("recorder_show_hud", isChecked).apply();
        });
    }

    private void setupSpinners() {
        Context context = mActivity;

        // Resolution Spinner
        ArrayAdapter<String> resAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, RESOLUTIONS);
        mSpinnerResolution.setAdapter(resAdapter);
        int selectedResIdx = 0;
        for (int i = 0; i < RESOLUTION_VALUES.length; i++) {
            if (RESOLUTION_VALUES[i] == LauncherPreferences.PREF_RECORDER_RESOLUTION) {
                selectedResIdx = i;
                break;
            }
        }
        mSpinnerResolution.setSelection(selectedResIdx);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LauncherPreferences.PREF_RECORDER_RESOLUTION = RESOLUTION_VALUES[position];
                LauncherPreferences.DEFAULT_PREF.edit().putInt("recorder_resolution", RESOLUTION_VALUES[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // FPS Spinner
        ArrayAdapter<String> fpsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, FRAME_RATES);
        mSpinnerFps.setAdapter(fpsAdapter);
        int selectedFpsIdx = 0;
        for (int i = 0; i < FPS_VALUES.length; i++) {
            if (FPS_VALUES[i] == LauncherPreferences.PREF_RECORDER_FPS) {
                selectedFpsIdx = i;
                break;
            }
        }
        mSpinnerFps.setSelection(selectedFpsIdx);
        mSpinnerFps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LauncherPreferences.PREF_RECORDER_FPS = FPS_VALUES[position];
                LauncherPreferences.DEFAULT_PREF.edit().putInt("recorder_fps", FPS_VALUES[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Bitrate Spinner
        ArrayAdapter<String> bitAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, BITRATES);
        mSpinnerBitrate.setAdapter(bitAdapter);
        int selectedBitIdx = 1; // 8 Mbps default
        for (int i = 0; i < BITRATE_VALUES.length; i++) {
            if (BITRATE_VALUES[i] == LauncherPreferences.PREF_RECORDER_BITRATE) {
                selectedBitIdx = i;
                break;
            }
        }
        mSpinnerBitrate.setSelection(selectedBitIdx);
        mSpinnerBitrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LauncherPreferences.PREF_RECORDER_BITRATE = BITRATE_VALUES[position];
                LauncherPreferences.DEFAULT_PREF.edit().putInt("recorder_bitrate", BITRATE_VALUES[position]).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupButtons() {
        mBtnMainAction.setOnClickListener(v -> {
            CrazeRecorderManager manager = CrazeRecorderManager.getInstance();
            if (manager.isIdle()) {
                disappear(false);
                manager.requestStartRecording(mActivity);
            } else {
                manager.stop(mActivity);
            }
        });

        mBtnPauseAction.setOnClickListener(v -> {
            CrazeRecorderManager manager = CrazeRecorderManager.getInstance();
            if (manager.isRecording()) {
                manager.pause(mActivity);
            } else if (manager.isPaused()) {
                manager.resume(mActivity);
            }
        });

        mBtnRecordings.setOnClickListener(v -> RecordingsListDialog.show(mActivity));
    }

    private void updateUI(int state) {
        if (mStatusTitle == null) return;

        boolean isRecordingOrPaused = (state == CrazeRecorderManager.STATE_RECORDING || state == CrazeRecorderManager.STATE_PAUSED);
        mSpinnerResolution.setEnabled(!isRecordingOrPaused);
        mSpinnerFps.setEnabled(!isRecordingOrPaused);
        mSpinnerBitrate.setEnabled(!isRecordingOrPaused);

        if (state == CrazeRecorderManager.STATE_RECORDING) {
            mStatusTitle.setText(R.string.crazer_recorder_status_recording);
            mStatusTitle.setTextColor(0xFFFF5252);
            mBtnMainAction.setText(R.string.crazer_recorder_stop);
            mBtnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF424242));
            mBtnPauseAction.setVisibility(View.VISIBLE);
            mBtnPauseAction.setText(R.string.crazer_recorder_pause);
        } else if (state == CrazeRecorderManager.STATE_PAUSED) {
            mStatusTitle.setText(R.string.crazer_recorder_status_paused);
            mStatusTitle.setTextColor(0xFFFFD54F);
            mBtnMainAction.setText(R.string.crazer_recorder_stop);
            mBtnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF424242));
            mBtnPauseAction.setVisibility(View.VISIBLE);
            mBtnPauseAction.setText(R.string.crazer_recorder_resume);
        } else {
            // IDLE
            mStatusTitle.setText(R.string.crazer_recorder_status_idle);
            mStatusTitle.setTextColor(0xFFFFFFFF);
            mStatusTimer.setText("00:00:00");
            mBtnMainAction.setText(R.string.crazer_recorder_start);
            mBtnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F));
            mBtnPauseAction.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStateChanged(int state) {
        updateUI(state);
    }

    @Override
    public void onDurationUpdated(long durationMillis, String formattedDuration) {
        if (mStatusTimer != null) {
            mStatusTimer.setText(formattedDuration);
        }
    }

    @Override
    public void onError(String message) {
        updateUI(CrazeRecorderManager.STATE_IDLE);
    }
}
