package com.merino.ddfilms.api

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.merino.ddfilms.R
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.MovieLists
import com.merino.ddfilms.model.Review
import com.merino.ddfilms.ui.auth.LoginActivity
import com.merino.ddfilms.utils.StringUtils.MOVIE_LIST
import com.merino.ddfilms.utils.StringUtils.WATCH_LIST
import com.merino.ddfilms.utils.TaskCompletionCallback
import com.merino.ddfilms.utils.Utils
import java.util.Collections
import java.util.Date

class FirebaseManager {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    fun logoutUser(context: Context) {
        firebaseAuth.signOut()

        // Redirigir al usuario a la pantalla de inicio de sesión
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        // Mostrar un mensaje de confirmación
        Toast.makeText(context, context.getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
    }

    fun getCurrentUserUID(): String {
        return firebaseAuth.currentUser!!.uid
    }

    fun getUserName(uid: String, callback: TaskCompletionCallback<String>) {
        firebaseFirestore.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    callback.onComplete(documentSnapshot.getString("user"), null)
                } else {
                    callback.onComplete(null, Exception("Documento no encontrado"))
                }
            }
            .addOnFailureListener { e -> callback.onComplete(null, e) }
    }

    fun getUserMail(uid: String, callback: TaskCompletionCallback<String>) {
        firebaseFirestore.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    callback.onComplete(documentSnapshot.getString("email"), null)
                } else {
                    callback.onComplete(null, Exception("Documento no encontrado"))
                }
            }
            .addOnFailureListener { e -> callback.onComplete(null, e) }
    }

    fun getTmdbApiKey(callback: TaskCompletionCallback<String>) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback.onComplete(firebaseRemoteConfig.getString("TMDB_API_KEY"), null)
            } else {
                callback.onComplete(null, Exception("TMDB_API_KEY_KEY no encontrado"))
            }
        }
    }

    fun createNewMovieList(listName: String, userID: String, callback: TaskCompletionCallback<String>) {
        // Verificar si ya existe una lista con el mismo nombre y usuario
        firebaseFirestore.collection("movieLists")
            .whereEqualTo("name", listName)
            .whereArrayContains("userID", userID)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isEmpty) {
                    callback.onComplete(null, Exception("La lista $listName ya existe."))
                } else if (task.isSuccessful) {
                    // Obtener avatar del usuario para la vista previa de miembros
                    getUserProfileImageUrl(userID) { avatarUrl, _ ->
                        val memberAvatars = ArrayList<String>()
                        if (!avatarUrl.isNullOrEmpty()) {
                            memberAvatars.add(avatarUrl)
                        }

                        val newList = HashMap<String, Any?>()
                        newList["name"] = listName
                        newList["userID"] = ArrayList(listOf(userID))
                        newList["movies"] = ArrayList<Any>()
                        newList["moviesCount"] = 0
                        newList["coverPreviews"] = ArrayList<Any>()
                        newList["memberAvatarsPreview"] = memberAvatars
                        newList["lastUpdated"] = Utils.formatDate(Date())

                        firebaseFirestore.collection("movieLists").add(newList)
                            .addOnSuccessListener { documentReference ->
                                firebaseFirestore.collection("users").document(userID)
                                    .update("movieLists", FieldValue.arrayUnion(documentReference.id))
                                    .addOnSuccessListener { callback.onComplete("Lista creada con éxito", null) }
                            }
                            .addOnFailureListener { callback.onComplete(null, Exception("Error al crear la lista $listName")) }
                    }
                } else {
                    callback.onComplete(null, Exception("Error al verificar la existencia de la lista $listName"))
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun getMovieLists(userID: String, callback: TaskCompletionCallback<List<MovieLists>>) {
        firebaseFirestore.collection("movieLists")
            .whereArrayContains("userID", userID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val lists = ArrayList<MovieLists>()
                for (doc in querySnapshot.documents) {
                    val list = MovieLists().apply {
                        id = doc.id
                        name = doc.getString("name")
                        this.userID = doc.get("userID") as? List<String>
                        coverPreviews = doc.get("coverPreviews") as? List<String>
                        memberAvatarsPreview = doc.get("memberAvatarsPreview") as? List<String>
                        lastUpdated = doc.getString("lastUpdated")
                        moviesCount = doc.getLong("moviesCount")?.toInt() ?: 0
                    }
                    lists.add(list)
                }
                callback.onComplete(lists, null)
            }
            .addOnFailureListener { e -> callback.onComplete(null, e) }
    }

    fun addMovieToList(collection: String, documentID: String, movie: Movie, callback: TaskCompletionCallback<String>) {
        getMovieListByID(documentID, collection) { documentSnapshot, error ->
            if (error != null) {
                callback.onComplete(null, error)
            } else {
                val listName = documentSnapshot!!.getString("name")
                val movieAlreadyExists = isMovieAlreadyExists(movie, documentSnapshot)
                if (movieAlreadyExists) {
                    callback.onComplete(null, Exception("La película ya se encuentra en la lista: $listName"))
                } else {
                    addMovie(movie, getCurrentUserUID(), callback, documentSnapshot, "Película agregada a la lista $listName", "Error al agregar la película a la lista")
                }
            }
        }
    }

    fun addMovieToWatchOrDiaryList(collection: String, movie: Movie, callback: TaskCompletionCallback<String>) {
        val listName = if (collection == WATCH_LIST) "pendientes" else "diario"
        val userID = getCurrentUserUID()
        getMovieListByID(userID, collection) { documentSnapshot, error ->
            if (error != null) {
                callback.onComplete(null, error)
            } else if (!documentSnapshot!!.exists()) {
                firebaseFirestore.collection(collection).document(userID).set(Collections.emptyMap<Any, Any>())
                    .addOnSuccessListener {
                        callback.onComplete("Lista de $listName creada con éxito", null)
                        addMovie(movie, userID, callback, documentSnapshot, "Película agregada a $listName", "Error al agregar la película a $listName")
                    }
                    .addOnFailureListener {
                        callback.onComplete(null, Exception("Error al crear la lista"))
                    }
            } else {
                val movieAlreadyExists = isMovieAlreadyExists(movie, documentSnapshot)
                if (movieAlreadyExists) {
                    callback.onComplete(null, Exception("La película ya se encuentra en $listName"))
                } else {
                    addMovie(movie, userID, callback, documentSnapshot, "Película agregada a $listName", "Error al agregar la película a $listName")
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addMovie(movie: Movie, userID: String, callback: TaskCompletionCallback<String>, documentSnapshot: DocumentSnapshot, successMessage: String, errorMessage: String) {
        val date = Date()
        val formattedDate = Utils.formatDate(date)
        movie.createdAt = formattedDate

        getUserName(userID) { userName, errorGetUser ->
            if (errorGetUser != null) {
                callback.onComplete(null, errorGetUser)
            } else {
                movie.addedBy = userName
                val docRef = documentSnapshot.reference

                firebaseFirestore.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    var moviesMaps = snapshot.get("movies") as? MutableList<Map<String, Any?>>
                    if (moviesMaps == null) {
                        moviesMaps = ArrayList()
                    }

                    val newMovieMap = movie.toMap()
                    moviesMaps.add(newMovieMap)

                    val count = moviesMaps.size
                    val coverPreviews = ArrayList<String>()
                    val start = Math.max(0, moviesMaps.size - 3)
                    for (i in start until moviesMaps.size) {
                        val poster = moviesMaps[i]["posterPath"] as? String
                        if (!poster.isNullOrEmpty()) {
                            coverPreviews.add(poster)
                        }
                    }

                    transaction.update(docRef, "movies", moviesMaps)
                    transaction.update(docRef, "moviesCount", count)
                    transaction.update(docRef, "coverPreviews", coverPreviews)
                    transaction.update(docRef, "lastUpdated", formattedDate)
                    null
                }
                .addOnSuccessListener { callback.onComplete(successMessage, null) }
                .addOnFailureListener { e -> callback.onComplete(null, Exception("$errorMessage: ${e.message}")) }
            }
        }
    }

    fun getMoviesFromList(listID: String, collectionName: String, callback: TaskCompletionCallback<List<Movie>>) {
        firebaseFirestore.collection(collectionName).document(listID).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    val movieList = parseMovieList(documentSnapshot)
                    callback.onComplete(movieList, null)
                } else {
                    callback.onComplete(null, Exception("No se han encontrado películas en la lista"))
                }
            }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al obtener la lista de películas")) }
    }

    private fun getMovieListByID(documentID: String, collection: String, callback: TaskCompletionCallback<DocumentSnapshot>) {
        firebaseFirestore.collection(collection).document(documentID).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot != null) {
                    callback.onComplete(documentSnapshot, null)
                } else {
                    callback.onComplete(null, Exception("No se han encontrado películas en la lista"))
                }
            }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al obtener la lista de películas")) }
    }

    private fun isMovieAlreadyExists(movie: Movie, documentSnapshot: DocumentSnapshot): Boolean {
        val movieList = parseMovieList(documentSnapshot)
        return movieList.any { it.originalTitle == movie.originalTitle }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseMovieList(document: DocumentSnapshot): List<Movie> {
        val movieMaps = document.get("movies") as? List<Map<String, Any?>>
        val movieList = ArrayList<Movie>()
        if (movieMaps == null || movieMaps.isEmpty()) return movieList

        for (movieMap in movieMaps) {
            val m = Movie.mapToMovie(movieMap)
            movieList.add(m)
        }
        return movieList
    }

    @Suppress("UNCHECKED_CAST")
    fun getListUsersIDsAndNames(listID: String, callback: TaskCompletionCallback<HashMap<String, String>>) {
        getMovieListByID(listID, MOVIE_LIST) { document, error ->
            if (error != null) {
                callback.onComplete(null, error)
            } else {
                val users = document!!.get("userID") as? List<String> ?: emptyList()
                val usersMap = HashMap<String, String>()
                if (users.isEmpty()) {
                    callback.onComplete(usersMap, null)
                    return@getMovieListByID
                }

                for (userID in users) {
                    getUserName(userID) { userName, errorGetUser ->
                        if (errorGetUser != null) {
                            callback.onComplete(null, errorGetUser)
                        } else {
                            usersMap[userID] = userName!!
                            if (usersMap.size == users.size) {
                                callback.onComplete(usersMap, null)
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateListName(listID: String, newListname: String, callback: TaskCompletionCallback<Boolean>) {
        getMovieListByID(listID, MOVIE_LIST) { document, error ->
            if (error != null) {
                callback.onComplete(null, error)
            } else {
                document!!.reference.update("name", newListname)
                    .addOnSuccessListener { callback.onComplete(true, null) }
                    .addOnFailureListener { callback.onComplete(null, Exception("Error al actualizar el nombre de la lista")) }
            }
        }
    }

    fun deleteList(listID: String, callback: TaskCompletionCallback<Boolean>) {
        getMovieListByID(listID, MOVIE_LIST) { document, error ->
            if (error != null) {
                callback.onComplete(null, error)
                return@getMovieListByID
            }

            deleteMovieListsIDFromUser(listID) { result, errorDeleteListsInUserCollection ->
                if (errorDeleteListsInUserCollection != null) {
                    callback.onComplete(null, errorDeleteListsInUserCollection)
                } else if (result != null) {
                    document!!.reference.delete()
                        .addOnSuccessListener { callback.onComplete(true, null) }
                        .addOnFailureListener { callback.onComplete(null, Exception("Error al eliminar la lista")) }
                }
            }
        }
    }

    private fun deleteMovieListsIDFromUser(listID: String, callback: TaskCompletionCallback<Boolean>) {
        getListUsersIDsAndNames(listID) { usersMap, errorGetUsers ->
            if (errorGetUsers != null) {
                callback.onComplete(null, errorGetUsers)
            } else if (usersMap != null) {
                for (userID in usersMap.keys) {
                    firebaseFirestore.collection("users").document(userID).update("movieLists", FieldValue.arrayRemove(listID))
                }
                callback.onComplete(true, null)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun deleteMovieFromList(collection: String, documentID: String, movie: Movie, callback: TaskCompletionCallback<Boolean>) {
        val db = FirebaseFirestore.getInstance()
        db.runTransaction { transaction ->
            val docRef = db.collection(collection).document(documentID)
            val snapshot = transaction.get(docRef)

            val movies = snapshot.get("movies") as? List<Map<String, Any?>> ?: emptyList()
            val updatedMovies = movies.filter { m ->
                val movieId = m["id"].toString()
                movieId != movie.id.toString()
            }

            val count = updatedMovies.size
            val coverPreviews = ArrayList<String>()
            val start = Math.max(0, updatedMovies.size - 3)
            for (i in start until updatedMovies.size) {
                val poster = updatedMovies[i]["posterPath"] as? String
                if (!poster.isNullOrEmpty()) {
                    coverPreviews.add(poster)
                }
            }

            transaction.update(docRef, "movies", updatedMovies)
            transaction.update(docRef, "moviesCount", count)
            transaction.update(docRef, "coverPreviews", coverPreviews)
            transaction.update(docRef, "lastUpdated", Utils.formatDate(Date()))
            null
        }
        .addOnSuccessListener { callback.onComplete(true, null) }
        .addOnFailureListener { e -> callback.onComplete(null, Exception("Error al eliminar la película de la lista: ${e.message}")) }
    }

    fun addListToUser(listID: String, callback: TaskCompletionCallback<Boolean>) {
        val uid = getCurrentUserUID()
        firebaseFirestore.collection("users").document(uid)
            .update("movieLists", FieldValue.arrayUnion(listID))
            .addOnSuccessListener {
                firebaseFirestore.collection("movieLists").document(listID)
                    .update("userID", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener {
                        getUserProfileImageUrl(uid) { avatarUrl, _ ->
                            if (!avatarUrl.isNullOrEmpty()) {
                                firebaseFirestore.collection("movieLists").document(listID)
                                    .update("memberAvatarsPreview", FieldValue.arrayUnion(avatarUrl))
                                    .addOnSuccessListener { callback.onComplete(true, null) }
                                    .addOnFailureListener { callback.onComplete(true, null) }
                            } else {
                                callback.onComplete(true, null)
                            }
                        }
                    }
                    .addOnFailureListener { callback.onComplete(null, Exception("Error al añadir la lista al usuario")) }
            }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al añadir la lista al usuario")) }
    }

    fun updateMovieAddedBy(movieID: Int, newAddedBy: String, listID: String, callback: TaskCompletionCallback<Boolean>) {
        firebaseFirestore.collection("movieLists").document(listID).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val movies = parseMovieList(snapshot)
                    var updated = false

                    for (m in movies) {
                        if (m.id == movieID) {
                            m.addedBy = newAddedBy
                            updated = true
                            break
                        }
                    }

                    if (updated) {
                        firebaseFirestore.collection("movieLists").document(listID)
                            .update("movies", movies)
                            .addOnSuccessListener { callback.onComplete(true, null) }
                            .addOnFailureListener { e -> callback.onComplete(false, e) }
                    } else {
                        callback.onComplete(false, Exception("Película no encontrada en la lista"))
                    }
                } else {
                    callback.onComplete(false, Exception("No existe la lista"))
                }
            }
            .addOnFailureListener { e -> callback.onComplete(false, e) }
    }

    fun postReview(review: Review, callback: TaskCompletionCallback<Review>) {
        val docRef = firebaseFirestore.collection("reviews").document()
        review.id = docRef.id

        docRef.set(review)
            .addOnSuccessListener { callback.onComplete(review, null) }
            .addOnFailureListener { e -> callback.onComplete(null, Exception("Error al añadir la reseña", e)) }
    }

    fun updateReview(review: Review, callback: TaskCompletionCallback<Review>) {
        firebaseFirestore.collection("reviews").document(review.id!!).set(review)
            .addOnSuccessListener { callback.onComplete(review, null) }
            .addOnFailureListener { e -> callback.onComplete(null, Exception("Error al actualizar reseña", e)) }
    }

    fun deleteReview(reviewId: String, callback: TaskCompletionCallback<Boolean>) {
        firebaseFirestore.collection("reviews").document(reviewId).delete()
            .addOnSuccessListener { callback.onComplete(true, null) }
            .addOnFailureListener { e -> callback.onComplete(null, Exception("Error al borrar la reseña", e)) }
    }

    fun getReviews(movieId: Int, callback: TaskCompletionCallback<List<Review>>) {
        firebaseFirestore.collection("reviews")
            .whereEqualTo("movieId", movieId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val reviews = ArrayList<Review>()
                for (document in querySnapshot.documents) {
                    document.toObject(Review::class.java)?.let { reviews.add(it) }
                }
                callback.onComplete(reviews, null)
            }
            .addOnFailureListener { e -> callback.onComplete(null, Exception("Error al obtener reseñas", e)) }
    }

    fun getAllReviews(callback: TaskCompletionCallback<List<Review>>) {
        firebaseFirestore.collection("reviews").get()
            .addOnSuccessListener { querySnapshot ->
                val reviews = ArrayList<Review>()
                for (document in querySnapshot.documents) {
                    document.toObject(Review::class.java)?.let { reviews.add(it) }
                }
                callback.onComplete(reviews, null)
            }
            .addOnFailureListener { e -> callback.onComplete(null, Exception("Error al obtener reseñas", e)) }
    }

    fun reviewAddLike(reviewId: String, userId: String, callback: TaskCompletionCallback<Boolean>) {
        val reviewRef = firebaseFirestore.collection("reviews").document(reviewId)
        reviewRef.update("likeCount", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { callback.onComplete(true, null) }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al añadir like a la lista")) }
    }

    fun reviewRemoveLike(reviewId: String, userId: String, callback: TaskCompletionCallback<Boolean>) {
        val reviewRef = firebaseFirestore.collection("reviews").document(reviewId)
        reviewRef.update("likeCount", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { callback.onComplete(true, null) }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al añadir like a la lista")) }
    }

    fun reviewAddDislike(reviewId: String, userId: String, callback: TaskCompletionCallback<Boolean>) {
        val reviewRef = firebaseFirestore.collection("reviews").document(reviewId)
        reviewRef.update("dislikeCount", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { callback.onComplete(true, null) }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al añadir dislike a la lista")) }
    }

    fun reviewRemoveDislike(reviewId: String, userId: String, callback: TaskCompletionCallback<Boolean>) {
        val reviewRef = firebaseFirestore.collection("reviews").document(reviewId)
        reviewRef.update("dislikeCount", FieldValue.arrayRemove(userId))
            .addOnSuccessListener { callback.onComplete(true, null) }
            .addOnFailureListener { callback.onComplete(null, Exception("Error al añadir dislike a la lista")) }
    }

    fun getUserProfileImageUrl(uid: String, callback: TaskCompletionCallback<String>) {
        firebaseFirestore.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    callback.onComplete(documentSnapshot.getString("userProfileImageUrl"), null)
                } else {
                    callback.onComplete(null, null)
                }
            }
            .addOnFailureListener { e -> callback.onComplete(null, e) }
    }

    @Suppress("UNCHECKED_CAST")
    fun updateUserProfileImage(userId: String, imagePath: String, callback: TaskCompletionCallback<Boolean>) {
        val reviewsTask = firebaseFirestore.collection("reviews")
            .whereEqualTo("userId", userId)
            .get()
        val listsTask = firebaseFirestore.collection("movieLists")
            .whereArrayContains("userID", userId)
            .get()

        Tasks.whenAllSuccess<QuerySnapshot>(reviewsTask, listsTask)
            .addOnSuccessListener { results ->
                val reviewsSnapshot = results[0]
                val listsSnapshot = results[1]

                firebaseFirestore.runTransaction { transaction ->
                    val userRef = firebaseFirestore.collection("users").document(userId)
                    val userSnapshot = transaction.get(userRef)

                    val reviewRefs = ArrayList<DocumentReference>()
                    for (doc in reviewsSnapshot.documents) {
                        val ref = doc.reference
                        transaction.get(ref)
                        reviewRefs.add(ref)
                    }

                    val listSnapshots = ArrayList<DocumentSnapshot>()
                    for (doc in listsSnapshot.documents) {
                        listSnapshots.add(transaction.get(doc.reference))
                    }

                    if (userSnapshot.exists()) {
                        transaction.update(userRef, "userProfileImageUrl", imagePath)
                    } else {
                        val userData = HashMap<String, Any?>()
                        userData["userProfileImageUrl"] = imagePath
                        transaction.set(userRef, userData, com.google.firebase.firestore.SetOptions.merge())
                    }

                    for (ref in reviewRefs) {
                        transaction.update(ref, "userProfileImageUrl", imagePath)
                    }

                    for (listSnap in listSnapshots) {
                        val userIDs = listSnap.get("userID") as? List<String>
                        var memberAvatars = listSnap.get("memberAvatarsPreview") as? List<String>
                        if (userIDs != null) {
                            val mutableAvatars = if (memberAvatars == null) ArrayList() else ArrayList(memberAvatars)
                            while (mutableAvatars.size < userIDs.size) {
                                mutableAvatars.add("")
                            }
                            val idx = userIDs.indexOf(userId)
                            if (idx != -1) {
                                mutableAvatars[idx] = imagePath
                                transaction.update(listSnap.reference, "memberAvatarsPreview", mutableAvatars)
                            }
                        }
                    }
                    null
                }
                .addOnSuccessListener { callback.onComplete(true, null) }
                .addOnFailureListener { e -> callback.onComplete(null, e) }
            }
            .addOnFailureListener { e -> callback.onComplete(null, e) }
    }

    fun getMovieListsPaginated(userID: String, lastVisible: DocumentSnapshot?, limit: Int, callback: TaskCompletionCallback<android.util.Pair<List<MovieLists>, DocumentSnapshot>>) {
        val query = firebaseFirestore.collection("movieLists")
            .whereArrayContains("userID", userID)
            .limit(limit.toLong())

        val finalQuery = if (lastVisible != null) query.startAfter(lastVisible) else query

        finalQuery.get(Source.CACHE)
            .addOnSuccessListener { cacheSnapshot ->
                if (cacheSnapshot != null && !cacheSnapshot.isEmpty) {
                    val lists = parseQuerySnapshot(cacheSnapshot)
                    val nextLastVisible = cacheSnapshot.documents[cacheSnapshot.size() - 1]
                    callback.onComplete(android.util.Pair(lists, nextLastVisible), null)
                }
                fetchFromServer(finalQuery, callback)
            }
            .addOnFailureListener {
                fetchFromServer(finalQuery, callback)
            }
    }

    private fun parseQuerySnapshot(querySnapshot: QuerySnapshot): List<MovieLists> {
        val lists = ArrayList<MovieLists>()
        for (doc in querySnapshot.documents) {
            val list = MovieLists().apply {
                id = doc.id
                name = doc.getString("name")
                userID = doc.get("userID") as? List<String>
                coverPreviews = doc.get("coverPreviews") as? List<String>
                memberAvatarsPreview = doc.get("memberAvatarsPreview") as? List<String>
                lastUpdated = doc.getString("lastUpdated")
                moviesCount = doc.getLong("moviesCount")?.toInt() ?: 0
            }
            lists.add(list)
        }
        return lists
    }

    private fun fetchFromServer(query: Query, callback: TaskCompletionCallback<android.util.Pair<List<MovieLists>, DocumentSnapshot>>) {
        query.get(Source.SERVER)
            .addOnSuccessListener { serverSnapshot ->
                val lists = parseQuerySnapshot(serverSnapshot)
                var nextLastVisible: DocumentSnapshot? = null
                if (!serverSnapshot.isEmpty) {
                    nextLastVisible = serverSnapshot.documents[serverSnapshot.size() - 1]
                }
                callback.onComplete(android.util.Pair(lists, nextLastVisible), null)
            }
            .addOnFailureListener { e -> callback.onComplete(null, e) }
    }

    companion object {
        private var instance: FirebaseManager? = null

        @JvmStatic
        fun getInstance(): FirebaseManager {
            if (instance == null) {
                instance = FirebaseManager()
            }
            return instance!!
        }
    }
}
