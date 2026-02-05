
// BilleteraCalendarSheet.kt (o al final de BilleteraAdminSheet.kt)
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.parqueame.ui.common_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.parqueame.R
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.time.ZoneOffset


@Composable
fun CalendarSheet(
    show: Boolean,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    if (!show) return

    val utc = ZoneOffset.UTC
    val zoneId = ZoneId.systemDefault()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(utc).toInstant().toEpochMilli()
    )
    val selectedDateMillis by remember { derivedStateOf { pickerState.selectedDateMillis } }

    val headerFmt = remember {
        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    }
    val selectedDateText = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            Instant.ofEpochMilli(it).atZone(utc).toLocalDate().format(headerFmt)
        } ?: "..."
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ======= Encabezado azul (igual al de BilleteraAdminSheet) =======
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF115ED0))
                    .padding(24.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.select_date_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_action),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = selectedDateText,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ======= Calendar + acciones (idéntico estilo) =======
            Surface(color = Color.White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    DatePicker(
                        state = pickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = DatePickerDefaults.colors(containerColor = Color.White)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 24.dp, bottom = 16.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel_caps), color = Color(0xFF115ED0))
                        }
                        TextButton(onClick = {
                            val millis = pickerState.selectedDateMillis
                            if (millis != null) {
                                onConfirm(Instant.ofEpochMilli(millis).atZone(utc).toLocalDate())
                            } else onDismiss()
                        })  {
                            Text(stringResource(R.string.ok_caps), color = Color(0xFF115ED0))
                        }
                    }
                }
            }
        }
    }
}
