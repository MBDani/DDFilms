package com.merino.ddfilms.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merino.ddfilms.R
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.ui.MovieDetailActivity
import java.util.ArrayList
import java.util.LinkedHashMap

class MovieAdapter : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    var movies: MutableList<Movie> = ArrayList()
        private set

    var moviesIdList: List<Int> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var usersList: List<String> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isEditMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var isAddMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private var longClickListener: OnItemLongClickListener? = null
    private var lastClickTime: Long = 0
    private val CLICK_INTERVAL: Long = 1200

    private var deleteClickListener: OnDeleteClickListener? = null
    private var addClickListener: OnAddClickListener? = null
    private var checkClickListener: OnCheckClickListener? = null
    private var addedByChangedListener: OnAddedByChangedListener? = null

    var listID: String? = null

    fun interface OnItemLongClickListener {
        fun onItemLongClick(position: Int): Boolean
    }

    fun interface OnDeleteClickListener {
        fun onDeleteClick(position: Int, movie: Movie)
    }

    fun interface OnAddClickListener {
        fun onAddClick(position: Int, movie: Movie)
    }

    fun interface OnCheckClickListener {
        fun onCheckClick(position: Int, movie: Movie)
    }

    fun interface OnAddedByChangedListener {
        fun onAddedByChanged(position: Int, movie: Movie, newAddedBy: String)
    }

    fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
        this.longClickListener = listener
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener?) {
        this.deleteClickListener = listener
    }

    fun setOnAddClickListener(listener: OnAddClickListener?) {
        this.addClickListener = listener
    }

    fun setOnCheckClickListener(listener: OnCheckClickListener?) {
        this.checkClickListener = listener
    }

    fun setOnAddedByChangedListener(listener: OnAddedByChangedListener?) {
        this.addedByChangedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    fun setMovies(movies: List<Movie>) {
        val moviesByDate = LinkedHashMap<String, MutableList<Movie>>()

        for (movie in movies) {
            val date = movie.createdAt
            if (date != null) {
                moviesByDate.getOrPut(date) { ArrayList() }.add(movie)
            }
        }

        if (moviesByDate.isNotEmpty()) {
            val sortedMovies = ArrayList<Movie>()
            for (entry in moviesByDate.entries) {
                sortedMovies.addAll(entry.value)
            }
            this.movies = sortedMovies
        } else {
            this.movies = ArrayList(movies)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.bind(movie)

        // Determine if we should show the createdAt TextView
        val createdAt = movie.createdAt
        if (createdAt != null && !isAddMode && (position == 0 || movies[position - 1].createdAt != createdAt)) {
            holder.createdAtTextView.text = holder.itemView.context.getString(R.string.added_on, createdAt)
            holder.createdAtTextView.visibility = View.VISIBLE
        } else {
            holder.createdAtTextView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = movies.size

    fun removeMovie(position: Int) {
        if (position in movies.indices) {
            movies.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val posterImageView: ImageView = itemView.findViewById(R.id.poster_image_view)
        val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        val overviewTextView: TextView = itemView.findViewById(R.id.overview_text_view)
        val yearTextView: TextView = itemView.findViewById(R.id.year_text_view)
        val voteAverageTextView: TextView = itemView.findViewById(R.id.vote_average_text_view)
        val addedBySpinner: Spinner = itemView.findViewById(R.id.added_by_spinner)
        val createdAtTextView: TextView = itemView.findViewById(R.id.created_at_text_view)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)
        val addButton: ImageButton = itemView.findViewById(R.id.add_button)
        val checkButton: ImageButton = itemView.findViewById(R.id.check_button)
        private lateinit var spinnerAdapter: ArrayAdapter<String>
        private var currentAddedBy = ""

        init {
            setupClickListeners()
            setupSpinner()
        }

        private fun setupSpinner() {
            spinnerAdapter = object : ArrayAdapter<String>(itemView.context, R.layout.spinner_item_layout, ArrayList()) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_layout, parent, false)
                    val textView = view.findViewById<TextView>(R.id.spinner_text)
                    textView.text = getItem(position)
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_item, parent, false)
                    val textView = view.findViewById<TextView>(R.id.dropdown_text)
                    val item = getItem(position)
                    textView.text = item

                    if (item == currentAddedBy) {
                        textView.setTextColor(context.resources.getColor(R.color.gold_dark, null))
                    } else {
                        textView.setTextColor(context.resources.getColor(android.R.color.white, null))
                    }

                    return view
                }
            }

            addedBySpinner.adapter = spinnerAdapter
        }

        private fun setupSpinnerListener() {
            addedBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedUser = spinnerAdapter.getItem(position) ?: return

                    if (addedByChangedListener != null && currentAddedBy != selectedUser) {
                        val movie = getMovieAtPosition(bindingAdapterPosition)
                        if (movie != null) {
                            currentAddedBy = selectedUser
                            addedByChangedListener?.onAddedByChanged(bindingAdapterPosition, movie, selectedUser)
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        private fun setupClickListeners() {
            itemView.setOnClickListener {
                if (!isEditMode) {
                    val currentTime = android.os.SystemClock.elapsedRealtime()
                    if (currentTime - lastClickTime < CLICK_INTERVAL) {
                        return@setOnClickListener
                    }
                    lastClickTime = currentTime

                    val movie = getMovieAtPosition(bindingAdapterPosition)
                    if (movie != null) {
                        val intent = Intent(itemView.context, MovieDetailActivity::class.java)
                        intent.putExtra("movie", movie)

                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            itemView.context as Activity,
                            posterImageView,
                            "moviePosterTransition"
                        )

                        itemView.context.startActivity(intent, options.toBundle())
                    }
                }
            }

            itemView.setOnLongClickListener {
                longClickListener?.onItemLongClick(bindingAdapterPosition) ?: false
            }

            deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    deleteClickListener?.onDeleteClick(position, movies[position])
                }
            }

            addButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    addClickListener?.onAddClick(position, movies[position])
                }
            }

            checkButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    checkClickListener?.onCheckClick(position, movies[position])
                }
            }
        }

        @SuppressLint("DefaultLocale")
        fun bind(movie: Movie) {
            Glide.with(itemView.context).load("https://image.tmdb.org/t/p/w500" + movie.posterPath).into(posterImageView)
            titleTextView.text = movie.title
            yearTextView.text = "(${movie.releaseDate})"
            overviewTextView.text = movie.overview
            voteAverageTextView.text = String.format("%.1f", movie.voteAverage)

            val shouldShowSpinner = !isAddMode && !movie.addedBy.isNullOrEmpty()

            if (shouldShowSpinner) {
                addedBySpinner.onItemSelectedListener = null

                val options = ArrayList<String>()

                if (usersList.isNotEmpty()) {
                    for (user in usersList) {
                        if (!options.contains(user)) {
                            options.add(user)
                        }
                    }
                    options.add("Común")
                } else {
                    val movieAddedBy = movie.addedBy
                    if (!movieAddedBy.isNullOrEmpty()) {
                        options.add(movieAddedBy)
                    }
                    options.add("Común")
                }

                spinnerAdapter.clear()
                spinnerAdapter.addAll(options)
                spinnerAdapter.notifyDataSetChanged()

                val movieAddedBy = movie.addedBy ?: ""
                currentAddedBy = movieAddedBy

                val selectedPos = options.indexOf(movieAddedBy)
                if (selectedPos >= 0) {
                    addedBySpinner.setSelection(selectedPos)
                }

                setupSpinnerListener()

                addedBySpinner.visibility = View.VISIBLE
                posterImageView.maxHeight = 500
                posterImageView.minimumHeight = 500
            } else {
                addedBySpinner.visibility = View.GONE
                addedBySpinner.onItemSelectedListener = null
            }

            deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE

            if (isAddMode) {
                val isAlreadyAdded = isMovieAlreadyAdded(movie, moviesIdList)
                addButton.visibility = if (isAlreadyAdded) View.GONE else View.VISIBLE
                checkButton.visibility = if (isAlreadyAdded) View.VISIBLE else View.GONE
            } else {
                addButton.visibility = View.GONE
                checkButton.visibility = View.GONE
            }
        }
    }

    fun getMovieAtPosition(position: Int): Movie? {
        return if (position in movies.indices) movies[position] else null
    }

    private fun isMovieAlreadyAdded(movie: Movie, moviesIdList: List<Int>): Boolean {
        return moviesIdList.contains(movie.id)
    }
}
