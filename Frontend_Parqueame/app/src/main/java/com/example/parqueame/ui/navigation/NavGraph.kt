@file:Suppress("unused")

package com.example.parqueame.ui.navigation

import androidx.navigation.navDeepLink
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.actividad.ActividadConductorScreen
import com.example.parqueame.ui.admin.AdminParqueosScreen
import com.example.parqueame.ui.admin.ayuda.AyudaAdminScreen
import com.example.parqueame.ui.admin.billetera.BilleteraAdminScreen
import com.example.parqueame.ui.admin.billetera.TransaccionesAdminScreen
import com.example.parqueame.ui.admin.parqueosInfo.ParqueoDetalleScreen
import com.example.parqueame.ui.admin.solicitudParqueo.SolicitudEditarParqueoScreen
import com.example.parqueame.ui.admin.solicitudParqueo.SolicitudParqueoScreen
import com.example.parqueame.ui.admin.solicitudParqueo.VerSolicitudScreen
import com.example.parqueame.ui.ayudaConductor.AyudaConductorScreen
import com.example.parqueame.ui.billetera.AddCardScreen
import com.example.parqueame.ui.billetera.BilleteraScreen
import com.example.parqueame.ui.billetera.BilleteraViewModel
import com.example.parqueame.ui.billetera.CardNicknameStore
import com.example.parqueame.ui.billetera.EditCardScreen
import com.example.parqueame.ui.billetera.UiCard
import com.example.parqueame.ui.cuenta.CuentaCompanyScreen
import com.example.parqueame.ui.cuenta.CuentaConductorScreen
import com.example.parqueame.ui.home.HomeCompanyScreen
import com.example.parqueame.ui.home.HomeConductorScreen
import com.example.parqueame.ui.parqueos.ParkingDetailScreen
import com.example.parqueame.ui.parqueos.ParqueosViewModel
import com.example.parqueame.ui.parqueos.ReservaConfirmacionScreen
import com.example.parqueame.ui.parqueos.ReservaFormScreen
import com.example.parqueame.ui.parqueos.ReservationSummaryUi
import com.example.parqueame.ui.parqueos.RutaReservaScreen
import com.example.parqueame.ui.recover_password.OtpScreen
import com.example.parqueame.ui.recover_password.RecoverPasswordScreen
import com.example.parqueame.ui.recover_password.ResetPasswordScreen
import com.example.parqueame.ui.recover_password.viewmodels.RecoverPasswordViewModel
import com.example.parqueame.ui.register.RegisterChoiceScreen
import com.example.parqueame.ui.register.RegisterCompanyFormScreen
import com.example.parqueame.ui.register.RegisterIndividualFormScreen
import com.example.parqueame.ui.register.viewmodels.RegisterChoiceViewModel
import com.example.parqueame.ui.resumen.ResumenFinalScreen
import com.example.parqueame.ui.home.HomeConductorCalificacionScreen
import com.example.parqueame.ui.splash.SplashRouter
import androidx.compose.runtime.mutableStateListOf
import com.example.parqueame.models.ParkingLotDto
import com.example.parqueame.ui.admin.solicitudParqueo.SolicitudCreadaScreen
import android.content.pm.PackageManager
import android.net.Uri

sealed class Screen(val route: String) {
    object Splash                  : Screen("splash")
    object Login                   : Screen("login")
    object RegisterChoice          : Screen("register_choice")
    object RegisterIndividualForm  : Screen("register_individual_form")
    object RegisterCompanyForm     : Screen("register_company_form")
    object RecoverPassword         : Screen("recover_password")
    object Otp                     : Screen("otp")
    object ResetPassword           : Screen("reset_password")
    object HomeConductor           : Screen("home_conductor")
    object HomeCompany             : Screen("home_company")
    object ActividadConductor      : Screen("actividad")
    object CuentaConductor         : Screen("cuenta")
    object CuentaCompany           : Screen("cuentacompany")
    object Billetera               : Screen("billetera")
    object AdminParqueosScreen     : Screen("admin_parqueos")
    object SolicitudParqueoScreen  : Screen("solicitud_parqueo")
    object SolicitudEditarParqueoScreen : Screen("solicitud_editar_parqueo")
    object AyudaAdminScreen        : Screen("ayuda_admin")
    object SolicitudCreadaScreen   : Screen("solicitud_creada")

    object VerSolicitudParqueoScreen : Screen("ver_solicitud_parqueo/{parkingId}") {
        fun build(parkingId: String) = "ver_solicitud_parqueo/$parkingId"
    }

    object ParkingDetailScreen     : Screen("parking_detail_conductor/{parqueoId}")
    object ParqueoDetalleScreen    : Screen("parqueo_detalle/{parkingId}")
    object BilleteraAdminScreen    : Screen("billetera_admin")
    object TransaccionesAdminScreen: Screen("transacciones_admin")
    object HomeConductorCalificacionScreen :
        Screen("home_calificacion?pid={pid}&pname={pname}&paddr={paddr}&pimg={pimg}&op={op}") {

        fun build(
            pid: String?,
            pname: String?,
            paddr: String?,
            pimg: String?,     // pásale null/"" si aún no tienes imagen
            op: String?
        ): String {
            fun enc(s: String?) = Uri.encode(s ?: "")
            return "home_calificacion" +
                    "?pid=${enc(pid)}" +
                    "&pname=${enc(pname)}" +
                    "&paddr=${enc(paddr)}" +
                    "&pimg=${enc(pimg)}" +
                    "&op=${enc(op)}"
        }
    }

    object EditarParqueo : Screen("admin/editar/{parkingId}") {
        fun build(parkingId: String) = "admin/editar/$parkingId"
    }

    // Deep link cuando el guardia valida el QR en la página HTML del backend
    object ReservaValidadaDeepLink : Screen("reservation/validated?token={token}") {
        const val URI_PATTERN: String = "parqueame://app/reservation/validated?token={token}"
}

    // ✅ Nueva pantalla: Resumen final con rating al final de la ruta
    object ResumenFinal : Screen("resumen_final/{reservationId}/{avgRating}") {
        fun build(reservationId: String, avgRatingText: String) =
            "resumen_final/$reservationId/$avgRatingText"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    val context = LocalContext.current

    val session = remember { SessionStore(context) }
    val userId   = session.userId.collectAsStateWithLifecycle(initialValue = null).value.orEmpty()
    val userDoc  = session.userDoc.collectAsStateWithLifecycle(initialValue = null).value.orEmpty()
    val userTipo = session.userTipo.collectAsStateWithLifecycle(initialValue = null).value.orEmpty()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) { SplashRouter(navController) }

        // ---------- Auth ----------
        composable(Screen.Login.route) {
            com.example.parqueame.ui.login.LoginScreen(
                navController = navController,
                onRegisterClick = {
                    navController.navigate(Screen.RegisterChoice.route) {
                        launchSingleTop = true
                    }
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.RecoverPassword.route)
                }
            )
        }

        // ---------- Registro (elige tipo) ----------
        composable(Screen.RegisterChoice.route) {
            val vm: RegisterChoiceViewModel = viewModel()
            RegisterChoiceScreen(
                viewModel = vm,
                onNavigateToIndividualForm = {
                    navController.navigate(Screen.RegisterIndividualForm.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToCompanyForm = {
                    navController.navigate(Screen.RegisterCompanyForm.route) {
                        launchSingleTop = true
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ---------- Formularios registro ----------
        composable(Screen.RegisterIndividualForm.route) {
            RegisterIndividualFormScreen(
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.RegisterCompanyForm.route) {
            RegisterCompanyFormScreen(
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRegistrationSuccess = {
                    Toast.makeText(
                        context,
                        "¡Registro exitoso! Ya puedes iniciar sesión",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Recuperar contraseña
        composable(Screen.RecoverPassword.route) {
            val vm: RecoverPasswordViewModel = viewModel()
            RecoverPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onSendClick = { email ->
                    vm.solicitarCodigo(
                        email = email,
                        onSuccess = {
                            navController.navigate(Screen.Otp.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            )
        }

        // OTP
        composable(Screen.Otp.route) {
            val vm: RecoverPasswordViewModel = viewModel()
            OtpScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onCodeSubmit = {
                    navController.navigate(Screen.ResetPassword.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // RESET PASSWORD
        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ---------- Homes ----------
        composable(Screen.HomeConductor.route) {
            HomeConductorScreen(
                navController = navController,
                userLat = 18.4710,
                userLng = -69.9360
            )
        }
        composable(Screen.HomeCompany.route) {
            HomeCompanyScreen(
                navController = navController
            )
        }

        composable(
            route = Screen.HomeConductorCalificacionScreen.route,
            arguments = listOf(
                navArgument("pid")   { type = NavType.StringType; defaultValue = "" },
                navArgument("pname") { type = NavType.StringType; defaultValue = "" },
                navArgument("paddr") { type = NavType.StringType; defaultValue = "" },
                navArgument("pimg")  { type = NavType.StringType; defaultValue = "" },
                navArgument("op")    { type = NavType.StringType; defaultValue = "" },
            )
        ) { backStackEntry ->
            val pid   = backStackEntry.arguments?.getString("pid").orEmpty()
            val pname = backStackEntry.arguments?.getString("pname").orEmpty()
            val paddr = backStackEntry.arguments?.getString("paddr").orEmpty()
            val pimg  = backStackEntry.arguments?.getString("pimg").orEmpty()
            val op    = backStackEntry.arguments?.getString("op").orEmpty()

            HomeConductorCalificacionScreen(
                navController = navController,
                // pasa los prefills; si vienen vacíos, el composable usará defaults
                prefillParkingName = pname.ifBlank { null },
                prefillParkingAddress = paddr.ifBlank { null },
                prefillImageUrl = pimg.ifBlank { null },
                prefillOperatorName = op.ifBlank { null }
            )
        }



        // ---------- Billetera / Cuenta / Actividad ----------
        composable(Screen.Billetera.route) {
            if (userId.isBlank()) {
                SmallLoading("Cargando sesión…")
            } else {
                BilleteraScreen(
                    navController = navController,
                    userId = userId
                )
            }
        }
        composable(Screen.CuentaConductor.route) { CuentaConductorScreen(navController) }
        composable(Screen.CuentaCompany.route) { CuentaCompanyScreen(navController) }
        composable(Screen.ActividadConductor.route) {
            ActividadConductorScreen(
                navController = navController,
                currentUserId = userId
            )
        }

        // ---------- Detalle de Parqueo ----------
        composable(
            "parkingDetail/{parqueoId}?distMeters={distMeters}",
            arguments = listOf(
                navArgument("parqueoId") { type = NavType.StringType },
                navArgument("distMeters") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val vm: ParqueosViewModel = viewModel(
                factory = ParqueosViewModel.provideFactory(backStackEntry.savedStateHandle)
            )
            val distMeters = backStackEntry.arguments?.getLong("distMeters") ?: -1L
            val uiState by vm.state.collectAsStateWithLifecycle()

            when (val s = uiState) {
                is com.example.parqueame.ui.parqueos.UiState.Loading -> CircularProgressIndicator()
                is com.example.parqueame.ui.parqueos.UiState.Error -> {
                    Column {
                        Text("Error: ${s.message}")
                        TextButton(onClick = { vm.reload() }) { Text("Reintentar") }
                        TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
                    }
                }
                is com.example.parqueame.ui.parqueos.UiState.Success -> {
                    ParkingDetailScreen(
                        navController = navController,
                        parqueo = s.data,
                        distanceMeters = distMeters,
                        onClose = { navController.popBackStack() },
                        onPrimaryAction = { /* acción principal */ }
                    )
                }
            }
        }

        // ---------- ADMIN / PARQUEOS ----------
        composable(Screen.AdminParqueosScreen.route) {
            if (userId.isBlank()) {
                Toast.makeText(context, "Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            } else {
                AdminParqueosScreen(
                    navController = navController,
                    userId = userId,
                    userDocumento = userDoc.takeIf { it.isNotBlank() },
                    userTipoDocumento = userTipo.takeIf { it.isNotBlank() }
                )
            }
        }

        composable(Screen.SolicitudParqueoScreen.route) {
            if (userId.isBlank() || userDoc.isBlank() || userTipo.isBlank()) {
                Toast.makeText(context, "Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            } else {
                SolicitudParqueoScreen(
                    navController = navController,
                    userId = userId,
                    userDocumento = userDoc,
                    userTipoDocumento = userTipo,
                    onSubmitClick = { navController.navigate(Screen.HomeCompany.route) }
                )
            }
        }

        composable(Screen.SolicitudEditarParqueoScreen.route) {
            if (userId.isBlank() && (userDoc.isBlank() || userTipo.isBlank())) {
                Toast.makeText(context, "Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            } else {
                SolicitudEditarParqueoScreen(
                    navController = navController,
                    onSubmitClick = { navController.navigate(Screen.HomeCompany.route) }
                )
            }
        }

        composable(
            route = Screen.EditarParqueo.route,
            arguments = listOf(navArgument("parkingId") { type = NavType.StringType })
        ) {
            if (userId.isBlank() && (userDoc.isBlank() || userTipo.isBlank())) {
                Toast.makeText(context, "Inicia sesión nuevamente", Toast.LENGTH_SHORT).show()
            } else {
                SolicitudEditarParqueoScreen(
                    navController = navController,
                    onSubmitClick = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.VerSolicitudParqueoScreen.route,
            arguments = listOf(navArgument("parkingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getString("parkingId")
            VerSolicitudScreen(
                navController = navController,
                parkingIdArg = pid
            )
        }

        composable(
            route = Screen.ParqueoDetalleScreen.route,
            arguments = listOf(navArgument("parkingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getString("parkingId") ?: ""
            ParqueoDetalleScreen(
                navController = navController,
                onClose = { navController.popBackStack() },
                parkingId = parkingId
            )
        }

        composable(Screen.BilleteraAdminScreen.route) {
            BilleteraAdminScreen(
                navController = navController,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.TransaccionesAdminScreen.route) {
            TransaccionesAdminScreen(
                navController = navController,
                onClose = { navController.popBackStack() }
            )
        }

        composable(Screen.AyudaAdminScreen.route) {
            AyudaAdminScreen(navController = navController)
        }

        composable(Screen.SolicitudCreadaScreen.route) {
            SolicitudCreadaScreen(navController = navController)
        }

        // ---------- Wallet ----------
        composable("wallet") {
            if (userId.isBlank()) {
                SmallLoading("Cargando billetera…")
            } else {
                BilleteraScreen(
                    navController = navController,
                    userId = userId,
                    onAddCard = { navController.navigate("wallet/addCard") }
                )
            }
        }

        composable("wallet/addCard") {
            AddCardScreen(
                navController = navController,
                onCardAdded = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("cardAdded", true)
                    navController.popBackStack()
                }
            )
        }

        composable("ayuda_conductor") {
            AyudaConductorScreen(navController)
        }

        // ---------- Editar tarjeta ----------
        composable(
            route = "wallet/editCard?brand={brand}&last4={last4}&holder={holder}&expiry={expiry}&nickname={nickname}&pmId={pmId}",
            arguments = listOf(
                navArgument("brand") { type = NavType.StringType; defaultValue = "" },
                navArgument("last4") { type = NavType.StringType; defaultValue = "" },
                navArgument("holder") { type = NavType.StringType; defaultValue = "" },
                navArgument("expiry") { type = NavType.StringType; defaultValue = "" },
                navArgument("nickname") { type = NavType.StringType; defaultValue = "" },
                navArgument("pmId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val vm: BilleteraViewModel = viewModel()
            val brand = backStackEntry.arguments?.getString("brand").orEmpty()
            val last4 = backStackEntry.arguments?.getString("last4").orEmpty()
            val holder = backStackEntry.arguments?.getString("holder").orEmpty()
            val expiry = backStackEntry.arguments?.getString("expiry").orEmpty()
            val nickname = backStackEntry.arguments?.getString("nickname").orEmpty()
            val pmId = backStackEntry.arguments?.getString("pmId").orEmpty()

            val card = UiCard(
                brand = brand,
                last4 = last4,
                holder = holder,
                expiry = expiry,
                nickname = nickname.ifBlank { null },
                pmId = pmId.ifBlank { null }
            )

            EditCardScreen(
                navController = navController,
                card = card,
                onCardUpdated = { updated ->
                    CardNicknameStore.set(context, updated.pmId, updated.nickname)
                    if (userId.isNotBlank()) vm.refreshCards(userId)
                    navController.popBackStack()
                }
            )
        }

        // ---------- RESERVA EN CURSO (QR en la app) ----------
        composable(
            route = "reserva/en_curso/{reservaId}",
            arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val reservaId = backStackEntry.arguments?.getString("reservaId").orEmpty()
            val summary = navController
                .previousBackStackEntry
                ?.savedStateHandle
                ?.get<com.example.parqueame.ui.parqueos.ReservationSummaryUi>("resumen_reserva_en_curso")

            if (summary == null || reservaId.isBlank()) {
                Column {
                    Text("Faltan datos de la reserva.")
                    TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
                }
            } else {
                com.example.parqueame.ui.parqueos.ReservaEnCursoScreen(
                    navController = navController,
                    summary = summary,
                    reservationId = reservaId
                )
            }
        }

        // ---------- RESERVA FORM ----------
        composable(
            route = "reservaForm/{parkingId}",
            arguments = listOf(navArgument("parkingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val parkingId: String = backStackEntry.arguments?.getString("parkingId") ?: ""

            val parqueoGuardado =
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<ParkingLotDto>("selected_parking_full")

            val vm: ParqueosViewModel = viewModel()
            val selected = parqueoGuardado ?: vm.parqueos.firstOrNull { it.id == parkingId }

            if (selected != null) {
                ReservaFormScreen(
                    navController = navController,
                    parqueo = selected,
                    parkingId = parkingId
                )
            } else {
                Text("No se encontró información del parqueo.")
            }
        }

        // ---------- CONFIRMACIÓN DE RESERVA ----------
        composable("reserva/confirmacion") {
            val summary =
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<ReservationSummaryUi>("resumen_reserva")

            if (summary == null) {
                Column {
                    Text("No hay datos de la reserva.")
                    TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
                }
            } else {
                ReservaConfirmacionScreen(
                    navController = navController,
                    summary = summary
                )
            }
        }

        // ---------- RUTA DESPUÉS DE CONFIRMAR ----------
        composable(
            "reserva/ruta/{reservaId}",
            arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val summary =
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<ReservationSummaryUi>("resumen_reserva")

            val reservaId = backStackEntry.arguments?.getString("reservaId") ?: ""

            if (summary == null) {
                Text("No hay datos de la reserva.")
            } else {
                val context = LocalContext.current
                val mapsApiKey = remember {
                    val app = context.applicationContext
                    runCatching {
                        val ai = app.packageManager.getApplicationInfo(
                            app.packageName,
                            PackageManager.GET_META_DATA
                        )
                        ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
                    }.getOrDefault("")
                }

                RutaReservaScreen(
                    navController = navController,
                    summary = summary,
                    reservationIdCreated = reservaId,
                    directionsApiKey = mapsApiKey
                )
            }
        }

        // ✅ NUEVO: Resumen final con rating pasado al final de la ruta
        composable(
            route = Screen.ResumenFinal.route,
            arguments = listOf(
                navArgument("reservationId") { type = NavType.StringType },
                navArgument("avgRating") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId").orEmpty()
            val avgRatingText = backStackEntry.arguments?.getString("avgRating").orEmpty()

            ResumenFinalScreen(
                reservationId = reservationId,
                avgRatingText = avgRatingText
            )
        }

        // Deep link: QR validado desde el backend
        composable(
            route = Screen.ReservaValidadaDeepLink.route,
            arguments = listOf(
                navArgument("token") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = false
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = Screen.ReservaValidadaDeepLink.URI_PATTERN }
            )
        ) {
            Toast.makeText(context, "Reserva validada. ¡Bienvenido!", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }
}

@Composable
private fun SmallLoading(text: String) {
    Column {
        CircularProgressIndicator()
        Text(text)
    }
}