// HomeCompanyScreen.kt
package com.example.parqueame.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.parqueame.R
import com.example.parqueame.ui.common_components.BottomNavBar
import com.example.parqueame.ui.common_components.BottomNavItem
import com.example.parqueame.ui.navigation.BottomNavDestination
import com.example.parqueame.ui.navigation.Screen
import com.example.parqueame.ui.theme.GradientBrush
import com.example.parqueame.ui.theme.RtlRomman
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.example.parqueame.ui.theme.DmSans
import com.example.parqueame.ui.theme.dmSans

// Toma la ruta real desde tu NavGraph (cuentacompany)
private val HOME_ROUTE: String = Screen.HomeCompany.route

@Composable
fun HomeCompanyScreen(navController: NavController) { // ✅ sin valor por defecto (evita overloads)
    var currentRoute by remember { mutableStateOf(HOME_ROUTE) }

    // Lee el argumento de pestaña cuando te naveguen con "?tab=inicio|parques"
    val backStackEntry by navController.currentBackStackEntryAsState()
    val requestedTab = backStackEntry?.arguments?.getString("tab")
    LaunchedEffect(requestedTab) {
        if (requestedTab == "inicio" || requestedTab == "parques") {
            currentRoute = requestedTab
        }
    }

    val navItems = BottomNavDestination.values().toList()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BottomNavBar(
                items = navItems.map { BottomNavItem(it.route, it.icon, it.label) },
                currentRoute = currentRoute,
                onItemClick = { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { padding ->
        when (currentRoute) {
            "inicio"  -> DashboardContent(Modifier.padding(padding))

            // 🔧 FIX: navegar a "admin_parqueos" una sola vez, fuera de la composición
            "parques" -> {
                LaunchedEffect(currentRoute) {
                    navController.navigate(Screen.AdminParqueosScreen.route) {
                        launchSingleTop = true
                    }
                }
                // Contenido mínimo mientras dispara la navegación (evita recomposición infinita)
                Spacer(Modifier.height(1.dp))
            }

            else      -> DashboardContent(Modifier.padding(padding))
        }
    }
}

// ---------- INICIO (Dashboard) ----------
@Composable
private fun DashboardContent(modifier: Modifier = Modifier) {
    // tabs desde recursos
    val tabs = listOf(
        stringResource(R.string.tab_weekly),
        stringResource(R.string.tab_monthly),
        stringResource(R.string.tab_yearly)
    )
    var selectedTab by remember { mutableIntStateOf(1) } // Mensual por defecto

    // meses y top label desde recursos
    val monthlyLabels = stringArrayResource(R.array.months_short_demo).toList()
    val topLabel = stringResource(R.string.top_label_one_k)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Encabezado — mismo diseño que HomeConductorScreen
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.nav_item_home), // "Inicio"
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = RtlRomman,
                ),
                modifier = Modifier
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = GradientBrush, blendMode = BlendMode.SrcIn)
                    },
                color = Color.Unspecified
            )
            IconButton(onClick = { /* TODO: exportar */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.cloud_download),
                    contentDescription = stringResource(R.string.download_action),
                    tint = Color(0xFF1D4ED8)
                )
            }
        }

        Text(
            stringResource(R.string.dashboard_label),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelLarge.copy(color = Color(0xFF002D62)),
            fontFamily = DmSans,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(10.dp))
        BranchSelectorCard()
        Spacer(Modifier.height(8.dp))

        TabsRow(tabs = tabs, selected = selectedTab, onSelect = { i -> selectedTab = i })

        Spacer(Modifier.height(8.dp))

        // Datos demo
        val monthlyValues = listOf(520f, 810f, 560f, 480f, 610f, 500f)

        LineChartCard(
            values = monthlyValues,
            labels = monthlyLabels,
            maxValueSteps = 5,
            topLabel = topLabel,
            height = 180.dp
        )

        Spacer(Modifier.height(8.dp))

        MetricsGrid(
            itemsTop = listOf(
                MetricData("3,456", stringResource(R.string.metric_total_customers), "+2.5%", trendUp = true),
                MetricData("1,029", stringResource(R.string.metric_total_cancellations), "0%", trendUp = false, neutral = true)
            ),
            itemsBottom = listOf(
                MetricData("$30,980", stringResource(R.string.metric_revenue), "-2.5%", trendUp = false, highlight = true),
                MetricData("230", stringResource(R.string.metric_new_customers), "-5%", trendUp = false)
            )
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BranchSelectorCard() {
    var expanded by remember { mutableStateOf(false) }
    val defaultBranch = stringResource(R.string.branch_downtown_center)
    var selected by remember { mutableStateOf(defaultBranch) }
    val location = stringResource(R.string.branch_location_sample)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = selected,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = dmSans,
                        fontSize = 18.sp
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = dmSans,
                        fontSize = 16.sp
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFEFF2FF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        "4.5",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF111827),
                        fontFamily = dmSans,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = Color(0xFF6B7280)
                    )
                }
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf(
                    stringResource(R.string.branch_downtown_center),
                    stringResource(R.string.branch_plaza_central),
                    stringResource(R.string.branch_bluemall)
                ).forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name, fontFamily = dmSans, fontSize = 14.sp) },
                        onClick = { selected = name; expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabsRow(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = index == selected
            val bg = if (isSelected) Color(0xFFEFF2FF) else Color.Transparent
            val txt = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF6B7280)
            Box(
                modifier = Modifier
                    .background(bg, RoundedCornerShape(30.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(label, color = txt, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, fontFamily = dmSans
                , fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun LineChartCard(
    values: List<Float>,
    labels: List<String>,
    maxValueSteps: Int,
    topLabel: String,
    height: Dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(height + 60.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(topLabel, color = Color(0xFF9CA3AF), fontSize = 16.sp, fontFamily = dmSans)
            Spacer(Modifier.height(4.dp))

            val max = (values.maxOrNull() ?: 1f)
            val min = (values.minOrNull() ?: 0f)
            val range = (max - min).coerceAtLeast(1f)
            val gridColor = Color(0xFFE5E7EB)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val w = size.width
                val h = size.height

                val stepY = h / maxValueSteps
                repeat(maxValueSteps + 1) { i ->
                    val y = h - i * stepY
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                if (values.size > 1) {
                    val dx = w / (values.size - 1)
                    val points = values.mapIndexed { idx, v ->
                        val norm = (v - min) / range
                        Offset(idx * dx, h - norm * h)
                    }

                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }

                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val last = points.last()
                    drawCircle(color = Color.White, radius = 6f, center = last)
                    drawCircle(color = Color(0xFF3B82F6), radius = 4f, center = last)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { lab ->
                    Text(lab, color = Color(0xFF9CA3AF), fontSize = 12.sp, fontFamily = dmSans, )
                }
            }
        }
    }
}

data class MetricData(
    val value: String,
    val label: String,
    val deltaText: String,
    val trendUp: Boolean,
    val highlight: Boolean = false,
    val neutral: Boolean = false
)

@Composable
private fun MetricsGrid(itemsTop: List<MetricData>, itemsBottom: List<MetricData>) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsTop.forEach { item -> MetricCard(item, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsBottom.forEach { item -> MetricCard(item, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun MetricCard(data: MetricData, modifier: Modifier = Modifier) {
    val gradient = if (data.highlight)
        Brush.verticalGradient(listOf(Color(0xFF0C56EA), Color(0xFF0AA0FF)))
    else null

    val contentColor = if (data.highlight) Color.White else Color(0xFF0F172A)
    val subColor = if (data.highlight) Color.White.copy(alpha = 0.9f) else Color(0xFF6B7280)
    val deltaColor = when {
        data.neutral -> if (data.highlight) Color.White else Color(0xFF6B7280)
        data.trendUp -> if (data.highlight) Color.White else Color(0xFF10B981)
        else -> if (data.highlight) Color.White else Color(0xFFEF4444)
    }

    Card(
        modifier = modifier.height(92.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = if (gradient == null)
            CardDefaults.cardColors(containerColor = Color(0xFFF6F7FB))
        else
            CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (gradient != null) Modifier.background(gradient) else Modifier)
                .padding(14.dp)
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = data.value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontFamily = dmSans,
                        fontSize = 22.sp
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.label,
                        style = MaterialTheme.typography.bodySmall.copy(color = subColor),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = dmSans,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.deltaText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = deltaColor,
                            fontFamily = dmSans,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}
