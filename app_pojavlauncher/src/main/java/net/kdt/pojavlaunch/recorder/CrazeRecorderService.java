package net.kdt.pojavlaunch.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.kdt.pojavlaunch.Tools;
import git.artdeell.mojo.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrazeRecorderService extends Service {
    private static final String TAG = "CrazeRecorderService";
    public static final String CHANNEL_ID = "crazerecorder_channel";
    public static final int NOTIFICATION_ID = 8842;

    public static final String ACTION_START = "net.kdt.pojavlaunch.recorder.ACTION_START";
    public static final String ACTION_PAUSE = "net.kdt.pojavlaunch.recorder.ACTION_PAUSE";
    public static final String ACTION_RESUME = "net.kdt.pojavlaunch.recorder.ACTION_RESUME";
    public static final String ACTION_STOP = "net.kdt.pojavlaunch.recorder.ACTION_STOP";

    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_DATA = "extra_data";
    public static final String EXTRA_WIDTH = "extra_width";
    public static final String EXTRA_HEIGHT = "extra_height";
    public static final String EXTRA_DPI = "extra_dpi";
    public static final String EXTRA_FPS = "extra_fps";
    public static final String EXTRA_BITRATE = "extra_bitrate";
    public static final String EXTRA_MIC_ENABLED = "extra_mic_enabled";

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;

    private String mCurrentOutputFile;
    private boolean mIsRecording = false;
    private boolean mIsPaused = false;
    private long mStartTime = 0;
    private long mElapsedTime = 0;
    private long mPauseStartTime = 0;

    private final Handler mTimerHandler = new Handler(Looper.getMainLooper());
    private final Runnable mTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsRecording && !mIsPaused) {
                long currentDuration = System.currentTimeMillis() - mStartTime - mElapsedTime;
                CrazeRecorderManager.getInstance().updateDuration(currentDuration);
                updateNotification();
                mTimerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        switch (action) {
            case ACTION_START:
                int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                Intent data = intent.getParcelableExtra(EXTRA_DATA);
                int width = intent.getIntExtra(EXTRA_WIDTH, 1920);
                int height = intent.getIntExtra(EXTRA_HEIGHT, 1080);
                int dpi = intent.getIntExtra(EXTRA_DPI, 320);
                int fps = intent.getIntExtra(EXTRA_FPS, 60);
                int bitrate = intent.getIntExtra(EXTRA_BITRATE, 8000000);
                boolean micEnabled = intent.getBooleanExtra(EXTRA_MIC_ENABLED, false);

                startRecordingInternal(resultCode, data, width, height, dpi, fps, bitrate, micEnabled);
                break;

            case ACTION_PAUSE:
                pauseRecordingInternal();
                break;

            case ACTION_RESUME:
                resumeRecordingInternal();
                break;

            case ACTION_STOP:
                stopRecordingInternal();
                break;
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.crazer_recorder_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.crazer_recorder_channel_desc));
            channel.setShowBadge(false);
            channel.enableVibration(false);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        long currentDuration = 0;
        if (mIsRecording) {
            if (mIsPaused) {
                currentDuration = mPauseStartTime - mStartTime - mElapsedTime;
            } else {
                currentDuration = System.currentTimeMillis() - mStartTime - mElapsedTime;
            }
        }
        if (currentDuration < 0) currentDuration = 0;

        String formattedTime = CrazeRecorderManager.formatDuration(currentDuration);
        String statusText = mIsPaused
                ? getString(R.string.crazer_recorder_status_paused) + " (" + formattedTime + ")"
                : getString(R.string.crazer_recorder_status_recording) + " (" + formattedTime + ")";

        Intent stopIntent = new Intent(this, CrazeRecorderService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent toggleIntent = new Intent(this, CrazeRecorderService.class);
        toggleIntent.setAction(mIsPaused ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent togglePendingIntent = PendingIntent.getService(this, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.crazer_recorder_title))
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_crazerecorder)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(
                        mIsPaused ? R.drawable.ic_record_play : R.drawable.ic_record_pause,
                        mIsPaused ? getString(R.string.crazer_recorder_resume) : getString(R.string.crazer_recorder_pause),
                        togglePendingIntent
                )
                .addAction(
                        R.drawable.ic_record_stop,
                        getString(R.string.crazer_recorder_stop),
                        stopPendingIntent
                );

        return builder.build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null && mIsRecording) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void startRecordingInternal(int resultCode, Intent data, int width, int height, int dpi, int fps, int bitrate, boolean micEnabled) {
        if (mIsRecording) {
            Log.w(TAG, "Already recording");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIFICATION_ID, buildNotification());
            }

            File dir = new File(Tools.DIR_GAME_HOME, "Recordings");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File outputFile = new File(dir, "CrazeRecorder_" + timestamp + ".mp4");
            mCurrentOutputFile = outputFile.getAbsolutePath();

            // Ensure dimensions are even numbers for encoders
            int safeWidth = (width / 2) * 2;
            int safeHeight = (height / 2) * 2;

            mMediaRecorder = new MediaRecorder();
            if (micEnabled) {
                try {
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                } catch (Throwable t) {
                    Log.e(TAG, "Could not set MIC audio source", t);
                }
            }

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            if (micEnabled) {
                try {
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mMediaRecorder.setAudioEncodingBitRate(128000);
                    mMediaRecorder.setAudioSamplingRate(44100);
                } catch (Throwable t) {
                    Log.e(TAG, "Could not set audio encoder", t);
                }
            }

            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoSize(safeWidth, safeHeight);
            mMediaRecorder.setVideoFrameRate(fps);
            mMediaRecorder.setVideoEncodingBitRate(bitrate);
            mMediaRecorder.setOutputFile(mCurrentOutputFile);
            mMediaRecorder.prepare();

            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            if (mMediaProjection == null) {
                throw new IllegalStateException("Failed to obtain MediaProjection");
            }

            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                    "CrazeRecorderDisplay",
                    safeWidth,
                    safeHeight,
                    dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mMediaRecorder.getSurface(),
                    null,
                    null
            );

            mMediaRecorder.start();
            mIsRecording = true;
            mIsPaused = false;
            mStartTime = System.currentTimeMillis();
            mElapsedTime = 0;

            mTimerHandler.post(mTimerRunnable);
            CrazeRecorderManager.getInstance().onRecordingStarted(mCurrentOutputFile);

        } catch (Throwable e) {
            Log.e(TAG, "Error starting screen recording", e);
            cleanup();
            CrazeRecorderManager.getInstance().onRecordingError(e.getMessage());
            stopForeground(true);
            stopSelf();
        }
    }

    private void pauseRecordingInternal() {
        if (!mIsRecording || mIsPaused) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mMediaRecorder != null) {
            try {
                mMediaRecorder.pause();
                mIsPaused = true;
                mPauseStartTime = System.currentTimeMillis();
                mTimerHandler.removeCallbacks(mTimerRunnable);
                updateNotification();
                CrazeRecorderManager.getInstance().onRecordingPaused();
            } catch (Throwable e) {
                Log.e(TAG, "Failed to pause recording", e);
            }
        }
    }

    private void resumeRecordingInternal() {
        if (!mIsRecording || !mIsPaused) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mMediaRecorder != null) {
            try {
                mMediaRecorder.resume();
                mElapsedTime += (System.currentTimeMillis() - mPauseStartTime);
                mIsPaused = false;
                mTimerHandler.post(mTimerRunnable);
                updateNotification();
                CrazeRecorderManager.getInstance().onRecordingResumed();
            } catch (Throwable e) {
                Log.e(TAG, "Failed to resume recording", e);
            }
        }
    }

    private void stopRecordingInternal() {
        if (!mIsRecording) {
            stopForeground(true);
            stopSelf();
            return;
        }

        mTimerHandler.removeCallbacks(mTimerRunnable);

        try {
            if (mMediaRecorder != null) {
                try {
                    mMediaRecorder.stop();
                } catch (Throwable t) {
                    Log.w(TAG, "MediaRecorder stop failed, file may be empty", t);
                }
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }

            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }

            if (mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }

            if (mCurrentOutputFile != null) {
                final File savedFile = new File(mCurrentOutputFile);
                if (savedFile.exists() && savedFile.length() > 0) {
                    MediaScannerConnection.scanFile(
                            getApplicationContext(),
                            new String[]{savedFile.getAbsolutePath()},
                            new String[]{"video/mp4"},
                            null
                    );
                    Tools.runOnUiThread(() -> Toast.makeText(
                            getApplicationContext(),
                            getString(R.string.crazer_recorder_saved_toast) + ": " + savedFile.getName(),
                            Toast.LENGTH_LONG
                    ).show());
                }
            }

            CrazeRecorderManager.getInstance().onRecordingStopped(mCurrentOutputFile);

        } catch (Throwable e) {
            Log.e(TAG, "Error stopping recording", e);
            CrazeRecorderManager.getInstance().onRecordingError(e.getMessage());
        } finally {
            cleanup();
            stopForeground(true);
            stopSelf();
        }
    }

    private void cleanup() {
        mIsRecording = false;
        mIsPaused = false;
        mTimerHandler.removeCallbacks(mTimerRunnable);

        if (mMediaRecorder != null) {
            try { mMediaRecorder.release(); } catch (Throwable ignored) {}
            mMediaRecorder = null;
        }
        if (mVirtualDisplay != null) {
            try { mVirtualDisplay.release(); } catch (Throwable ignored) {}
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            try { mMediaProjection.stop(); } catch (Throwable ignored) {}
            mMediaProjection = null;
        }
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
