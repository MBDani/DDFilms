package com.merino.ddfilms.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.merino.ddfilms.R
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.Review
import com.merino.ddfilms.utils.Utils.showMessage

class WriteReviewDialogFragment(
    private val movie: Movie?,
    private var review: Review?
) : DialogFragment() {

    private var currentRating = 0f
    private val ratingStars = arrayOfNulls<ImageView>(5)
    private lateinit var reviewEditText: TextInputEditText
    private var listener: OnReviewSubmittedListener? = null

    interface OnReviewSubmittedListener {
        fun onReviewSubmitted(review: Review)
        fun onReviewDeleted(review: Review) {}
    }

    fun setOnReviewSubmittedListener(listener: OnReviewSubmittedListener?) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_write_review, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)

        // Configurar título del diálogo
        val dialogTitle = view.findViewById<TextView>(R.id.dialog_title)
        if (review != null && review?.id != null) {
            dialogTitle.setText(R.string.update_review_title)
        } else {
            dialogTitle.setText(R.string.write_review_title)
        }

        review?.let { rev ->
            rev.reviewText?.let { reviewEditText.setText(it) }
            setRating(rev.rating)
        }

        setupStarRating()
        setupButtons(view)

        // Configurar título de la película
        val movieTitleReview = view.findViewById<TextView>(R.id.movie_title_review)
        movie?.let { movieTitleReview.text = it.title }
    }

    private fun initViews(view: View) {
        reviewEditText = view.findViewById(R.id.review_edit_text)

        // Inicializar array de estrellas
        ratingStars[0] = view.findViewById(R.id.rating_star_1)
        ratingStars[1] = view.findViewById(R.id.rating_star_2)
        ratingStars[2] = view.findViewById(R.id.rating_star_3)
        ratingStars[3] = view.findViewById(R.id.rating_star_4)
        ratingStars[4] = view.findViewById(R.id.rating_star_5)
    }

    private fun setupStarRating() {
        for (i in ratingStars.indices) {
            val starIndex = i
            val star = ratingStars[i] ?: continue

            star.setOnClickListener {
                // Si ya está seleccionada esta estrella completa, hacer media estrella
                val newRating = if (currentRating == (starIndex + 1).toFloat()) {
                    starIndex + 0.5f
                } else {
                    (starIndex + 1).toFloat()
                }
                setRating(newRating)
            }

            // Long click para hacer media estrella directamente
            star.setOnLongClickListener {
                setRating(starIndex + 0.5f)
                true
            }
        }

        updateStarDisplay()
    }

    private fun setRating(rating: Float) {
        currentRating = Math.max(0f, Math.min(5f, rating))
        updateStarDisplay()
    }

    private fun updateStarDisplay() {
        val fullStars = currentRating.toInt()
        val hasHalfStar = (currentRating % 1) == 0.5f

        for (i in 0 until 5) {
            val star = ratingStars[i] ?: continue

            if (i < fullStars) {
                // Estrella completa
                star.setImageResource(R.drawable.ic_star_filled)
                star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold_dark))
            } else if (i == fullStars && hasHalfStar) {
                // Media estrella
                star.setImageResource(R.drawable.ic_star_half)
                star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold_dark))
            } else {
                // Estrella vacía
                star.setImageResource(R.drawable.ic_star_empty)
                star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray_medium))
            }
        }
    }

    private fun setupButtons(view: View) {
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancel_button)
        val publishButton = view.findViewById<MaterialButton>(R.id.publish_button)
        val deleteButton = view.findViewById<View>(R.id.delete_button)

        cancelButton.setOnClickListener { dismiss() }

        publishButton.setOnClickListener {
            if (currentRating > 0) {
                submitReview()
            } else {
                showMessage(context, "Es obligatoria la puntucación")
            }
        }

        val rev = review
        if (rev != null && rev.id != null) {
            publishButton.setText(R.string.update_review_button)
            deleteButton.visibility = View.VISIBLE
            deleteButton.setOnClickListener { deleteReview() }
        } else {
            publishButton.setText(R.string.publish_review)
            deleteButton.visibility = View.GONE
        }
    }

    private fun deleteReview() {
        val rev = review
        if (listener != null && rev != null && rev.id != null) {
            listener?.onReviewDeleted(rev)
        }
        dismiss()
    }

    private fun submitReview() {
        if (listener != null && movie != null) {
            val reviewTextContent = if (reviewEditText.text != null) {
                reviewEditText.text.toString().trim()
            } else ""

            var rev = review
            if (rev == null) {
                rev = Review()
                review = rev
            }
            rev.rating = currentRating
            rev.reviewText = reviewTextContent
            listener?.onReviewSubmitted(rev)
        }

        dismiss()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
