@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.parqueame.ui.common_components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.parqueame.R
import com.example.parqueame.ui.LocalAppLanguage
import androidx.compose.ui.Alignment

@Composable
fun LanguageOptionsSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    onLanguageChange: (String) -> Unit
) {
    if (!showSheet) return

    val currentLanguage = LocalAppLanguage.current
    val isEs = currentLanguage.startsWith("es")
    val isEn = currentLanguage.startsWith("en")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.language_sheet_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE7EAF0))
            Spacer(Modifier.height(8.dp))

            SheetActionRow(
                title = stringResource(R.string.language_spanish),
                selected = isEs,
                onClick = {
                    onLanguageChange("es")
                    onDismiss()
                }
            )
            HorizontalDivider(color = Color(0xFFE7EAF0))

            SheetActionRow(
                title = stringResource(R.string.language_english),
                selected = isEn,
                onClick = {
                    onLanguageChange("en")
                    onDismiss()
                }
            )
            HorizontalDivider(color = Color(0xFFE7EAF0))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            ) {
                Text(stringResource(R.string.cancel), color = Color(0xFFDC2626))
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun SheetActionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF111827)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color(0xFF10B981)
            )
        }
    }
}