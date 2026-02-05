// BilleteraScreen.kt
package com.example.parqueame.ui.billetera

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.GradientIcon
import com.example.parqueame.ui.theme.DmSans
import com.stripe.android.customersheet.*
import com.example.parqueame.ui.common_components.rememberTopErrorBanner
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BilleteraScreen(
    navController: NavController,
    userId: String? = null,
    onEditCard: (Int) -> Unit = {},   // compat
    onAddCard: () -> Unit = {}        // compat
) {
    val context = LocalContext.current
    val vm: BilleteraViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val showSuccess = rememberTopSuccessBanner()
    val showError = rememberTopErrorBanner()

    val xUserId = userId.orEmpty()

    // ——— CARGA INICIAL ———
    LaunchedEffect(xUserId) {
        if (xUserId.isNotBlank()) {
            vm.bootstrap(xUserId)
            vm.refreshCards(xUserId)
        }
    }

    // ——— REFRESH al volver de AddCardScreen ———
    val cardAddedFlow = remember(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.getStateFlow("cardAdded", false)
    }
    val cardAdded by (cardAddedFlow?.collectAsState() ?: remember { mutableStateOf(false) })

    LaunchedEffect(cardAdded) {
        if (cardAdded) {
            // 1) Persistir alias local si AddCardScreen lo envió
            val newPmId = navController.previousBackStackEntry?.savedStateHandle?.get<String>("newPmId")
            val newNickname = navController.previousBackStackEntry?.savedStateHandle?.get<String>("newNickname")
            if (!newPmId.isNullOrBlank()) {
                CardNicknameStore.set(context, newPmId, newNickname)
            }

            // 2) Refrescar tarjetas del backend (si aplica)
            val xUserId = userId.orEmpty()
            if (xUserId.isNotBlank()) vm.refreshCards(xUserId)

            // 3) Limpiar flags
            navController.currentBackStackEntry?.savedStateHandle?.set("cardAdded", false)
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("newPmId")
            navController.previousBackStackEntry?.savedStateHandle?.remove<String>("newNickname")
        }
    }

    // Navegación a añadir tarjeta
    val openAddCardForm: () -> Unit = {
        navController.navigate("wallet/addCard")
    }

    // Navegación a editar tarjeta
    fun openEditCardForm(card: UiCard) {
        val route = buildString {
            append("wallet/editCard?")
            append("brand=${Uri.encode(card.brand)}")
            append("&last4=${Uri.encode(card.last4)}")
            append("&holder=${Uri.encode(card.holder)}")
            append("&expiry=${Uri.encode(card.expiry)}")
            append("&nickname=${Uri.encode(card.nickname ?: "")}")
            append("&pmId=${Uri.encode(card.pmId ?: "")}")
        }
        navController.navigate(route)
    }

    // ——— NICKNAMES locales (SharedPreferences) ———
    val cardsWithNicknames by remember(state.cards) {
        derivedStateOf {
            val ctx = context
            state.cards.map { c ->
                c.copy(
                    nickname =
                        CardNicknameStore.get(ctx, c.pmId)
                            ?: CardNicknameStore.getByBrandLast4(ctx, c.brand, c.last4)
                )
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        when {
            state.isLoading -> LoaderUI()
            state.errorMessage != null -> {
                ErrorUI(
                    error = state.errorMessage!!,
                    onRetry = {
                        if (xUserId.isNotBlank()) {
                            vm.bootstrap(xUserId)
                            vm.refreshCards(xUserId)
                        }
                    },
                    navController = navController
                )
            }
            else -> {
                MainUI(
                    navController = navController,
                    cards = cardsWithNicknames,
                    onAddNew = openAddCardForm,
                    onDeleteAt = { index, closeSheets ->
                        val ui = cardsWithNicknames.getOrNull(index) ?: return@MainUI
                        val pmId = ui.pmId ?: return@MainUI
                        if (xUserId.isBlank()) return@MainUI

                        vm.detachPaymentMethod(pmId, xUserId) { ok ->
                            if (ok) {
                                closeSheets() // cerrar ok
                                showSuccess(context.getString(R.string.delete_payment_method_success))
                            } else {
                                showError(context.getString(R.string.delete_payment_method_error))
                            }
                        }
                    },
                    onEditAt = { index ->
                        val ui = cardsWithNicknames.getOrNull(index) ?: return@MainUI
                        openEditCardForm(ui)
                    }
                )
            }
        }
    }
}

/* ===================== UI ===================== */

@Composable
private fun LoaderUI(message: String = stringResource(R.string.loading_wallet)) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
private fun ErrorUI(error: String, onRetry: () -> Unit, navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.loading_error_title), fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(error, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry_action)) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { navController.popBackStack() }) { Text(stringResource(R.string.go_back_action)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainUI(
    navController: NavController,
    cards: List<UiCard>,
    onAddNew: () -> Unit,
    onDeleteAt: (index: Int, closeSheets: () -> Unit) -> Unit,
    onEditAt: (Int) -> Unit
) {
    // índice seleccionado resiliente tras borrar
    var selected by rememberSaveable { mutableStateOf(0) }
    selected = selected.coerceIn(0, (cards.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(pageCount = { cards.size.coerceAtLeast(1) }, initialPage = selected)

    var showPicker by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }

    // EMPTY
    if (cards.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mi Billetera",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = DmSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 18.sp,
                                    color = Color(0xFF002D62)
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        GradientIcon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.back_icon),
                            contentDescription = "Cerrar",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { navController.popBackStack() }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp)
                        .background(Color.White)
                )
            },
            containerColor = Color.White
        ) { padding ->
            EmptyWalletState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 25.dp),
                onAddNew = onAddNew
            )
        }
        return
    }

    // NON-EMPTY
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.my_wallet_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = DmSans,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = Color(0xFF002D62)
                            )
                        )
                    }
                },
                navigationIcon = {
                    GradientIcon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.back_icon),
                        contentDescription = stringResource(R.string.close_action),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { navController.popBackStack() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
                    .background(Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .padding(horizontal = 25.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectedCardRow(
                card = cards[pagerState.currentPage],
                onChevronClick = { showPicker = true }
            )

            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) { page ->
                CreditCardCard(cards[page], modifier = Modifier.fillMaxWidth())
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                DotsIndicator(count = cards.size, selected = pagerState.currentPage)
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.card_information_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.trespuntos_horizontall),
                        contentDescription = stringResource(R.string.more_options_cd),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                showOptions = true
                            }
                    )
                }

                Spacer(Modifier.height(10.dp))
                InfoRow(stringResource(R.string.card_name_info_label), cards[pagerState.currentPage].holder)
                InfoRow(stringResource(R.string.expiry_date_info_label), cards[pagerState.currentPage].expiryLong())
                InfoRow(stringResource(R.string.card_number_info_label), "**** **** **** ${cards[pagerState.currentPage].last4}")
                InfoRow(stringResource(R.string.company_info_label), cards[pagerState.currentPage].brand)
                InfoRow(
                    stringResource(R.string.nickname_info_label),
                    cards[pagerState.currentPage].nickname ?: "—"
                )
            }

        }
    }

    if (showPicker) {
        CardPickerSheet(
            cards = cards,
            onSelect = { index ->
                showPicker = false
                selected = index
            },
            onAddNew = {
                showPicker = false
                onAddNew()
            },
            onDismiss = { showPicker = false }
        )
    }

    val current = cards[pagerState.currentPage]
    val brandIconRes = brandMenuIconRes(current.brand)

    CardOptionsSheet(
        visible = showOptions,
        title = "${current.brand} ${current.last4} (${current.holder})",
        iconRes = brandIconRes,
        onEdit = {
            showOptions = false
            onEditAt(pagerState.currentPage)
        },
        onDelete = {
            val idx = pagerState.currentPage
            deleteIndex = idx
            showDelete = true
        },
        onDismiss = { showOptions = false }
    )

    if (showDelete && deleteIndex != null) {
        val idx = deleteIndex!!
        val card = cards[idx]
        ConfirmDeleteSheet(
            title = "${card.brand} ${card.last4} (${card.holder})",
            brand = card.brand,
            onConfirm = {
                val justCloseSheets = {
                    showDelete = false
                    showOptions = false
                }
                onDeleteAt(idx, justCloseSheets)
            },
            onDismiss = { showDelete = false }
        )
    }
}

/* ====== MODELO UI ====== */
data class UiCard(
    val brand: String,
    val last4: String,
    val holder: String,
    val expiry: String,
    val nickname: String?,
    val pmId: String? = null
) {
    @Composable
    fun expiryLong(): String {
        val (mm, yy) = expiry.split("/")
        val months = stringArrayResource(R.array.months_full)
        val month = months.getOrNull(mm.toInt() - 1) ?: stringResource(R.string.unknown_month)
        return "$month 20$yy"
    }
}

/* ====== Helpers UI ====== */

@Composable
private fun SelectedCardRow(card: UiCard, onChevronClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val brandIconRes = brandMenuIconRes(card.brand)
            val iconTint = when (card.brand.trim().lowercase()) {
                "visa" -> Color(0xFF003098)
                "american express", "amex" -> Color(0xFF006ECD)
                else -> Color.Unspecified
            }
            Icon(
                painter = painterResource(brandIconRes),
                contentDescription = card.brand,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("${card.brand} ${card.last4} (${card.holder})")
        }
        IconButton(onClick = onChevronClick) {
            Icon(
                painter = painterResource(id = R.drawable.flecha_derecha_icon),
                contentDescription = stringResource(R.string.management_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 15.dp)
            )
        }
    }
}

@Composable
private fun CreditCardCard(card: UiCard, modifier: Modifier = Modifier) {
    val backgroundBrush = Brush.horizontalGradient(listOf(Color(0xFF2DA6FF), Color(0xFF0B55D6)))
    val brandLogo = brandLogoRes(card.brand)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(brush = backgroundBrush)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Image(
                painter = painterResource(id = brandLogo),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(160.dp)
                    .alpha(0.12f),
                contentScale = ContentScale.Fit
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chip),
                        contentDescription = stringResource(R.string.chip_cd),
                        modifier = Modifier.size(48.dp)
                    )
                    Icon(
                        painter = painterResource(id = brandLogo),
                        contentDescription = card.brand,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(60.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.card_number_masked_format, card.last4),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = DmSans,
                    letterSpacing = 2.sp
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            stringResource(R.string.card_name_label_on_card),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = DmSans
                        )
                        Text(
                            card.holder,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSans
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.expiry_date_label_on_card),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = DmSans
                        )
                        Text(
                            card.expiry,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSans
                        )
                    }
                }
            }
        }
    }
}

@Composable private fun DotsIndicator(count: Int, selected: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == selected) Color(0xFF03A9F4) else Color(0xFFE0E6EF))
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = DmSans,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = DmSans,
                fontSize = 15.sp,
            ),
            color = Color(0x80000000)
        )
        Spacer(Modifier.height(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardPickerSheet(
    cards: List<UiCard>,
    onSelect: (Int) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState, onDismissRequest = onDismiss,
        containerColor = Color.White, dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 25.dp)
        ) {
            cards.forEachIndexed { index, card ->
                CardRow(
                    iconRes = brandMenuIconRes(card.brand),
                    title = "${card.brand} ${card.last4} (${card.holder})",
                    onClick = { onSelect(index) },
                )
                HorizontalDivider(thickness = 0.6.dp, color = Color(0xFFE7EAF0))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddNew() }
                    .padding(vertical = 14.dp, horizontal = 25.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientIcon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.plus_icon),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.add_another_payment_method),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun CardRow(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 25.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardOptionsSheet(
    visible: Boolean,
    onDismiss: () -> Unit = {},
    title: String,
    @DrawableRes iconRes: Int? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 10.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(thickness = 0.6.dp, color = Color(0xFFE7EAF0))
            Spacer(Modifier.height(30.dp))
            OptionRow(text = stringResource(R.string.edit_payment_method_action)) {
                onEdit()
                onDismiss()
            }
            Divider(thickness = 0.6.dp, color = Color(0xFFE7EAF0))
            OptionRow(text = stringResource(R.string.delete_payment_method_action)) {
                onDelete()
                onDismiss()
            }
            Divider(thickness = 0.6.dp, color = Color(0xFFE7EAF0))
            Text(
                text = stringResource(R.string.cancel),
                color = Color(0xFFEB4D4D),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp, horizontal = 8.dp)
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun OptionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, fontFamily = DmSans, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteSheet(
    title: String,
    brand: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {},
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = brandMenuIconRes(brand)),
                    contentDescription = brand,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium)
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
                thickness = 0.6.dp,
                color = Color(0xFFE7EAF0)
            )

            Text(
                text = stringResource(R.string.confirm_delete_payment_method_prompt),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = DmSans, fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(22.dp))

            GradientButton(
                text = stringResource(R.string.delete_action),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = onConfirm
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.cancel),
                color = Color(0xFFDB1212),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSans),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 8.dp)
            )
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/* Empty state */
@Composable
private fun EmptyWalletState(
    modifier: Modifier = Modifier,
    onAddNew: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF2DA6FF), Color(0xFF0B55D6))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.card_icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Tu billetera está vacía",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color(0xFF002D62)
            )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Añade un método de pago para pagar más rápido y de forma segura.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = DmSans,
                fontSize = 15.sp
            ),
            color = Color(0x80000000),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(26.dp))

        GradientButton(
            text = "Añadir método de pago",
            onClick = onAddNew,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}

/* ====== Recursos (NO composables) ====== */

@DrawableRes
private fun brandMenuIconRes(brand: String): Int = when (brand.trim().lowercase()) {
    "visa" -> R.drawable.visa_card_icon
    "mastercard", "master card", "mc" -> R.drawable.mc_card_icon
    "american express", "american_express", "amex" -> R.drawable.amex_card_icon
    else -> R.drawable.card_icon
}

@DrawableRes
private fun brandLogoRes(brand: String): Int = when (brand.trim().lowercase()) {
    "visa" -> R.drawable.visa_logo
    "mastercard", "master card", "mc" -> R.drawable.logos_mastercard
    "american express", "american_express", "amex" -> R.drawable.american_express_logo
    else -> R.drawable.visa_logo
}