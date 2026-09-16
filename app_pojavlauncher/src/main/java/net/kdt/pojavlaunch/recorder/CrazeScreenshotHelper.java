package net.kdt.pojavlaunch.recorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.game.GameView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Captures clean Minecraft screenshots directly from the game's hardware render surface
 * (SurfaceView / TextureView), completely excluding all on-screen touch buttons,
 * floating HUDs, drawer handles, and Android UI overlays.
 */
public class CrazeScreenshotHelper {
    private static final String TAG = "CrazeScreenshotHelper";

    public interface ScreenshotCallback {
        void onScreenshotSaved(File file);
        void onScreenshotFailed(String error);
    }

    public static void takeCleanGameScreenshot(Activity activity, GameView gameView) {
        takeCleanGameScreenshot(activity, gameView, null);
    }

    public static void takeCleanGameScreenshot(Activity activity, GameView gameView, ScreenshotCallback callback) {
        if (activity == null || gameView == null) {
            if (callback != null) callback.onScreenshotFailed("Game view is not available");
            return;
        }

        View surfaceView = gameView.getGameSurface();
        if (surfaceView == null) {
            // Surface may still be initializing
            Toast.makeText(activity, "Game surface is not ready yet", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onScreenshotFailed("Game surface is null");
            return;
        }

        int width = surfaceView.getWidth();
        int height = surfaceView.getHeight();

        if (width <= 0 || height <= 0) {
            width = gameView.getWidth();
            height = gameView.getHeight();
        }

        if (width <= 0 || height <= 0) {
            Toast.makeText(activity, "Game window dimensions unavailable", Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onScreenshotFailed("Invalid dimensions");
            return;
        }

        final int finalWidth = width;
        final int finalHeight = height;

        if (surfaceView instanceof TextureView) {
            TextureView textureView = (TextureView) surfaceView;
            Bitmap bitmap = textureView.getBitmap(Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888));
            if (bitmap != null) {
                saveBitmapToFile(activity, bitmap, callback);
            } else {
                Toast.makeText(activity, "Failed to extract TextureView framebuffer", Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onScreenshotFailed("TextureView bitmap extraction returned null");
            }
        } else if (surfaceView instanceof SurfaceView && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SurfaceView sv = (SurfaceView) surfaceView;
            Bitmap bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
            Handler handler = new Handler(Looper.getMainLooper());
            try {
                PixelCopy.request(sv, bitmap, copyResult -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        saveBitmapToFile(activity, bitmap, callback);
                    } else {
                        Log.e(TAG, "PixelCopy failed with error code: " + copyResult);
                        Toast.makeText(activity, "PixelCopy capture failed (code " + copyResult + ")", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onScreenshotFailed("PixelCopy failed: " + copyResult);
                    }
                }, handler);
            } catch (Throwable t) {
                Log.e(TAG, "PixelCopy request error", t);
                Toast.makeText(activity, "Screenshot capture error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onScreenshotFailed(t.getMessage());
            }
        } else {
            // Fallback for older devices: capture surface drawing cache
            try {
                surfaceView.setDrawingCacheEnabled(true);
                surfaceView.buildDrawingCache(true);
                Bitmap cache = surfaceView.getDrawingCache();
                if (cache != null) {
                    Bitmap copy = Bitmap.createBitmap(cache);
                    surfaceView.setDrawingCacheEnabled(false);
                    saveBitmapToFile(activity, copy, callback);
                } else {
                    Toast.makeText(activity, "Unable to capture game frame", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onScreenshotFailed("Drawing cache null");
                }
            } catch (Throwable t) {
                Log.e(TAG, "Fallback capture error", t);
                if (callback != null) callback.onScreenshotFailed(t.getMessage());
            }
        }
    }

    private static void saveBitmapToFile(Activity activity, Bitmap bitmap, ScreenshotCallback callback) {
        new Thread(() -> {
            try {
                File screenshotsDir = new File(Tools.DIR_GAME_HOME, "screenshots");
                if (!screenshotsDir.exists()) {
                    screenshotsDir.mkdirs();
                }

                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.US).format(new Date());
                File screenshotFile = new File(screenshotsDir, "Screenshot_" + timestamp + ".png");

                try (OutputStream os = new FileOutputStream(screenshotFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.flush();
                }

                // Index in Android MediaStore
                MediaScannerConnection.scanFile(
                        activity,
                        new String[]{screenshotFile.getAbsolutePath()},
                        new String[]{"image/png"},
                        null
                );

                Tools.runOnUiThread(() -> {
                    Toast.makeText(
                            activity,
                            "Saved screenshot as " + screenshotFile.getName(),
                            Toast.LENGTH_LONG
                    ).show();
                    if (callback != null) {
                        callback.onScreenshotSaved(screenshotFile);
                    }
                });

            } catch (Throwable e) {
                Log.e(TAG, "Failed to save screenshot bitmap", e);
                Tools.runOnUiThread(() -> {
                    Toast.makeText(activity, "Failed to save screenshot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onScreenshotFailed(e.getMessage());
                });
            }
        }).start();
    }
}
