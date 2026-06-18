package com.merino.ddfilms.api;

import static com.merino.ddfilms.utils.StringUtils.MOVIE_LIST;
import static com.merino.ddfilms.utils.StringUtils.WATCH_LIST;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.MovieLists;
import com.merino.ddfilms.model.Review;
import com.google.firebase.firestore.Query;
import com.merino.ddfilms.ui.auth.LoginActivity;
import com.merino.ddfilms.utils.TaskCompletionCallback;
import com.merino.ddfilms.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firebaseFirestore;
    private final FirebaseRemoteConfig firebaseRemoteConfig;


    public FirebaseManager() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseFirestore = FirebaseFirestore.getInstance();
        this.firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void logoutUser(Context context) {
        firebaseAuth.signOut();

        // Redirigir al usuario a la pantalla de inicio de sesión
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        // Mostrar un mensaje de confirmación
        Toast.makeText(context, "Sesión cerrada con éxito", Toast.LENGTH_SHORT).show();
    }

    public String getCurrentUserUID() {
        return Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
    }

    public void getUserName(String uid, TaskCompletionCallback<String> callback) {
        firebaseFirestore.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                callback.onComplete(documentSnapshot.getString("user"), null);
            } else {
                callback.onComplete(null, new Exception("Documento no encontrado"));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, e));
    }

    public void getUserMail(String uid, TaskCompletionCallback<String> callback) {
        firebaseFirestore.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                callback.onComplete(documentSnapshot.getString("email"), null);
            } else {
                callback.onComplete(null, new Exception("Documento no encontrado"));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, e));
    }

    public void getTmdbApiKey(TaskCompletionCallback<String> callback) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onComplete(firebaseRemoteConfig.getString("TMDB_API_KEY"), null);
            } else {
                callback.onComplete(null, new Exception("TMDB_API_KEY_KEY no encontrado"));
            }
        });
    }

    public void createNewMovieList(String listName, String userID, TaskCompletionCallback<String> callback) {
        // Verificar si ya existe una lista con el mismo nombre y usuario
        firebaseFirestore.collection("movieLists")
                .whereEqualTo("name", listName)
                .whereArrayContains("userID", userID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        callback.onComplete(null, new Exception("La lista " + listName + " ya existe."));
                    } else if (task.isSuccessful()) {
                        // Obtener avatar del usuario para la vista previa de miembros
                        getUserProfileImageUrl(userID, (avatarUrl, errorGetAvatar) -> {
                            List<String> memberAvatars = new ArrayList<>();
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                memberAvatars.add(avatarUrl);
                            }

                            Map<String, Object> newList = new HashMap<>();
                            newList.put("name", listName);
                            newList.put("userID", new ArrayList<>(Collections.singletonList(userID)));
                            newList.put("movies", new ArrayList<>());
                            newList.put("moviesCount", 0);
                            newList.put("coverPreviews", new ArrayList<>());
                            newList.put("memberAvatarsPreview", memberAvatars);
                            newList.put("lastUpdated", Utils.formatDate(new java.util.Date()));

                            firebaseFirestore.collection("movieLists").add(newList).addOnSuccessListener(documentReference -> {
                                firebaseFirestore.collection("users").document(userID)
                                        .update("movieLists", FieldValue.arrayUnion(documentReference.getId()))
                                        .addOnSuccessListener(success -> callback.onComplete("Lista creada con éxito", null));
                            }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al crear la lista " + listName)));
                        });
                    } else {
                        callback.onComplete(null, new Exception("Error al verificar la existencia de la lista " + listName));
                    }
                });
    }

    public void getMovieLists(String userID, TaskCompletionCallback<List<MovieLists>> callback) {
        firebaseFirestore.collection("movieLists")
                .whereArrayContains("userID", userID)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<MovieLists> lists = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        MovieLists list = new MovieLists();
                        list.setId(doc.getId());
                        list.setName(doc.getString("name"));
                        list.setUserID((List<String>) doc.get("userID"));
                        list.setCoverPreviews((List<String>) doc.get("coverPreviews"));
                        list.setMemberAvatarsPreview((List<String>) doc.get("memberAvatarsPreview"));
                        list.setLastUpdated(doc.getString("lastUpdated"));

                        Long countVal = doc.getLong("moviesCount");
                        list.setMoviesCount(countVal != null ? countVal.intValue() : 0);

                        lists.add(list);
                    }
                    callback.onComplete(lists, null);
                })
                .addOnFailureListener(e -> callback.onComplete(null, e));
    }

    public void addMovieToList(String collection, String documentID, Movie movie, TaskCompletionCallback<String> callback) {
        getMovieListByID(documentID, collection, (documentSnapshot, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
            } else {
                String listName = documentSnapshot.getString("name");
                boolean movieAlreadyExists = isMovieAlreadyExists(movie, documentSnapshot);
                if (movieAlreadyExists) {
                    callback.onComplete(null, new Exception("La película ya se encuentra en la lista: " + listName));
                } else {
                    // Setteamos la fecha de creación
                    addMovie(movie, getCurrentUserUID(), callback, documentSnapshot, "Película agregada a la lista " + listName, "Error al agregar la película a la lista");
                }
            }
        });
    }

    public void addMovieToWatchOrDiaryList(String collection, Movie movie, TaskCompletionCallback<String> callback) {
        String listName = collection.equals(WATCH_LIST) ? "pendientes" : "diario";
        String userID = getCurrentUserUID();
        getMovieListByID(userID, collection, (documentSnapshot, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
            } else if (!documentSnapshot.exists()) {
                firebaseFirestore.collection(collection).document(userID).set(Collections.emptyMap()).addOnSuccessListener(aVoid -> {
                    callback.onComplete("Lista de " + listName + " creada con éxito", null);
                    addMovie(movie, userID, callback, documentSnapshot, "Película agregada a la lista de " + listName, "Error al agregar la película a la lista de");
                }).addOnFailureListener(e -> {
                    callback.onComplete(null, new Exception("Error al crear la lista"));
                });
            } else {
                boolean movieAlreadyExists = isMovieAlreadyExists(movie, documentSnapshot);
                if (movieAlreadyExists) {
                    callback.onComplete(null, new Exception("La película ya se encuentra en " + listName));
                } else {
                    addMovie(movie, userID, callback, documentSnapshot, "Película agregada a " + listName, "Error al agregar la película a " + listName);
                }
            }
        });
    }

    private void addMovie(Movie movie, String userID, TaskCompletionCallback<String> callback, DocumentSnapshot documentSnapshot, String successMessage, String errorMessage) {
        Date date = new Date();
        String formattedDate = Utils.formatDate(date);
        movie.setCreatedAt(formattedDate);

        getUserName(userID, (userName, errorGetUser) -> {
            if (errorGetUser != null) {
                callback.onComplete(null, errorGetUser);
            } else {
                movie.setAddedBy(userName);
                DocumentReference docRef = documentSnapshot.getReference();

                firebaseFirestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef);

                    List<Map<String, Object>> moviesMaps = (List<Map<String, Object>>) snapshot.get("movies");
                    if (moviesMaps == null) {
                        moviesMaps = new ArrayList<>();
                    }

                    Map<String, Object> newMovieMap = movie.toMap();
                    moviesMaps.add(newMovieMap);

                    int count = moviesMaps.size();

                    List<String> coverPreviews = new ArrayList<>();
                    int start = Math.max(0, moviesMaps.size() - 3);
                    for (int i = start; i < moviesMaps.size(); i++) {
                        String poster = (String) moviesMaps.get(i).get("posterPath");
                        if (poster != null && !poster.isEmpty()) {
                            coverPreviews.add(poster);
                        }
                    }

                    transaction.update(docRef, "movies", moviesMaps);
                    transaction.update(docRef, "moviesCount", count);
                    transaction.update(docRef, "coverPreviews", coverPreviews);
                    transaction.update(docRef, "lastUpdated", formattedDate);

                    return null;
                }).addOnSuccessListener(result -> {
                    callback.onComplete(successMessage, null);
                }).addOnFailureListener(e -> {
                    callback.onComplete(null, new Exception(errorMessage + ": " + e.getMessage()));
                });
            }
        });
    }

    public void getMoviesFromList(String listID, String collectionName, TaskCompletionCallback<List<Movie>> callback) {
        firebaseFirestore.collection(collectionName).document(listID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot != null) {
                List<Movie> movieList = parseMovieList(documentSnapshot);
                callback.onComplete(movieList, null);
            } else {
                callback.onComplete(null, new Exception("No se han encontrado películas en la lista"));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener la lista de películas")));
    }

    private void getMovieListByID(String documentID, String collection, TaskCompletionCallback<DocumentSnapshot> callback) {
        firebaseFirestore.collection(collection).document(documentID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot != null) {
                callback.onComplete(documentSnapshot, null);
            } else {
                callback.onComplete(null, new Exception("No se han encontrado películas en la lista"));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener la lista de películas")));
    }

    private boolean isMovieAlreadyExists(Movie movie, DocumentSnapshot documentSnapshot) {
        List<Movie> movieList = parseMovieList(documentSnapshot);
        return movieList.stream().anyMatch(movieMap -> movieMap.getOriginalTitle().equals(movie.getOriginalTitle()));
    }

    @NonNull
    private static List<Movie> parseMovieList(DocumentSnapshot document) {
        List<Map<String, Object>> movieMaps = (List<Map<String, Object>>) document.get("movies");
        List<Movie> movieList = new ArrayList<>();

        if (movieMaps == null || movieMaps.isEmpty()) return movieList;

        // Parseamos los objetos Movie de la lista
        for (Map<String, Object> movieMap : movieMaps) {
            Movie m = Movie.mapToMovie(movieMap);
            movieList.add(m);
        }
        return movieList;
    }

    public void getListUsersIDsAndNames(String listID, TaskCompletionCallback<HashMap<String, String>> callback) {
        getMovieListByID(listID, MOVIE_LIST, (document, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
            } else {
                // Obtenemos el array de string de usuarios
                List<String> users = (List<String>) document.get("userID");

                // Por cada userID tenemos que buscar la base de datos su nombre de usuario
                HashMap<String, String> usersMap = new HashMap<>();
                for (String userID : users) {
                    getUserName(userID, (userName, errorGetUser) -> {
                        if (errorGetUser != null) {
                            callback.onComplete(null, errorGetUser);
                        } else {
                            usersMap.put(userID, userName);
                            if (usersMap.size() == users.size()) {
                                callback.onComplete(usersMap, null);
                            }
                        }
                    });
                }
            }
        });
    }

    public void updateListName(String listID, String
            newListname, TaskCompletionCallback<Boolean> callback) {
        getMovieListByID(listID, MOVIE_LIST, (document, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
            } else {
                document.getReference().update("name", newListname)
                        .addOnSuccessListener(success -> callback.onComplete(true, null))
                        .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al actualizar el nombre de la lista")));
            }
        });
    }

    public void deleteList(String listID, TaskCompletionCallback<Boolean> callback) {
        getMovieListByID(listID, MOVIE_LIST, (document, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
            }

            // Antes de borrar tenemos que obtener toda la lista de usuarios que están en la lista para borrarla de su perfil
            deleteMovieListsIDFromUser(listID, (result, errorDeleteListsInUserCollection) -> {
                if (errorDeleteListsInUserCollection != null) {
                    callback.onComplete(null, errorDeleteListsInUserCollection);
                } else if (result != null) {
                    // Borramos la lista
                    document.getReference().delete()
                            .addOnSuccessListener(success -> callback.onComplete(true, null))
                            .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al eliminar la lista")));
                }
            });
        });
    }

    private void deleteMovieListsIDFromUser(String listID, TaskCompletionCallback<Boolean> callback) {
        getListUsersIDsAndNames(listID, (usersMap, errorGetUsers) -> {
            if (errorGetUsers != null) {
                callback.onComplete(null, errorGetUsers);
            } else if (usersMap != null) {
                // Recorremos y borramos del campo movieLists de cada usuario el id de la lista
                for (String userID : usersMap.keySet()) {
                    firebaseFirestore.collection("users").document(userID).update("movieLists", FieldValue.arrayRemove(listID));
                }
                callback.onComplete(true, null);
            }
        });
    }

    public void deleteMovieFromList(String collection, String documentID, Movie movie, TaskCompletionCallback<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.runTransaction(transaction -> {
            DocumentReference docRef = db.collection(collection).document(documentID);
            DocumentSnapshot snapshot = transaction.get(docRef);

            List<Map<String, Object>> movies = (List<Map<String, Object>>) snapshot.get("movies");
            if (movies == null) {
                movies = new ArrayList<>();
            }
            List<Map<String, Object>> updatedMovies = movies.stream()
                    .filter(m -> {
                        String movieId = String.valueOf(m.get("id"));
                        return !movieId.equals(String.valueOf(movie.getId()));
                    })
                    .collect(Collectors.toList());

            int count = updatedMovies.size();

            List<String> coverPreviews = new ArrayList<>();
            int start = Math.max(0, updatedMovies.size() - 3);
            for (int i = start; i < updatedMovies.size(); i++) {
                String poster = (String) updatedMovies.get(i).get("posterPath");
                if (poster != null && !poster.isEmpty()) {
                    coverPreviews.add(poster);
                }
            }

            transaction.update(docRef, "movies", updatedMovies);
            transaction.update(docRef, "moviesCount", count);
            transaction.update(docRef, "coverPreviews", coverPreviews);
            transaction.update(docRef, "lastUpdated", Utils.formatDate(new Date()));
            return null;
        }).addOnSuccessListener(result -> {
            callback.onComplete(true, null);
        }).addOnFailureListener(e -> {
            callback.onComplete(null, new Exception("Error al eliminar la película de la lista: " + e.getMessage()));
        });
    }

    public void addListToUser(String listID, TaskCompletionCallback<Boolean> callback) {
        String uid = getCurrentUserUID();
        firebaseFirestore.collection("users").document(uid)
                .update("movieLists", FieldValue.arrayUnion(listID))
                .addOnSuccessListener(success -> {
                    firebaseFirestore.collection("movieLists").document(listID)
                            .update("userID", FieldValue.arrayUnion(uid))
                            .addOnSuccessListener(success2 -> {
                                getUserProfileImageUrl(uid, (avatarUrl, errorGetAvatar) -> {
                                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                        firebaseFirestore.collection("movieLists").document(listID)
                                                .update("memberAvatarsPreview", FieldValue.arrayUnion(avatarUrl))
                                                .addOnSuccessListener(success3 -> callback.onComplete(true, null))
                                                .addOnFailureListener(e -> callback.onComplete(true, null));
                                    } else {
                                        callback.onComplete(true, null);
                                    }
                                });
                            })
                            .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al añadir la lista al usuario")));
                })
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al añadir la lista al usuario")));
    }

    public void updateMovieAddedBy(int movieID, String newAddedBy, String listID, TaskCompletionCallback<Boolean> callback) {
        firebaseFirestore.collection("movieLists").document(listID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<Movie> movies = parseMovieList(snapshot);
                        boolean updated = false;

                        for (Movie m : movies) {
                            if (m.getId() == movieID) {
                                m.setAddedBy(newAddedBy);
                                updated = true;
                                break;
                            }
                        }

                        if (updated) {
                            firebaseFirestore.collection("movieLists").document(listID)
                                    .update("movies", movies)
                                    .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
                                    .addOnFailureListener(e -> callback.onComplete(false, e));
                        } else {
                            callback.onComplete(false, new Exception("Película no encontrada en la lista"));
                        }
                    } else {
                        callback.onComplete(false, new Exception("No existe la lista"));
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(false, e));
    }

    public void postReview(Review review, TaskCompletionCallback<Review> callback) {
        // Genera un nuevo DocumentReference; Firebase genera un id único para este documento.
        DocumentReference docRef = firebaseFirestore.collection("reviews").document();

        // Asigna el id generado al objeto Review.
        review.setId(docRef.getId());

        docRef.set(review)
                .addOnSuccessListener(aVoid -> callback.onComplete(review, null))
                .addOnFailureListener(e ->
                        callback.onComplete(null, new Exception("Error al añadir la reseña", e))
                );
    }

    public void updateReview(Review review, TaskCompletionCallback<Review> callback) {
        firebaseFirestore.collection("reviews").document(review.getId()).set(review)
                .addOnSuccessListener(aVoid -> callback.onComplete(review, null))
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al actualizar reseña", e)));
    }

    public void deleteReview(String reviewId, TaskCompletionCallback<Boolean> callback) {
        firebaseFirestore.collection("reviews").document(reviewId).delete()
                .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al borrar la reseña", e)));
    }

    public void getReviews(Integer movieId, TaskCompletionCallback<List<Review>> callback) {
        firebaseFirestore.collection("reviews")
                .whereEqualTo("movieId", movieId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        reviews.add(document.toObject(Review.class));
                    }
                    callback.onComplete(reviews, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, new Exception("Error al obtener reseñas", e))
                );
    }

    public void getAllReviews(TaskCompletionCallback<List<Review>> callback) {
        firebaseFirestore.collection("reviews")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        reviews.add(document.toObject(Review.class));
                    }
                    callback.onComplete(reviews, null);
                })
                .addOnFailureListener(e ->
                        callback.onComplete(null, new Exception("Error al obtener reseñas", e))
                );
    }

    public void reviewAddLike(String reviewId, String userId, TaskCompletionCallback<Boolean> callback) {
        DocumentReference reviewRef = firebaseFirestore.collection("reviews").document(reviewId);
        reviewRef.update("likeCount", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(success -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al añadir like a la lista")));
    }

    public void reviewRemoveLike(String reviewId, String userId, TaskCompletionCallback<Boolean> callback) {
        DocumentReference reviewRef = firebaseFirestore.collection("reviews").document(reviewId);
        reviewRef.update("likeCount", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(success -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al añadir like a la lista")));
    }

    public void reviewAddDislike(String reviewId, String userId, TaskCompletionCallback<Boolean> callback) {
        DocumentReference reviewRef = firebaseFirestore.collection("reviews").document(reviewId);
        reviewRef.update("dislikeCount", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(success -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al añadir dislike a la lista")));
    }

    public void reviewRemoveDislike(String reviewId, String userId, TaskCompletionCallback<Boolean> callback) {
        DocumentReference reviewRef = firebaseFirestore.collection("reviews").document(reviewId);
        reviewRef.update("dislikeCount", FieldValue.arrayRemove(userId))
                .addOnSuccessListener(success -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al añadir dislike a la lista")));
    }

    public void getUserProfileImageUrl(String uid, TaskCompletionCallback<String> callback) {
        firebaseFirestore.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                callback.onComplete(documentSnapshot.getString("userProfileImageUrl"), null);
            } else {
                callback.onComplete(null, null); // Return null instead of error to handle new/empty profiles gracefully
            }
        }).addOnFailureListener(e -> callback.onComplete(null, e));
    }

    public void updateUserProfileImage(String userId, String imagePath, TaskCompletionCallback<Boolean> callback) {
        Task<QuerySnapshot> reviewsTask = firebaseFirestore.collection("reviews")
                .whereEqualTo("userId", userId)
                .get();
        Task<QuerySnapshot> listsTask = firebaseFirestore.collection("movieLists")
                .whereArrayContains("userID", userId)
                .get();

        Tasks.whenAllSuccess(reviewsTask, listsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot reviewsSnapshot = (QuerySnapshot) results.get(0);
                    QuerySnapshot listsSnapshot = (QuerySnapshot) results.get(1);

                    firebaseFirestore.runTransaction(transaction -> {
                        DocumentReference userRef = firebaseFirestore.collection("users").document(userId);
                        DocumentSnapshot userSnapshot = transaction.get(userRef);

                        // Read all review documents first (all transaction reads must precede writes)
                        List<DocumentReference> reviewRefs = new ArrayList<>();
                        for (DocumentSnapshot doc : reviewsSnapshot.getDocuments()) {
                            DocumentReference ref = doc.getReference();
                            transaction.get(ref);
                            reviewRefs.add(ref);
                        }

                        // Read all movieList documents first
                        List<DocumentSnapshot> listSnapshots = new ArrayList<>();
                        for (DocumentSnapshot doc : listsSnapshot.getDocuments()) {
                            listSnapshots.add(transaction.get(doc.getReference()));
                        }

                        // Write operations
                        if (userSnapshot.exists()) {
                            transaction.update(userRef, "userProfileImageUrl", imagePath);
                        } else {
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("userProfileImageUrl", imagePath);
                            transaction.set(userRef, userData, com.google.firebase.firestore.SetOptions.merge());
                        }

                        for (DocumentReference ref : reviewRefs) {
                            transaction.update(ref, "userProfileImageUrl", imagePath);
                        }

                        for (DocumentSnapshot listSnap : listSnapshots) {
                            List<String> userIDs = (List<String>) listSnap.get("userID");
                            List<String> memberAvatars = (List<String>) listSnap.get("memberAvatarsPreview");
                            if (userIDs != null) {
                                if (memberAvatars == null) {
                                    memberAvatars = new ArrayList<>();
                                } else {
                                    memberAvatars = new ArrayList<>(memberAvatars);
                                }
                                while (memberAvatars.size() < userIDs.size()) {
                                    memberAvatars.add("");
                                }
                                int idx = userIDs.indexOf(userId);
                                if (idx != -1) {
                                    memberAvatars.set(idx, imagePath);
                                    transaction.update(listSnap.getReference(), "memberAvatarsPreview", memberAvatars);
                                }
                            }
                        }

                        return null;
                    }).addOnSuccessListener(result -> {
                        callback.onComplete(true, null);
                    }).addOnFailureListener(e -> {
                        callback.onComplete(null, e);
                    });
                })
                .addOnFailureListener(e -> {
                    callback.onComplete(null, e);
                });
    }

    public void getMovieListsPaginated(String userID, DocumentSnapshot lastVisible, int limit, TaskCompletionCallback<android.util.Pair<List<MovieLists>, DocumentSnapshot>> callback) {
        Query query = firebaseFirestore.collection("movieLists")
                .whereArrayContains("userID", userID)
                .limit(limit);

        final Query finalQuery;
        if (lastVisible != null) {
            finalQuery = query.startAfter(lastVisible);
        } else {
            finalQuery = query;
        }

        // Try Cache first for instant initial render
        finalQuery.get(Source.CACHE)
                .addOnSuccessListener(cacheSnapshot -> {
                    if (cacheSnapshot != null && !cacheSnapshot.isEmpty()) {
                        List<MovieLists> lists = parseQuerySnapshot(cacheSnapshot);
                        DocumentSnapshot nextLastVisible = cacheSnapshot.getDocuments().get(cacheSnapshot.size() - 1);
                        callback.onComplete(new android.util.Pair<>(lists, nextLastVisible), null);
                    }
                    // Fetch from server in the background to ensure it is up to date
                    fetchFromServer(finalQuery, callback);
                })
                .addOnFailureListener(e -> {
                    // If cache fails or is empty, fetch from server directly
                    fetchFromServer(finalQuery, callback);
                });
    }

    private List<MovieLists> parseQuerySnapshot(QuerySnapshot querySnapshot) {
        List<MovieLists> lists = new ArrayList<>();
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            MovieLists list = new MovieLists();
            list.setId(doc.getId());
            list.setName(doc.getString("name"));
            list.setUserID((List<String>) doc.get("userID"));
            list.setCoverPreviews((List<String>) doc.get("coverPreviews"));
            list.setMemberAvatarsPreview((List<String>) doc.get("memberAvatarsPreview"));
            list.setLastUpdated(doc.getString("lastUpdated"));

            Long countVal = doc.getLong("moviesCount");
            list.setMoviesCount(countVal != null ? countVal.intValue() : 0);

            lists.add(list);
        }
        return lists;
    }

    private void fetchFromServer(Query query, TaskCompletionCallback<android.util.Pair<List<MovieLists>, DocumentSnapshot>> callback) {
        query.get(Source.SERVER)
                .addOnSuccessListener(serverSnapshot -> {
                    List<MovieLists> lists = parseQuerySnapshot(serverSnapshot);
                    DocumentSnapshot nextLastVisible = null;
                    if (!serverSnapshot.isEmpty()) {
                        nextLastVisible = serverSnapshot.getDocuments().get(serverSnapshot.size() - 1);
                    }
                    callback.onComplete(new android.util.Pair<>(lists, nextLastVisible), null);
                })
                .addOnFailureListener(e -> callback.onComplete(null, e));
    }
}


