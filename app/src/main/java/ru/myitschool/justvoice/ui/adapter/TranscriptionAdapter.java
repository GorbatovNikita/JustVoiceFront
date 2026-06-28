package ru.myitschool.justvoice.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;

public class TranscriptionAdapter extends RecyclerView.Adapter<TranscriptionAdapter.ViewHolder> {

    private List<TranscriptionTask> tasks;
    private final OnTaskClickListener clickListener;
    private final OnTaskLongClickListener longClickListener;

    public interface OnTaskClickListener {
        void onTaskClick(TranscriptionTask task);
    }

    public interface OnTaskLongClickListener {
        void onTaskLongClick(TranscriptionTask task);
    }

    public TranscriptionAdapter(List<TranscriptionTask> tasks,
                                OnTaskClickListener clickListener,
                                OnTaskLongClickListener longClickListener) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transcription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranscriptionTask task = tasks.get(position);
        holder.bind(task, clickListener, longClickListener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void setTasks(List<TranscriptionTask> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<TranscriptionTask> getTasks() {
        return tasks;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFilename;
        TextView tvStatus;
        TextView tvTopic;
        TextView tvDuration;
        TextView tvTimestamp;
        View statusIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFilename = itemView.findViewById(R.id.tv_filename);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTopic = itemView.findViewById(R.id.tv_topic);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        void bind(TranscriptionTask task, OnTaskClickListener clickListener,
                  OnTaskLongClickListener longClickListener) {
            tvFilename.setText(task.getEffectiveDisplayName());

            String status = task.getStatus() != null ? task.getStatus().toLowerCase() : "pending";
            tvStatus.setText(status.toUpperCase());

            switch (status) {
                case "completed":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pill_green);
                    statusIndicator.setBackgroundResource(android.R.color.holo_green_dark);
                    break;
                case "processing":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pill_orange);
                    statusIndicator.setBackgroundResource(android.R.color.holo_orange_dark);
                    break;
                case "pending":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pill_blue);
                    statusIndicator.setBackgroundResource(android.R.color.holo_blue_dark);
                    break;
                case "failed":
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pill_red);
                    statusIndicator.setBackgroundResource(android.R.color.holo_red_dark);
                    break;
                default:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pill_gray);
                    statusIndicator.setBackgroundResource(android.R.color.darker_gray);
                    break;
            }

            if (task.getTopic() != null && !task.getTopic().isEmpty()) {
                tvTopic.setText(task.getTopic().toUpperCase());
                tvTopic.setVisibility(View.VISIBLE);
            } else {
                tvTopic.setVisibility(View.GONE);
            }

            if (task.getDuration() != null && task.getDuration() > 0) {
                int totalSeconds = (int) Math.round(task.getDuration());
                int minutes = totalSeconds / 60;
                int seconds = totalSeconds % 60;
                tvDuration.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
                tvDuration.setVisibility(View.VISIBLE);
            } else if (task.getSegmentsCount() > 0) {
                tvDuration.setText(task.getSegmentsCount() + " segments");
                tvDuration.setVisibility(View.VISIBLE);
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(task.getCreatedAt())));

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTaskClick(task);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onTaskLongClick(task);
                }
                return true;
            });
        }
    }
}