package com.example.parqueame.ui.admin.billetera

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.admin.parqueosInfo.SheetOption
import com.example.parqueame.ui.common_components.LabelledTextField
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BilleteraAdminSheet(
    showCalendar: Boolean = false,
    showSheet: Boolean = false,
    onDismiss: () -> Unit,
    navController: NavController,
    onConfirm: (String, String) -> Unit,
    onDatePicked: (Long?) -> Unit = {}
) {
    var showForm by remember { mutableStateOf(false) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SheetOption(text = stringResource(R.string.edit_bank_account_action)) { showForm = true }
                Divider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SheetOption(text = stringResource(R.string.cancel), isCancel = true) { onDismiss() }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showForm) {
        ModalBottomSheet(
            onDismissRequest = { showForm = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            containerColor = Color.White
        ) {
            var numeroCuenta by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                LabelledTextField(
                    stringResource(R.string.account_number_label),
                    numeroCuenta,
                    { numeroCuenta = it },
                    stringResource(R.string.account_number_label),
                    KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(16.dp))
                LabelledTextField(
                    stringResource(R.string.enter_your_password_label),
                    password,
                    { password = it },
                    stringResource(R.string.enter_your_password_label),
                    KeyboardType.Text
                )
                Spacer(modifier = Modifier.height(24.dp))
                Divider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Color.Red) }
                    TextButton(onClick = {
                        if (numeroCuenta.isNotBlank() && password.isNotBlank()) {
                            onConfirm(numeroCuenta, password)
                            onDismiss()
                        }
                    }) { Text(stringResource(R.string.edit_bank_account_action), color = Color(0xFF115ED0)) }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCalendar) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val pickerState = rememberDatePickerState()
                val selectedDateMillis by remember { derivedStateOf { pickerState.selectedDateMillis } }

                val formatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                val selectedDateText = remember(selectedDateMillis) {
                    selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
                    } ?: "..."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF115ED0))
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.select_date_label),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_action),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedDateText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

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
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White),
                            colors = DatePickerDefaults.colors(containerColor = Color.White)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 24.dp, bottom = 16.dp)
                        ) {
                            TextButton(onClick = { onDismiss() }) {
                                Text(stringResource(R.string.cancel_caps), color = Color(0xFF115ED0))
                            }
                            TextButton(onClick = {
                                onDatePicked(selectedDateMillis)
                                onDismiss()
                            }) {
                                Text(stringResource(R.string.ok_caps), color = Color(0xFF115ED0))
                            }
                        }
                    }
                }
            }
        }
    }
}