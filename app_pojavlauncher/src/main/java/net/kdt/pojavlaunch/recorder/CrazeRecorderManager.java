package net.kdt.pojavlaunch.recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CrazeRecorderManager {
    public static final int REQUEST_CODE_MEDIA_PROJECTION = 9912;
    public static final int REQUEST_CODE_AUDIO_PERMISSION = 9913;

    public static final int STATE_IDLE = 0;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_PAUSED = 2;

    private static CrazeRecorderManager sInstance;

    private int mCurrentState = STATE_IDLE;
    private long mCurrentDurationMillis = 0;
    private String mLastSavedFilePath = null;

    private WeakReference<ControlLayout> mControlLayoutRef;
    private final List<RecordStateListener> mListeners = new ArrayList<>();

    public interface RecordStateListener {
        void onStateChanged(int state);
        void onDurationUpdated(long durationMillis, String formattedDuration);
        void onError(String message);
    }

    public static synchronized CrazeRecorderManager getInstance() {
        if (sInstance == null) {
            sInstance = new CrazeRecorderManager();
        }
        return sInstance;
    }

    private CrazeRecorderManager() {}

    public void setControlLayout(ControlLayout controlLayout) {
        this.mControlLayoutRef = new WeakReference<>(controlLayout);
    }

    public void addListener(RecordStateListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
            listener.onStateChanged(mCurrentState);
            listener.onDurationUpdated(mCurrentDurationMillis, formatDuration(mCurrentDurationMillis));
        }
    }

    public void removeListener(RecordStateListener listener) {
        mListeners.remove(listener);
    }

    public int getState() {
        return mCurrentState;
    }

    public boolean isRecording() {
        return mCurrentState == STATE_RECORDING;
    }

    public boolean isPaused() {
        return mCurrentState == STATE_PAUSED;
    }

    public boolean isIdle() {
        return mCurrentState == STATE_IDLE;
    }

    public long getCurrentDurationMillis() {
        return mCurrentDurationMillis;
    }

    public String getLastSavedFilePath() {
        return mLastSavedFilePath;
    }

    public void toggleRecording(Context context) {
        if (isIdle()) {
            if (context instanceof Activity) {
                requestStartRecording((Activity) context);
            }
        } else {
            stop(context);
        }
    }

    public void requestStartRecording(Activity activity) {
        if (mCurrentState != STATE_IDLE) {
            return;
        }

        // Check microphone permission if mic audio is enabled
        if (LauncherPreferences.PREF_RECORDER_MIC_ENABLED) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_PERMISSION);
                return;
            }
        }

        // Request MediaProjection Screen Capture permission
        MediaProjectionManager projectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (projectionManager != null) {
            activity.startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION);
        } else {
            Toast.makeText(activity, "MediaProjection service not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    public void onMediaProjectionResult(Activity activity, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            Toast.makeText(activity, "Screen capture permission was declined", Toast.LENGTH_SHORT).show();
            return;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

        int screenW = Math.max(metrics.widthPixels, metrics.heightPixels); // Landscape width
        int screenH = Math.min(metrics.widthPixels, metrics.heightPixels); // Landscape height
        int dpi = metrics.densityDpi;

        int targetResolution = LauncherPreferences.PREF_RECORDER_RESOLUTION;
        int targetWidth, targetHeight;

        if (targetResolution == 720) {
            targetHeight = 720;
            targetWidth = (int) (720f * ((float) screenW / screenH));
        } else if (targetResolution == 1080) {
            targetHeight = 1080;
            targetWidth = (int) (1080f * ((float) screenW / screenH));
        } else {
            // Native resolution
            targetWidth = screenW;
            targetHeight = screenH;
        }

        // Enforce even dimensions
        targetWidth = (targetWidth / 2) * 2;
        targetHeight = (targetHeight / 2) * 2;

        int fps = LauncherPreferences.PREF_RECORDER_FPS;
        int bitrate = LauncherPreferences.PREF_RECORDER_BITRATE;
        boolean micEnabled = LauncherPreferences.PREF_RECORDER_MIC_ENABLED;

        // Apply invisible controls if requested
        if (LauncherPreferences.PREF_RECORDER_EXCLUDE_CONTROLS) {
            setControlsHidden(true);
        }

        Intent serviceIntent = new Intent(activity, CrazeRecorderService.class);
        serviceIntent.setAction(CrazeRecorderService.ACTION_START);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_RESULT_CODE, resultCode);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_DATA, data);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_WIDTH, targetWidth);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_HEIGHT, targetHeight);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_DPI, dpi);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_FPS, fps);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_BITRATE, bitrate);
        serviceIntent.putExtra(CrazeRecorderService.EXTRA_MIC_ENABLED, micEnabled);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(serviceIntent);
        } else {
            activity.startService(serviceIntent);
        }
    }

    public void pause(Context context) {
        Intent intent = new Intent(context, CrazeRecorderService.class);
        intent.setAction(CrazeRecorderService.ACTION_PAUSE);
        context.startService(intent);
    }

    public void resume(Context context) {
        Intent intent = new Intent(context, CrazeRecorderService.class);
        intent.setAction(CrazeRecorderService.ACTION_RESUME);
        context.startService(intent);
    }

    public void stop(Context context) {
        Intent intent = new Intent(context, CrazeRecorderService.class);
        intent.setAction(CrazeRecorderService.ACTION_STOP);
        context.startService(intent);
    }

    public void toggleGhostControls() {
        ControlLayout layout = getControlLayout();
        if (layout != null) {
            boolean currentHidden = layout.isRecordingHideControls();
            setControlsHidden(!currentHidden);
        }
    }

    public boolean areControlsHidden() {
        ControlLayout layout = getControlLayout();
        return layout != null && layout.isRecordingHideControls();
    }

    public void setControlsHidden(boolean hide) {
        Tools.runOnUiThread(() -> {
            ControlLayout layout = getControlLayout();
            if (layout != null) {
                layout.setRecordingHideControls(hide);
            }
        });
    }

    public void updateControlsVisibility() {
        Tools.runOnUiThread(() -> {
            ControlLayout layout = getControlLayout();
            if (layout != null) {
                layout.updateButtonOpacity();
            }
        });
    }

    private ControlLayout getControlLayout() {
        return mControlLayoutRef != null ? mControlLayoutRef.get() : null;
    }

    // Called from Service
    void onRecordingStarted(String outputFile) {
        mCurrentState = STATE_RECORDING;
        mCurrentDurationMillis = 0;
        mLastSavedFilePath = outputFile;
        notifyStateChanged();
    }

    void onRecordingPaused() {
        mCurrentState = STATE_PAUSED;
        notifyStateChanged();
    }

    void onRecordingResumed() {
        mCurrentState = STATE_RECORDING;
        notifyStateChanged();
    }

    void onRecordingStopped(String outputFile) {
        mCurrentState = STATE_IDLE;
        mCurrentDurationMillis = 0;
        mLastSavedFilePath = outputFile;
        // Restore controls visibility when recording stops
        setControlsHidden(false);
        notifyStateChanged();
    }

    void onRecordingError(String error) {
        mCurrentState = STATE_IDLE;
        mCurrentDurationMillis = 0;
        setControlsHidden(false);
        notifyStateChanged();
        Tools.runOnUiThread(() -> {
            for (RecordStateListener listener : new ArrayList<>(mListeners)) {
                listener.onError(error);
            }
        });
    }

    void updateDuration(long durationMillis) {
        mCurrentDurationMillis = durationMillis;
        String formatted = formatDuration(durationMillis);
        Tools.runOnUiThread(() -> {
            for (RecordStateListener listener : new ArrayList<>(mListeners)) {
                listener.onDurationUpdated(durationMillis, formatted);
            }
        });
    }

    private void notifyStateChanged() {
        Tools.runOnUiThread(() -> {
            for (RecordStateListener listener : new ArrayList<>(mListeners)) {
                listener.onStateChanged(mCurrentState);
            }
        });
    }

    public static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
}
