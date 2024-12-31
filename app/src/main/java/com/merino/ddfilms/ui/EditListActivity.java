package com.merino.ddfilms.ui;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.merino.ddfilms.R;
import com.merino.ddfilms.api.FirebaseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EditListActivity extends AppCompatActivity {
    private EditText listNameInput;
    private TextView usersCountText;
    private String listID;
    private String currentListName;

    private HashMap<String, String> mapUsers;

    FirebaseManager firebaseManager = new FirebaseManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_list);

        // Inicializar vistas
        listNameInput = findViewById(R.id.list_name_input);
        usersCountText = findViewById(R.id.users_count_text);

        // Obtener datos
        listID = getIntent().getStringExtra("listID");
        currentListName = getIntent().getStringExtra("listName");

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Editar lista");

        // Cargar datos actuales
        listNameInput.setText(currentListName);
        loadListUsers();
    }

    private void loadListUsers() {
        firebaseManager.getListUsersIDsAndNames(listID, (mapUsersResult, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else if (mapUsersResult != null) {
                mapUsers = mapUsersResult;
                List<String> userNames = new ArrayList<>(mapUsersResult.values());
                showUserInList(userNames);
            }
        });
    }

    private void showUserInList(List<String> users) {
        StringBuilder usersText = new StringBuilder();
        for (String user : users) {
            usersText.append(user).append("\n");
        }
        usersCountText.setText(usersText.toString());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        String newName = listNameInput.getText().toString().trim();
        if (!newName.equals(currentListName)) {
            saveListName(newName);
        } else {
            super.onBackPressed();
        }
    }

    private void saveListName(String newName) {
        firebaseManager.updateListName(listID, newName, (result, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else if (result != null && result) {
                showMessage(getApplicationContext(), "Nombre de la lista actualizado con éxito");
                finish();
                // TODO ver que hacemos
            }
        });
    }
}
