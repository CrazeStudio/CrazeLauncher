package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;
import git.artdeell.mojo.R;

public class LauncherPreferenceMainFragment extends LauncherBasePreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        mVisibilityUpdater = this::updateNotificationVisibility;
        addPreferencesFromResource(R.xml.pref_main);
        setupNotificationRequestPreference();
    }
}
