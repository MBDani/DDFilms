package com.merino.ddfilms.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.MovieAdapter
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.utils.EdgeToEdgeHelper
import com.merino.ddfilms.utils.StringUtils.MOVIE_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList

class MovieListActivity : AppCompatActivity() {

    private lateinit var movieAdapter: MovieAdapter
    private lateinit var movieListRecyclerView: RecyclerView
    private lateinit var toolbar: Toolbar
    private lateinit var fabAdd: FloatingActionButton
    private var isEditMode = false
    private var listID: String? = null
    private var listName: String? = null
    private var doneMenuItem: MenuItem? = null
    private var moreActionsMenuItem: MenuItem? = null
    private val firebaseManager = FirebaseManager()
    private var movieList: MutableList<Movie> = ArrayList()
    private var usersNames: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_list)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        loadMoviesFromList(listID)
        fabAdd.setOnClickListener { setupAddMovieFragment() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.coordinator_layout)) { _, insets ->
            ViewCompat.onApplyWindowInsets(findViewById(R.id.appbar_layout), insets)
            insets
        }

        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.appbar_layout), true, false)
        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(android.R.id.content), false, true)
    }

    private fun setupViews() {
        movieListRecyclerView = findViewById(R.id.movie_list_recycler_view)
        toolbar = findViewById(R.id.toolbar)
        fabAdd = findViewById(R.id.fab_add)

        listID = intent.getStringExtra("listID")
        listName = intent.getStringExtra("listName")
    }

    private fun setupAddMovieFragment() {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("collection", MOVIE_LIST)
            putExtra("documentID", listID)
            putExtra("listName", listName)
            val moviesID = movieList.map { it.id }.toIntArray()
            putExtra("moviesID", moviesID)
        }
        startActivity(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = listName
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_mode_menu, menu)
        doneMenuItem = menu.findItem(R.id.action_done)
        doneMenuItem?.isVisible = false

        menuInflater.inflate(R.menu.list_menu, menu)
        moreActionsMenuItem = menu.findItem(R.id.action_more)
        moreActionsMenuItem?.isVisible = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isEditMode) {
                    exitEditMode()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }
                return true
            }
            R.id.action_more -> {
                showBottomSheet()
                return true
            }
            R.id.action_done -> {
                exitEditMode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter().apply {
            listID = this@MovieListActivity.listID
            setOnItemLongClickListener { _ ->
                if (!isEditMode) {
                    enterEditMode()
                }
                true
            }
            setOnDeleteClickListener { position, movie ->
                firebaseManager.deleteMovieFromList(MOVIE_LIST, listID!!, movie) { result, error ->
                    if (error != null) {
                        showMessage(applicationContext, error.message)
                    } else if (result != null) {
                        removeMovie(position)
                        movieList.remove(movie)
                    }
                }
            }
            setOnAddedByChangedListener { _, movie, newAddedBy ->
                movie.addedBy = newAddedBy
                updateMovieAddedByInDatabase(movie.id, newAddedBy)
            }
        }
        movieListRecyclerView.adapter = movieAdapter
        movieListRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_list_options, null)

        bottomSheetView.findViewById<View>(R.id.option_delete).setOnClickListener {
            bottomSheetDialog.dismiss()
            showDeleteConfirmation()
        }

        bottomSheetView.findViewById<View>(R.id.option_edit).setOnClickListener {
            bottomSheetDialog.dismiss()
            openEditListScreen()
        }

        bottomSheetView.findViewById<View>(R.id.option_share).setOnClickListener {
            bottomSheetDialog.dismiss()
            shareListLink(listID)
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_list_dialog_title, listName))
            .setMessage(R.string.delete_list_dialog_message)
            .setPositiveButton(R.string.dialog_yes) { _, _ -> deleteList() }
            .setNegativeButton(R.string.dialog_no) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteList() {
        firebaseManager.deleteList(listID!!) { _, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, getString(R.string.list_deleted_success))
                finish()
            }
        }
    }

    private fun openEditListScreen() {
        val intent = Intent(this, EditListActivity::class.java).apply {
            putExtra("listID", listID)
            putExtra("listName", listName)
        }
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateMoviesList(movies: List<Movie>) {
        val reversed = movies.reversed()
        movieAdapter.setMovies(reversed)
        movieAdapter.notifyDataSetChanged()
        movieList = ArrayList(movies)
        loadListUsers()
    }

    private fun loadListUsers() {
        firebaseManager.getListUsersIDsAndNames(listID!!) { mapUsersResult, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (mapUsersResult != null) {
                val rawNames = ArrayList(mapUsersResult.values)
                val uniqueUsers = ArrayList<String>()
                for (user in rawNames) {
                    if (!uniqueUsers.contains(user)) {
                        uniqueUsers.add(user)
                    }
                }
                usersNames = uniqueUsers

                movieAdapter.usersList = usersNames
                movieAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadMoviesFromList(listID: String?) {
        if (listID == null) return
        firebaseManager.getMoviesFromList(listID, MOVIE_LIST) { movies, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (movies != null) {
                updateMoviesList(movies)
            }
        }
    }

    private fun updateMovieAddedByInDatabase(movieID: Int, newAddedBy: String) {
        firebaseManager.updateMovieAddedBy(movieID, newAddedBy, listID!!) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (result != null) {
                showMessage(applicationContext, getString(R.string.movie_updated_success))
            }
        }
    }

    private fun shareListLink(listID: String?) {
        val webLink = "https://shimmering-puffpuff-0e63fa.netlify.app/?listID=$listID&listName=$listName"

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Lista de películas compartida")
            putExtra(Intent.EXTRA_TEXT, "¡Echa un vistazo a esta lista de películas! $webLink")
        }

        val chooser = Intent.createChooser(shareIntent, "Compartir lista mediante...")
        startActivity(chooser)
    }

    private fun enterEditMode() {
        isEditMode = true
        doneMenuItem?.isVisible = true
        moreActionsMenuItem?.isVisible = false
        movieAdapter.isEditMode = true
        movieAdapter.listID = listID
        fabAdd.hide()
    }

    private fun exitEditMode() {
        isEditMode = false
        doneMenuItem?.isVisible = false
        moreActionsMenuItem?.isVisible = true
        supportActionBar?.title = listName
        movieAdapter.isEditMode = false
        fabAdd.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        movieAdapter.isEditMode = false
    }

    override fun onResume() {
        super.onResume()
        loadMoviesFromList(listID)
    }
}
