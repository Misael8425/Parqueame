package com.example.parqueame.ui.cuenta

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.data.PreferencesManager
import com.example.parqueame.models.ActualizarPerfilRequest
import com.example.parqueame.models.UsuarioPerfil
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.common_components.BottomNavItem
import com.example.parqueame.ui.common_components.BottomNavBar
import com.example.parqueame.ui.common_components.CuentaOpcionCard
import com.example.parqueame.ui.common_components.PerfilOptionsHandler
// import com.example.parqueame.ui.common_components.LanguageOptionsSheet // ← ya no se usa
import com.example.parqueame.ui.LanguageManager
import com.example.parqueame.ui.LocalAppLanguage
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner
import com.example.parqueame.ui.navigation.BottomNavDestination
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import com.example.parqueame.ui.theme.dmSans
import com.example.parqueame.utils.subirImagenACloudinary
import kotlinx.coroutines.launch

private val CUENTA_ROUTE: String = Screen.CuentaCompany.route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentaCompanyScreen(
    navController: NavController
) {

    val context = LocalContext.current
    val showError = rememberTopErrorBanner()
    val showMessage = rememberTopSuccessBanner()

    val preferencesManager = remember { PreferencesManager(navController.context) }
    val sessionStore = remember { SessionStore(navController.context) }

    val apiService = remember { RetrofitInstance.apiService }
    var perfil by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val navItems = BottomNavDestination.values().toList()
    var currentRoute by remember { mutableStateOf(CUENTA_ROUTE) }

    var showSheet by remember { mutableStateOf(false) }       // hoja de opciones de perfil
    var showLangSheet by remember { mutableStateOf(false) }   // hoja de opciones de idioma
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val correo = preferencesManager.getCorreo()
            if (correo.isNotBlank()) {
                val response = apiService.obtenerPerfil(correo)
                if (response.isSuccessful) {
                    perfil = response.body()
                } else {
                    errorMessage = "Error al cargar perfil (${response.code()})"
                }
            } else {
                errorMessage = "No se encontró el correo guardado"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Error: ${e.localizedMessage}"
        }
    }

    errorMessage?.let { msg ->
        Toast.makeText(navController.context, msg, Toast.LENGTH_LONG).show()
        errorMessage = null
    }

    // Handler de opciones de perfil (SIN cambios de lógica)
    PerfilOptionsHandler(
        showSheet = showSheet,
        onDismiss = { showSheet = false },
        navController = navController,
        onImageChange = { uri: Uri? ->
            scope.launch {
                val correo = preferencesManager.getCorreo()
                val imageUrl = if (uri != null) subirImagenACloudinary(uri, navController.context) else null

                if (correo.isNotBlank()) {
                    val response = apiService.actualizarPerfil(
                        ActualizarPerfilRequest(
                            correo = correo,
                            nuevaFotoUrl = imageUrl
                        )
                    )
                    if (response.isSuccessful) {
                        perfil = perfil?.copy(fotoUrl = imageUrl)
                        val msg = if (imageUrl == null)
                            context.getString(R.string.profile_photo_removed)
                        else
                            context.getString(R.string.profile_photo_updated)
                        showMessage(msg)
                    } else {
                        showError(context.getString(R.string.profile_photo_update_error))
                    }
                }
            }
        },
        onEditName = { nuevoNombre ->
            scope.launch {
                val correo = preferencesManager.getCorreo()
                if (correo.isNotBlank()) {
                    val response = apiService.actualizarPerfil(
                        ActualizarPerfilRequest(
                            correo = correo,
                            nuevoNombre = nuevoNombre
                        )
                    )
                    if (response.isSuccessful) {
                        perfil = perfil?.copy(nombre = nuevoNombre)
                        showMessage(context.getString(R.string.profile_name_updated))
                    } else {
                        showError(context.getString(R.string.profile_name_update_error))
                    }
                }
            }
        },
        currentName = perfil?.nombre ?: ""
    )

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BottomNavBar(
                items = navItems.map { BottomNavItem(it.route, it.icon, it.label) },
                currentRoute = currentRoute,
                onItemClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .background(Color.White)
        ) {

            // ► Nuevo “marco” de diseño (solo UI): más aire y jerarquía visual
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp) // antes 16dp
            ) {

                // Encabezado con degradado y peso visual similar al ejemplo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.account_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = RtlRomman,
                            fontWeight = FontWeight.Medium // de Bold → Medium para look más fino
                        ),
                        modifier = Modifier
                            .graphicsLayer(alpha = 0.99f)
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                            },
                        color = Color.Unspecified
                    )
                    IconButton(onClick = { showSheet = true }) {
                        Icon(
                            painter = painterResource(R.drawable.tres_puntos_icon),
                            contentDescription = stringResource(R.string.more_options),
                            modifier = Modifier.size(24.dp),
                            tint = Color.Gray
                        )
                    }
                }

                // Bloque de perfil con avatar grande y placeholder estilizado
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!perfil?.fotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = perfil?.fotoUrl,
                            contentDescription = stringResource(R.string.profile_photo_cd),
                            modifier = Modifier
                                .size(125.dp) // antes 72dp
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(125.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.cuenta_icon),
                                contentDescription = stringResource(R.string.profile_photo_cd),
                                modifier = Modifier.size(60.dp),
                                tint = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = perfil?.nombre ?: stringResource(R.string.loading_ellipsis),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp, // antes 20sp
                            modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
                        )
                        Text(
                            text = perfil?.rol ?: stringResource(R.string.company_role_fallback),
                            fontSize = 16.sp, // ligera subida
                            modifier = Modifier
                                .graphicsLayer(alpha = 0.99f)
                                .padding(start = 10.dp)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                                },
                            color = Color.Unspecified
                        )
                    }
                }

                // Grid de opciones con más aire entre tarjetas
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CuentaOpcionCard(
                            label = stringResource(R.string.wallet),
                            iconRes = R.drawable.billetera_icon,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.BilleteraAdminScreen.route) }
                        )
                        CuentaOpcionCard(
                            label = stringResource(R.string.help),
                            iconRes = R.drawable.ayuda_icon,
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.AyudaAdminScreen.route) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentLanguage = LocalAppLanguage.current
                        val langShort = if (currentLanguage.startsWith("en")) "EN" else "ES"

                        CuentaOpcionCard(
                            label = stringResource(R.string.language),
                            iconRes = R.drawable.idioma_icon,
                            extraText = langShort,
                            modifier = Modifier.weight(1f),
                            onClick = { showLangSheet = true }
                        )
                        CuentaOpcionCard(
                            label = stringResource(R.string.logout),
                            iconRes = R.drawable.cerrar_icon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    sessionStore.clear()
                                    preferencesManager.cerrarSesion()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.CuentaCompany.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ===== Selector de idioma con el mismo diseño que Conductor (ModalBottomSheet) =====
        if (showLangSheet) {
            ModalBottomSheet(onDismissRequest = { showLangSheet = false }) {
                val currentLanguage = LocalAppLanguage.current

                Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.language_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    LanguageRow(
                        label = stringResource(R.string.language_spanish),
                        selected = currentLanguage.lowercase().startsWith("es")
                    ) {
                        LanguageManager.setAppLanguageWithoutRestart(navController.context, "es")
                        showLangSheet = false
                        navController.navigate(Screen.CuentaCompany.route) {
                            popUpTo(Screen.CuentaCompany.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }

                    LanguageRow(
                        label = stringResource(R.string.language_english),
                        selected = currentLanguage.lowercase().startsWith("en")
                    ) {
                        LanguageManager.setAppLanguageWithoutRestart(navController.context, "en")
                        showLangSheet = false
                        navController.navigate(Screen.CuentaCompany.route) {
                            popUpTo(Screen.CuentaCompany.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color(0x143366FF) else Color.Transparent
    val fg = if (selected) Color(0xFF3366FF) else MaterialTheme.colorScheme.onSurface
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = fg)
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = fg
                )
            }
        }
    }
}

@Preview
@Composable
fun CuentaCompanyScreenPreview() {
    CuentaCompanyScreen(
        navController = rememberNavController()
    )
}