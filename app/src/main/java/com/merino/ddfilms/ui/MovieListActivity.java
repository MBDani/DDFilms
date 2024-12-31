package com.merino.ddfilms.ui;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.Movie;

import java.util.Collections;
import java.util.List;

public class MovieListActivity extends AppCompatActivity {

    private MovieAdapter movieAdapter;
    private RecyclerView movieListRecyclerView;
    private Toolbar toolbar;
    private FloatingActionButton fabAdd;
    private boolean isEditMode = false;
    private String listID;
    private String listName;
    private MenuItem doneMenuItem;
    private MenuItem moreActionsMenuItem;
    private final FirebaseManager firebaseManager = new FirebaseManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        setupViews();
        setupToolbar();
        setupRecyclerView();
        loadMoviesFromList(listID);
    }

    private void setupViews() {
        movieListRecyclerView = findViewById(R.id.movie_list_recycler_view);
        toolbar = findViewById(R.id.toolbar);
        fabAdd = findViewById(R.id.fab_add);

        Intent intent = getIntent();
        listID = intent.getStringExtra("listID");
        listName = intent.getStringExtra("listName");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(listName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Creamos el menú para la edición
        getMenuInflater().inflate(R.menu.edit_mode_menu, menu);
        doneMenuItem = menu.findItem(R.id.action_done);
        doneMenuItem.setVisible(false);

        // Creamos el menú con las acciones y es el que dejamos por defecto
        getMenuInflater().inflate(R.menu.list_menu, menu);
        moreActionsMenuItem = menu.findItem(R.id.action_more);
        moreActionsMenuItem.setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isEditMode) {
                    exitEditMode();
                } else {
                    onBackPressed();
                }
                return true;
            case R.id.action_more:
                showBottomSheet();
                return true;
            case R.id.action_done:
                exitEditMode();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        movieAdapter = new MovieAdapter();
        movieAdapter.setOnItemLongClickListener(position -> {
            if (!isEditMode) {
                enterEditMode();
            }
            return true;
        });

        movieAdapter.setOnDeleteClickListener((position, movie) -> {
            firebaseManager.deleteMovieFromList(listID, movie, (result, error) -> {
                if (error != null) {
                    showMessage(getApplicationContext(), error.getMessage());
                } else if (result != null) {
                    movieAdapter.removeMovie(position);
                }
            });
        });


        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                Collections.swap(movieAdapter.getMovies(), fromPosition, toPosition);
                movieAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // No implementamos swipe
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return isEditMode;
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(movieListRecyclerView);

        movieListRecyclerView.setAdapter(movieAdapter);
        movieListRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private void setupFAB() {
        fabAdd.setOnClickListener(v -> {
            // TODO Implementar añadir película
        });
    }

    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_list_options, null);

        bottomSheetView.findViewById(R.id.option_delete).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showDeleteConfirmation();
        });

        bottomSheetView.findViewById(R.id.option_edit).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openEditListScreen();
        });

        bottomSheetView.findViewById(R.id.option_share).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            shareListLink(listID);
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this).setTitle("Eliminar lista " + listName).setMessage("¿Estás seguro de que quieres eliminar esta lista? Esta acción no se puede deshacer.").setPositiveButton("Sí", (dialog, which) -> {
            deleteList();
        }).setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        }).show();
    }

    private void deleteList() {
        firebaseManager.deleteList(listID, (result, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else {
                showMessage(getApplicationContext(), "Lista eliminada con éxito");
                finish();
            }
        });
    }

    private void openEditListScreen() {
        Intent intent = new Intent(this, EditListActivity.class);
        intent.putExtra("listID", listID);
        intent.putExtra("listName", listName);
        startActivity(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateMoviesList(List<Movie> movies) {
        // Ponemos las películas agregadas más recientemente primero
        Collections.reverse(movies);
        movieAdapter.setMovies(movies);
        movieAdapter.notifyDataSetChanged();
    }

    private void loadMoviesFromList(String listID) {
        firebaseManager.loadMovieFromListName(listID, (movies, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else if (movies != null) {
                updateMoviesList(movies);
            }
        });
    }

    private void shareListLink(String listID) {
        // Crear el deep link
        String webLink = "https://shimmering-puffpuff-0e63fa.netlify.app/?listID=" + listID + "&listName=" + listName;

        // Crear el intent para compartir
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Lista de películas compartida");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "¡Echa un vistazo a esta lista de películas! " + webLink);

        // Crear el chooser con las apps disponibles
        Intent chooser = Intent.createChooser(shareIntent, "Compartir lista mediante...");
        startActivity(chooser);
    }

    private void enterEditMode() {
        isEditMode = true;
        doneMenuItem.setVisible(true);
        moreActionsMenuItem.setVisible(false);
        movieAdapter.setEditMode(true);
        movieAdapter.setListID(listID);
        fabAdd.hide();
    }

    private void exitEditMode() {
        isEditMode = false;
        doneMenuItem.setVisible(false);
        moreActionsMenuItem.setVisible(true);
        getSupportActionBar().setTitle(listName);
        movieAdapter.setEditMode(false);
        fabAdd.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        movieAdapter.setEditMode(false);
    }


}


