package com.merino.ddfilms.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.MovieListAdapter
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.ui.viewModel.MovieListViewModel
import com.merino.ddfilms.utils.StringUtils.MOVIE_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList

class MovieListDialogFragment(private val movie: Movie) : DialogFragment() {

    private lateinit var viewModel: MovieListViewModel
    private lateinit var adapter: MovieListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_DDFilms)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_movie_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val createListButton = view.findViewById<LinearLayout>(R.id.create_list_button)
        val closeButton = view.findViewById<ImageView>(R.id.close_button)

        viewModel = ViewModelProvider(this)[MovieListViewModel::class.java]

        adapter = MovieListAdapter(requireContext(), ArrayList(), this::addMovieToSelectedList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        viewModel.movieLists.observe(viewLifecycleOwner) { movieLists ->
            adapter.updateData(movieLists)
        }

        createListButton.setOnClickListener { showCreateListDialog() }
        closeButton.setOnClickListener { dismiss() }

        loadMovieLists()

        dialog?.window?.setBackgroundDrawableResource(R.drawable.main_background_gradient)

        return view
    }

    private fun loadMovieLists() {
        viewModel.loadMovieLists { lists, error ->
            if (lists != null) {
                viewModel.setMovieLists(lists)
            } else if (error != null) {
                showMessage(context, error.message)
            }
        }
    }

    private fun addMovieToSelectedList(list: com.merino.ddfilms.model.MovieLists) {
        val documentID = list.id
        viewModel.addMovieToList(MOVIE_LIST, documentID!!, movie) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, result)
                dismiss()
            }
        }
    }

    private fun showCreateListDialog() {
        val createListDialog = CreateListDialogFragment()
        createListDialog.setOnListCreatedListener(this::createNewList)
        createListDialog.show(childFragmentManager, "CreateListDialog")
    }

    private fun createNewList(listName: String) {
        viewModel.createNewMovieList(listName) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, result)
                loadMovieLists()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setWindowAnimations(R.style.DialogCompleteScreenAnimation)
    }
}
