package com.example.parqueame.ui.admin.solicitudParqueo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.ui.theme.dmSans
import androidx.compose.ui.res.stringResource
import com.example.parqueame.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSection(
    schedules: SnapshotStateList<SolicitudParqueoState>,
    hours: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.schedule_section_title),
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp, bottom = 10.dp)
            )
            Row(
                modifier = Modifier
                    .clickable { schedules += SolicitudParqueoState(start = hours.first(), end = hours.last()) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF2563EB))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.schedule_add_new), color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
            }
        }

        schedules.forEach { schedule ->
            key(schedule.id) {
                var showDays by remember(schedule.id) { mutableStateOf(false) }
                var openStart by remember(schedule.id) { mutableStateOf(false) }
                var openEnd by remember(schedule.id) { mutableStateOf(false) }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                ) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.schedule_delete),
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    schedules.removeAll { it.id == schedule.id }
                                    if (schedules.isEmpty())
                                        schedules += SolicitudParqueoState(start = hours.first(), end = hours.last())
                                }
                                .padding(start = 14.dp, top = 8.dp)
                        )
                        Text(stringResource(R.string.schedule_select_days), color = Color(0xFF2563EB), fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showDays = true })
                    }

                    Spacer(Modifier.height(8.dp))

                    if (schedule.days.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            schedule.days.sortedBy { it.order }.joinToString(" • ") { it.short },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 14.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                        // Inicio
                        Column(Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.schedule_start_time_label),
                                fontFamily = dmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 14.dp))
                            ExposedDropdownMenuBox(
                                modifier = Modifier.clip(RoundedCornerShape(18.dp)),
                                expanded = openStart,
                                onExpandedChange = { openStart = !openStart }
                            ) {
                                TextField(
                                    value = schedule.start, onValueChange = {}, readOnly = true,
                                    trailingIcon = {
                                        CompositionLocalProvider(LocalContentColor provides Color(0xFF2563EB)) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = openStart)
                                        }
                                    },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF5F5F5),
                                        unfocusedContainerColor = Color(0xFFF5F5F5),
                                        disabledContainerColor = Color(0xFFF5F5F5),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                                ExposedDropdownMenu(
                                    containerColor = Color.White,
                                    expanded = openStart,
                                    onDismissRequest = { openStart = false }) {
                                    hours.forEach { h ->
                                        DropdownMenuItem(text = { Text(h) }, onClick = {
                                            val i = schedules.indexOfFirst { it.id == schedule.id }
                                            if (i >= 0) schedules[i] = schedules[i].copy(start = h)
                                            openStart = false
                                        })
                                    }
                                }
                            }
                        }
                        // Fin
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.schedule_end_time_label),
                                fontFamily = dmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 14.dp))
                            ExposedDropdownMenuBox(
                                modifier = Modifier.clip(RoundedCornerShape(18.dp)),
                                expanded = openEnd,
                                onExpandedChange = { openEnd = !openEnd }
                            ) {
                                TextField(
                                    value = schedule.end, onValueChange = {}, readOnly = true,
                                    trailingIcon = {
                                        CompositionLocalProvider(LocalContentColor provides Color(0xFF2563EB)) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = openEnd)
                                        }
                                    },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF5F5F5),
                                        unfocusedContainerColor = Color(0xFFF5F5F5),
                                        disabledContainerColor = Color(0xFFF5F5F5),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                                ExposedDropdownMenu(
                                    containerColor = Color.White,
                                    expanded = openEnd,
                                    onDismissRequest = { openEnd = false }) {
                                    hours.forEach { h ->
                                        DropdownMenuItem(text = { Text(h) }, onClick = {
                                            val i = schedules.indexOfFirst { it.id == schedule.id }
                                            if (i >= 0) schedules[i] = schedules[i].copy(end = h)
                                            openEnd = false
                                        })
                                    }
                                }
                            }
                        }
                    }

                    if (showDays) {
                        DaysOfWeekDialog(
                            initial = schedule.days,
                            onDismiss = { showDays = false },
                            onConfirm = { chosen ->
                                val i = schedules.indexOfFirst { it.id == schedule.id }
                                if (i >= 0) schedules[i] = schedules[i].copy(days = chosen)
                                showDays = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}