package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieListAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.MovieLists;
import com.merino.ddfilms.ui.MovieListActivity;
import com.merino.ddfilms.ui.viewModel.MovieListViewModel;

import java.util.ArrayList;
import java.util.List;

public class ListsFragment extends Fragment {

    private MovieListAdapter adapter;
    private final List<MovieLists> listsList = new ArrayList<>();
    private DocumentSnapshot lastVisible = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PAGE_SIZE = 10;

    private ProgressBar initialLoadingProgress;
    private ProgressBar loadMoreProgress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lists, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.lists_recycler_view);
        LinearLayout createListButton = view.findViewById(R.id.create_list_button);
        initialLoadingProgress = view.findViewById(R.id.lists_loading_progress);
        loadMoreProgress = view.findViewById(R.id.lists_load_more_progress);

        adapter = new MovieListAdapter(getContext(), listsList, this::navigateToListMoviesActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadNextPage();
                    }
                }
            }
        });

        createListButton.setOnClickListener(v -> showCreateListDialog());

        return view;
    }

    private void resetPaginationAndLoad() {
        boolean isFirstLoad = listsList.isEmpty();

        lastVisible = null;
        isLastPage = false;

        if (isFirstLoad) {
            listsList.clear();
            adapter.notifyDataSetChanged();
            initialLoadingProgress.setVisibility(View.VISIBLE);
        }

        isLoading = true;

        FirebaseManager.getInstance().getMovieListsPaginated(
                FirebaseManager.getInstance().getCurrentUserUID(),
                null,
                PAGE_SIZE,
                (pair, error) -> {
                    isLoading = false;
                    initialLoadingProgress.setVisibility(View.GONE);
                    if (error != null) {
                        showMessage(getContext(), getString(R.string.error_loading_lists, error.getMessage()));
                    } else if (pair != null) {
                        List<MovieLists> newLists = pair.first;
                        lastVisible = pair.second;

                        if (newLists.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }

                        listsList.clear();
                        listsList.addAll(newLists);
                        adapter.notifyDataSetChanged();
                    }
                }
        );
    }

    private void loadNextPage() {
        isLoading = true;
        loadMoreProgress.setVisibility(View.VISIBLE);

        FirebaseManager.getInstance().getMovieListsPaginated(
                FirebaseManager.getInstance().getCurrentUserUID(),
                lastVisible,
                PAGE_SIZE,
                (pair, error) -> {
                    isLoading = false;
                    loadMoreProgress.setVisibility(View.GONE);
                    if (error != null) {
                        showMessage(getContext(), getString(R.string.error_generic, error.getMessage()));
                    } else if (pair != null) {
                        List<MovieLists> newLists = pair.first;
                        lastVisible = pair.second;

                        if (newLists.isEmpty() || newLists.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }

                        int startSize = listsList.size();
                        listsList.addAll(newLists);
                        adapter.notifyItemRangeInserted(startSize, newLists.size());
                    }
                }
        );
    }

    private void showCreateListDialog() {
        CreateListDialogFragment createListDialog = new CreateListDialogFragment();
        createListDialog.setOnListCreatedListener(this::createNewList);
        createListDialog.show(getChildFragmentManager(), "CreateListDialog");
    }

    private void createNewList(String listName) {
        MovieListViewModel vm = new ViewModelProvider(this).get(MovieListViewModel.class);
        vm.createNewMovieList(listName, (result, error) -> {
            if (error != null) {
                showMessage(getContext(), error.getMessage());
            } else if (result != null) {
                showMessage(getContext(), result);
                resetPaginationAndLoad();
            }
        });
    }

    private void navigateToListMoviesActivity(MovieLists list) {
        Intent intent = new Intent(getContext(), MovieListActivity.class);
        intent.putExtra("listID", list.getId());
        intent.putExtra("listName", list.getName());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        resetPaginationAndLoad();
    }
}
