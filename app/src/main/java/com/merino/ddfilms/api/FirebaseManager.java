package com.merino.ddfilms.api;


import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.merino.ddfilms.ui.auth.LoginActivity;
import com.merino.ddfilms.utils.TaskCompletionCallback;

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

    public void getUserName(TaskCompletionCallback<String> callback) {
        String uid = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onComplete(documentSnapshot.getString("user"), null);
                    } else {
                        callback.onComplete(null, new Exception("Documento no encontrado"));
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(null, e));
    }

    public void getTmdbApiKey(TaskCompletionCallback<String> callback) {
        firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onComplete(firebaseRemoteConfig.getString("TMDB_API_KEY"), null);
                    } else {
                        callback.onComplete(null, new Exception("TMDB_API_KEY_KEY no encontrado"));
                    }
                });
    }

}
