package com.example.parqueame.ui.admin.solicitudParqueo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.common_components.GradientIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocInfraestructuraSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit
) {
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
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column (
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.infrastructure_report_title),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = Color(0xFF115ED0))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.infrastructure_report_description),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal, color = Color.Black)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    GradientIcon(
                        imageVector = Icons.Default.Description,
                        contentDescription = stringResource(R.string.infrastructure_report_icon_cd),
                        modifier = Modifier
                            .size(90.dp)
                    )
                }
                GradientButton(
                    text = stringResource(R.string.close_action),
                    onClick = { onDismiss() },
                    modifier = Modifier
                        .width(220.dp)
                        .padding(vertical = 24.dp),
                    enabled = true
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}