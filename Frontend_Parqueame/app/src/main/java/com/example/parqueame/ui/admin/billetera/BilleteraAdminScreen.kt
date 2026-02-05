package com.example.parqueame.ui.admin.billetera

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.TransactionDto
import com.example.parqueame.models.UpdateBankAccountRequest
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.theme.GradientEnd
import com.example.parqueame.ui.theme.GradientStart
import kotlinx.coroutines.launch

@Composable
fun BilleteraAdminScreen(
    navController: NavController,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val session = remember { SessionStore(ctx) }
    val userId = session.userId.collectAsState(initial = null).value.orEmpty()

    var showSheet by remember { mutableStateOf(false) }
    var total by remember { mutableStateOf<Double?>(null) }
    var recent by remember { mutableStateOf<List<TransactionDto>>(emptyList()) }
    var greetingName by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId.isBlank()) return@LaunchedEffect
        loading = true
        try {
            val summaryResp = RetrofitInstance.apiService.getWalletSummary(userId)
            if (summaryResp.isSuccessful) {
                val body = summaryResp.body()
                total = body?.totalIncome
                recent = body?.recentTransactions ?: emptyList()
            } else {
                total = null
                recent = emptyList()
            }

            val perfilResp = RetrofitInstance.apiService.obtenerPerfilPorId(userId)
            if (perfilResp.isSuccessful) {
                greetingName = perfilResp.body()?.nombre
            } else {
                greetingName = null
            }
        } catch (_: Exception) {
            total = null
            recent = emptyList()
            greetingName = null
        } finally {
            loading = false
        }
    }

    BilleteraAdminSheet(
        showSheet = showSheet,
        onDismiss = { showSheet = false },
        navController = navController,
        onConfirm = { numeroCuenta, password ->
            if (userId.isBlank()) {
                Toast.makeText(ctx, R.string.login_again_toast, Toast.LENGTH_SHORT).show()
                return@BilleteraAdminSheet
            }
            scope.launch {
                try {
                    val req = UpdateBankAccountRequest(accountNumber = numeroCuenta, password = password)
                    val r = RetrofitInstance.apiService.updateBankAccount(userId, req)
                    if (r.isSuccessful) {
                        Toast.makeText(ctx, R.string.bank_account_updated_toast, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, R.string.bank_account_update_failed_toast, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    val errorMsg = "${ctx.getString(R.string.error_prefix_toast)} ${e.message}"
                    Toast.makeText(ctx, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(brush = Brush.verticalGradient(colors = listOf(GradientStart, GradientEnd)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.close_button_cd),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.my_wallet_title),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )
                }
                Spacer(Modifier.height(28.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(start = 16.dp, top = 24.dp, end = 16.dp)
                    ) {
                        Text(
                            text = greetingName?.let { stringResource(R.string.greeting_user, it) } ?: stringResource(R.string.greeting_default),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.5f)
                            ),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.total_income),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (loading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = total?.let { stringResource(R.string.currency_format_dop, it) } ?: "—",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.width(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.more_options_cd),
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.latest_transactions),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = stringResource(R.string.view_all),
                    color = Color(0xFF115ED0),
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.TransaccionesAdminScreen.route)
                    }
                )
            }
            Spacer(Modifier.height(16.dp))

            if (recent.isNotEmpty()) {
                val t = recent.first()
                TransactionCard(
                    name = t.parkingLotName,
                    address = t.parkingLotAddress,
                    date = t.date.take(10),
                    price = t.amount.toInt(),
                    onClick = {}
                )
            } else {
                Text(
                    text = stringResource(R.string.no_recent_transactions),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TransactionCard(
    name: String,
    address: String,
    date: String,
    price: Int?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val priceText = price?.let { stringResource(R.string.transaction_amount_format, it) } ?: "—"
                Text(
                    priceText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF115ED0),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    date,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BilleteraAdminScreenPreview() {
    BilleteraAdminScreen(
        navController = rememberNavController(),
        onClose = {}
    )
}