package com.example.parqueame.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.*
import com.example.parqueame.ui.register.components_register.RoleOptionCard
import com.example.parqueame.ui.register.viewmodels.RegisterChoiceViewModel
import com.example.parqueame.ui.register.viewmodels.Role
import com.example.parqueame.ui.theme.DmSans
import com.example.parqueame.ui.theme.PIconColor
import com.example.parqueame.ui.theme.RtlRomman
import com.example.parqueame.utils.isLight
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun RegisterChoiceScreen(
    viewModel: RegisterChoiceViewModel,
    onNavigateToIndividualForm: () -> Unit,
    onNavigateToCompanyForm: () -> Unit,
    onBackClick: () -> Unit,
    backgroundColor: Color = Color.White
) {
    val selectedRole by viewModel.selectedRole.collectAsState()
    val backInteractionSource = remember { MutableInteractionSource() }
    val buttonInteractionSource = remember { MutableInteractionSource() }

    val conductorIcon: Painter = painterResource(id = R.drawable.car_icon)
    val administradorIcon: Painter = painterResource(id = R.drawable.p_icon)

    val useDarkText = backgroundColor == Color.White || backgroundColor.isLight()
    val textColor = if (useDarkText) Color.Black else Color.White

    val mostrarError = rememberTopErrorBanner() // ✅ Nuevo estado para errores

    val whiteBackground = Color.White
    val useDarkIconsStatusBar = whiteBackground.luminance() > 0.5f
    val systemUiController = rememberSystemUiController()
    val useDarkIconsNavBar = whiteBackground.luminance() > 0.5f

    SideEffect {
        systemUiController.setStatusBarColor(
            color = whiteBackground,
            darkIcons = useDarkIconsStatusBar
        )
        systemUiController.setNavigationBarColor(
            color = whiteBackground,
            darkIcons = useDarkIconsNavBar
        )
    }

    val mustSelectMsg = stringResource(R.string.must_select_option_error)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)

    ) {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 20.dp),
        ) {

            Column (

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 70.dp, bottom = 50.dp)


            ){

                Column (

                    verticalArrangement = Arrangement.spacedBy((-12).dp)

                ) {


                    Text(
                        text = stringResource(R.string.register_question_l1),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = RtlRomman,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00A1FF), Color(0xFF003099)),
                                start = Offset(0f, 0f),
                                end = Offset(0f, 100f)
                            )
                        )
                    )

                    Text(
                        text = stringResource(R.string.register_question_l2),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = RtlRomman,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF00A1FF), Color(0xFF003099)),
                                start = Offset(0f, 0f),
                                end = Offset(0f, 100f)
                            )
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))


        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ){
            Text(
                text = stringResource(R.string.register_subtitle),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = DmSans,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            RoleOptionCard(
                icon = conductorIcon,
                title = stringResource(R.string.role_driver_title),
                description = stringResource(R.string.role_driver_desc),
                onClick = { viewModel.selectRole(Role.CONDUCTOR) },
                iconTintColor = PIconColor,
                isSelected = selectedRole == Role.CONDUCTOR,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            RoleOptionCard(
                icon = administradorIcon,
                title = stringResource(R.string.role_admin_title),
                description = stringResource(R.string.role_admin_desc),
                onClick = { viewModel.selectRole(Role.ADMINISTRADOR) },
                iconTintColor = PIconColor,
                isSelected = selectedRole == Role.ADMINISTRADOR,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(50.dp))

            GradientButton(
                text = stringResource(R.string.next_action),
                onClick = {
                    when (selectedRole) {
                        Role.CONDUCTOR -> onNavigateToIndividualForm()
                        Role.ADMINISTRADOR -> onNavigateToCompanyForm()
                        null -> mostrarError("Debe seleccionar una opción.") // ✅ Mensaje emergente
                    }
                },
                modifier = Modifier.width(200.dp),
                interactionSource = buttonInteractionSource,
                indication = null
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.back_with_arrow),
                color = textColor,
                modifier = Modifier
                    .clickable(
                        interactionSource = backInteractionSource,
                        indication = null,
                        onClick = { onBackClick() }
                    )
                    .padding(vertical = 12.dp)
            )
        }
        }
    }
}
