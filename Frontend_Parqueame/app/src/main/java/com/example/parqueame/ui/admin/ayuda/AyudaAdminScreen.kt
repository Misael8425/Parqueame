package com.example.parqueame.ui.admin.ayuda

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.GradientIcon

@Composable
fun AyudaAdminScreen(
    navController: NavController
){
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {navController.popBackStack()}) {
                    GradientIcon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.close_content_description),
                        modifier = Modifier
                            .size(28.dp)
                    )
                }
                Text(
                    stringResource(R.string.faq_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF003099))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                stringResource(R.string.all_topics),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF003099),
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            val faqs = listOf(
                FaqItem(
                    stringResource(R.string.faq_q1),
                    stringResource(R.string.faq_a1)
                ),
                FaqItem(
                    stringResource(R.string.faq_q2),
                    stringResource(R.string.faq_a2)
                ),
                FaqItem(
                    stringResource(R.string.faq_q3),
                    stringResource(R.string.faq_a3)
                ),
                FaqItem(
                    stringResource(R.string.faq_q4),
                    stringResource(R.string.faq_a4)
                ),
                FaqItem(
                    stringResource(R.string.faq_q5),
                    stringResource(R.string.faq_a5)
                ),
                FaqItem(
                    stringResource(R.string.faq_q6),
                    stringResource(R.string.faq_a6)
                ),
                FaqItem(
                    stringResource(R.string.faq_q7),
                    stringResource(R.string.faq_a7)
                ),
                FaqItem(
                    stringResource(R.string.faq_q8),
                    stringResource(R.string.faq_a8)
                ),
                FaqItem(
                    stringResource(R.string.faq_q9),
                    stringResource(R.string.faq_a9)
                )
            )

            faqs.forEach { item ->
                FaqRow(item = item, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AyudaAdminScreenPreview(){ // Renamed for clarity in previews
    AyudaAdminScreen(
        navController = rememberNavController()
    )
}