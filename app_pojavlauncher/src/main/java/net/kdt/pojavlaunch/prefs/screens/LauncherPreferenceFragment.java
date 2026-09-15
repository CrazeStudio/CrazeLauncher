package net.kdt.pojavlaunch.prefs.screens;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.CrazeAnimationUtils;

/**
 * Root Settings Fragment for CrazeLauncher.
 * Features a modern master-detail dual pane layout with frosted glass aesthetic in landscape mode,
 * and a standard clean categorized preference layout in portrait mode.
 */
public class LauncherPreferenceFragment extends Fragment {

    private int mSelectedCategory = 0;

    private View[] mCategoryContainers;
    private ImageView[] mCategoryIcons;
    private TextView[] mCategoryTexts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            mSelectedCategory = savedInstanceState.getInt("selected_category", 0);
        }

        View backButton = view.findViewById(R.id.btn_settings_back);
        if (backButton != null) {
            CrazeAnimationUtils.attachTouchFeedback(backButton);
            backButton.setOnClickListener(v -> Tools.backToMainMenu(requireActivity()));
        }

        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            setupLandscapeDeck(view);
        } else {
            setupPortraitContainer();
        }

        CrazeAnimationUtils.animateEntrance(view, 0);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_category", mSelectedCategory);
    }

    private void setupPortraitContainer() {
        if (getChildFragmentManager().findFragmentById(R.id.settings_content_frame) == null) {
            getChildFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_fade_slide_enter, R.anim.fragment_fade_slide_exit)
                    .replace(R.id.settings_content_frame, new LauncherPreferenceMainFragment())
                    .commit();
        }
    }

    private void setupLandscapeDeck(@NonNull View root) {
        mCategoryContainers = new View[]{
                root.findViewById(R.id.category_video),
                root.findViewById(R.id.category_controls),
                root.findViewById(R.id.category_java),
                root.findViewById(R.id.category_misc),
                root.findViewById(R.id.category_experimental)
        };

        mCategoryIcons = new ImageView[]{
                root.findViewById(R.id.icon_video),
                root.findViewById(R.id.icon_controls),
                root.findViewById(R.id.icon_java),
                root.findViewById(R.id.icon_misc),
                root.findViewById(R.id.icon_experimental)
        };

        mCategoryTexts = new TextView[]{
                root.findViewById(R.id.text_video),
                root.findViewById(R.id.text_controls),
                root.findViewById(R.id.text_java),
                root.findViewById(R.id.text_misc),
                root.findViewById(R.id.text_experimental)
        };

        for (int i = 0; i < mCategoryContainers.length; i++) {
            final int index = i;
            if (mCategoryContainers[i] != null) {
                CrazeAnimationUtils.attachTouchFeedback(mCategoryContainers[i]);
                mCategoryContainers[i].setOnClickListener(v -> selectCategory(index));
            }
        }

        ViewGroup categoriesContainer = root.findViewById(R.id.categories_container);
        if (categoriesContainer != null) {
            CrazeAnimationUtils.animateStaggeredChildren(categoriesContainer);
        }

        selectCategory(mSelectedCategory);
    }

    public void selectCategory(int index) {
        if (index < 0 || index >= 5) return;
        mSelectedCategory = index;

        updateCategoryStyles();

        Fragment targetFragment;
        switch (index) {
            case 0:
                targetFragment = new LauncherPreferenceVideoFragment();
                break;
            case 1:
                targetFragment = new LauncherPreferenceControlFragment();
                break;
            case 2:
                targetFragment = new LauncherPreferenceJavaFragment();
                break;
            case 3:
                targetFragment = new LauncherPreferenceMiscellaneousFragment();
                break;
            case 4:
                targetFragment = new LauncherPreferenceExperimentalFragment();
                break;
            default:
                targetFragment = new LauncherPreferenceVideoFragment();
                break;
        }

        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fragment_fade_enter, R.anim.fragment_fade_exit)
                .replace(R.id.settings_content_frame, targetFragment)
                .commit();
    }

    private void updateCategoryStyles() {
        if (mCategoryContainers == null) return;

        int activeColor = Color.WHITE;
        int inactiveTextColor = Color.parseColor("#8D9AA8");
        int inactiveIconColor = Color.parseColor("#8D9AA8");

        for (int i = 0; i < mCategoryContainers.length; i++) {
            boolean isSelected = (i == mSelectedCategory);

            if (mCategoryContainers[i] != null) {
                mCategoryContainers[i].setBackgroundResource(
                        isSelected ? R.drawable.bg_craze_category_active : R.drawable.bg_craze_category_inactive
                );
            }

            if (mCategoryIcons != null && mCategoryIcons[i] != null) {
                mCategoryIcons[i].setImageTintList(ColorStateList.valueOf(isSelected ? activeColor : inactiveIconColor));
            }

            if (mCategoryTexts != null && mCategoryTexts[i] != null) {
                mCategoryTexts[i].setTextColor(isSelected ? Color.WHITE : inactiveTextColor);
            }
        }
    }
}
