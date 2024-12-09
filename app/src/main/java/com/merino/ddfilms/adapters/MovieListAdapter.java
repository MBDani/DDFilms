package com.merino.ddfilms.adapters;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.merino.ddfilms.R;

import java.util.ArrayList;
import java.util.List;

public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.ViewHolder> {

    private final Context context;
    private final List<String> movieLists;
    private final Runnable onItemClick;

    public MovieListAdapter(Context context, List<String> movieLists, Runnable onItemClick) {
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
                onItemClick.run();
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
