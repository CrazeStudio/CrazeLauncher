package net.kdt.pojavlaunch.prefs.screens;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

public abstract class LauncherBasePreferenceFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    protected Runnable mVisibilityUpdater = () -> {};

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.setBackgroundColor(Color.parseColor("#0B0E14"));
        super.onViewCreated(view, savedInstanceState);

        setDivider(new ColorDrawable(Color.parseColor("#191E28")));
        setDividerHeight((int) (1 * getResources().getDisplayMetrics().density));

        RecyclerView listView = getListView();
        if (listView != null) {
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            float density = getResources().getDisplayMetrics().density;
            int paddingSide;
            if (isLandscape && getParentFragment() != null) {
                paddingSide = (int) (20 * density);
            } else if (isLandscape) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int maxContentWidth = (int) (640 * density);
                paddingSide = screenWidth > maxContentWidth ? (screenWidth - maxContentWidth) / 2 : (int) (24 * density);
            } else {
                paddingSide = (int) (16 * density);
            }
            int paddingTopBottom = (int) (12 * density);
            listView.setPadding(paddingSide, paddingTopBottom, paddingSide, paddingTopBottom);
            listView.setClipToPadding(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mVisibilityUpdater.run();
    }

    @Override
    public void onPause() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        if (sharedPreferences != null) sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        LauncherPreferences.loadPreferences(getContext());
    }

    protected Preference requirePreference(CharSequence key) {
        Preference preference = findPreference(key);
        if (preference != null) return preference;
        throw new IllegalStateException("Preference " + key + " is null");
    }

    @SuppressWarnings("unchecked")
    protected <T extends Preference> T requirePreference(CharSequence key, Class<T> preferenceClass) {
        Preference preference = requirePreference(key);
        if (preferenceClass.isInstance(preference)) return (T) preference;
        throw new IllegalStateException("Preference " + key + " is not an instance of " + preferenceClass.getSimpleName());
    }

    protected LauncherActivity getLauncherActivity() {
        Activity activity = getActivity();
        if (activity instanceof LauncherActivity) {
            return (LauncherActivity) activity;
        }
        return null;
    }

    protected void setupNotificationRequestPreference() {
        Preference mRequestNotificationPermissionPreference = findPreference("notification_permission_request");
        if (mRequestNotificationPermissionPreference == null) return;
        Activity activity = getActivity();
        if (activity instanceof LauncherActivity) {
            mRequestNotificationPermissionPreference.setOnPreferenceClickListener(preference -> {
                ((LauncherActivity) activity).askForPermission(33, Manifest.permission.POST_NOTIFICATIONS);
                return true;
            });
        } else {
            mRequestNotificationPermissionPreference.setVisible(false);
        }
        updateNotificationVisibility();
    }

    protected void updateNotificationVisibility() {
        Preference pref = findPreference("notification_permission_request");
        LauncherActivity activity = getLauncherActivity();
        if (pref != null && activity != null) {
            pref.setVisible(!activity.checkForPermission(33, Manifest.permission.POST_NOTIFICATIONS));
        }
    }
}
