package com.merino.ddfilms.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.merino.ddfilms.R;
import com.merino.ddfilms.ui.fragment.SearchFragment;

import java.util.Objects;

public class SearchActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        toolbar = findViewById(R.id.toolbar);
        setupToolbar();
        if (savedInstanceState == null) {
            Bundle bundle = getIntent().getExtras();
            SearchFragment searchFragment = new SearchFragment();
            searchFragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, searchFragment)
                    .commit();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        String listName = getIntent().getStringExtra("listName");
        getSupportActionBar().setTitle("Añadiendo películas a " + listName);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
