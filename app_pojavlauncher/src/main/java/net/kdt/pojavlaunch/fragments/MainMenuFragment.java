package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;

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

        if (navHome != null) {
            attachTouchAnimation(navHome);
            navHome.setOnClickListener(v -> {
                if (mVersionSpinner != null) mVersionSpinner.reloadProfiles();
            });
        }
        if (navInstances != null) {
            attachTouchAnimation(navInstances);
            navInstances.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        }
        if (navMods != null) {
            attachTouchAnimation(navMods);
            navMods.setOnClickListener(v -> Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null));
        }
        if (navPacks != null) {
            attachTouchAnimation(navPacks);
            navPacks.setOnClickListener(v -> openGameDirectory(requireContext()));
        }
        if (navSettings != null) {
            attachTouchAnimation(navSettings);
            navSettings.setOnClickListener(v -> Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, null));
        }

        if (mPlayButton != null) {
            attachTouchAnimation(mPlayButton);
            mPlayButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
        }

        if (mEditProfileButton != null) {
            attachTouchAnimation(mEditProfileButton);
            mEditProfileButton.setOnClickListener(v -> {
                if (mVersionSpinner != null) {
                    mVersionSpinner.openProfileEditor(requireActivity());
                }
            });
        }

        if (mCustomControlButton != null) {
            attachTouchAnimation(mCustomControlButton);
            mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        }

        if (mInstallJarButton != null) {
            attachTouchAnimation(mInstallJarButton);
            mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation());
        }

        if (mShareLogsButton != null) {
            attachTouchAnimation(mShareLogsButton);
            mShareLogsButton.setOnClickListener(v -> shareLog(requireContext()));
        }

        if (mOpenDirectoryButton != null) {
            attachTouchAnimation(mOpenDirectoryButton);
            mOpenDirectoryButton.setOnClickListener(v -> openGameDirectory(requireContext()));
        }

        View mCardNewInstance = view.findViewById(R.id.card_new_instance);
        View mCardMods = view.findViewById(R.id.card_mods_modpacks);
        View mCardModsDirect = view.findViewById(R.id.card_mods);
        View mCardModpacksDirect = view.findViewById(R.id.card_modpacks);

        if (mCardNewInstance != null) {
            attachTouchAnimation(mCardNewInstance);
            mCardNewInstance.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        }

        if (mCardMods != null) {
            attachTouchAnimation(mCardMods);
            mCardMods.setOnClickListener(v -> Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null));
        }

        if (mCardModsDirect != null) {
            attachTouchAnimation(mCardModsDirect);
            mCardModsDirect.setOnClickListener(v -> Tools.swapFragment(requireActivity(), SearchModFragment.class, SearchModFragment.TAG, null));
        }

        if (mCardModpacksDirect != null) {
            attachTouchAnimation(mCardModpacksDirect);
            mCardModpacksDirect.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        }

        if (mNewsButton != null) {
            attachTouchAnimation(mNewsButton);
            mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
            mNewsButton.setOnLongClickListener((v) -> {
                Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
                return true;
            });
        }
        if (mDiscordButton != null) {
            attachTouchAnimation(mDiscordButton);
            mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.social_media_invite)));
        }
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

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private static void attachTouchAnimation(View v) {
        if (v == null) return;
        v.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                    break;
            }
            return false;
        });
    }
}
