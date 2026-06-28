package ru.myitschool.justvoice.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.myitschool.justvoice.R;
import ru.myitschool.justvoice.data.local.entity.TranscriptionTask;


public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private List<SearchResultItem> results;
    private final OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(TranscriptionTask result);
    }

    public SearchAdapter(List<SearchResultItem> results, OnResultClickListener listener) {
        this.results = results != null ? results : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResultItem item = results.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void setResults(List<SearchResultItem> results) {
        this.results = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvMatchedIn;
        TextView tvTopic;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMatchedIn = itemView.findViewById(R.id.tv_matched_in);
            tvTopic = itemView.findViewById(R.id.tv_topic);
        }

        void bind(SearchResultItem item, OnResultClickListener listener) {
            tvName.setText(item.getTask().getEffectiveDisplayName());
            tvMatchedIn.setText("Найдено в: " + item.getMatchedIn());

            if (item.getTask().getTopic() != null) {
                tvTopic.setText(item.getTask().getTopic().toUpperCase());
                tvTopic.setVisibility(View.VISIBLE);
            } else {
                tvTopic.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onResultClick(item.getTask());
            });
        }
    }

    public static class SearchResultItem {
        private final TranscriptionTask task;
        private final String matchedIn;
        private final String query;

        public SearchResultItem(TranscriptionTask task, String matchedIn, String query) {
            this.task = task;
            this.matchedIn = matchedIn;
            this.query = query;
        }

        public TranscriptionTask getTask() { return task; }
        public String getMatchedIn() { return matchedIn; }
        public String getQuery() { return query; }
    }
}