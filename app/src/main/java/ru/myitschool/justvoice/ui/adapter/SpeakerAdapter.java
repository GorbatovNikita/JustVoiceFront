package ru.myitschool.justvoice.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.remote.dto.Speaker;

public class SpeakerAdapter extends RecyclerView.Adapter<SpeakerAdapter.ViewHolder> {

    private List<Speaker> speakers;
    private final OnSpeakerAction listener;

    public interface OnSpeakerAction {
        void onRename(Speaker speaker);
        void onDelete(Speaker speaker);
    }

    public SpeakerAdapter(List<Speaker> speakers, OnSpeakerAction listener) {
        this.speakers = speakers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_speaker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(speakers.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return speakers.size();
    }

    public void setSpeakers(List<Speaker> speakers) {
        this.speakers = speakers;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSegments;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_speaker_name);
            tvSegments = itemView.findViewById(R.id.tv_segments_count);
        }

        void bind(Speaker speaker, OnSpeakerAction listener) {
            tvName.setText(speaker.getName());

            itemView.setOnLongClickListener(v -> {
                new android.app.AlertDialog.Builder(itemView.getContext())
                        .setTitle(speaker.getName())
                        .setItems(new String[]{"Rename", "Delete"}, (dialog, which) -> {
                            if (which == 0) listener.onRename(speaker);
                            if (which == 1) listener.onDelete(speaker);
                        })
                        .show();
                return true;
            });
        }
    }
}