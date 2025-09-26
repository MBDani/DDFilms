package com.merino.ddfilms.api;

import static com.merino.ddfilms.utils.StringUtils.MOVIE_LIST;
import static com.merino.ddfilms.utils.StringUtils.WATCH_LIST;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.Review;
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
        firebaseFirestore.collection("movieLists").whereEqualTo("name", listName).whereEqualTo("userID", userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                // La lista ya existe
                callback.onComplete(null, new Exception("La lista " + listName + " ya existe."));
            } else if (task.isSuccessful()) {
                // Crear una nueva lista
                Map<String, Object> newList = new HashMap<>();
                newList.put("name", listName);
                newList.put("userID", new ArrayList<>(Collections.singletonList(userID)));
                newList.put("movies", new ArrayList<>());

                firebaseFirestore.collection("movieLists").add(newList).addOnSuccessListener(documentReference -> {
                    // Actualizamos para el userID su lista de películas con el nuevo documento
                    firebaseFirestore.collection("users").document(userID).update("movieLists", FieldValue.arrayUnion(documentReference.getId())).addOnSuccessListener(success -> callback.onComplete("Lista creada con éxito", null));
                }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al crear la lista " + listName)));
            } else {
                callback.onComplete(null, new Exception("Error al verificar la existencia de la lista " + listName));
            }
        });
    }

    public void getMovieListsNameAndID(String userID, TaskCompletionCallback<HashMap<String, String>> callback) {
        // Recuperamos la lista de películas que tiene el usuario
        firebaseFirestore.collection("users").document(userID).get().addOnSuccessListener(documentSnapshot -> {
            List<String> movieListIds = (List<String>) documentSnapshot.get("movieLists");

            if (movieListIds == null || movieListIds.isEmpty()) {
                callback.onComplete(null, new Exception("No tienes ninguna lista"));
                return;
            }

            // Recuperamos los nombres de las listas de películas
            HashMap<String, String> listNameMap = new HashMap<>();
            movieListIds.forEach(id -> getMovieListByID(id, "movieLists", (document, error) -> {
                if (error != null) {
                    callback.onComplete(null, error);
                } else {
                    String listName = document.getString("name");
                    if (listName != null) {
                        listNameMap.put(id, listName);

                        if (listNameMap.size() == movieListIds.size())
                            callback.onComplete(listNameMap, null);
                    }
                }
            }));
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener las películas del usuario")));
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
        // Setteamos la fecha de creación
        Date date = new Date();
        String formattedDate = Utils.formatDate(date);
        movie.setCreatedAt(formattedDate);

        // Obtenemos el nombre del usuario y guardamos en la base de datos
        getUserName(userID, (userName, errorGetUser) -> {
            if (errorGetUser != null) {
                callback.onComplete(null, errorGetUser);
            } else {
                movie.setAddedBy(userName);
                documentSnapshot.getReference().update("movies", FieldValue.arrayUnion(movie))
                        .addOnSuccessListener(success -> callback.onComplete(successMessage, null))
                        .addOnFailureListener(e -> callback.onComplete(null, new Exception(errorMessage)));
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
                callback.onComplete(null, new Exception("No se encontró la lista de películas"));
            }
            List<Map<String, Object>> updatedMovies = movies.stream()
                    .filter(m -> {
                        String movieId = String.valueOf(m.get("id"));
                        return !movieId.equals(String.valueOf(movie.getId()));
                    })
                    .collect(Collectors.toList());

            transaction.update(docRef, "movies", updatedMovies);
            return null;
        }).addOnSuccessListener(result -> {
            callback.onComplete(true, null);
        }).addOnFailureListener(e -> {
            callback.onComplete(null, new Exception("Error al eliminar la película de la lista: " + e.getMessage()));
        });
    }

    public void addListToUser(String listID, TaskCompletionCallback<Boolean> callback) {
        // Añadimos la lista al usuario campo movieLists
        firebaseFirestore.collection("users").document(getCurrentUserUID())
                .update("movieLists", FieldValue.arrayUnion(listID))
                .addOnSuccessListener(success -> {
                    // Añadimos en la colección movieLists por el id el usuario
                    firebaseFirestore.collection("movieLists").document(listID)
                            .update("userID", FieldValue.arrayUnion(getCurrentUserUID()))
                            .addOnSuccessListener(success2 -> callback.onComplete(true, null))
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
}


