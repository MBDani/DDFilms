package com.merino.ddfilms.api;


import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
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

                firebaseFirestore.collection("movieLists").add(newList).addOnSuccessListener(documentReference -> callback.onComplete(null, null)).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al crear la lista " + listName)));
            } else {
                callback.onComplete(null, new Exception("Error al verificar la existencia de la lista " + listName));
            }
        });
    }


    public void getMovieLists(String userID, TaskCompletionCallback<List<String>> callback) {
        firebaseFirestore.collection("movieLists").whereEqualTo("userID", userID).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> lists = new ArrayList<>();
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                lists.add(document.getString("name"));
            }
            callback.onComplete(lists, null);
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener las listas de películas")));
    }

    public void addMovieToList(String listName, Movie movie, String userID, TaskCompletionCallback<String> callback) {
        firebaseFirestore.collection("movieLists").whereEqualTo("name", listName).whereEqualTo("userID", userID).get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean movieAlreadyExists = isMovieAlreadyExists(movie, queryDocumentSnapshots);
            if (movieAlreadyExists) {
                callback.onComplete(null, new Exception("La película ya se encuentra en la lista: " + listName));
            } else {
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    document.getReference().update("movies", FieldValue.arrayUnion(movie)).addOnSuccessListener(success -> callback.onComplete("Película agregada a la lista " + listName, null)).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al agregar la película a la lista")));
                }
            }
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener la lista de películas")));
    }

    private boolean isMovieAlreadyExists(Movie movie, QuerySnapshot queryDocumentSnapshots) {
        boolean movieAlreadyExists = false;
        for (DocumentSnapshot document : queryDocumentSnapshots) {
            List<Map<String, Object>> movieMaps = (List<Map<String, Object>>) document.get("movies");
            if (movieMaps != null) {
                List<Movie> movieList = parseMovieList(document);
                movieAlreadyExists = movieList.stream().anyMatch(movieMap -> movieMap.getOriginalTitle().equals(movie.getOriginalTitle()));
            }
        }
        return movieAlreadyExists;
    }

    public void loadMovieFromListName(String listName, String userID, TaskCompletionCallback<List<Movie>> callback) {
        firebaseFirestore.collection("movieLists").whereEqualTo("name", listName).whereEqualTo("userID", userID).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                List<Movie> movieList = parseMovieList(document);
                callback.onComplete(movieList, null);
            } else {
                callback.onComplete(null, new Exception("No se han encontrado películas en la lista"));
            }
        }).addOnFailureListener(e -> callback.onComplete(null, new Exception("Error al obtener la lista de películas")));
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


