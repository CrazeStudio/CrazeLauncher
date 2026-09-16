package net.kdt.pojavlaunch.recorder;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Tools;
import git.artdeell.mojo.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingsListDialog {

    public static void show(Context context) {
        File dir = new File(Tools.DIR_GAME_HOME, "Recordings");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp4"));
        List<File> videoList = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            videoList.addAll(Arrays.asList(files));
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_recordings_list, null);
        TextView emptyView = dialogView.findViewById(R.id.recordings_empty_text);
        RecyclerView recyclerView = dialogView.findViewById(R.id.recordings_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        RecordingsAdapter adapter = new RecordingsAdapter(context, videoList, emptyView);
        recyclerView.setAdapter(adapter);

        if (videoList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.crazer_recorder_view_recordings)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.ViewHolder> {
        private final Context mContext;
        private final List<File> mFiles;
        private final TextView mEmptyView;
        private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        public RecordingsAdapter(Context context, List<File> files, TextView emptyView) {
            this.mContext = context;
            this.mFiles = files;
            this.mEmptyView = emptyView;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_recording_video, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = mFiles.get(position);
            holder.title.setText(file.getName());

            double sizeMb = file.length() / (1024.0 * 1024.0);
            String formattedDate = mDateFormat.format(new Date(file.lastModified()));
            holder.subtitle.setText(String.format(Locale.US, "%.1f MB • %s", sizeMb, formattedDate));

            holder.btnPlay.setOnClickListener(v -> {
                try {
                    Tools.openPath(mContext, file, false);
                } catch (Throwable t) {
                    Toast.makeText(mContext, "Could not open video: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnShare.setOnClickListener(v -> {
                try {
                    Tools.openPath(mContext, file, true);
                } catch (Throwable t) {
                    Toast.makeText(mContext, "Could not share video: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.instance_delete)
                        .setMessage(R.string.crazer_recorder_delete_confirm)
                        .setPositiveButton(R.string.global_yes, (d, w) -> {
                            file.delete();
                            int currentPos = holder.getAdapterPosition();
                            if (currentPos != RecyclerView.NO_POSITION && currentPos < mFiles.size()) {
                                mFiles.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                if (mFiles.isEmpty()) {
                                    mEmptyView.setVisibility(View.VISIBLE);
                                }
                            }
                        })
                        .setNegativeButton(R.string.global_no, null)
                        .show();
            });

            holder.itemView.setOnClickListener(v -> {
                try {
                    Tools.openPath(mContext, file, false);
                } catch (Throwable t) {
                    Toast.makeText(mContext, "Could not open video: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mFiles.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;
            ImageButton btnPlay;
            ImageButton btnShare;
            ImageButton btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.video_item_title);
                subtitle = itemView.findViewById(R.id.video_item_subtitle);
                btnPlay = itemView.findViewById(R.id.video_item_btn_play);
                btnShare = itemView.findViewById(R.id.video_item_btn_share);
                btnDelete = itemView.findViewById(R.id.video_item_btn_delete);
            }
        }
    }
}
