package com.merino.ddfilms.api;


import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.ui.auth.LoginActivity;
import com.merino.ddfilms.utils.TaskCompletionCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public String getCurrentUser() {
        return Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
    }

    public void getUserName(TaskCompletionCallback<String> callback) {
        String uid = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        firebaseFirestore.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                callback.onComplete(documentSnapshot.getString("user"), null);
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
        // Verificar si ya existe una lista con el mismo nombre
        firebaseFirestore.collection("movieLists").whereEqualTo("name", listName).whereEqualTo("userID", userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                // La lista ya existe
                callback.onComplete(null, new Exception("La lista " + listName + " ya existe."));
            } else if (task.isSuccessful()) {
                // Crear una nueva lista
                Map<String, Object> newList = new HashMap<>();
                newList.put("name", listName);
                newList.put("userID", userID);
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

    public void getMovieLists(String userID, TaskCompletionCallback<HashMap<String, String>> callback) {
        // Recuperamos la lista de películas que tiene el usuario
        firebaseFirestore.collection("users").document(userID).get().addOnSuccessListener(documentSnapshot -> {
            List<String> movieListIds = (List<String>) documentSnapshot.get("movieLists");
            if (movieListIds != null) {
                // Recuperamos los nombres de las listas de películas
                movieListIds.forEach(id -> getMovieListByID(id, (document, error) -> {
                    if (error != null) {
                        callback.onComplete(null, error);
                    } else {
                        String listName = document.getString("name");
                        if (listName != null) {
                            HashMap<String, String> listNameMap = new HashMap<>();
                            listNameMap.put(id, listName);
                            callback.onComplete(listNameMap, null);
                        }
                    }
                }));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener las listas de películas")));
    }

    public void addMovieToList(String listID, Movie movie, TaskCompletionCallback<String> callback) {
        getMovieListByID(listID, (documentSnapshot, error) -> {
            if (error != null) {
                callback.onComplete(null, error);
            } else {
                String listName = documentSnapshot.getString("name");
                boolean movieAlreadyExists = isMovieAlreadyExists(movie, documentSnapshot);
                if (movieAlreadyExists) {
                    callback.onComplete(null, new Exception("La película ya se encuentra en la lista: " + listName));
                } else {
                    documentSnapshot.getReference().update("movies", FieldValue.arrayUnion(movie)).addOnSuccessListener(success -> callback.onComplete("Película agregada a la lista " + listName, null)).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al agregar la película a la lista")));
                }
            }
        });
    }

    public void loadMovieFromListName(String listID, TaskCompletionCallback<List<Movie>> callback) {
        firebaseFirestore.collection("movieLists").document(listID).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot != null) {
                List<Movie> movieList = parseMovieList(documentSnapshot);
                callback.onComplete(movieList, null);
            } else {
                callback.onComplete(null, new Exception("No se han encontrado películas en la lista"));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener la lista de películas")));
    }

    private void getMovieListByID(String listID, TaskCompletionCallback<DocumentSnapshot> callback) {
        firebaseFirestore.collection("movieLists").document(listID).get().addOnSuccessListener(documentSnapshot -> {
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

        // Parseamos los objetos Movie de la lista
        for (Map<String, Object> movieMap : movieMaps) {
            Movie m = Movie.mapToMovie(movieMap);
            movieList.add(m);
        }
        return movieList;
    }
}


