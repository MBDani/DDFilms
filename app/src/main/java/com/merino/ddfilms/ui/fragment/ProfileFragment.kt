package com.merino.ddfilms.ui.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.merino.ddfilms.DDFilmsApplication
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.ui.MainActivity
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.StringUtils.DIARY_LIST
import com.merino.ddfilms.utils.StringUtils.WATCH_LIST

class ProfileFragment : Fragment() {

    private val firebaseManager = FirebaseManager.getInstance()
    private lateinit var preferences: SharedPreferences

    private val currentProfileImageUrlState = mutableStateOf<String?>(null)
    private val profileNameState = mutableStateOf("Cargando...")
    private val profileEmailState = mutableStateOf("")
    private val themeModeState = mutableStateOf("system")
    private val notificationsEnabledState = mutableStateOf(false)

    private val watchlistCountState = mutableIntStateOf(0)
    private val diaryCountState = mutableIntStateOf(0)
    private val reviewsCountState = mutableIntStateOf(0)

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)
        themeModeState.value = preferences.getString("theme_mode", "system") ?: "system"
        notificationsEnabledState.value = preferences.getBoolean("notifications_enabled", false)

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            saveNotificationsEnabled(isGranted)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadUserData()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CinematicTheme {
                    ProfileScreen(
                        profileName = profileNameState.value,
                        profileEmail = profileEmailState.value,
                        profileImageUrl = currentProfileImageUrlState.value,
                        themeMode = themeModeState.value,
                        notificationsEnabled = notificationsEnabledState.value,
                        watchlistCount = watchlistCountState.intValue,
                        diaryCount = diaryCountState.intValue,
                        reviewsCount = reviewsCountState.intValue,
                        onEditAvatarClick = {
                            val dialog = ProfilePicturePickerDialog.newInstance(currentProfileImageUrlState.value)
                            dialog.setOnProfileImageUpdatedListener { newImagePath ->
                                currentProfileImageUrlState.value = newImagePath
                            }
                            dialog.show(parentFragmentManager, "ProfilePicturePickerDialog")
                        },
                        onThemeChange = { mode ->
                            themeModeState.value = mode
                            preferences.edit().putString("theme_mode", mode).apply()
                            DDFilmsApplication.applyTheme(mode)
                        },
                        onNotificationsChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        saveNotificationsEnabled(true)
                                    }
                                } else {
                                    saveNotificationsEnabled(true)
                                }
                            } else {
                                saveNotificationsEnabled(false)
                            }
                        },
                        onWatchlistClick = {
                            (activity as? MainActivity)?.loadFragment(WatchlistFragment(), addToBackStack = true)
                        },
                        onDiaryClick = {
                            (activity as? MainActivity)?.loadFragment(DiaryFragment(), addToBackStack = true)
                        },
                        onReviewsClick = {
                            (activity as? MainActivity)?.loadFragment(ReviewsFragment(), addToBackStack = true)
                        },
                        onLogoutClick = {
                            (activity as? MainActivity)?.showLogoutConfirmationDialog()
                        }
                    )
                }
            }
        }
    }

    private fun loadUserData() {
        val uid = firebaseManager.getCurrentUserUID() ?: return

        firebaseManager.getUserName(uid) { userName, _ ->
            if (userName != null) {
                profileNameState.value = userName
            }
        }

        firebaseManager.getUserMail(uid) { userEmail, _ ->
            if (userEmail != null) {
                profileEmailState.value = userEmail
            }
        }

        firebaseManager.getUserProfileImageUrl(uid) { url, _ ->
            if (url != null) {
                currentProfileImageUrlState.value = url
            }
        }

        firebaseManager.getMoviesFromList(uid, WATCH_LIST) { movies, _ ->
            watchlistCountState.intValue = movies?.size ?: 0
        }

        firebaseManager.getMoviesFromList(uid, DIARY_LIST) { movies, _ ->
            diaryCountState.intValue = movies?.size ?: 0
        }

        firebaseManager.getAllReviews { reviews, _ ->
            reviewsCountState.intValue = reviews?.filter { it.userId == uid }?.size ?: 0
        }
    }

    private fun saveNotificationsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean("notifications_enabled", enabled)
            .putBoolean("notifications_prompted", true)
            .apply()
        notificationsEnabledState.value = enabled
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileName: String,
    profileEmail: String,
    profileImageUrl: String?,
    themeMode: String,
    notificationsEnabled: Boolean,
    watchlistCount: Int,
    diaryCount: Int,
    reviewsCount: Int,
    onEditAvatarClick: () -> Unit,
    onThemeChange: (String) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onWatchlistClick: () -> Unit,
    onDiaryClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Avatar circular
        Box(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp)
                .size(110.dp)
        ) {
            AsyncImage(
                model = profileImageUrl ?: R.drawable.ic_default_profile,
                contentDescription = "Avatar de Perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )

            IconButton(
                onClick = onEditAvatarClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = "Editar avatar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Nombre y Email
        Text(
            text = profileName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = profileEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Tarjetas de Estadísticas (Fidelidad Letterboxd)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatCard(
                title = "Watchlist",
                count = watchlistCount,
                iconRes = R.drawable.ic_watchlist,
                onClick = onWatchlistClick,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Diario",
                count = diaryCount,
                iconRes = R.drawable.ic_diary,
                onClick = onDiaryClick,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Reseñas",
                count = reviewsCount,
                iconRes = R.drawable.ic_review,
                onClick = onReviewsClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tarjeta de Ajustes
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Tema selector
                Text(
                    text = "Apariencia de la aplicación",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modes = listOf("light" to "Claro", "dark" to "Oscuro", "system" to "Sistema")
                    modes.forEach { (mode, label) ->
                        val selected = themeMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onThemeChange(mode) },
                            label = { Text(label) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_done),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray.copy(alpha = 0.2f))

                // Notificaciones switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notificaciones",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Recibir alertas sobre listas compartidas o novedades",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsChange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón Cerrar Sesión
        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_sign_out),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Cerrar sesión", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
