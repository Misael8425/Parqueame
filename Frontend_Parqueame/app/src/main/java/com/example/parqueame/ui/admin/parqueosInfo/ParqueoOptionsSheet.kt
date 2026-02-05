package com.example.parqueame.ui.admin.parqueosInfo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.common_components.rememberTopSuccessBanner
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParqueoOptionsSheet(
    showTime: Boolean,
    showSheet: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    navController: NavController
) {

    val context = LocalContext.current
    val showMessage = rememberTopSuccessBanner()
    var showDisableConfirm by remember { mutableStateOf(false) }

    if (showDisableConfirm) {
        ModalBottomSheet(
            onDismissRequest = { showDisableConfirm = false },
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
                Text(
                    text = buildAnnotatedString {

                        withStyle(style = SpanStyle(color = Color.Black)) {
                            append(stringResource(R.string.disable_confirmation_formatted))
                        }
                        withStyle(style = SpanStyle(color = Color(0xFF115ED0))) {
                            append(currentName)
                        }
                        withStyle(style = SpanStyle(color = Color.Black)) {
                            append("?")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GradientButton(
                        text = stringResource(R.string.disable_action),
                        onClick = {
                            showDisableConfirm = false
                            onDismiss()
                            showMessage(context.getString(R.string.parking_disabled_success))
                        },
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(200.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.cancel),
                    color = Color.Black,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { showDisableConfirm = false }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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
                    .padding(horizontal = 16.dp)
            ) {
                SheetOption(text = stringResource(R.string.edit_parking_action)) {
                    onDismiss()
                    navController.navigate(Screen.SolicitudEditarParqueoScreen.route)
                }

                Divider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                SheetOption(text = stringResource(R.string.disable_parking_action)) {
                    showDisableConfirm = true
                }

                Divider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                SheetOption(text = stringResource(R.string.cancel), isCancel = true) {
                    onDismiss()
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showTime) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedules_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                )

                Divider(
                    color = Color.Gray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                SheetOption(text = stringResource(R.string.close_action), isCancel = true) { onDismiss() }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SheetOption(
    text: String,
    isCancel: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (isCancel) Color.Red else Color.Black,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize
        )
    }
}