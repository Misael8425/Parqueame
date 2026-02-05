package com.example.parqueame.ui.admin.solicitudParqueo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.parqueame.R
import com.example.parqueame.ui.admin.DaysOTWeek

@Composable
fun DaysOfWeekDialog(
    initial: Set<DaysOTWeek>,
    onDismiss: () -> Unit,
    onConfirm: (Set<DaysOTWeek>) -> Unit
) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected) },
                enabled = selected.isNotEmpty()
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.button_cancel)) } },
        title = { Text(stringResource(R.string.dialog_days_of_week_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.dialog_days_of_week_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                DaysOTWeek.entries.forEach { day ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (selected.contains(day)) selected - day else selected + day
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = selected.contains(day),
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + day else selected - day
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF2563EB),
                                uncheckedColor = Color.Gray,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(day.labelRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (selected.isEmpty()) {
                    Text(
                        stringResource(R.string.dialog_days_of_week_error),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    )
}