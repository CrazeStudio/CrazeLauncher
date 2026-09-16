package net.kdt.pojavlaunch.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceIconProvider;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.utils.CrazeAnimationUtils;

import java.io.IOException;
import java.util.List;

public class InstanceManagementFragment extends Fragment {
    public static final String TAG = "InstanceManagementFragment";

    private LinearLayout mInstancesContainer;

    public InstanceManagementFragment() {
        super(R.layout.fragment_instance_management);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mInstancesContainer = view.findViewById(R.id.instances_container);

        View webLinkCard = view.findViewById(R.id.web_link_card);
        View createCard = view.findViewById(R.id.card_create_instance_action);

        if (webLinkCard != null) {
            CrazeAnimationUtils.attachTouchFeedback(webLinkCard);
            webLinkCard.setOnClickListener(v -> {
                String url = "https://Crazelauncher-steel.vercel.app";
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("CrazeLauncher Web", url);
                    if (clipboard != null) clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "Copied web link to clipboard: " + url, Toast.LENGTH_LONG).show();
                }
            });
        }

        if (createCard != null) {
            CrazeAnimationUtils.attachTouchFeedback(createCard);
            createCard.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class, ProfileTypeSelectFragment.TAG, null));
        }

        loadAndDisplayInstances();
        CrazeAnimationUtils.animateEntrance(view, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplayInstances();
    }

    private void loadAndDisplayInstances() {
        if (mInstancesContainer == null) return;
        mInstancesContainer.removeAllViews();

        try {
            List<Instance> instances = Instances.loadAllInstances();
            Instance selected = Instances.loadSelectedInstance();
            LayoutInflater inflater = LayoutInflater.from(requireContext());

            for (Instance instance : instances) {
                View itemView = inflater.inflate(R.layout.item_instance_manage, mInstancesContainer, false);
                CrazeAnimationUtils.attachTouchFeedback(itemView);

                ImageView iconView = itemView.findViewById(R.id.instance_item_icon);
                TextView nameView = itemView.findViewById(R.id.instance_item_name);
                TextView versionView = itemView.findViewById(R.id.instance_item_version);
                TextView activeBadge = itemView.findViewById(R.id.instance_item_active_badge);
                ImageButton selectBtn = itemView.findViewById(R.id.instance_item_select_btn);
                ImageButton editBtn = itemView.findViewById(R.id.instance_item_edit_btn);
                ImageButton deleteBtn = itemView.findViewById(R.id.instance_item_delete_btn);

                // Set icon
                if (iconView != null) {
                    iconView.setImageDrawable(InstanceIconProvider.fetchIcon(getResources(), instance));
                }

                // Set name
                String name = Tools.validOrNullString(instance.name);
                if (name == null) name = instance.getInstanceRoot() != null ? instance.getInstanceRoot().getName() : "Instance";
                final String instanceName = name;
                if (nameView != null) nameView.setText(instanceName);

                // Set version & modloader info
                String mcVer = instance.getMinecraftVersion();
                String loader = instance.getModLoaderType();
                String verText = "Minecraft " + (mcVer != null ? mcVer : (instance.versionId != null ? instance.versionId : "Unknown"));
                if (loader != null && !loader.isEmpty()) {
                    verText += " • " + loader.toUpperCase();
                }
                if (versionView != null) versionView.setText(verText);

                // Check if active
                boolean isActive = selected != null && selected.getInstanceRoot() != null &&
                        instance.getInstanceRoot() != null && selected.getInstanceRoot().equals(instance.getInstanceRoot());

                if (activeBadge != null) {
                    activeBadge.setVisibility(isActive ? View.VISIBLE : View.GONE);
                }

                // Actions
                if (selectBtn != null) {
                    selectBtn.setOnClickListener(v -> {
                        Instances.setSelectedInstance(instance);
                        Toast.makeText(requireContext(), "Selected active instance: " + instanceName, Toast.LENGTH_SHORT).show();
                        loadAndDisplayInstances();
                    });
                }

                if (editBtn != null) {
                    editBtn.setOnClickListener(v -> {
                        Instances.setSelectedInstance(instance);
                        Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
                    });
                }

                if (deleteBtn != null) {
                    deleteBtn.setOnClickListener(v -> {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Instance")
                                .setMessage("Are you sure you want to delete '" + instanceName + "'? All saved data and files inside this instance will be removed.")
                                .setPositiveButton("Delete", (dialog, which) -> {
                                    try {
                                        InstanceIconProvider.dropIcon(instance);
                                        Instances.removeInstance(instance);
                                        Toast.makeText(requireContext(), "Instance deleted", Toast.LENGTH_SHORT).show();
                                        loadAndDisplayInstances();
                                    } catch (IOException e) {
                                        Tools.showError(requireContext(), e);
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }

                mInstancesContainer.addView(itemView);
            }
        } catch (IOException e) {
            Tools.showError(requireContext(), e);
        }
    }
}
