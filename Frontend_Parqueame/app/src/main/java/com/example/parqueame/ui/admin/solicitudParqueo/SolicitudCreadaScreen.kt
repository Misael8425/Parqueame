package com.example.parqueame.ui.admin.solicitudParqueo

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.ui.common_components.GradientButton
import com.example.parqueame.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.example.parqueame.R

@Composable
fun SolicitudCreadaScreen(
    navController: NavController,
    onGoHome: () -> Unit = {
        navController.navigate(Screen.HomeCompany.route) {
            popUpTo(0)
            launchSingleTop = true
        }
    }
) {
    val brandBlue = Color(0xFF2563EB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onGoHome) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.close_cd),
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de success
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.success_cd),
                tint = brandBlue,
                modifier = Modifier.size(96.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.request_created_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.request_created_message),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GradientButton(
                    text = stringResource(R.string.go_home),
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SolicitudCreadaScreenPreview() {
    val navController = rememberNavController()
    SolicitudCreadaScreen(
        navController = navController,
        onGoHome = {}
    )
}