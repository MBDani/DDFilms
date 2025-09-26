package com.merino.ddfilms.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.ui.MovieDetailActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    @Setter
    @Getter
    private List<Movie> movies = new ArrayList<>();
    @Setter
    @Getter
    private List<Integer> moviesIdList = new ArrayList<>();
    @Setter
    @Getter
    private List<String> usersList = new ArrayList<>();
    private boolean isEditMode = false;
    private boolean isAddMode = false;
    private OnItemLongClickListener longClickListener;

    private OnDeleteClickListener deleteClickListener;
    private OnAddClickListener addClickListener;
    private OnCheckClickListener checkClickListener;
    private OnAddedByChangedListener addedByChangedListener;

    @Setter
    private String listID;

    public interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position, Movie movie);
    }

    public interface OnAddClickListener {
        void onAddClick(int position, Movie movie);
    }

    public interface OnCheckClickListener {
        void onCheckClick(int position, Movie movie);
    }

    public interface OnAddedByChangedListener {
        void onAddedByChanged(int position, Movie movie, String newAddedBy);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setOnAddClickListener(OnAddClickListener listener) {
        this.addClickListener = listener;
    }

    public void setOnCheckClickListener(OnCheckClickListener listener) {
        this.checkClickListener = listener;
    }

    public void setOnAddedByChangedListener(OnAddedByChangedListener listener) {
        this.addedByChangedListener = listener;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
    }

    public void setAddMode(boolean addMode) {
        isAddMode = addMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMovies(List<Movie> movies) {
        Map<String, List<Movie>> moviesByDate = new LinkedHashMap<>();

        for (Movie movie : movies) {
            if (movie.getCreatedAt() != null) {
                moviesByDate.computeIfAbsent(movie.getCreatedAt(), k -> new ArrayList<>()).add(movie);
            }
        }

        if (!moviesByDate.isEmpty()) {
            List<Movie> sortedMovies = new ArrayList<>();
            for (Map.Entry<String, List<Movie>> entry : moviesByDate.entrySet()) {
                sortedMovies.addAll(entry.getValue());
            }
            this.movies = sortedMovies;
        } else {
            this.movies = movies;
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);

        // Determine if we should show the createdAt TextView
        if (movie.getCreatedAt() != null && !isAddMode && (position == 0 || !Objects.equals(movies.get(position - 1).getCreatedAt(), movie.getCreatedAt()))) {
            holder.createdAtTextView.setText("Añadido el " + movie.getCreatedAt());
            holder.createdAtTextView.setVisibility(View.VISIBLE);
        } else {
            holder.createdAtTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public void removeMovie(int position) {
        movies.remove(position);
        notifyItemRemoved(position);
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView posterImageView;
        private final TextView titleTextView;
        private final TextView overviewTextView;
        private final TextView yearTextView;
        private final TextView voteAverageTextView;
        private final Spinner addedBySpinner;
        private final TextView createdAtTextView;
        private final ImageButton deleteButton;
        private final ImageButton addButton;
        private final ImageButton checkButton;
        private ArrayAdapter<String> spinnerAdapter;
        private String currentAddedBy = "";

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.poster_image_view);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            overviewTextView = itemView.findViewById(R.id.overview_text_view);
            yearTextView = itemView.findViewById(R.id.year_text_view);
            voteAverageTextView = itemView.findViewById(R.id.vote_average_text_view);
            addedBySpinner = itemView.findViewById(R.id.added_by_spinner);
            createdAtTextView = itemView.findViewById(R.id.created_at_text_view);
            deleteButton = itemView.findViewById(R.id.delete_button);
            addButton = itemView.findViewById(R.id.add_button);
            checkButton = itemView.findViewById(R.id.check_button);

            setupClickListeners();
            setupSpinner();
        }

        private void setupSpinner() {
            // Crear adapter personalizado vacío inicialmente
            spinnerAdapter = new ArrayAdapter<String>(itemView.getContext(), R.layout.spinner_item_layout, new ArrayList<>()) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = view.findViewById(R.id.spinner_text);
                    textView.setText(getItem(position));
                    return view;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.spinner_dropdown_item, parent, false);
                    }
                    TextView textView = convertView.findViewById(R.id.dropdown_text);
                    String item = getItem(position);
                    textView.setText(item);

                    // Cambiar color según si está seleccionado o no
                    if (Objects.equals(item, currentAddedBy)) {
                        textView.setTextColor(itemView.getContext().getResources().getColor(R.color.gold_dark));
                    } else {
                        textView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.white));
                    }

                    return convertView;
                }
            };

            addedBySpinner.setAdapter(spinnerAdapter);
        }

        private void setupSpinnerListener() {
            // Listener para detectar cambios
            addedBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedUser = spinnerAdapter.getItem(position);

                    // Solo notificar si realmente cambió el valor
                    if (addedByChangedListener != null && !Objects.equals(currentAddedBy, selectedUser)) {
                        Movie movie = getMovieAtPosition(getBindingAdapterPosition());
                        if (movie != null) {
                            // Actualizar el valor actual para futuras comparaciones
                            currentAddedBy = selectedUser;
                            addedByChangedListener.onAddedByChanged(getBindingAdapterPosition(), movie, selectedUser);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // No hacer nada
                }
            });
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                if (!isEditMode) {
                    Movie movie = getMovieAtPosition(getAdapterPosition());
                    if (movie != null) {
                        Intent intent = new Intent(itemView.getContext(), MovieDetailActivity.class);
                        intent.putExtra("movie", movie);

                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) itemView.getContext(), posterImageView, "moviePosterTransition");

                        itemView.getContext().startActivity(intent, options.toBundle());
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onItemLongClick(getBindingAdapterPosition());
                }
                return false;
            });

            deleteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    deleteClickListener.onDeleteClick(position, movies.get(position));
                }
            });

            addButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    addClickListener.onAddClick(position, movies.get(position));
                }
            });

            checkButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    checkClickListener.onCheckClick(position, movies.get(position));
                }
            });
        }

        @SuppressLint("DefaultLocale")
        public void bind(Movie movie) {
            Glide.with(itemView.getContext()).load("https://image.tmdb.org/t/p/w500" + movie.getPosterPath()).into(posterImageView);
            titleTextView.setText(movie.getTitle());
            yearTextView.setText("(" + movie.getReleaseDate() + ")");
            overviewTextView.setText(movie.getOverview());
            voteAverageTextView.setText(String.format("%.1f", movie.getVoteAverage()));

            boolean shouldShowSpinner = !isAddMode && movie.getAddedBy() != null && !movie.getAddedBy().isEmpty();

            if (shouldShowSpinner) {
                addedBySpinner.setOnItemSelectedListener(null);

                List<String> options = new ArrayList<>();

                if (usersList != null && !usersList.isEmpty()) {
                    for (String user : usersList) {
                        if (!options.contains(user)) {
                            options.add(user);
                        }
                    }
                    options.add("Común"); // Dejamos siempre esta última opción
                }

                // Actualizar adapter del spinner
                spinnerAdapter.clear();
                spinnerAdapter.addAll(options);
                spinnerAdapter.notifyDataSetChanged();

                String movieAddedBy = movie.getAddedBy();
                currentAddedBy = movieAddedBy;

                int selectedPosition = options.indexOf(movieAddedBy);
                if (selectedPosition >= 0) {
                    addedBySpinner.setSelection(selectedPosition);
                }

                setupSpinnerListener();

                addedBySpinner.setVisibility(View.VISIBLE);
                posterImageView.setMaxHeight(500);
                posterImageView.setMinimumHeight(500);
            } else {
                addedBySpinner.setVisibility(View.GONE);
                addedBySpinner.setOnItemSelectedListener(null);
            }

            deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            if (isAddMode) {
                boolean isAlreadyAdded = isMovieAlreadyAdded(movie, moviesIdList);
                addButton.setVisibility(isAlreadyAdded ? View.GONE : View.VISIBLE);
                checkButton.setVisibility(isAlreadyAdded ? View.VISIBLE : View.GONE);
            } else {
                addButton.setVisibility(View.GONE);
                checkButton.setVisibility(View.GONE);
            }
        }
    }

    public Movie getMovieAtPosition(int position) {
        if (position >= 0 && position < movies.size()) {
            return movies.get(position);
        }
        return null;
    }

    private boolean isMovieAlreadyAdded(Movie movie, List<Integer> moviesIdList) {
        for (Integer id : moviesIdList) {
            if (Objects.equals(id, movie.getId())) {
                return true;
            }
        }
        return false;
    }
}
