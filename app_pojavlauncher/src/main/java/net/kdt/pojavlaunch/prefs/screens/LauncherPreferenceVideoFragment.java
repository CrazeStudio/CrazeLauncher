package net.kdt.pojavlaunch.prefs.screens;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.plugins.LibraryPlugin;
import net.kdt.pojavlaunch.prefs.CustomSeekBarPreference;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.RendererCompatUtil;

/**
 * Fragment for any settings video related
 */
public class LauncherPreferenceVideoFragment extends LauncherPreferenceFragment {

    private final ActivityResultLauncher<String> mPickApkLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && getContext() != null) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                        Toast.makeText(getContext(), "Opening Android Package Installer...", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("LauncherPrefVideo", "Failed to launch package installer for APK", e);
                        Toast.makeText(getContext(), "Error opening package installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
    );

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_video);
        int resolution = (int) (LauncherPreferences.PREF_SCALE_FACTOR * 100);

        CustomSeekBarPreference resolutionSeekbar = requirePreference("resolutionRatio",
                CustomSeekBarPreference.class);
        resolutionSeekbar.setSuffix(" %");

        // #724 bug fix
        if (resolution < 25) {
            resolutionSeekbar.setValue(100);
        } else {
            resolutionSeekbar.setValue(resolution);
        }

        // Sustained performance is only available since Nougat
        SwitchPreference sustainedPerfSwitch = requirePreference("sustainedPerformance",
                SwitchPreference.class);
        sustainedPerfSwitch.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        sustainedPerfSwitch.setChecked(LauncherPreferences.PREF_SUSTAINED_PERFORMANCE);

        requirePreference("alternate_surface", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
        requirePreference("force_vsync", SwitchPreferenceCompat.class).setChecked(LauncherPreferences.PREF_FORCE_VSYNC);

        // Show ANGLE switch only if AnglePlugin is available
        LibraryPlugin angle = LibraryPlugin.discoverPlugin(getContext(), LibraryPlugin.ID_ANGLE_PLUGIN);
        SwitchPreferenceCompat angleSwitch = requirePreference("use_angle", SwitchPreferenceCompat.class);
        angleSwitch.setVisible(angle != null);
        angleSwitch.setChecked(LauncherPreferences.PREF_USE_ANGLE);

        // Same but for ZINK plugin
        SwitchPreference legacyZink = requirePreference("zinkForceLegacy", SwitchPreference.class);
        legacyZink.setChecked(LauncherPreferences.PREF_ZINK_FORCE_LEGACY);
        if(!Architecture.isx86Device()) {
            LibraryPlugin zink = LibraryPlugin.discoverPlugin(getContext(), LibraryPlugin.ID_ZINK_PLUGIN);
            legacyZink.setVisible(zink != null);
        }
        else {
            legacyZink.setVisible(false);
        }

        Preference addApkPref = findPreference("fcl_add_plugin_apk");
        if (addApkPref != null) {
            addApkPref.setOnPreferenceClickListener(preference -> {
                mPickApkLauncher.launch("*/*");
                return true;
            });
        }

        refreshRendererList();
        computeVisibility();
    }

    private void refreshRendererList() {
        ListPreference rendererListPreference = findPreference("renderer");
        if (rendererListPreference != null && getContext() != null) {
            RendererCompatUtil.releaseRenderersCache();
            RendererCompatUtil.RenderersList renderersList = RendererCompatUtil.getCompatibleRenderers(getContext());
            rendererListPreference.setEntries(renderersList.rendererDisplayNames);
            rendererListPreference.setEntryValues(renderersList.rendererIds.toArray(new String[0]));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if(activity != null) {
            requirePreference("ignoreNotch").setVisible(LauncherPreferences.hasNotch(activity));
        }
        refreshRendererList();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String s) {
        super.onSharedPreferenceChanged(p, s);
        computeVisibility();
    }

    private void computeVisibility(){
        requirePreference("force_vsync", SwitchPreferenceCompat.class)
                .setVisible(LauncherPreferences.PREF_USE_ALTERNATE_SURFACE);
    }
}
