package ru.myitschool.justvoice.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionSegment;

public class SegmentAdapter extends RecyclerView.Adapter<SegmentAdapter.ViewHolder> {

    private List<TranscriptionSegment> segments;
    private final OnSegmentClickListener listener;

    public interface OnSegmentClickListener {
        void onSegmentClick(TranscriptionSegment segment);
    }

    public SegmentAdapter(List<TranscriptionSegment> segments, OnSegmentClickListener listener) {
        this.segments = segments != null ? segments : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_segment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranscriptionSegment segment = segments.get(position);
        holder.bind(segment, listener);
    }

    @Override
    public int getItemCount() {
        return segments.size();
    }

    public void setSegments(List<TranscriptionSegment> segments) {
        this.segments = segments != null ? segments : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSpeaker;
        TextView tvTimestamp;
        TextView tvText;
        TextView tvConfidence;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSpeaker = itemView.findViewById(R.id.tv_speaker);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvText = itemView.findViewById(R.id.tv_text);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
        }

        void bind(TranscriptionSegment segment, OnSegmentClickListener listener) {
            tvSpeaker.setText(segment.getSpeaker() != null ? segment.getSpeaker() : "Speaker");

            String timestamp = formatTime(segment.getStartTime()) + " - " + formatTime(segment.getEndTime());
            tvTimestamp.setText(timestamp);

            tvText.setText(segment.getText());

            if (segment.getConfidence() != null) {
                tvConfidence.setText(String.format(Locale.getDefault(), "%.0f%%", segment.getConfidence() * 100));
                tvConfidence.setVisibility(View.VISIBLE);
            } else {
                tvConfidence.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSegmentClick(segment);
                }
            });
        }

        private String formatTime(Double seconds) {
            if (seconds == null) return "0:00";
            int minutes = (int) (seconds / 60);
            int secs = (int) (seconds % 60);
            return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
        }
    }
}