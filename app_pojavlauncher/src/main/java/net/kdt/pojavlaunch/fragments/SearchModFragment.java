package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.runOnUiThread;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.math.MathUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.R;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.modloaders.modpacks.ModItemAdapter;
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi;
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi;
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters;
import net.kdt.pojavlaunch.profiles.VersionSelectorDialog;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class SearchModFragment extends Fragment implements ModItemAdapter.SearchResultCallback {

    public static final String TAG = "SearchModFragment";
    public static final String ARG_PROJECT_TYPE = "ARG_PROJECT_TYPE";
    public static final String ARG_IS_MODPACK = "ARG_IS_MODPACK";
    private View mOverlay;
    private float mOverlayTopCache; // Padding cache reduce resource lookup

    private final RecyclerView.OnScrollListener mOverlayPositionListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            mOverlay.setY(MathUtils.clamp(mOverlay.getY() - dy, -mOverlay.getHeight(), mOverlayTopCache));
        }
    };

    private EditText mSearchEditText;
    private ImageButton mFilterButton;
    private RecyclerView mRecyclerview;
    private ModItemAdapter mModItemAdapter;
    private ProgressBar mSearchProgressBar;
    private TextView mStatusTextView;
    private ColorStateList mDefaultTextColor;
    private ModpackApi modpackApi;

    private TextView mChipModpack;
    private TextView mChipMod;
    private TextView mChipResourcepack;
    private TextView mChipShader;

    private final SearchFilters mSearchFilters;

    private Button mImportButton;
    private TextView mFilterBadge;
    private View mFilterClear;
    private TextView mBrowseHeaderTitle;
    private TextView mBrowseHeaderSubtitle;
    private boolean mIsLandscape;
    private TaskCountListener mTaskCountListener;

    ActivityResultLauncher<String> mImportLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                Context context = getContext();
                if (context == null) return;
                ContentResolver contentResolver = context.getContentResolver();
                PojavApplication.sExecutorService.execute(() -> {
                    performLocalInstall(uri, context, contentResolver);
                });
            });

    public void performLocalInstall(Uri uri, Context context, ContentResolver contentResolver) {
            String fileName = Tools.getFileName(context, uri);
            if (fileName == null) return;
            File outFile = new File(Tools.DIR_CACHE, fileName + ".cf");
            ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, R.string.multirt_progress_caching);
            try (InputStream inputStream = contentResolver.openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(outFile)) {
                if (inputStream == null) return;
                IOUtils.copy(inputStream, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                Tools.showErrorRemote("Error", e);
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
                return;
            }
            try {
                modpackApi.installLocalModpack(fileName, outFile, null);
            } catch (IOException e) {
                Tools.showErrorRemote("Error", e);
            } finally {
                outFile.delete();
                ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK);
            }
    }

    public SearchModFragment(){
        super(R.layout.fragment_mod_search);
        mSearchFilters = new SearchFilters();
        mSearchFilters.isModpack = true;
        mSearchFilters.projectType = "modpack";
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        modpackApi = new CommonApi(context.getString(R.string.curseforge_api_key));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        if(args != null) {
            if(args.containsKey(ARG_PROJECT_TYPE)) {
                mSearchFilters.projectType = args.getString(ARG_PROJECT_TYPE);
            }
            if(args.containsKey(ARG_IS_MODPACK)) {
                mSearchFilters.isModpack = args.getBoolean(ARG_IS_MODPACK, true);
            }
        }

        // Auto-select MC version filter if not already set and not in modpack mode
        if (mSearchFilters.mcVersion == null || mSearchFilters.mcVersion.isEmpty()) {
            Instance selectedInstance = Instances.loadSelectedInstance();
            if (selectedInstance != null) {
                String mcVer = selectedInstance.getMinecraftVersion();
                if (mcVer != null && !mcVer.isEmpty()) {
                    if (!"modpack".equals(mSearchFilters.projectType) && !mSearchFilters.isModpack) {
                        mSearchFilters.mcVersion = mcVer;
                    }
                }
            }
        }
        // You can only access resources after attaching to current context
        mModItemAdapter = new ModItemAdapter(getResources(), modpackApi, this);
        ProgressKeeper.addTaskCountListener(mModItemAdapter);
        mOverlayTopCache = getResources().getDimension(R.dimen.fragment_padding_medium);

        mOverlay = view.findViewById(R.id.search_mod_overlay);
        mSearchEditText = view.findViewById(R.id.search_mod_edittext);
        mSearchProgressBar = view.findViewById(R.id.search_mod_progressbar);
        mRecyclerview = view.findViewById(R.id.search_mod_list);
        mStatusTextView = view.findViewById(R.id.search_mod_status_text);
        mFilterButton = view.findViewById(R.id.search_mod_filter);

        mChipModpack = view.findViewById(R.id.chip_type_modpack);
        mChipMod = view.findViewById(R.id.chip_type_mod);
        mChipResourcepack = view.findViewById(R.id.chip_type_resourcepack);
        mChipShader = view.findViewById(R.id.chip_type_shader);

        setupChips();

        mDefaultTextColor = mStatusTextView.getTextColors();

        mFilterBadge = view.findViewById(R.id.search_mod_filter_badge);
        mFilterClear = view.findViewById(R.id.search_mod_filter_clear);
        mBrowseHeaderTitle = view.findViewById(R.id.browse_header_title);
        mBrowseHeaderSubtitle = view.findViewById(R.id.browse_header_subtitle);

        if (mFilterClear != null) {
            mFilterClear.setOnClickListener(v -> {
                mSearchFilters.mcVersion = "";
                updateFilterBadge();
                searchMods(mSearchEditText != null ? mSearchEditText.getText().toString() : "");
            });
        }

        mIsLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        Context ctx = getContext();
        if (ctx != null) {
            if (mIsLandscape) {
                mRecyclerview.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(ctx, 2));
            } else {
                mRecyclerview.setLayoutManager(new LinearLayoutManager(ctx));
            }
        }
        mRecyclerview.setAdapter(mModItemAdapter);

        if (!mIsLandscape) {
            mRecyclerview.addOnScrollListener(mOverlayPositionListener);
            mOverlay.post(()->{
               int overlayHeight = mOverlay.getHeight();
               mRecyclerview.setPadding(mRecyclerview.getPaddingLeft(),
                       mRecyclerview.getPaddingTop() + overlayHeight,
                       mRecyclerview.getPaddingRight(),
                       mRecyclerview.getPaddingBottom());
            });
        }

        mSearchEditText.setOnEditorActionListener((v, actionId, event) -> {
            searchMods(mSearchEditText.getText().toString());
            mSearchEditText.clearFocus();
            return false;
        });

        mFilterButton.setOnClickListener(v -> displayFilterDialog());
        mImportButton = view.findViewById(R.id.mineButton_import_local_modpack);
        if (mImportButton != null) {
            mImportButton.setOnClickListener(v -> {
                mImportLauncher.launch("*/*");
            });
        }
        mTaskCountListener = taskCount -> {
            Activity act = getActivity();
            if (act != null) {
                act.runOnUiThread(() -> {
                    if (mImportButton != null) {
                        mImportButton.setEnabled(taskCount == 0);
                    }
                });
            }
            return false;
        };
        ProgressKeeper.addTaskCountListener(mTaskCountListener);

        updateFilterBadge();
        updateHeaderLabels(mSearchFilters.projectType);
        searchMods(null);
    }

    private void setupChips() {
        if (mChipModpack == null) return;

        mChipModpack.setOnClickListener(v -> selectType("modpack"));
        mChipMod.setOnClickListener(v -> selectType("mod"));
        mChipResourcepack.setOnClickListener(v -> selectType("resourcepack"));
        mChipShader.setOnClickListener(v -> selectType("shader"));

        updateChipVisuals();
    }

    private void selectType(String type) {
        mSearchFilters.projectType = type;
        mSearchFilters.isModpack = "modpack".equals(type);
        if (!"modpack".equals(type) && (mSearchFilters.mcVersion == null || mSearchFilters.mcVersion.isEmpty())) {
            Instance selectedInstance = Instances.loadSelectedInstance();
            if (selectedInstance != null) {
                String mcVer = selectedInstance.getMinecraftVersion();
                if (mcVer != null && !mcVer.isEmpty()) {
                    mSearchFilters.mcVersion = mcVer;
                }
            }
        }
        updateChipVisuals();
        updateFilterBadge();
        updateHeaderLabels(type);
        searchMods(mSearchEditText != null ? mSearchEditText.getText().toString() : "");
    }

    private void updateFilterBadge() {
        if (mFilterBadge != null) {
            String ver = mSearchFilters.mcVersion;
            if (ver != null && !ver.isEmpty()) {
                mFilterBadge.setText("MC: " + ver);
                mFilterBadge.setTextColor(Color.parseColor("#10B981"));
                if (mFilterClear != null) mFilterClear.setVisibility(View.VISIBLE);
            } else {
                mFilterBadge.setText("All MC Versions");
                mFilterBadge.setTextColor(Color.parseColor("#C0D0E0"));
                if (mFilterClear != null) mFilterClear.setVisibility(View.GONE);
            }
        }
    }

    private void updateHeaderLabels(String type) {
        if (mBrowseHeaderTitle != null) {
            String title;
            switch (type != null ? type : "modpack") {
                case "mod":
                    title = "EXPLORE MODS";
                    break;
                case "resourcepack":
                    title = "RESOURCE PACKS";
                    break;
                case "shader":
                    title = "SHADERS";
                    break;
                default:
                    title = "EXPLORE MODPACKS";
                    break;
            }
            mBrowseHeaderTitle.setText(title);
        }
        if (mBrowseHeaderSubtitle != null) {
            mBrowseHeaderSubtitle.setText("Tap any item to view & install");
        }
    }

    private void updateChipVisuals() {
        String current = mSearchFilters.projectType != null ? mSearchFilters.projectType : "modpack";
        setChipStyle(mChipModpack, "modpack".equals(current));
        setChipStyle(mChipMod, "mod".equals(current));
        setChipStyle(mChipResourcepack, "resourcepack".equals(current));
        setChipStyle(mChipShader, "shader".equals(current));

        if (mImportButton != null) {
            mImportButton.setVisibility("modpack".equals(current) ? View.VISIBLE : View.GONE);
        }
    }

    private void setChipStyle(TextView chip, boolean isSelected) {
        if (chip == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_craze_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_craze_chip_unselected);
            chip.setTextColor(Color.parseColor("#8E98A5"));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ProgressKeeper.removeTaskCountListener(mModItemAdapter);
        if (!mIsLandscape && mRecyclerview != null) {
            mRecyclerview.removeOnScrollListener(mOverlayPositionListener);
        }
        if (mTaskCountListener != null) { ProgressKeeper.removeTaskCountListener(mTaskCountListener); }
    }

    @Override
    public void onSearchFinished() {
        if (mSearchProgressBar != null) mSearchProgressBar.setVisibility(View.GONE);
        if (mStatusTextView != null) mStatusTextView.setVisibility(View.GONE);
    }

    @Override
    public void onSearchError(int error) {
        if (mSearchProgressBar != null) mSearchProgressBar.setVisibility(View.GONE);
        if (mStatusTextView != null) {
            mStatusTextView.setVisibility(View.VISIBLE);
            switch (error) {
                case ERROR_INTERNAL:
                    mStatusTextView.setTextColor(Color.RED);
                    mStatusTextView.setText(R.string.search_modpack_error);
                    break;
                case ERROR_NO_RESULTS:
                    if (mDefaultTextColor != null) mStatusTextView.setTextColor(mDefaultTextColor);
                    mStatusTextView.setText(R.string.search_modpack_no_result);
                    break;
            }
        }
    }

    private void searchMods(String name) {
        mSearchProgressBar.setVisibility(View.VISIBLE);
        mSearchFilters.name = name == null ? "" : name;
        mModItemAdapter.performSearchQuery(mSearchFilters);
    }

    private void displayFilterDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(R.layout.dialog_mod_filters)
                .create();

        // setup the view behavior
        dialog.setOnShowListener(dialogInterface -> {
            TextView mSelectedVersion = dialog.findViewById(R.id.search_mod_selected_mc_version_textview);
            Button mSelectVersionButton = dialog.findViewById(R.id.search_mod_mc_version_button);
            Button mApplyButton = dialog.findViewById(R.id.search_mod_apply_filters);
            View mClearButton = dialog.findViewById(R.id.search_mod_clear_filters);

            assert mSelectVersionButton != null;
            assert mSelectedVersion != null;
            assert mApplyButton != null;

            if (mClearButton != null) {
                mClearButton.setOnClickListener(v -> mSelectedVersion.setText(""));
            }

            // Setup the expendable list behavior
            mSelectVersionButton.setOnClickListener(v -> VersionSelectorDialog.open(v.getContext(), true, (id, snapshot)-> mSelectedVersion.setText(id)));

            // Apply visually all the current settings
            String currentFilterVersion = mSearchFilters.mcVersion;
            if (currentFilterVersion == null || currentFilterVersion.isEmpty()) {
                Instance selectedInstance = Instances.loadSelectedInstance();
                if (selectedInstance != null) {
                    currentFilterVersion = selectedInstance.getMinecraftVersion();
                }
            }
            mSelectedVersion.setText(currentFilterVersion != null ? currentFilterVersion : "");

            // Apply the new settings
            mApplyButton.setOnClickListener(v -> {
                mSearchFilters.mcVersion = mSelectedVersion.getText().toString();
                updateFilterBadge();
                searchMods(mSearchEditText.getText().toString());
                dialogInterface.dismiss();
            });
        });

        dialog.show();
    }
}
