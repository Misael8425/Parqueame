package com.example.parqueame.ui.common_components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.parqueame.R
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.dmSans
import com.yalantis.ucrop.UCrop
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilOptionsHandler(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    onImageChange: (Uri?) -> Unit,
    onEditName: (String) -> Unit,
    currentName: String
) {
    val context = LocalContext.current
    val showError = rememberTopErrorBanner()
    val showMessage = rememberTopSuccessBanner()

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                val resultUri = UCrop.getOutput(result.data ?: return@rememberLauncherForActivityResult)
                resultUri?.let {
                    onImageChange(it)
                    showMessage(context.getString(R.string.image_cropped_success))
                }
            }
            UCrop.RESULT_ERROR -> {
                val cropError = UCrop.getError(result.data ?: return@rememberLauncherForActivityResult)
                val errText = cropError?.message ?: "-"
                showError(context.getString(R.string.image_crop_error, errText))
            }
            else -> {
                Toast.makeText(context, context.getString(R.string.image_crop_canceled), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val fileName = "perfil_crop_${System.currentTimeMillis()}.jpg"
                val destinationFile = File(context.cacheDir, fileName)
                val destinationUri = Uri.fromFile(destinationFile)

                val options = UCrop.Options().apply {
                    setCircleDimmedLayer(true)
                    setShowCropFrame(false)
                    setShowCropGrid(false)
                    setDimmedLayerColor(0x99000000.toInt())
                    setToolbarTitle(context.getString(R.string.crop_toolbar_title))
                    setStatusBarColor(0xFF2196F3.toInt())
                    setToolbarColor(0xFF2196F3.toInt())
                    setToolbarWidgetColor(0xFFFFFFFF.toInt())
                    setActiveControlsWidgetColor(0xFFFFFFFF.toInt())
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(false)
                    setCompressionQuality(90)
                    setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
                }

                val uCrop = UCrop.of(uri, destinationUri)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(600, 600)
                    .withOptions(options)

                val uCropIntent = uCrop.getIntent(context).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }

                cropLauncher.launch(uCropIntent)

            } catch (e: Exception) {
                e.printStackTrace()
                showError(
                    context.getString(
                        R.string.image_process_error,
                        (e.localizedMessage ?: "-")
                    )
                )
            }
        } else {
            showError(context.getString(R.string.image_not_selected))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(context, context.getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
            pickLauncher.launch("image/*")
        } else {
            Toast.makeText(context, context.getString(R.string.permissions_required_images), Toast.LENGTH_LONG).show()
        }
    }

    fun checkAndRequestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) {
            pickLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    var showEditSheet by remember { mutableStateOf(false) }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color.White
        ) {
            EditarNombreBottomSheet(
                initialName = currentName,
                onDismiss = { showEditSheet = false },
                onConfirm = { nuevoNombre -> onEditName(nuevoNombre) }
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = Color.White
        ) {
            Column(Modifier.fillMaxWidth()) {
                SheetOption(text = stringResource(R.string.edit_profile_photo_action)) {
                    onDismiss()
                    checkAndRequestPermissions()
                }
                SheetOption(text = stringResource(R.string.remove_profile_photo_action)) {
                    onImageChange(null)
                    showMessage(context.getString(R.string.profile_photo_removed))
                    onDismiss()
                }
                SheetOption(text = stringResource(R.string.edit_name_action)) {
                    onDismiss()
                    showEditSheet = true
                }
                SheetOption(text = stringResource(R.string.change_password_action)) {
                    onDismiss()
                    navController.navigate("recover_password")
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                SheetOption(text = stringResource(R.string.cancel), isCancel = true) {
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun EditarNombreBottomSheet(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nombre by remember { mutableStateOf(TextFieldValue(initialName)) }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.prompt_new_name),
                fontSize = 16.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F1F1),
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    singleLine = true,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = Color.Red, fontSize = 14.sp)
                }
                TextButton(
                    onClick = {
                        val newName = nombre.text.trim()
                        if (newName.isNotBlank()) {
                            onConfirm(newName)
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(R.string.change_name_button), color = Color(0xFF115ED0), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SheetOption(
    text: String,
    modifier: Modifier = Modifier,
    isCancel: Boolean = false,
    onClick: () -> Unit = {}
)
{
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null // ✅ Quitar sombra y efecto
            ) {
                onClick()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = if (isCancel) Color.Red else Color.Black,
            fontSize = 16.sp
        )
    }
}

@Composable
fun CuentaOpcionCard(
    label: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    extraText: String? = null,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .height(60.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null // ✅ Quitar sombra y efecto visual
            ) {
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier
                        .size(26.dp)
                        .padding(start = 8.dp)
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                        },
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = dmSans,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            if (extraText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = extraText,
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_action),
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

