package com.merino.ddfilms.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;

import java.util.List;
import java.util.function.Consumer;

public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.ViewHolder> {

    private final Context context;
    private final List<String> movieLists;
    private final Consumer<String> onItemClick;

    public MovieListAdapter(Context context, List<String> movieLists, Consumer<String> onItemClick) {
        this.context = context;
        this.movieLists = movieLists;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String listName = movieLists.get(position);
        holder.textView.setText(listName);
        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) {
                onItemClick.accept(listName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movieLists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }
    }

    public void updateData(List<String> newMovieLists) {
        this.movieLists.clear();
        this.movieLists.addAll(newMovieLists);
        notifyDataSetChanged();
    }
}
