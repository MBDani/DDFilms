package com.merino.ddfilms.intentFilter;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.ui.MovieListActivity;
import com.merino.ddfilms.utils.TaskCompletionCallback;

public class IntentFilterActivity extends AppCompatActivity {

    FirebaseManager firebaseManager = new FirebaseManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Procesar el Intent que inició esta Activity
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Manejar nuevos intents que lleguen mientras esta Activity está activa
        handleDeepLink(intent);
    }

    /**
     * Método para procesar el Intent recibido
     * @param intent Intent que contiene el deep link
     */
    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            String listID = data.getPathSegments().get(0); // Obtiene el listID desde la URL
            String listName = data.getQueryParameter("listName"); // Obtiene el listName

            if (listID != null && listName != null) {
                addListToUser(listID, (result, error) -> {
                    if (error != null) {
                        showMessage(getApplicationContext(), error.getMessage());
                    } else {
                        showMessage(getApplicationContext(), "Se ha agregado la lista al usuario");
                        navigateToMovieListActivity(listID, listName);
                    }
                });
            } else {
                // Parámetros faltantes
                Toast.makeText(this, "Datos incompletos", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // El Intent no tiene un URI asociado
            Toast.makeText(this, "Enlace no válido", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void addListToUser(String listID,TaskCompletionCallback<Boolean> callback) {
       firebaseManager.addListToUser(listID, callback);
    }

    private void navigateToMovieListActivity(String listID, String listName) {
        Intent intent = new Intent(this, MovieListActivity.class);
        intent.putExtra("listID", listID);
        intent.putExtra("listName", listName);
        startActivity(intent);
        finish();
    }
}
