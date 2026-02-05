package com.example.parqueame.ui.admin.billetera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.R
import com.example.parqueame.api.RetrofitInstance
import com.example.parqueame.models.TransactionDto
import com.example.parqueame.session.SessionStore
import com.example.parqueame.ui.common_components.GradientIcon
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesAdminScreen(
    navController: NavController,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val session = remember { SessionStore(ctx) }
    val userId = session.userId.collectAsState(initial = null).value.orEmpty()

    var showCalendar by remember { mutableStateOf(false) }
    var selectedYm by remember { mutableStateOf(YearMonth.now()) }

    var items by remember { mutableStateOf<List<TransactionDto>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Descargar PDF
    val pdfName = remember(selectedYm) {
        "${ctx.getString(R.string.transactions_pdf_prefix)}_${selectedYm.year}_${selectedYm.monthValue.toString().padStart(2, '0')}.pdf"
    }
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) writePdfToUri(ctx, uri, items, selectedYm)
    }

    // Carga desde backend al cambiar usuario o mes
    LaunchedEffect(userId, selectedYm) {
        if (userId.isBlank()) return@LaunchedEffect
        loading = true
        error = null
        try {
            val start = selectedYm.atDay(1)
            val endExclusive = selectedYm.plusMonths(1).atDay(1)
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            val resp = RetrofitInstance.apiService.getWalletTransactions(
                userId = userId,
                startDate = fmt.format(start),
                endDate = fmt.format(endExclusive)
            )
            items = if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
            if (!resp.isSuccessful) error = ctx.getString(R.string.could_not_load_transactions)
        } catch (e: Exception) {
            items = emptyList()
            error = e.message ?: ctx.getString(R.string.unknown_error)
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                GradientIcon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.close_action),
                    modifier = Modifier.size(35.dp)
                )
            }
            Text(
                text = stringResource(R.string.transactions_title),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF115ED0)
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Chip de mes (misma UI) + botón de descarga
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
            ) {
                TextButton(
                    modifier = Modifier.padding(20.dp, 1.dp),
                    onClick = { showCalendar = true },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0B66FF))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientIcon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(R.string.select_date_label),
                            modifier = Modifier.size(25.dp)
                        )
                        Text(
                            text = "   ${selectedYm.asLocalizedMonthYear()}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                        )
                    }
                }
            }

            IconButton(onClick = { createDocLauncher.launch(pdfName) }) {
                GradientIcon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.download_pdf_cd),
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        Spacer(Modifier.height(30.dp))

        // Lista
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = Color.Red)
            items.isEmpty() -> Text(stringResource(R.string.no_transactions_for_month), color = Color.Gray)
            else -> {
                items.forEach { tx ->
                    TransactionCard(
                        name = tx.parkingLotName,
                        address = tx.parkingLotAddress,
                        date = tx.date.take(10),
                        price = tx.amount.toInt(),
                        onClick = {}
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    BilleteraAdminSheet(
        showCalendar = showCalendar,
        showSheet = false,
        onDismiss = { showCalendar = false },
        navController = navController,
        onConfirm = { _, _ -> },
        onDatePicked = { millis ->
            millis ?: return@BilleteraAdminSheet
            val localDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            selectedYm = YearMonth.of(localDate.year, localDate.month)
        }
    )
}

@Composable
private fun YearMonth.asLocalizedMonthYear(): String {
    val months = stringArrayResource(R.array.months_full)
    val monthName = months.getOrNull(this.monthValue - 1) ?: this.month.toString()
    return "${monthName.replaceFirstChar { it.titlecase() }} ${this.year}"
}


private fun writePdfToUri(
    context: Context,
    uri: Uri,
    rows: List<TransactionDto>,
    yearMonth: YearMonth
) {
    val doc = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36
    val contentWidth = pageWidth - margin * 2

    val titlePaint = Paint().apply { isAntiAlias = true; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val headerPaint = Paint().apply { isAntiAlias = true; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val cellPaint = Paint().apply { isAntiAlias = true; textSize = 11f }
    val grayPaint = Paint().apply { isAntiAlias = true; textSize = 10f; color = android.graphics.Color.DKGRAY }

    val col = listOf(
        (contentWidth * 0.16f).toInt(),
        (contentWidth * 0.26f).toInt(),
        (contentWidth * 0.32f).toInt(),
        (contentWidth * 0.12f).toInt(),
        (contentWidth * 0.14f).toInt()
    )

    // Getting localized strings for PDF
    val months = context.resources.getStringArray(R.array.months_full)
    val monthName = months.getOrNull(yearMonth.monthValue - 1) ?: yearMonth.month.toString()
    val localizedMonthYear = "${monthName.replaceFirstChar { it.titlecase() }} ${yearMonth.year}"
    val pdfTitle = "${context.getString(R.string.transactions_pdf_title_prefix)} $localizedMonthYear"
    val pdfSubtitle = context.getString(R.string.generated_by_parqueame)
    val headers = context.resources.getStringArray(R.array.pdf_transaction_headers)


    fun drawPage(n: Int, start: Int): Int {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, n).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        var x = margin.toFloat()
        var y = (margin + 20).toFloat()

        canvas.drawText(pdfTitle, x, y, titlePaint)
        y += 10f
        canvas.drawText(pdfSubtitle, x, y + 14f, grayPaint)
        y += 28f

        var cx = x
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, cx, y, headerPaint); cx += col[i]
        }
        y += 16f

        val nf = NumberFormat.getCurrencyInstance(Locale("es", "DO"))
            .apply { currency = java.util.Currency.getInstance("DOP") }

        var idx = start
        while (idx < rows.size) {
            if (y > pageHeight - margin - 24) break
            val t = rows[idx]
            cx = x
            canvas.drawText(t.date.take(10), cx, y, cellPaint); cx += col[0]
            drawEllipsized(canvas, t.parkingLotName.orEmpty(), cx, y, col[1], cellPaint); cx += col[1]
            drawEllipsized(canvas, t.parkingLotAddress.orEmpty(), cx, y, col[2], cellPaint); cx += col[2]
            canvas.drawText(nf.format(t.amount), cx, y, cellPaint); cx += col[3]
            canvas.drawText(t.status, cx, y, cellPaint)
            y += 16f
            idx++
        }

        doc.finishPage(page)
        return idx
    }

    var printed = 0; var pageNo = 1
    while (printed < rows.size || (rows.isEmpty() && pageNo == 1)) {
        printed = drawPage(pageNo, printed); pageNo++
        if (rows.isEmpty()) break
    }

    context.contentResolver.openOutputStream(uri)?.use { os -> doc.writeTo(os) }
    doc.close()
}

private fun drawEllipsized(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Int,
    paint: Paint
) {
    val ellipsis = "…"
    if (paint.measureText(text) <= width) { canvas.drawText(text, x, y, paint); return }
    var end = text.length
    while (end > 0 && paint.measureText(text, 0, end) > width) end--
    while (end > 0 && paint.measureText(text, 0, end) + paint.measureText(ellipsis) > width) end--
    canvas.drawText(text.substring(0, end) + ellipsis, x, y, paint)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransaccionesAdminScreenPreview() {
    TransaccionesAdminScreen(
        navController = rememberNavController(),
        onClose = {}
    )
}