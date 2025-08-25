package com.merino.ddfilms.ui;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.merino.ddfilms.R;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.ui.components.Fab.ActivityFabController;
import com.merino.ddfilms.ui.components.Fab.FabHost;
import com.merino.ddfilms.ui.components.Fab.ShowsFab;
import com.merino.ddfilms.ui.fragment.DiaryFragment;
import com.merino.ddfilms.ui.fragment.ListsFragment;
import com.merino.ddfilms.ui.fragment.PopularFragment;
import com.merino.ddfilms.ui.fragment.ProfileFragment;
import com.merino.ddfilms.ui.fragment.ReviewsFragment;
import com.merino.ddfilms.ui.fragment.SearchFragment;
import com.merino.ddfilms.ui.fragment.SettingsFragment;
import com.merino.ddfilms.ui.fragment.WatchlistFragment;

import java.util.Objects;


public class MainActivity extends AppCompatActivity implements ActivityFabController {

    private DrawerLayout drawerLayout;
    private FirebaseManager firebaseManager = new FirebaseManager();
    private FloatingActionButton activityFab;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("DDFilms");

        // Configurar el DrawerLayout con el NavigationView
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.primary_light, null));
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.navigation_view);

        personalizedNavHeader(navigationView);


        navigationView.setNavigationItemSelectedListener(item -> {
            // Manejar la navegación al seleccionar una opción
            switch (item.getItemId()) {
                case R.id.nav_popular:
                    loadFragment(new PopularFragment());
                    break;
                case R.id.nav_search:
                    loadFragment(new SearchFragment());
                    break;
                case R.id.nav_profile:
                    loadFragment(new ProfileFragment());
                    break;
                case R.id.nav_watchlist:
                    loadFragment(new WatchlistFragment());
                    break;
                case R.id.nav_lists:
                    loadFragment(new ListsFragment());
                    break;
                case R.id.nav_diary:
                    loadFragment(new DiaryFragment());
                    break;
                case R.id.nav_reviews:
                    loadFragment(new ReviewsFragment());
                    break;
                case R.id.nav_settings:
                    loadFragment(new SettingsFragment());
                    break;
                case R.id.nav_sign_out:
                    showLogoutConfirmationDialog();
                    break;
                default:
                    return super.onOptionsItemSelected(item);
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // FAB en Activity (overlay)
        activityFab = findViewById(R.id.activity_fab_add);
        activityFab.setOnClickListener(v -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (current instanceof FabHost) {
                ((FabHost) current).onFabClicked();
            }
        });

        // Registrar callback para actualizaciones automáticas cuando cambian fragments
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentResumed(fm, f);
                        updateFabVisibility(f);
                    }
                }, false
        );

        // Cargar fragment inicial
        if (savedInstanceState == null) {
            loadFragment(new SearchFragment());
        }
    }

    private void personalizedNavHeader(NavigationView navigationView) {
        // Obtén la vista del header
        View headerView = navigationView.getHeaderView(0);

        // Referencias a los elementos del header
        ImageView profileImage = headerView.findViewById(R.id.profile_image);
        TextView profileName = headerView.findViewById(R.id.profile_name);
        TextView profileMail = headerView.findViewById(R.id.profile_email);

        // Recuperamos el nombre del usuario y el email
        String uid = firebaseManager.getCurrentUserUID();
        firebaseManager.getUserName(uid, (userName, error) -> {
            if (error != null) {
                Log.e("FirebaseManager", "Error: " + error.getMessage());
                return;
            }
            profileName.setText(userName);
        });
        firebaseManager.getUserMail(uid, (userEmail, error) -> {
            if (error != null) {
                Log.e("FirebaseManager", "Error: " + error.getMessage());
                return;
            }
            profileMail.setText(userEmail);
        });

        // Todo recuperar imagen del usuario
        String profileImageUrl = null; // URL de la imagen o null si no hay

        // Carga la imagen con Glide o Picasso
        if (profileImageUrl != null) {
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.ic_default_profile) // Imagen genérica
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.ic_default_profile); // Imagen genérica
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();

        // Ejecutamos transacciones pendientes para asegurarnos del fragment actual
        getSupportFragmentManager().executePendingTransactions();

        // Actualizamos visibilidad inmediatamente
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (current != null) updateFabVisibility(current);
    }

    private void updateFabVisibility(Fragment fragment) {
        if (fragment instanceof ShowsFab) {
            showFab();
        } else {
            hideFab();
        }
    }

    @Override
    public void showFab() {
        if (activityFab != null && activityFab.getVisibility() != View.VISIBLE) {
            activityFab.show();
            activityFab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideFab() {
        if (activityFab != null && activityFab.getVisibility() == View.VISIBLE) {
            activityFab.hide();
            activityFab.setVisibility(View.GONE);
        }
    }

    public void setFabVisibility(boolean visible) {
        if (visible) showFab(); else hideFab();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this).setTitle("Cerrar sesión").setMessage("¿Estás seguro de que quieres cerrar sesión?").setPositiveButton("Sí", (dialog, which) -> {
            FirebaseManager.getInstance().logoutUser(this);
        }).setNegativeButton("No", (dialog, which) -> {
            // Simplemente cierra el diálogo
            dialog.dismiss();
        }).show();
    }

    @Override
    public void onBackPressed() {
        // Cerrar el menú lateral si está abierto
        if (drawerLayout.isDrawerOpen(findViewById(R.id.navigation_view))) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    public void setToolbarTitle(String title) {
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
    }
}