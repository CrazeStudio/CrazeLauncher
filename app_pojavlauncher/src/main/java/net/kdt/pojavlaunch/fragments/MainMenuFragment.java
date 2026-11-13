package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.fragments.InstanceManagementFragment;
import net.kdt.pojavlaunch.fragments.ProfileTypeSelectFragment;
import net.kdt.pojavlaunch.fragments.SearchModFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.CrazeAnimationUtils;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.util.Locale;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        View mNewsButton = view.findViewById(R.id.news_button);
        View mDiscordButton = view.findViewById(R.id.social_media_button);
        View mCustomControlButton = view.findViewById(R.id.custom_control_button);
        View mInstallJarButton = view.findViewById(R.id.install_jar_button);
        View mShareLogsButton = view.findViewById(R.id.share_logs_button);
        View mOpenDirectoryButton = view.findViewById(R.id.open_files_button);
        View mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        View mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        // Sidebar Navigation
        View navHome = view.findViewById(R.id.nav_home);
        View navInstances = view.findViewById(R.id.nav_instances);
        View navMods = view.findViewById(R.id.nav_mods);
        View navPacks = view.findViewById(R.id.nav_packs);
        View navSettings = view.findViewById(R.id.nav_settings);
        View navResourcePacks = view.findViewById(R.id.nav_resource_packs);
        View navShaderPacks = view.findViewById(R.id.nav_shader_packs);
        View navDownloads = view.findViewById(R.id.nav_downloads);
        View navAbout = view.findViewById(R.id.nav_about);

        if (navHome != null) {
            CrazeAnimationUtils.attachTouchFeedback(navHome);
            navHome.setOnClickListener(v -> {
                if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
            });
        }
        if (navInstances != null) {
            CrazeAnimationUtils.attachTouchFeedback(navInstances);
            navInstances.setOnClickListener(v -> Tools.swapFragment(requireActivity(), InstanceManagementFragment.class, InstanceManagementFragment.TAG, null));
        }
        if (navMods != null) {
            CrazeAnimationUtils.attachTouchFeedback(navMods);
            navMods.setOnClickListener(v -> Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null));
        }
        if (navPacks != null) {
            CrazeAnimationUtils.attachTouchFeedback(navPacks);
            navPacks.setOnClickListener(v -> openGameDirectory(requireContext()));
        }
        if (navSettings != null) {
            CrazeAnimationUtils.attachTouchFeedback(navSettings);
            navSettings.setOnClickListener(v -> Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null));
        }

        if (navResourcePacks != null) {
            CrazeAnimationUtils.attachTouchFeedback(navResourcePacks);
            navResourcePacks.setOnClickListener(v -> openProjectBrowser("resourcepack"));
        }
        if (navShaderPacks != null) {
            CrazeAnimationUtils.attachTouchFeedback(navShaderPacks);
            navShaderPacks.setOnClickListener(v -> openProjectBrowser("shader"));
        }
        if (navDownloads != null) {
            CrazeAnimationUtils.attachTouchFeedback(navDownloads);
            navDownloads.setOnClickListener(v -> showDownloadsMenu());
        }
        if (navAbout != null) {
            CrazeAnimationUtils.attachTouchFeedback(navAbout);
            navAbout.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("CrazeLauncher")
                    .setMessage("A modern Minecraft: Java Edition launcher for Android, powered by PojavLauncher.")
                    .setPositiveButton("GitHub", (dialog, which) -> Tools.openURL(requireContext(), "https://github.com/CrazeStudio/CrazeLauncher"))
                    .setNegativeButton(android.R.string.ok, null)
                    .show());
        }

        // Keep the system panel truthful: show configured launcher values, not mock values.
        TextView ramValue = view.findViewById(R.id.system_ram_value);
        android.widget.ProgressBar ramBar = view.findViewById(R.id.system_ram_bar);
        int totalMemoryMb = Tools.getTotalDeviceMemory(requireContext());
        int allocatedMb = LauncherPreferences.PREF_RAM_ALLOCATION;
        if (ramValue != null) {
            ramValue.setText(String.format(Locale.getDefault(), "%.1f GB / %.1f GB",
                    allocatedMb / 1024f, totalMemoryMb / 1024f));
        }
        if (ramBar != null) {
            ramBar.setMax(Math.max(totalMemoryMb, 1));
            ramBar.setProgress(Math.min(allocatedMb, totalMemoryMb));
        }
        TextView javaValue = view.findViewById(R.id.system_java_value);
        if (javaValue != null) {
            String runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME;
            javaValue.setText(runtime == null || runtime.trim().isEmpty() ? "Built-in" : runtime);
        }
        TextView rendererValue = view.findViewById(R.id.system_renderer_value);
        if (rendererValue != null) {
            rendererValue.setText(LauncherPreferences.PREF_RENDERER == null
                    ? "Default" : LauncherPreferences.PREF_RENDERER);
        }
        View systemCard = view.findViewById(R.id.system_settings_card);
        if (systemCard != null) {
            CrazeAnimationUtils.attachTouchFeedback(systemCard);
            systemCard.setOnClickListener(v -> Tools.swapFragment(requireActivity(),
                    LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null));
        }

        if (mPlayButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mPlayButton);
            mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
        }

        if (mEditProfileButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mEditProfileButton);
            mEditProfileButton.setOnClickListener(v -> {
                if (mVersionSpinner != null) {
                    mVersionSpinner.openProfileEditor(requireActivity());
                }
            });
        }

        if (mCustomControlButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mCustomControlButton);
            mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        }

        if (mInstallJarButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mInstallJarButton);
            mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        }

        if (mShareLogsButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mShareLogsButton);
            mShareLogsButton.setOnClickListener(v -> shareLog(requireContext()));
        }

        if (mOpenDirectoryButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mOpenDirectoryButton);
            mOpenDirectoryButton.setOnClickListener(v -> openGameDirectory(requireContext()));
        }

        View mCardNewInstance = view.findViewById(R.id.card_new_instance);
        View mCardMods = view.findViewById(R.id.card_mods_modpacks);
        View mCardModsDirect = view.findViewById(R.id.card_mods);
        View mCardModpacksDirect = view.findViewById(R.id.card_modpacks);

        if (mCardNewInstance != null) {
            CrazeAnimationUtils.attachTouchFeedback(mCardNewInstance);
            mCardNewInstance.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        }

        if (mCardMods != null) {
            CrazeAnimationUtils.attachTouchFeedback(mCardMods);
            mCardMods.setOnClickListener(v -> openProjectBrowser("modpack"));
        }

        if (mCardModsDirect != null) {
            CrazeAnimationUtils.attachTouchFeedback(mCardModsDirect);
            mCardModsDirect.setOnClickListener(v -> openProjectBrowser("mod"));
        }

        if (mCardModpacksDirect != null) {
            CrazeAnimationUtils.attachTouchFeedback(mCardModpacksDirect);
            mCardModpacksDirect.setOnClickListener(v -> openProjectBrowser("modpack"));
        }

        if (mNewsButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mNewsButton);
            mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
            mNewsButton.setOnLongClickListener((v) -> {
                Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
                return true;
            });
        }
        if (mDiscordButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(mDiscordButton);
            mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        }

        CrazeAnimationUtils.attachTouchFeedbackRecursively(view);
        CrazeAnimationUtils.animateEntrance(view, 0);
    }

    private void openProjectBrowser(String projectType) {
        Bundle args = new Bundle();
        args.putString(SearchModFragment.ARG_PROJECT_TYPE, projectType);
        args.putBoolean(SearchModFragment.ARG_IS_MODPACK, "modpack".equals(projectType));
        Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, args);
    }

    private void showDownloadsMenu() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_download_manager, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        dialog.setOnShowListener(ignored -> {
            View versions = dialogView.findViewById(R.id.option_download_versions);
            View mods = dialogView.findViewById(R.id.option_browse_mods);
            View editor = dialogView.findViewById(R.id.option_instance_editor);
            if (versions != null) versions.setOnClickListener(v -> {
                dialog.dismiss();
                Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null);
            });
            if (mods != null) mods.setOnClickListener(v -> {
                dialog.dismiss();
                openProjectBrowser("mod");
            });
            if (editor != null) editor.setOnClickListener(v -> {
                dialog.dismiss();
                Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
            });
        });
        dialog.show();
    }

    private void openGameDirectory(Context context) {
        Instance instance = Instances.loadSelectedInstance();
        if(instance == null) {
            Toast.makeText(context, R.string.no_instance, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = instance.getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
